package com.example.gyun_home.calcifer.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gyun_home.calcifer.R
import kotlinx.android.synthetic.main.item_comment.view.*

data class MessageDTO(var myMessage:Boolean? = null , var message:String? = null)

class RecyclerViewAdapter(messageDTOs: ArrayList<MessageDTO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    var messageDTO = messageDTOs
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
        return CustomViewHolder(view)
    }



    override fun getItemCount(): Int {
        return messageDTO.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(!messageDTO[position].myMessage!!) {
            holder.itemView.righttextView.visibility = View.VISIBLE
            holder.itemView.righttextView.text = messageDTO[position].message
            holder.itemView.lefttextView.visibility = View.INVISIBLE
        }else {
            holder.itemView.lefttextView.visibility = View.VISIBLE
            holder.itemView.lefttextView.text = messageDTO[position].message
            holder.itemView.righttextView.visibility = View.INVISIBLE
        }

    }

    class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view) {

    }


}