package me.phh.treble.app


object XiaomiSettings : Settings {
    val dt2w = "xiaomi_double_tap_to_wake"

    override fun enabled() = Tools.vendorFp.lowercase().startsWith("xiaomi") ||
                             Tools.vendorFp.lowercase().startsWith("redmi/") ||
                             Tools.vendorFp.lowercase().startsWith("poco/")
}

class XiaomiSettingsFragment : SettingsFragment() {
    override val preferencesResId = R.xml.pref_xiaomi
}
