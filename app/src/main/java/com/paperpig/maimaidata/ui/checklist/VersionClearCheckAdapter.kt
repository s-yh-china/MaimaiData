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
import com.paperpig.maimaidata.databinding.ItemClearCheckHeaderBinding
import com.paperpig.maimaidata.databinding.ItemLevelHeaderBinding
import com.paperpig.maimaidata.databinding.ItemSongCheckBinding
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.GameSongObject
import com.paperpig.maimaidata.model.SongRank
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.toDp

class VersionClearCheckAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayMode = 0

    private var dataList: List<SongWithRecordEntity> = listOf()

    private var versionSelect: String? = null
    private var difficultySelect: DifficultyType = DifficultyType.BASIC

    private var groupData: Map<String, List<Pair<GameSongObject, SongWithRecordEntity>>> = mapOf()

    private fun getFormatData(): Map<String, List<Pair<GameSongObject, SongWithRecordEntity>>> = when (displayMode) {
        0 -> {
            dataList
                .filter { it.songData.version == versionSelect }
                .mapNotNull { song -> GameSongObject.formSongWithRecord(song, difficultySelect)?.let { it to song } }
                .sortedByDescending { it.first.chart.internalLevel }
                .groupBy { it.first.chart.level }
        }

        1 -> {
            dataList
                .flatMap { song ->
                    song.charts
                        .filter { (song.recordsMap[it.difficultyType]?.rate ?: SongRank.D) < SongRank.A }
                        .map { GameSongObject.formSongWithRecord(song, it.difficultyType)!! to song }
                }
                .sortedByDescending { it.first.chart.internalLevel }
                .groupBy { it.first.chart.level }
        }

        else -> mapOf()
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_LEVEL = 1
        const val TYPE_NORMAL = 2
    }

    class HeaderViewHolder(binding: ItemClearCheckHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        val clearCount = binding.clearCount
    }

    class LevelHolder(binding: ItemLevelHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        val levelTitle = binding.levelTitle
    }

    class ViewHolder(binding: ItemSongCheckBinding) : RecyclerView.ViewHolder(binding.root) {
        val songJacket = binding.songJacket
        val songRecordMark = binding.songRecordMark
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemClearCheckHeaderBinding.inflate(
                    LayoutInflater.from(context), parent, false
                )
            )

            TYPE_LEVEL -> LevelHolder(
                ItemLevelHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            else -> ViewHolder(
                ItemSongCheckBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val format = context.getString(R.string.name_plate_achieved)

                when (displayMode) {
                    0 -> {
                        val groupFlatten = groupData.values.flatten()
                        val groupSize = groupFlatten.size

                        holder.clearCount.text = String.format(format, groupFlatten.count { (it.first.record?.rate ?: SongRank.D) >= SongRank.A }, groupSize)
                    }

                    1 -> {
                        val allSongsSize = dataList.sumOf { it.charts.size }
                        val groupSize = groupData.values.flatten().size
                        holder.clearCount.text = String.format(format, allSongsSize - groupSize, allSongsSize)
                    }
                }
            }

            is LevelHolder -> {
                holder.levelTitle.text = "Level " + getSongAt(position).first.chart.level
            }

            is ViewHolder -> {
                val data = getSongAt(position)
                holder.itemView.setOnClickListener { SongDetailActivity.actionStart(holder.itemView.context, data.second) }

                holder.songJacket.apply {
                    setBackgroundColor(ContextCompat.getColor(holder.itemView.context, data.first.chart.difficultyType.color))
                    GlideApp.with(holder.itemView.context).load(MaimaiDataClient.IMAGE_BASE_URL + data.first.song.imageUrl).into(this)
                }

                data.first.record?.let {
                    holder.songJacket.colorFilter = PorterDuffColorFilter(Color.argb(128, 128, 128, 128), PorterDuff.Mode.SRC_ATOP)
                    when (displayMode) {
                        0 -> {
                            GlideApp.with(holder.itemView.context).load(if (it.rate >= SongRank.A) R.drawable.music_icon_clear else it.rate.icon)
                                .override(50.toDp().toInt(), 22.toDp().toInt())
                                .into(holder.songRecordMark)
                        }

                        1 -> {
                            GlideApp.with(holder.itemView.context).load(it.rate.icon)
                                .override(50.toDp().toInt(), 22.toDp().toInt())
                                .into(holder.songRecordMark)
                        }
                    }
                } ?: run {
                    holder.songJacket.colorFilter = null
                    holder.songRecordMark.setImageDrawable(null)
                }
            }
        }
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

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newSongData: List<SongWithRecordEntity>) {
        dataList = newSongData
        groupData = getFormatData()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(version: String, difficulty: DifficultyType) {
        versionSelect = version
        difficultySelect = difficulty
        displayMode = if (version == "霸者") 1 else 0
        groupData = getFormatData()
        notifyDataSetChanged()
    }

    private fun getSongAt(position: Int): Pair<GameSongObject, SongWithRecordEntity> {
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
}