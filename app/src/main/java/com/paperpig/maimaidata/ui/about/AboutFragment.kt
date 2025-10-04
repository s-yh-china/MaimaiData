package com.paperpig.maimaidata.ui.about

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.utils.UpdateManager
import com.paperpig.maimaidata.utils.setQuickClick
import java.text.SimpleDateFormat
import java.util.Locale

class AboutFragment : PreferenceFragmentCompat() {
    companion object {
        const val PROJECT_URL = "https://github.com/s-yh-china/MaimaiData"
        const val FEEDBACK_URL = "https://github.com/s-yh-china/MaimaiData/issues"
    }

    private val updateManager by lazy {
        UpdateManager(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.abot_preferences, rootKey)

        findPreference<Preference>("version")?.apply {
            summary = BuildConfig.VERSION_NAME
            setQuickClick {
                Toast.makeText(context, getString(R.string.maimai_data_apk_check), Toast.LENGTH_SHORT).show()
                updateManager.checkAppUpdate(true) {}
            }
        }
        findPreference<Preference>("base_data_version")?.apply {
            summary = SpUtil.getDataVersion()
            setQuickClick {
                Toast.makeText(context, getString(R.string.maimai_data_data_check), Toast.LENGTH_SHORT).show()
                updateManager.checkDataUpdate(requireActivity()) {}
            }
        }
        findPreference<Preference>("last_time_update_chart_stats")?.apply {
            summary = SpUtil.getLastUpdateChartStats().let {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it)
            }
            setQuickClick {
                Toast.makeText(context, getString(R.string.maimai_data_chart_stats_check), Toast.LENGTH_SHORT).show()
                updateManager.checkChartStatusUpdate(requireActivity(), true)
            }
        }
        findPreference<Preference>("project_url")?.apply {
            summary = PROJECT_URL
            setOnPreferenceClickListener {
                openUrl(PROJECT_URL)
                true
            }
        }
        findPreference<Preference>("feedback")?.apply {
            summary = FEEDBACK_URL
            setOnPreferenceClickListener {
                openUrl(FEEDBACK_URL)
                true
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW).apply { data = url.toUri() })
    }
}