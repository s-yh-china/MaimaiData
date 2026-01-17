package com.paperpig.maimaidata.ui.about

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.paperpig.maimaidata.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        findPreference<EditTextPreference>("nickname")?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

        findPreference<SwitchPreference>("prober_update_use_api")?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean) {
                if (preference is SwitchPreference) {
                    var timer: CountDownTimer?

                    val dialog = MaterialDialog.Builder(requireContext())
                        .title(R.string.prober_update_api_warning_title)
                        .content(R.string.prober_update_api_warning_content)
                        .positiveText(getString(R.string.prober_update_api_warning_wait_button, 9999))
                        .onPositive { dialog, _ ->
                            preference.isChecked = true
                            dialog.dismiss()
                        }
                        .negativeText(R.string.common_cancel)
                        .onNegative { dialog, _ ->
                            dialog.dismiss()
                        }
                        .cancelable(false)
                        .build()

                    val positiveButton: TextView? = dialog.getActionButton(DialogAction.POSITIVE)
                    positiveButton?.isEnabled = false

                    timer = object : CountDownTimer(10000L, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            val secondsLeft = millisUntilFinished / 1000 + 1
                            positiveButton?.text = getString(R.string.prober_update_api_warning_wait_button, secondsLeft)
                        }

                        override fun onFinish() {
                            positiveButton?.text = getString(R.string.prober_update_api_warning_ok_button)
                            positiveButton?.isEnabled = true
                        }
                    }.start()

                    dialog.setOnDismissListener {
                        timer?.cancel()
                    }

                    dialog.show()
                }
                false
            } else {
                true
            }
        }
    }
}