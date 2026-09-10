package com.sozo.callmanager.network

import com.sozo.callmanager.data.CallRecord
import com.sozo.callmanager.data.DemoSession
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * TODO: replace BASE_URL with the real Sozo Spring Boot backend, e.g.
 *   https://api.sozo.yourdomain.com/api/mobile/calls
 * This is intentionally a thin OkHttp call (no Retrofit) to keep the demo simple.
 */
object BackendApi {

    private const val BASE_URL = "https://REPLACE_WITH_YOUR_BACKEND/api/mobile/calls"
    private val client = OkHttpClient()

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Failure(val message: String) : SyncResult()
    }

    fun syncCalls(records: List<CallRecord>, callback: (SyncResult) -> Unit) {
        val array = JSONArray()
        records.forEach { r ->
            val obj = JSONObject()
            obj.put("employeeId", DemoSession.employeeId)
            obj.put("customerNumber", r.number)
            obj.put("callType", r.type.name)
            obj.put("startedAtMillis", r.timestampMillis)
            obj.put("durationSeconds", r.durationSeconds)
            array.put(obj)
        }

        val body = array.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer ${DemoSession.authToken ?: ""}")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(SyncResult.Failure(e.message ?: "Network error \u2014 backend not reachable yet"))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    callback(SyncResult.Success(records.size))
                } else {
                    callback(SyncResult.Failure("Server responded ${response.code}"))
                }
                response.close()
            }
        })
    }
}
