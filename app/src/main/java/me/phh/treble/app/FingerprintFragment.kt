package me.phh.treble.app

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager


object FingerprintSettings : Settings {
    const val fingerprintKey = "key_fingerprint"
    const val productKey = "key_product"
    const val deviceKey = "key_device"
    const val manufacturerKey = "key_manufacturer"
    const val brandKey = "key_brand"
    const val modelKey = "key_model"

    override fun enabled() = true
}

data class FpData(
    val fingerprint: String, val product: String, val device: String,
    val manufacture: String, val brand: String, val model: String
)

class FingerprintFragment : SettingsFragment() {
    override val preferencesResId = R.xml.pref_fingerprint

    private val fields = mutableListOf<EditTextPreference>()
    private val fps = mutableListOf<FpData>()
    private lateinit var currentFp: FpData
    private lateinit var pref: SharedPreferences
    private lateinit var handler: Handler

    companion object {
        private const val REQUEST_CODE = 5
        private const val FP_PREF_KEY = "FP_PREF_KEY"
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
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

        findPreference<Preference>("key_test_fp")?.setOnPreferenceClickListener {
            testNextFp()
            true
        }
        findPreference<Preference>("key_reset")?.setOnPreferenceClickListener {
            pref.edit()
                .putStringSet(FP_PREF_KEY, emptySet())
                .apply()
            true
        }
        fps.addAll(readFp())
        pref = PreferenceManager
            .getDefaultSharedPreferences(context)
        handler = Handler(Looper.getMainLooper())
    }

    private fun FingerprintFragment.testNextFp() {
        @Suppress("DEPRECATION")
        nextFp()?.let {
            currentFp = it

            fields[0].text = it.fingerprint
            fields[0].summary = it.fingerprint
            fields[1].text = it.product
            fields[1].summary = it.product
            fields[2].text = it.device
            fields[2].summary = it.device
            fields[3].text = it.manufacture
            fields[3].summary = it.manufacture
            fields[4].text = it.brand
            fields[4].summary = it.brand
            fields[5].text = it.model
            fields[5].summary = it.model

            startTestActivity()
        } ?: Toast.makeText(context, "No more fp to test", Toast.LENGTH_SHORT).show()
    }

    private fun nextFp(): FpData? {
        val set = pref.getStringSet(FP_PREF_KEY, mutableSetOf())!!
        val tmp = ArrayList(fps)
        tmp.shuffle()
        return tmp.find {
            it.fingerprint.length <= 91 && it.fingerprint !in set
        }
    }

    private fun restartGms() {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE)!!
        val forceStopPackage = am.javaClass.getDeclaredMethod(
            "forceStopPackage",
            String::class.java
        )
        forceStopPackage.isAccessible = true
        forceStopPackage.invoke(am, "com.google.android.gms")
    }

    private fun readFp(): List<FpData> {
        @Suppress("DEPRECATION")
        return context.resources.openRawResource(R.raw.fp).bufferedReader()
            .lines()
            .toArray()
            .toList()
            .filterIsInstance<String>()
            .mapNotNull {
                val split = it.split('\t')
                if (split.size != 6) return@mapNotNull null
                FpData(
                    split[0],
                    split[1],
                    split[2],
                    split[3],
                    split[4],
                    split[5],
                )
            }
    }

    private fun startTestActivity() {
        restartGms()
        val intent = Intent("my.custom.verify")
            .setComponent(
                ComponentName.createRelative(
                    "com.henrikherzig.playintegritychecker",
                    ".MainActivity"
                )
            )
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            when (resultCode) {
                1, 2 -> {
                    AlertDialog.Builder(context)
                        .setTitle("Success")
                        .create()
                        .show()
                }
                -1 -> {
                    handler.postDelayed(::startTestActivity, 30_000)
                }
                else -> {
                    val set = mutableSetOf<String>()
                    set.addAll(pref.getStringSet(FP_PREF_KEY, mutableSetOf())!!)
                    set.add(currentFp.fingerprint)
                    pref.edit()
                        .putStringSet(FP_PREF_KEY, set)
                        .apply()
                    testNextFp()
                }
            }
        }
    }
}
