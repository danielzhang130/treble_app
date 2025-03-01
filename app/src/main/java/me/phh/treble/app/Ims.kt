package me.phh.treble.app

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.ServiceManager
import android.os.SystemProperties
import android.preference.PreferenceManager
import android.telephony.TelephonyManager
import android.util.Log
import dalvik.system.PathClassLoader
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object Ims: EntryStartup {
    lateinit var ctxt: WeakReference<Context>

    val networkListener = object: ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i("PHH", "Network $network is available!")
        }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log.i("PHH", "Received info about network $network, got $networkCapabilities")
        }
    }

    fun requestNetwork() {
        val c = ctxt.get() ?: return
        val cm = c.getSystemService(ConnectivityManager::class.java) ?: return

        val nwRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_IMS)
                .build()
        cm.requestNetwork(nwRequest, networkListener)
        registeredNetwork = true
        Log.d("PHH", "Adding \"ims\" APN")

        val tm = c.getSystemService(TelephonyManager::class.java)!!
        val operator = tm.simOperator
        if(tm.simOperator == null || tm.simOperator == "") {
            Log.d("PHH","No current carrier bailing out")
            return
        }

        val mcc = operator.substring(0, 3)
        val mnc = operator.substring(3, operator.length)
        Log.d("PHH", "Got mcc = $mcc and mnc = $mnc")

        val cr = c.contentResolver

        val cursor = cr.query(
                Uri.parse("content://telephony/carriers/current"),
                arrayOf("name", "type", "apn", "carrier_enabled", "edited"),
                "name = ?", arrayOf("PHH IMS"), null
        )

        if(cursor != null && cursor.moveToFirst()) {
            Log.d("PHH", "PHH IMS APN for this provider is already here with data $cursor")
            return
        }
        Log.d("PHH", "No APN called PHH IMS, adding our own")

        val cv = ContentValues()
        cv.put("name", "PHH IMS")
        cv.put("apn", "ims")
        cv.put("type", "ims")
        cv.put("edited", "1")
        cv.put("user_editable", "1")
        cv.put("user_visible", "1")
        cv.put("protocol", "IPV4V6")
        cv.put("roaming_protocol", "IPV6")
        cv.put("modem_cognitive", "1")
        cv.put("numeric", operator)
        cv.put("mcc", mcc)
        cv.put("mnc", mnc)

        val res = cr.insert(Uri.parse("content://telephony/carriers"), cv)
        Log.d("PHH", "Insert APN returned $res")
    }

    var registeredNetwork = false
    val spListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        val c = ctxt.get() ?: return@OnSharedPreferenceChangeListener
        val cm = c.getSystemService(ConnectivityManager::class.java) ?: return@OnSharedPreferenceChangeListener

        when(key) {
            ImsSettings.requestNetwork -> {
                val value = sp.getBoolean(key, false)
                if(value) {
                    requestNetwork()
                } else {
                    if(registeredNetwork) {
                        cm.unregisterNetworkCallback(networkListener)
                        registeredNetwork = false
                    }
                }
            }
            ImsSettings.forceEnableSettings -> {
                val value = if(sp.getBoolean(key, false)) "1" else "0"
                Misc.safeSetprop("persist.dbg.volte_avail_ovr", value)
                Misc.safeSetprop("persist.dbg.wfc_avail_ovr", value)
                Misc.safeSetprop("persist.dbg.allow_ims_off", value)
            }
        }
    }

    val mHidlService = android.hidl.manager.V1_0.IServiceManager.getService()

    val mAllSlots = listOf("imsrild1", "imsrild2", "imsrild3", "slot1", "slot2", "slot3", "imsSlot1", "imsSlot2", "mtkSlot1", "mtkSlot2", "imsradio0", "imsradio1")
    val gotMtkP = mAllSlots
            .find { i -> mHidlService.get("vendor.mediatek.hardware.radio@3.0::IRadio", i) != null } != null
    val gotMtkQ = mAllSlots
            .find { i -> mHidlService.get("vendor.mediatek.hardware.mtkradioex@1.0::IMtkRadioEx", i) != null } != null
    val gotMtkR = mAllSlots
            .find { i -> mHidlService.get("vendor.mediatek.hardware.mtkradioex@2.0::IMtkRadioEx", i) != null } != null
    val gotMtkS = mAllSlots
            .find { i -> mHidlService.get("vendor.mediatek.hardware.mtkradioex@3.0::IMtkRadioEx", i) != null } != null
    val gotMtkAidl = mAllSlots
            .find { i -> ServiceManager.getService("vendor.mediatek.hardware.mtkradioex.ims.IMtkRadioExIms/" + i) != null } != null
    val gotQcomHidl = mAllSlots
            .find { i -> mHidlService.get("vendor.qti.hardware.radio.ims@1.0::IImsRadio", i) != null } != null
    val gotQcomHidlMoto = gotQcomHidl
            && SystemProperties.get("ro.product.vendor.brand", "N/A").equals("motorola")
    val gotQcomAidl = mAllSlots
            .find { i -> ServiceManager.getService("vendor.qti.hardware.radio.ims.IImsRadio/" + i) != null } != null
    val gotSLSI = mAllSlots
            .find { i -> mHidlService.get("vendor.samsung_slsi.telephony.hardware.radio@1.0::IOemSamsungslsi", i) != null } != null
    val gotSPRD = mAllSlots
            .find { i -> mHidlService.get("vendor.sprd.hardware.radio@1.0::IExtRadio", i) != null } != null
    val gotHW = mAllSlots
            .find { i -> mHidlService.get("vendor.huawei.hardware.radio@1.0::IRadio", i) != null } != null

    override fun startup(ctxt: Context) {
        if (!ImsSettings.enabled()) return
        val gotFloss = ctxt.packageManager.getInstalledPackages(0).find { it.packageName == "me.phh.ims" } != null

        val sp = PreferenceManager.getDefaultSharedPreferences(ctxt)
        sp.registerOnSharedPreferenceChangeListener(spListener)

        this.ctxt = WeakReference(ctxt.applicationContext)

        val allOverlays = listOf("me.phh.treble.overlay.mtkims_telephony", "me.phh.treble.overlay.cafims_telephony", "me.phh.treble.overlay.hwims_telephony")
        val selectOverlay = when {
            gotFloss -> "me.phh.treble.overlay.flossims_telephony"
            gotMtkP || gotMtkQ || gotMtkR || gotMtkS || gotMtkAidl -> "me.phh.treble.overlay.mtkims_telephony"
            gotQcomHidl || gotQcomAidl -> "me.phh.treble.overlay.cafims_telephony"
            gotSLSI -> "me.phh.treble.overlay.slsiims_telephony"
            gotSPRD -> "me.phh.treble.overlay.sprdims_telephony"
            gotHW -> "me.phh.treble.overlay.hwims_telephony"
            else -> null
        }
        if (gotFloss) {
            Misc.safeSetprop("persist.sys.phh.ims.floss", "true")
        } else {
            Misc.safeSetprop("persist.sys.phh.ims.floss", "false")
        }
        if(selectOverlay != null) {
            allOverlays
                    .filter { it != selectOverlay }
                    .forEach { OverlayPicker.setOverlayEnabled(it, false) }
            OverlayPicker.setOverlayEnabled(selectOverlay, true)
        }

        requestNetwork()
    }
}
