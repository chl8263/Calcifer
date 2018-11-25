package com.example.gyun_home.calcifer

import ai.api.AIConfiguration
import ai.api.AIDataService
import ai.api.model.AIRequest
import ai.api.model.Result
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import com.example.gyun_home.calcifer.adapter.RecyclerViewAdapter
import com.example.gyun_home.calcifer.model.MessageDTO
import com.example.gyun_home.calcifer.model.WeatherDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var dateFormatFromString = SimpleDateFormat("yyyy-MM-dd")

    var messageDTOs = ArrayList<MessageDTO>()
    var aiDataService: AIDataService? = null

    var weatherDateFormatFromString = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    var weatherDateFormatToString = SimpleDateFormat("MM월 dd일 hh시")

    val initChatBotText : String = "시간표 와 날씨, 음식 메뉴를 알려주는 챗봇 입니다."
    val initChatBotHello : String = "반갑다고 인사해주세요!"
    val initChatBotSchedule : String = "시간표 예시 : 월요일 수업좀 알려줘"
    val initChatBotWeather : String = "날씨 예시 : 서울 날씨는?"
    val initChatBotMenu : String = "음식 메뉴 예시 : 내일 점심은?"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView.adapter = RecyclerViewAdapter(messageDTOs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        var config = AIConfiguration("5fd84bf2f6074a85b4c781c55eb6b381", AIConfiguration.SupportedLanguages.Korean)
        aiDataService = AIDataService(config)


        messageDTOs.add(MessageDTO(false, initChatBotText))
        messageDTOs.add(MessageDTO(false, initChatBotHello))
        messageDTOs.add(MessageDTO(false, initChatBotSchedule))
        messageDTOs.add(MessageDTO(false, initChatBotWeather))
        messageDTOs.add(MessageDTO(false, initChatBotMenu))
        recyclerView.adapter.notifyDataSetChanged()
        recyclerView.smoothScrollToPosition(messageDTOs.size - 1)

        button.setOnClickListener {
            if (!TextUtils.isEmpty(editText.text)) {
                messageDTOs.add(MessageDTO(true, editText.text.toString()))
                recyclerView.adapter.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(messageDTOs.size - 1)
                TalkAsyncTask().execute(editText.text.toString())
                editText.setText("")
            }
        }
    }

    inner class TalkAsyncTask : AsyncTask<String, Void, Result>() {
        override fun doInBackground(vararg p0: String?): Result? {
            var aiRequest = AIRequest()
            aiRequest.setQuery(p0[0])
            return aiDataService?.request(aiRequest)?.result
        }

        override fun onPostExecute(result: Result?) {
            super.onPostExecute(result)
            if (result != null) {
                makeMessage(result)
            }
        }
    }

    fun makeMessage(result: Result) {
        when (result.metadata.intentName) {

            "Weather" -> {
                var city = result.parameters["geo-city"]
                if (city == null) {
                    weatherMessage("서울특별시")
                } else {
                    weatherMessage(city.asString)
                }
            }

            "Schedule" -> {
                var date = result.parameters["date"]?.asString
                if (date == null) {
                    var dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    scheduleMessage(dayOfWeek)
                } else {
                    var dateFromString = dateFormatFromString.parse(date)

                    var cal = Calendar.getInstance()
                    cal.time = dateFromString
                    var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    scheduleMessage(dayOfWeek)
                }
            }
            "Meal" ->{
                var meal = result.parameters["meal"]?.asString
                var date = result.parameters["date"]?.asString

                if(date == null){
                    dateFormatFromString.format(Date())
                }

                mealMessage(meal,date)
            }
            else -> {
                var speech = result.fulfillment.speech
                messageDTOs.add(MessageDTO(false, speech))
                recyclerView.adapter.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(messageDTOs.size - 1)
            }
        }

    }
    fun mealMessage(meal: String? , date : String?){
        FirebaseFirestore.getInstance().collection("meals").whereEqualTo("mealtime",meal).whereEqualTo("date",date).get().addOnCompleteListener {
            task ->
                if(task.isSuccessful){
                    for(document in task.result){

                        var message = "메뉴는 ${document.data["menu"]} 입니다"
                        messageDTOs.add(MessageDTO(false,message))
                        recyclerView.adapter.notifyDataSetChanged()
                        recyclerView.smoothScrollToPosition(messageDTOs.size - 1)
                        break
                    }
                }
        }
    }
    fun weatherMessage(city : String){
        var weatherUrl = "https://api.openweathermap.org/data/2.5/forecast?id=524901&APPID=6dd85201b495e2749fc41fda4ed8d8c5&q=${city}&units=metric"
        var request = Request.Builder().url(weatherUrl).build()
        OkHttpClient().newCall(request).enqueue(object : Callback{  //okhttp 는 sub Thread로 동작함

            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                var result = response.body()?.string()

                var weatherDTO = Gson().fromJson(result,WeatherDTO::class.java)

                for(item in weatherDTO.list!!){
                    var weatherItemUnixTime = weatherDateFormatFromString.parse(item.dt_txt).time
                    if(weatherItemUnixTime > System.currentTimeMillis()){
                        var temp = item.main?.temp

                        var description = item.weather!![0].description

                        var time = weatherDateFormatToString.format(weatherItemUnixTime)

                        var humidity = item.main?.humidity

                        var message = time + " " + city + "의 기온은 " + temp + "도 하늘은 " + description + " 습도는 "+ humidity + "% 입니다."

                        runOnUiThread {
                            messageDTOs.add(MessageDTO(false,message))

                            recyclerView.adapter.notifyDataSetChanged()
                            recyclerView.smoothScrollToPosition(messageDTOs.size-1)
                        }
                        break   //가장 가까운 미래의 날씨만 나오게 하기위함
                    }
                }
            }

        })
    }

    fun scheduleMessage(dayofweek: Int?) {

        var dayOfWeekString: String? = null
        when (dayofweek) {
            1 -> {
                dayOfWeekString = "일요일"
            }
            2 -> {
                dayOfWeekString = "월요일"
            }
            3 -> {
                dayOfWeekString = "화요일"
            }
            4 -> {
                dayOfWeekString = "수요일"
            }
            5 -> {
                dayOfWeekString = "목요일"
            }
            6 -> {
                dayOfWeekString = "금요일"
            }
            7 -> {
                dayOfWeekString = "토요일"
            }

        }
        FirebaseFirestore.getInstance().collection("schedules").whereEqualTo("dayofweek", dayOfWeekString).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (document in task.result) {
                    var message = dayOfWeekString + "의 수업은 " + document.data["lecture"] + "입니다."
                    messageDTOs.add(MessageDTO(false, message))
                    recyclerView.adapter.notifyDataSetChanged()
                    recyclerView.smoothScrollToPosition(messageDTOs.size - 1)
                    break
                }
            }
        }
    }
}
//api.openweathermap.org/data/2.5/forecast?id=524901&APPID=6dd85201b495e2749fc41fda4ed8d8c5&q=Seoul&units=metric