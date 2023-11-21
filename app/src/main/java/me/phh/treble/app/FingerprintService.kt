package me.phh.treble.app

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemProperties
import android.preference.PreferenceManager
import android.util.Log


object FingerprintService: EntryStartup {


    private val spListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when(key) {
            FingerprintSettings.fingerprintKey -> {
                val value = sp.getString(key, null) ?: return@OnSharedPreferenceChangeListener
                SystemProperties.set("persist.fingerprint", value)
                Log.d("PHH", "Setting spoof fingerprint to $value")
            }

            FingerprintSettings.productKey -> {
                val value = sp.getString(key, null) ?: return@OnSharedPreferenceChangeListener
                SystemProperties.set("persist.product", value)
                Log.d("PHH", "Setting spoof product to $value")
            }

            FingerprintSettings.deviceKey -> {
                val value = sp.getString(key, null) ?: return@OnSharedPreferenceChangeListener
                SystemProperties.set("persist.device", value)
                Log.d("PHH", "Setting spoof device to $value")
            }

            FingerprintSettings.manufacturerKey -> {
                val value = sp.getString(key, null) ?: return@OnSharedPreferenceChangeListener
                SystemProperties.set("persist.manufacturer", value)
                Log.d("PHH", "Setting spoof manufacturer to $value")
            }

            FingerprintSettings.brandKey -> {
                val value = sp.getString(key, null) ?: return@OnSharedPreferenceChangeListener
                SystemProperties.set("persist.brand", value)
                Log.d("PHH", "Setting spoof brand to $value")
            }

            FingerprintSettings.modelKey -> {
                val value = sp.getString(key, null) ?: return@OnSharedPreferenceChangeListener
                SystemProperties.set("persist.model", value)
                Log.d("PHH", "Setting spoof model to $value")
            }
        }
    }

    override fun startup(ctxt: Context) {
        if (!FingerprintSettings.enabled()) return

        val sp = PreferenceManager.getDefaultSharedPreferences(ctxt)
        sp.registerOnSharedPreferenceChangeListener(spListener)
    }
}
