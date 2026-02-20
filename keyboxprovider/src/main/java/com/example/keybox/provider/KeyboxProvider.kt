package com.example.keybox.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.time.Duration.Companion.minutes

class KeyboxProvider : ContentProvider() {

    private lateinit var client: OkHttpClient
    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var job: Job? = null

    companion object {
        private const val PREFS_NAME = "KeyboxProvider"
        private const val KEY_KEYBOX = "key_keybox"
        private const val KEY_LAST_FETCHED = "key_last_fetched"

        private const val AUTHORITY = "com.example.keybox.provider"
        val CONTENT_URI: Uri = "content://$AUTHORITY/".toUri()
        private const val HTTP_URL =
            "https://raw.githubusercontent.com/KOWX712/Tricky-Addon-Update-Target-List/main/.extra"
        private const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000 // 24 hours
        private var LAST_FETCHED: Long = 0

        @Volatile
        private var KEYBOX: String? = null
    }

    override fun onCreate(): Boolean {
        client = OkHttpClient()
        val prefs = getSharedPreferences()
        KEYBOX = prefs.getString(KEY_KEYBOX, KEYBOX)
        LAST_FETCHED = prefs.getLong(KEY_LAST_FETCHED, LAST_FETCHED)
        return true
    }

    private fun getSharedPreferences(): SharedPreferences =
        requireContext().createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private suspend fun fetchKeybox() {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(HTTP_URL)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use
                    }
                    response.body?.string()?.let { data ->
                        try {
                            val hexBytes =
                                data.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            val decodedHex = String(hexBytes, Charsets.UTF_8)
                            val decodedBytes = Base64.decode(decodedHex, Base64.DEFAULT)

                            // todo verify content
                            KEYBOX = String(decodedBytes, Charsets.US_ASCII)
                            LAST_FETCHED = System.currentTimeMillis()

                            // Save to SharedPreferences
                            val prefs = getSharedPreferences()
                            prefs.edit().apply {
                                putString(KEY_KEYBOX, KEYBOX)
                                putLong(KEY_LAST_FETCHED, LAST_FETCHED)
                                apply()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun shouldRefresh(): Boolean {
        return KEYBOX == null || (System.currentTimeMillis() - LAST_FETCHED) > REFRESH_INTERVAL_MS
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(javaClass.simpleName, "got query $uri")
        if (uri == CONTENT_URI) {
            val matrixCursor = MatrixCursor(arrayOf("value"))
            return KEYBOX?.let {
                matrixCursor.addRow(arrayOf(it))
                matrixCursor
            }
        }
        return null
    }

    override fun refresh(
        uri: Uri,
        extras: Bundle?,
        cancellationSignal: CancellationSignal?
    ): Boolean {
        Log.d(javaClass.simpleName, "refresh $uri")
        if (uri == CONTENT_URI) {
            if (!shouldRefresh() || job != null) return true
            job = scope.launch {
                while (true) {
                    Log.d(javaClass.simpleName, "background fetch")
                    fetchKeybox()
                    if (KEYBOX != null) {
                        Log.d(javaClass.simpleName, "fetch success")
                        job = null
                        return@launch
                    }
                    delay(1.minutes)
                }
            }
            return true
        }
        return false
    }

    override fun getType(uri: Uri): String {
        return "vnd.android.cursor.item/vnd.com.example.string.provider"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}
