package com.paperpig.maimaidata.ui.rating.best50

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ItemPlayerRtsongLayoutBinding
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.GameSongObject
import com.paperpig.maimaidata.model.GameSongObjectWithRating
import com.paperpig.maimaidata.model.SongType.UTAGE
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.toDp

class RecordAdapter(val version: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var recordList = listOf<Pair<GameSongObjectWithRating, SongWithRecordEntity>>()

    inner class RecordHolder(binding: ItemPlayerRtsongLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val songLevel: TextView = binding.songLevel
        val songDiff: View = binding.songDiff
        val songJacket: ImageView = binding.songJacket
        val songJacketContainer: FrameLayout = binding.songJacketContainer
        val songTitle: TextView = binding.songTitle
        val songAcc: TextView = binding.songAcc
        val songRating: TextView = binding.songRating
        val songFc: ImageView = binding.songFc
        val songFs: ImageView = binding.songFs
        val songRank: ImageView = binding.songRank
        val songType: ImageView = binding.songType
        val out: FrameLayout = binding.outer
        val container: RelativeLayout = binding.container
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = RecordHolder(
        ItemPlayerRtsongLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder) {
            is RecordHolder -> {
                val context = viewHolder.itemView.context
                val data = recordList[position]
                val songObject = data.first.obj
                val rating = data.first.rating
                viewHolder.songTitle.text = songObject.song.title
                viewHolder.songLevel.text = songObject.chart.internalLevel.toString()
                viewHolder.songAcc.text = String.format(context.getString(R.string.maimaidx_achievement_desc), songObject.record?.achievements)
                viewHolder.songRating.text = String.format(context.getString(R.string.rating_scope), rating, (songObject.chart.internalLevel * 22.512).toInt())

                viewHolder.itemView.setOnClickListener { SongDetailActivity.actionStart(viewHolder.itemView.context, data.second, songObject.chart.difficultyType) }
                GlideApp.with(context)
                    .load(MaimaiDataClient.IMAGE_BASE_URL + songObject.song.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade()).apply(
                        RequestOptions.bitmapTransform(
                            RoundedCorners(5.toDp().toInt())
                        )
                    ).into(viewHolder.songJacket)

                (viewHolder.out.background as GradientDrawable).setColor(ContextCompat.getColor(context, songObject.chart.difficultyType.shadowColor))
                (viewHolder.container.background as GradientDrawable).setColor(ContextCompat.getColor(context, songObject.chart.difficultyType.backgroundColor))
                (viewHolder.songJacketContainer.background as GradientDrawable).setColor(ContextCompat.getColor(context, songObject.chart.difficultyType.shadowColor))

                viewHolder.songFc.setImageDrawable(ContextCompat.getDrawable(context, songObject.recordOrDef.fc.icon))
                viewHolder.songFs.setImageDrawable(ContextCompat.getDrawable(context, songObject.recordOrDef.fs.icon))
                viewHolder.songRank.setImageDrawable(ContextCompat.getDrawable(context, songObject.recordOrDef.rate.icon))

                viewHolder.songDiff.setBackgroundResource(songObject.chart.difficultyType.rtsongDrawable)

                if (songObject.song.type != UTAGE) {
                    viewHolder.songType.setImageResource(songObject.song.type.icon)
                } else {
                    viewHolder.songType.setImageDrawable(null)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return recordList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<SongWithRecordEntity>) {
        recordList = list
            .filter { it.songData.isNew == (version != 0) }
            .flatMap { song ->
                song.records.map { record ->
                    GameSongObject.formSongWithRecord(song, record.difficultyType)!!.withRating() to song
                }
            }
            .sortedByDescending { it.first.rating }
            .take(if (version == 0) 35 else 15)
        notifyDataSetChanged()
    }
}