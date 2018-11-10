package com.example.gyun_home.calcifer

import ai.api.AIConfiguration
import ai.api.AIDataService
import ai.api.model.AIRequest
import ai.api.model.Result
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.util.Log
import com.example.gyun_home.calcifer.adapter.MessageDTO
import com.example.gyun_home.calcifer.adapter.RecyclerViewAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var dateFormatFromString = SimpleDateFormat("yyyy-MM-dd")

    var messageDTOs = arrayListOf<MessageDTO>()
    var aiDataService: AIDataService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView.adapter = RecyclerViewAdapter(messageDTOs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        var config = AIConfiguration("5fd84bf2f6074a85b4c781c55eb6b381", AIConfiguration.SupportedLanguages.Korean)
        aiDataService = AIDataService(config)

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
            "Schedule" -> {
                var date = result.parameters["date"]?.asString
                var dateFromString = dateFormatFromString.parse(date)

                var cal = Calendar.getInstance()
                cal.time = dateFromString
                var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                scheduleMessage(dayOfWeek)
            }
            else -> {
                var speech = result.fulfillment.speech
                messageDTOs.add(MessageDTO(false, speech))
                recyclerView.adapter.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(messageDTOs.size - 1)
            }
        }

    }

    fun scheduleMessage(dayofweek : Int?){
        var dayOfWeekString : String? = null
        when (dayofweek){
            1->{
                dayOfWeekString = "일요일"
            }
            2->{
                dayOfWeekString = "월요일"
            }
            3->{
                dayOfWeekString = "화요일"
            }
            4->{
                dayOfWeekString = "수요일"
            }
            5->{
                dayOfWeekString = "목요일"
            }
            6->{
                dayOfWeekString = "금요일"
            }
            7->{
                dayOfWeekString = "토요일"
            }
        }
        FirebaseFirestore.getInstance().collection("schedules").whereEqualTo("datofweek",dayOfWeekString).get().addOnCompleteListener { 
            task ->
            if(task.isSuccessful){
                for(document in task.result){
                    var message = dayOfWeekString + "의 수업은 "+document.data["lecture"]+"입니다."
                    messageDTOs.add(MessageDTO(false,message))
                    recyclerView.adapter.notifyDataSetChanged()
                    recyclerView.smoothScrollToPosition(messageDTOs.size-1)
                    break
                }
            }
        }
    }
}
