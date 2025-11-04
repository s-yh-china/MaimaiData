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
import com.paperpig.maimaidata.model.GameSongObject
import com.paperpig.maimaidata.repository.ChartStatsRepository
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.setCopyOnLongClick
import com.paperpig.maimaidata.utils.setShrinkOnTouch
import com.paperpig.maimaidata.utils.toDp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.ceil

private const val ARG_SONG_DATA = "song_data"

class SongLevelFragment : BaseFragment<FragmentSongLevelBinding>() {
    private lateinit var binding: FragmentSongLevelBinding
    private lateinit var data: GameSongObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            data = it.getParcelable(ARG_SONG_DATA)!!
        }
    }

    override fun getViewBinding(container: ViewGroup?): FragmentSongLevelBinding {
        binding = FragmentSongLevelBinding.inflate(layoutInflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        data.record?.let {
            binding.chartStatusGroup.visibility = View.VISIBLE
            binding.chartNoStatusGroup.visibility = View.GONE
            binding.chartAchievement.text = getString(R.string.maimaidx_achievement_desc, it.achievements)
            binding.chartRank.setImageDrawable(ContextCompat.getDrawable(requireContext(), it.rate.icon))
            binding.chartFcap.setImageDrawable(ContextCompat.getDrawable(requireContext(), it.fc.icon))
            binding.chartFsfsd.setImageDrawable(ContextCompat.getDrawable(requireContext(), it.fs.icon))
            if (it.playCount == -1) {
                binding.chartPlayCountGroup.visibility = View.GONE
            } else {
                binding.chartPlayCountGroup.visibility = View.VISIBLE
                binding.playCount.text = it.playCount.toString()
            }
        } ?: run {
            binding.chartStatusGroup.visibility = View.GONE
            binding.chartNoStatusGroup.visibility = View.VISIBLE
            binding.recordTips.setOnClickListener { Toast.makeText(context, R.string.no_record_tips, Toast.LENGTH_LONG).show() }
        }

        val song = data.song
        val chart = data.chart
        ChartStatsRepository.getInstance().getChartStatsBySongIdAndDifficulty(song.id, chart.difficultyType).observe(requireActivity()) {
            binding.songFitDiff.text = it?.fitDifficulty?.toString() ?: "-"
        }

        val format = DecimalFormat("0.#####%")
        format.roundingMode = RoundingMode.DOWN

        chart.oldInternalLevel?.let {
            if (it < chart.internalLevel) {
                binding.songLevel.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_red))
                binding.songLevel.text = getString(R.string.inner_level_up, chart.internalLevel)
                binding.oldLevel.text = getString(R.string.inner_level_old, it)
            } else if (it > chart.internalLevel) {
                binding.songLevel.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_green))
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
            text = chart.charter
            setShrinkOnTouch()
            setCopyOnLongClick(chart.charter)
        }

        binding.chartView.setMaxValues(MaimaiDataApplication.instance.maxNotesStats?.let { listOf(it.total, it.tap, it.hold, it.slide, it.touch, it.`break`) } ?: emptyList())
        val noteValueList = listOf(chart.noteTotal, chart.noteTap, chart.noteHold, chart.noteSlide, chart.noteTouch, chart.noteBreak)
        binding.chartView.setValues(noteValueList)
        binding.chartView.setBarColor(song.bgColor)

        val totalScore = (chart.noteTap + chart.noteTouch) + chart.noteHold * 2 + chart.noteSlide * 3 + chart.noteBreak * 5

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

        notesAchievementStoke.setStroke(4.toDp().toInt(), ContextCompat.getColor(requireContext(), song.strokeColor))
        notesAchievementInnerStoke.setStroke(3.toDp().toInt(), ContextCompat.getColor(requireContext(), song.bgColor))

        val dxstarAchievementStoke = (binding.dxScoreLayout.background as LayerDrawable).findDrawableByLayerId(R.id.note_achievement_stroke) as GradientDrawable
        val dxstarAchievementInnerStoke = (binding.dxScoreLayout.background as LayerDrawable).findDrawableByLayerId(R.id.note_achievement_inner_stroke) as GradientDrawable

        dxstarAchievementStoke.setStroke(4.toDp().toInt(), ContextCompat.getColor(requireContext(), song.strokeColor))
        dxstarAchievementInnerStoke.setStroke(3.toDp().toInt(), ContextCompat.getColor(requireContext(), song.bgColor))

        val maxDxScore = data.chart.noteTotal * 3
        binding.minDxScore1.text = ceil(maxDxScore * 0.85).toInt().toString()
        binding.minDxScore2.text = ceil(maxDxScore * 0.9).toInt().toString()
        binding.minDxScore3.text = ceil(maxDxScore * 0.93).toInt().toString()
        binding.minDxScore4.text = ceil(maxDxScore * 0.95).toInt().toString()
        binding.minDxScore5.text = ceil(maxDxScore * 0.97).toInt().toString()

        if (!song.version.contains("舞萌")) {
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
        fun newInstance(data: GameSongObject) = SongLevelFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_SONG_DATA, data)
            }
        }
    }
}