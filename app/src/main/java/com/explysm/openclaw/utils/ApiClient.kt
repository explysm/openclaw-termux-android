package com.explysm.openclaw.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun post(url: String, json: String, callback: (Result<String>) -> Unit) {
        val body = json.toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) callback(Result.failure(IOException("Unexpected code $response")))
                    callback(Result.success(response.body!!.string()))
                }
            }
        })
    }

    fun get(url: String, callback: (Result<String>) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) callback(Result.failure(IOException("Unexpected code $response")))
                    callback(Result.success(response.body!!.string()))
                }
            }
        })
    }
}
