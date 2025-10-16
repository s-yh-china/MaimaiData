package com.paperpig.maimaidata.ui.about

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.utils.SpUtil

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        findPreference<EditTextPreference>("nickname")?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

        val cleanUserIdPreference = findPreference<Preference>("clear_user_id_and_rebind")
        val proberUpdateUseApi = findPreference<SwitchPreference>("prober_update_use_api")

        cleanUserIdPreference?.isVisible = proberUpdateUseApi?.isChecked == true
        if (SpUtil.getUserId().isNullOrEmpty()) {
            cleanUserIdPreference?.summary = getString(R.string.settings_clear_user_id_not_bind)
            cleanUserIdPreference?.isEnabled = false
        } else {
            cleanUserIdPreference?.summary = getString(R.string.settings_clear_user_id_bind)
            cleanUserIdPreference?.isEnabled = true
        }
        cleanUserIdPreference?.setOnPreferenceClickListener { preference ->
            SpUtil.saveUserId("")
            Toast.makeText(requireContext(), "UserID已清除", Toast.LENGTH_SHORT).show()
            preference.summary = getString(R.string.settings_clear_user_id_not_bind)
            preference.isEnabled = false
            true
        }

        proberUpdateUseApi?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean) {
                if (preference is SwitchPreference) {
                    var timer: CountDownTimer?

                    val dialog = MaterialDialog.Builder(requireContext())
                        .title(R.string.prober_update_api_warning_title)
                        .content(R.string.prober_update_api_warning_content)
                        .positiveText(getString(R.string.prober_update_api_warning_wait_button, 9999))
                        .onPositive { dialog, which ->
                            preference.isChecked = true
                            cleanUserIdPreference?.isVisible = true
                            dialog.dismiss()
                        }
                        .negativeText(R.string.common_cancel)
                        .onNegative { dialog, which ->
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
                cleanUserIdPreference?.isVisible = false
                true
            }
        }
    }
}