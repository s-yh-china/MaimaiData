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
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongFC
import com.paperpig.maimaidata.model.SongFS
import com.paperpig.maimaidata.model.SongRank
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.toDp

class VersionCheckAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayMode = 0

    private var dataList: List<SongWithRecordEntity> = listOf()

    private var versionSelect: String? = null

    private var groupData: Map<String, List<SongWithRecordEntity>> = mapOf()

    private fun getFormatData(): Map<String, List<SongWithRecordEntity>> = dataList
        .filter { it.songData.version == versionSelect || (versionSelect == "maimai" && it.songData.version == "maimai PLUS") }
        .sortedByDescending { it.chartsMap[DifficultyType.MASTER]?.internalLevel }
        .groupBy { it.chartsMap[DifficultyType.MASTER]?.level ?: "?" }

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemCheckHeaderBinding.inflate(
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

                val groupFlatten = groupData.values.flatten()
                val groupSize = groupFlatten.size

                holder.tripleSCount.text = String.format(
                    format, groupFlatten.count { it.getRecordOrDef(DifficultyType.MASTER).rate >= SongRank.SSS }, groupSize
                )

                holder.fcCount.text = String.format(
                    format, groupFlatten.count { it.getRecordOrDef(DifficultyType.MASTER).fc >= SongFC.FC }, groupSize
                )

                @Suppress("KotlinConstantConditions") // Android Studio bug
                holder.apCount.text = String.format(
                    format, groupFlatten.count { it.getRecordOrDef(DifficultyType.MASTER).fc >= SongFC.AP }, groupSize
                )

                holder.fsdCount.text = String.format(
                    format, groupFlatten.count { it.getRecordOrDef(DifficultyType.MASTER).fs >= SongFS.FDX }, groupSize
                )
            }

            is LevelHolder -> {
                holder.levelTitle.text = "Level " + getSongAt(position).chartsMap[DifficultyType.MASTER]?.level
            }

            is ViewHolder -> {
                val data = getSongAt(position)
                holder.itemView.setOnClickListener { SongDetailActivity.actionStart(holder.itemView.context, data, DifficultyType.MASTER) }

                holder.songJacket.apply {
                    setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.master))
                    GlideApp.with(holder.itemView.context).load(MaimaiDataClient.IMAGE_BASE_URL + data.songData.imageUrl).into(this)
                }

                data.recordsMap[DifficultyType.MASTER]?.let {
                    holder.songJacket.colorFilter = PorterDuffColorFilter(Color.argb(128, 128, 128, 128), PorterDuff.Mode.SRC_ATOP)
                    when (displayMode) {
                        0 -> {
                            GlideApp.with(holder.itemView.context).load(it.rate.icon)
                                .override(50.toDp().toInt(), 22.toDp().toInt())
                                .into(holder.songRecordMark)
                        }

                        1 -> {
                            GlideApp.with(holder.itemView.context).load(it.fc.icon)
                                .override(30.toDp().toInt(), 30.toDp().toInt())
                                .into(holder.songRecordMark)
                        }

                        2 -> {
                            GlideApp.with(holder.itemView.context).load(it.fs.icon)
                                .override(30.toDp().toInt(), 30.toDp().toInt())
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
    fun updateDisplay() {
        displayMode = (displayMode + 1) % 3
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newSongData: List<SongWithRecordEntity>) {
        dataList = newSongData
        groupData = getFormatData()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(version: String) {
        versionSelect = version
        groupData = getFormatData()
        notifyDataSetChanged()
    }

    private fun getSongAt(position: Int): SongWithRecordEntity {
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