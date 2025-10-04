package com.paperpig.maimaidata.ui.rating.best50

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity

class ProberVersionAdapter : RecyclerView.Adapter<ProberVersionAdapter.ViewHolder>() {

    private var dataList = listOf<SongWithRecordEntity>()

    var b35Adapter: RecordAdapter = RecordAdapter(0)
    var b15Adapter: RecordAdapter = RecordAdapter(1)

    class ViewHolder(val recyclerView: RecyclerView) : RecyclerView.ViewHolder(recyclerView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recyclerView = RecyclerView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return ViewHolder(recyclerView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.recyclerView.apply {
            adapter = if (position == 0) b35Adapter else b15Adapter
            layoutManager = LinearLayoutManager(holder.itemView.context)
        }
    }

    override fun getItemCount(): Int {
        return 2
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<SongWithRecordEntity>) {
        dataList = data
        b35Adapter.setData(data)
        b15Adapter.setData(data)
        notifyDataSetChanged()
    }
}