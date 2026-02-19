package com.example.keybox.provider

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.UserHandle
import android.util.Log
import kotlin.concurrent.thread

class EntryService: Service() {
    companion object {
        var service: EntryService? = null
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun tryC(fnc: () -> Unit) {
        try {
            fnc()
        } catch(e: Throwable) {
            Log.e(javaClass.simpleName, "Caught", e)
        }
    }

    override fun onCreate() {
        service = this

        thread {
            tryC {
                val cr = contentResolver
                val uri = KeyboxProvider.CONTENT_URI
                cr.refresh(uri, null, null)
            }
        }
    }
}

class Starter: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val caller = UserHandle.getCallingUserId()
        if(caller != 0) {
            Log.d(javaClass.simpleName, "Service called from user none 0, ignore")
            return
        }
        Log.d(javaClass.simpleName, "Starting service")
        //TODO: Check current user == "admin" == 0
        when(intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                context.startServiceAsUser(Intent(context, EntryService::class.java), UserHandle.SYSTEM)
            }
        }
    }
}

interface EntryStartup {
    fun startup(ctxt: Context)
}
