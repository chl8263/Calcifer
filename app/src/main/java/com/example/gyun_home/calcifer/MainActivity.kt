package com.example.gyun_home.calcifer

import ai.api.AIConfiguration
import ai.api.AIDataService
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import com.example.gyun_home.calcifer.adapter.MessageDTO
import com.example.gyun_home.calcifer.adapter.RecyclerViewAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var messageDTOs = arrayListOf<MessageDTO>()
    var aiDataService : AIDataService = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView.adapter = RecyclerViewAdapter(messageDTOs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        var config = AIConfiguration("5fd84bf2f6074a85b4c781c55eb6b381",AIConfiguration.SupportedLanguages.Korean)

        button.setOnClickListener {
            if(!TextUtils.isEmpty(editText.text)){
                messageDTOs.add(MessageDTO(true,editText.text.toString()))
                recyclerView.adapter.notifyDataSetChanged()
                recyclerView.smoothScrollToPosition(messageDTOs.size-1)
                editText.setText("")
            }
        }
    }
}
