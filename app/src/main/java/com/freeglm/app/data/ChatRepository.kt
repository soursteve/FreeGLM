package com.freeglm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val CHAT_URL = "https://u1s6661r6b71-d.space-z.ai/api/chat"

/**
 * Talks to the FreeGLM chat endpoint, which streams its reply as
 * Server-Sent Events: lines of `data: {"content":"..."}` terminated by `data: [DONE]`.
 */
class ChatRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamChat(messages: List<ChatMessage>): Flow<String> = callbackFlow {
        val requestBody = json.encodeToString(ChatRequest(messages))
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(CHAT_URL)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(IOException("HTTP ${resp.code}: ${resp.message}"))
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        close(IOException("Empty response body"))
                        return
                    }
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data:")) continue
                            val data = line.removePrefix("data:").trim()
                            if (data.isEmpty() || data == "[DONE]") {
                                if (data == "[DONE]") break
                                continue
                            }
                            val chunk = runCatching {
                                json.decodeFromString<ChatChunk>(data)
                            }.getOrNull() ?: continue
                            if (chunk.error != null) {
                                close(IOException(chunk.error))
                                return
                            }
                            chunk.content?.let { text -> trySend(text) }
                        }
                        close()
                    } catch (e: IOException) {
                        close(e)
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)
}
