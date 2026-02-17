package me.phh.treble.app

import android.content.ContentProvider
import android.content.ContentValues
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.time.Duration.Companion.minutes

class StringProvider : ContentProvider() {

    private lateinit var client: OkHttpClient
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val AUTHORITY = "me.phh.treble.app.stringprovider"
        val CONTENT_URI: Uri = "content://$AUTHORITY/string".toUri()
        private const val HTTP_URL =
            "https://raw.githubusercontent.com/KOWX712/Tricky-Addon-Update-Target-List/main/.extra"
        private const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000 // 24 hours
        private var LAST_FETCHED: Long = 0

        @Volatile
        private var CACHED_STRING: String? = null
    }

    override fun onCreate(): Boolean {
        client = OkHttpClient()
        return true
    }

    private suspend fun fetchString() {
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
                            CACHED_STRING = String(decodedBytes, Charsets.US_ASCII)
                            LAST_FETCHED = System.currentTimeMillis()
                            context?.contentResolver?.notifyChange(CONTENT_URI, null)
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
        return CACHED_STRING == null || (System.currentTimeMillis() - LAST_FETCHED) > REFRESH_INTERVAL_MS
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        if (uri == CONTENT_URI) {
            val matrixCursor = MatrixCursor(arrayOf("value"))
            return CACHED_STRING?.let {
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
        if (uri == CONTENT_URI) {
            if (!shouldRefresh()) return true
            if (CACHED_STRING == null) {
                Log.d(javaClass.simpleName, "background fetch")
                scope.launch {
                    while (CACHED_STRING == null) {
                        delay(1.minutes)
                        Log.d(javaClass.simpleName, "background fetch again")
                        fetchString()
                    }
                }
            }
            return true
        }
        return false
    }

    override fun getType(uri: Uri): String {
        return "vnd.android.cursor.item/vnd.me.phh.treble.app.stringprovider.string"
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
