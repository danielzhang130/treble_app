package me.phh.treble.app

import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference


object FingerprintSettings : Settings {
    val fingerprintKey = "key_fingerprint"
    val productKey = "key_product"
    val deviceKey = "key_device"
    val manufacturerKey = "key_manufacturer"
    val brandKey = "key_brand"
    val modelKey = "key_model"

    override fun enabled() = true
}

class FingerprintFragment : SettingsFragment() {
    override val preferencesResId = R.xml.pref_fingerprint

    private val fields = mutableListOf<EditTextPreference>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        fields.addAll(
            listOf<EditTextPreference>(
                findPreference(FingerprintSettings.fingerprintKey) ?: return,
                findPreference(FingerprintSettings.productKey) ?: return,
                findPreference(FingerprintSettings.deviceKey) ?: return,
                findPreference(FingerprintSettings.manufacturerKey) ?: return,
                findPreference(FingerprintSettings.brandKey) ?: return,
                findPreference(FingerprintSettings.modelKey) ?: return
            )
        )
        fields.forEach(SettingsActivity::bindPreferenceSummaryToValue)

        findPreference<Preference>("key_restart_gms")?.setOnPreferenceClickListener {
            restartGms()
            true
        }
    }

    private fun restartGms() {
        try {
            Runtime.getRuntime().exec("am force-stop com.google.android.gms").waitFor()
        } catch (t: Throwable) {
            Log.d("PHH", "Failed to restart GMS")
        }
    }
}
