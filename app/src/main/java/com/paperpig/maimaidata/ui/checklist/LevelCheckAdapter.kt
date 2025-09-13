package com.paperpig.maimaidata.ui.checklist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ItemCheckHeaderBinding
import com.paperpig.maimaidata.databinding.ItemLevelHeaderBinding
import com.paperpig.maimaidata.databinding.ItemSongCheckBinding
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.DsSongData
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.toDp

class LevelCheckAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    //0为显示完成率标识，1为显示FC/AP标识，2为显示FDX标识
    private var displayMode = 0

    //歌曲信息列表
    private var dataList: List<SongWithChartsEntity> = emptyList()

    //个人记录列表
    private var recordList: List<RecordEntity> = emptyList()

    //指定难度
    private var levelSelect: String? = null

    //显示的数据
    private var groupData: Map<Double, List<DsSongData>> = mapOf()

    /**
     * 转换为adapter数据源
     */
    private fun getFormatData(): Map<Double, List<DsSongData>> {
        return dataList.flatMap { datum ->
            datum.charts.indices
                .filter { i -> datum.charts[i].level == levelSelect }
                .map { i ->
                    DsSongData(
                        datum.songData.id,
                        datum.songData.title,
                        datum.songData.type,
                        datum.songData.imageUrl,
                        i,
                        datum.charts[i].ds
                    )
                }
        }.sortedByDescending { it.ds }.groupBy { it.ds }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_LEVEL = 1
        const val TYPE_NORMAL = 2
    }

    class HeaderViewHolder(binding: ItemCheckHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        val tripleSCount = binding.tripleSCount
        val fcCount = binding.fcCount
        val apCount = binding.apCount
        val fsdCount = binding.fsdCount
    }

    class LevelViewHolder(binding: ItemLevelHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        val levelTitle = binding.levelTitle
    }

    class ItemViewHolder(binding: ItemSongCheckBinding) : RecyclerView.ViewHolder(binding.root) {
        val songJacket = binding.songJacket
        val songRecordMark = binding.songRecordMark
        val songType = binding.songType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemCheckHeaderBinding.inflate(
                    LayoutInflater.from(
                        context
                    ), parent, false
                )
            )

            TYPE_LEVEL -> LevelViewHolder(
                ItemLevelHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            else -> ItemViewHolder(
                ItemSongCheckBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val format = context.getString(R.string.name_plate_achieved)

            val groupFlatten = groupData.values.flatten()
            val groupSize = groupData.values.sumOf { it.size }

            holder.tripleSCount.text = String.format(
                format, recordList.count {
                    it.achievements >= 100 && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.songId }
                }, groupSize
            )

            holder.fcCount.text = String.format(
                format, recordList.count {
                    it.fc.isNotEmpty() && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.songId }
                }, groupSize
            )

            holder.apCount.text = String.format(
                format, recordList.count {
                    (it.fc == "ap" || it.fc == "app") && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.songId }
                }, groupSize
            )

            holder.fsdCount.text = String.format(
                format, recordList.count {
                    (it.fs == "fsd" || it.fs == "fsdp") && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.songId }
                }, groupSize
            )
        }
        if (holder is LevelViewHolder) {
            val data = getSongAt(position)
            holder.levelTitle.text = data.ds.toString()

        }
        if (holder is ItemViewHolder) {
            val data = getSongAt(position)
            holder.itemView.setOnClickListener {
                dataList.find { it.songData.id == data.songId }?.let {
                    SongDetailActivity.actionStart(holder.itemView.context, it)
                }
            }

            holder.songJacket.apply {
                setBackgroundColor(ContextCompat.getColor(holder.itemView.context, getBorderColor(data.levelIndex)))
                GlideApp.with(holder.itemView.context).load(MaimaiDataClient.IMAGE_BASE_URL + data.imageUrl).into(holder.songJacket)
            }
            if (data.type == Constants.CHART_TYPE_DX) {
                GlideApp.with(holder.itemView.context).load(R.drawable.ic_deluxe).into(holder.songType)
            } else {
                GlideApp.with(holder.itemView.context).load(R.drawable.ic_standard).into(holder.songType)
            }

            recordList.find { it.songId == data.songId && it.levelIndex == data.levelIndex }
                ?.let { record ->
                    holder.songJacket.colorFilter =
                        PorterDuffColorFilter(Color.argb(128, 128, 128, 128), PorterDuff.Mode.SRC_ATOP)
                    when (displayMode) {
                        0 -> {
                            GlideApp.with(holder.itemView.context).load(record.getRankIcon())
                                .override(
                                    50.toDp().toInt(),
                                    22.toDp().toInt()
                                )
                                .into(holder.songRecordMark)
                        }

                        1 -> {
                            GlideApp.with(holder.itemView.context).load(record.getFcIcon())
                                .override(
                                    30.toDp().toInt(),
                                    30.toDp().toInt()
                                )
                                .into(holder.songRecordMark)
                        }

                        2 -> {
                            GlideApp.with(holder.itemView.context).load(record.getFsIcon())
                                .override(
                                    30.toDp().toInt(),
                                    30.toDp().toInt()
                                )
                                .into(holder.songRecordMark)
                        }

                        else -> {}
                    }
                } ?: run {
                holder.songJacket.colorFilter = null
                holder.songRecordMark.setImageDrawable(null)
            }
        }
    }

    fun updateDisplay() {
        displayMode = (displayMode + 1) % 3
        notifyDataSetChanged()
    }

    fun setData(dataList: List<SongWithChartsEntity>, recordList: List<RecordEntity>) {
        this.dataList = dataList
        this.recordList = recordList
        groupData = getFormatData()
        notifyDataSetChanged()
    }

    fun updateData(newLevelSelect: String) {
        levelSelect = newLevelSelect
        groupData = getFormatData()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return groupData.size + groupData.values.sumOf { it.size } + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_HEADER
        }
        var count = 0
        for (groupDatum in groupData) {
            val size = groupDatum.value.size + 1
            if (position - 1 < count + size) {
                return if (position - 1 == count) TYPE_LEVEL else TYPE_NORMAL
            }
            count += size
        }
        throw IllegalArgumentException()
    }

    private fun getSongAt(position: Int): DsSongData {
        var count = 0
        for (groupDatum in groupData) {
            val size = groupDatum.value.size + 1
            if (position - 1 < count + size) {
                return if (position - 1 == count) groupDatum.value[0] else groupDatum.value[position - count - 1 - 1]
            }
            count += size
        }
        throw IllegalArgumentException()
    }

    private fun getBorderColor(levelIndex: Int): Int {
        return when (levelIndex) {
            0 -> R.color.basic
            1 -> R.color.advanced
            2 -> R.color.expert
            3 -> R.color.master
            4 -> R.color.remaster_border
            else -> 0
        }
    }
}