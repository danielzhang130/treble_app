package com.example.keybox.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class Starter: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(javaClass.simpleName, "Starting service")
        when(intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val cr = context.contentResolver
                val uri = KeyboxProvider.CONTENT_URI
                cr.refresh(uri, null, null)
            }
        }
    }
}
