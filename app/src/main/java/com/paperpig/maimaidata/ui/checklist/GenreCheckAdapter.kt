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
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongType
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.toDp

class GenreCheckAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayMode = 0

    private var dataList: List<SongWithChartsEntity> = listOf()
    private var recordList: List<RecordEntity> = listOf()

    private var genreSelect: String? = null
    private var difficultySelect: DifficultyType = DifficultyType.MASTER
    private var groupData: Map<String, List<SongWithChartsEntity>> = mapOf()

    private fun getFormatData(): Map<String, List<SongWithChartsEntity>> = dataList
        .filter { it.songData.genre == genreSelect }
        .sortedByDescending { it.chartsMap[getActualDifficulty(it)]?.internalLevel }
        .groupBy { it.chartsMap[getActualDifficulty(it)]!!.level }

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

    class LevelHolder(binding: ItemLevelHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        val levelTitle = binding.levelTitle
    }

    class ViewHolder(binding: ItemSongCheckBinding) : RecyclerView.ViewHolder(binding.root) {
        val songJacket = binding.songJacket
        val songRecordMark = binding.songRecordMark
        val songType = binding.songType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemCheckHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }

            TYPE_LEVEL -> {
                val binding = ItemLevelHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LevelHolder(binding)
            }

            else -> {
                val binding = ItemSongCheckBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ViewHolder(binding)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val format = context.getString(R.string.name_plate_achieved)

                val groupFlatten = groupData.values.flatten()
                val groupSize = groupData.values.sumOf { it.size }

                holder.tripleSCount.text = String.format(
                    format, recordList.count {
                        val songInGroup = groupFlatten.find { data -> data.songData.id == it.songId }
                        songInGroup != null && getActualDifficulty(songInGroup) == it.difficultyType && it.achievements >= 100
                    }, groupSize
                )

                holder.fcCount.text = String.format(
                    format, recordList.count {
                        val songInGroup = groupFlatten.find { data -> data.songData.id == it.songId }
                        songInGroup != null && getActualDifficulty(songInGroup) == it.difficultyType && it.fc.isNotEmpty()
                    }, groupSize
                )

                holder.apCount.text = String.format(
                    format, recordList.count {
                        val songInGroup = groupFlatten.find { data -> data.songData.id == it.songId }
                        songInGroup != null && getActualDifficulty(songInGroup) == it.difficultyType && (it.fc == "ap" || it.fc == "app")
                    }, groupSize
                )

                holder.fsdCount.text = String.format(
                    format, recordList.count {
                        val songInGroup = groupFlatten.find { data -> data.songData.id == it.songId }
                        songInGroup != null && getActualDifficulty(songInGroup) == it.difficultyType && (it.fs == "fsd" || it.fs == "fsdp")
                    }, groupSize
                )
            }

            is LevelHolder -> {
                val data = getSongAt(position)
                holder.levelTitle.text = "Level " + data.chartsMap[getActualDifficulty(data)]?.level
            }

            is ViewHolder -> {
                val song = getSongAt(position)
                val actualDifficulty = getActualDifficulty(song)
                holder.itemView.setOnClickListener {
                    SongDetailActivity.actionStart(holder.itemView.context, song)
                }

                holder.songJacket.apply {
                    setBackgroundColor(ContextCompat.getColor(holder.itemView.context, actualDifficulty.color))
                    GlideApp.with(holder.itemView.context).load(MaimaiDataClient.IMAGE_BASE_URL + song.songData.imageUrl).into(this)
                }
                if (song.songData.type == SongType.DX) {
                    GlideApp.with(holder.itemView.context).load(R.drawable.ic_deluxe).into(holder.songType)
                } else if (song.songData.type == SongType.SD) {
                    GlideApp.with(holder.itemView.context).load(R.drawable.ic_standard).into(holder.songType)
                }

                recordList.find { it.songId == song.songData.id && it.difficultyType == actualDifficulty }
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
    }

    private fun getActualDifficulty(song: SongWithChartsEntity): DifficultyType {
        return if (song.charts.size <= difficultySelect.difficultyIndex) {
            DifficultyType.from(song.songData.type, difficultySelect.difficultyIndex - 1)
        } else {
            difficultySelect
        }
    }

    override fun getItemCount(): Int {
        return groupData.size + groupData.values.sumOf { it.size } + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_HEADER
        }

        var currentPosition = 1
        for ((_, songs) in groupData) {
            if (position == currentPosition) {
                return TYPE_LEVEL
            }
            currentPosition++
            if (position < currentPosition + songs.size) {
                return TYPE_NORMAL
            }
            currentPosition += songs.size
        }

        return TYPE_NORMAL
    }

    fun updateDisplay() {
        displayMode = (displayMode + 1) % 3
        notifyDataSetChanged()
    }

    fun setData(newSongData: List<SongWithChartsEntity>, newRecordList: List<RecordEntity>) {
        dataList = newSongData
        recordList = newRecordList
        updateData(genreSelect, difficultySelect)
    }

    fun updateData(genre: String?, difficulty: DifficultyType) {
        genreSelect = genre
        difficultySelect = difficulty
        groupData = getFormatData()
        notifyDataSetChanged()
    }

    private fun getSongAt(position: Int): SongWithChartsEntity {
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