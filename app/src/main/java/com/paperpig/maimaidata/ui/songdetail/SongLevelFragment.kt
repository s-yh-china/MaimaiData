package com.paperpig.maimaidata.ui.songdetail

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentSongLevelBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.SongType
import com.paperpig.maimaidata.repository.ChartStatsRepository
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.setCopyOnLongClick
import com.paperpig.maimaidata.utils.setShrinkOnTouch
import com.paperpig.maimaidata.utils.toDp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private const val ARG_SONG_DATA = "song_data"
private const val ARG_POSITION = "position"
private const val ARG_RECORD = "record"

class SongLevelFragment : BaseFragment<FragmentSongLevelBinding>() {
    private lateinit var binding: FragmentSongLevelBinding
    private lateinit var data: SongWithChartsEntity
    private var record: RecordEntity? = null
    private var position: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            data = it.getParcelable(ARG_SONG_DATA)!!
            position = it.getInt(ARG_POSITION)
            record = it.getParcelable(ARG_RECORD)
        }
    }

    override fun getViewBinding(container: ViewGroup?): FragmentSongLevelBinding {
        binding = FragmentSongLevelBinding.inflate(layoutInflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (record != null) {
            binding.chartStatusGroup.visibility = View.VISIBLE
            binding.chartNoStatusGroup.visibility = View.GONE
            binding.chartAchievement.text = getString(R.string.maimaidx_achievement_desc, record!!.achievements)
            binding.chartRank.setImageDrawable(ContextCompat.getDrawable(requireContext(), record!!.getRankIcon()))
            binding.chartFcap.setImageDrawable(ContextCompat.getDrawable(requireContext(), record!!.getFcIcon()))
            binding.chartFsfsd.setImageDrawable(ContextCompat.getDrawable(requireContext(), record!!.getFsIcon()))
        } else {
            binding.chartStatusGroup.visibility = View.GONE
            binding.chartNoStatusGroup.visibility = View.VISIBLE

            binding.recordTips.setOnClickListener {
                Toast.makeText(context, R.string.no_record_tips, Toast.LENGTH_LONG).show()
            }
        }
        val chart = data.charts[position]
        val songData = data.songData
        ChartStatsRepository.getInstance(AppDataBase.getInstance().chartStatsDao())
            .getChartStatsBySongIdAndDifficultyIndex(songData.id, position).observe(requireActivity()) {
                binding.songFitDiff.text = it?.fitDifficulty?.toString() ?: "-"
            }

        val totalScore = (chart.noteTap + chart.noteTouch) + chart.noteHold * 2 + chart.noteSlide * 3 + chart.noteBreak * 5
        val format = DecimalFormat("0.#####%")
        format.roundingMode = RoundingMode.DOWN

        chart.oldInternalLevel?.let {
            if (it < chart.internalLevel) {
                binding.songLevel.setTextColor(ContextCompat.getColor(requireContext(), R.color.mmd_color_red))
                binding.songLevel.text = getString(R.string.inner_level_up, chart.internalLevel)
                binding.oldLevel.text = getString(R.string.inner_level_old, it)
            } else if (it > chart.internalLevel) {
                binding.songLevel.setTextColor(ContextCompat.getColor(requireContext(), R.color.mmd_color_green))
                binding.songLevel.text = getString(R.string.inner_level_down, chart.internalLevel)
                binding.oldLevel.text = getString(R.string.inner_level_old, it)
            } else {
                binding.songLevel.text = "${chart.internalLevel}"
                binding.oldLevel.text = getString(R.string.inner_level_old, it)
            }
        } ?: run {
            binding.songLevel.text = chart.internalLevel.toString()
        }

        binding.chartDesigner.apply {
            text = data.charts[position].charter
            setShrinkOnTouch()
            setCopyOnLongClick(data.charts[position].charter)
        }

        binding.chartView.setMaxValues((MaimaiDataApplication.instance.maxNotesStats?.let {
            listOf(it.total, it.tap, it.hold, it.slide, it.touch, it.`break`)
        }) ?: emptyList())
        val noteValueList = listOf(
            chart.noteTotal,
            chart.noteTap,
            chart.noteHold,
            chart.noteSlide,
            chart.noteTouch,
            chart.noteBreak
        )
        binding.chartView.setValues(noteValueList)

        binding.chartView.setBarColor(songData.bgColor)

        binding.tapGreatScore.text = format.format(1f / totalScore * 0.2)
        binding.tapGoodScore.text = format.format(1f / totalScore * 0.5)
        binding.tapMissScore.text = format.format(1f / totalScore)
        binding.holdGreatScore.text = format.format(2f / totalScore * 0.2)
        binding.holdGoodScore.text = format.format(2f / totalScore * 0.5)
        binding.holdMissScore.text = format.format(2f / totalScore)
        binding.slideGreatScore.text = format.format(3f / totalScore * 0.2)
        binding.slideGoodScore.text = format.format(3f / totalScore * 0.5)
        binding.slideMissScore.text = format.format(3f / totalScore)
        binding.breakGreat4xScore.text = format.format(5f / totalScore * 0.2 + (0.01 / chart.noteBreak) * 0.6)
        binding.breakGreat3xScore.text = format.format(5f / totalScore * 0.4 + (0.01 / chart.noteBreak) * 0.6)
        binding.breakGreat25xScore.text = format.format(5f / totalScore * 0.5 + (0.01 / chart.noteBreak) * 0.6)
        binding.breakGoodScore.text = format.format(5f / totalScore * 0.6 + (0.01 / chart.noteBreak) * 0.7)
        binding.breakMissScore.text = format.format(5f / totalScore + 0.01 / chart.noteBreak)
        binding.break50Score.text = format.format(0.01 / chart.noteBreak * 0.25)
        binding.break100Score.text = (format.format((0.01 / chart.noteBreak) * 0.5))

        val notesAchievementStoke = (binding.noteAchievementLayout.background as LayerDrawable).findDrawableByLayerId(R.id.note_achievement_stroke) as GradientDrawable
        val notesAchievementInnerStoke = (binding.noteAchievementLayout.background as LayerDrawable).findDrawableByLayerId(R.id.note_achievement_inner_stroke) as GradientDrawable

        notesAchievementStoke.setStroke(4.toDp().toInt(), ContextCompat.getColor(requireContext(), songData.strokeColor))

        notesAchievementInnerStoke.setStroke(3.toDp().toInt(), ContextCompat.getColor(requireContext(), songData.bgColor))

        if (!songData.version.contains("舞萌")) {
            binding.finaleGroup.visibility = View.VISIBLE
            binding.finaleAchievement.text =
                String.format(
                    getString(R.string.maimai_achievement_format),
                    BigDecimal(
                        (chart.noteTap * 500 + chart.noteHold * 1000 + chart.noteSlide * 1500 + chart.noteBreak * 2600) * 1.0 /
                            (chart.noteTap * 500 + chart.noteHold * 1000 + chart.noteSlide * 1500 + chart.noteBreak * 2500) * 100
                    ).setScale(2, RoundingMode.DOWN)
                )
        } else {
            binding.finaleGroup.visibility = View.GONE
        }
    }

    companion object {
        fun newInstance(chart: SongWithChartsEntity, position: Int, record: RecordEntity?) =
            SongLevelFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SONG_DATA, chart)
                    putInt(ARG_POSITION, position)
                    putParcelable(ARG_RECORD, record)
                }
            }
    }
}