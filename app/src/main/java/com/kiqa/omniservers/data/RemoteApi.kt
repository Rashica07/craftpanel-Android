package com.kiqa.omniservers.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the Remote API that OMNIServers on Mac (port 8642) exposes:
 * `GET /api/servers`, `POST /api/servers/{id}/start|stop`, `GET|POST …/console`, `GET …/players`.
 * Plain HTTP with a bearer token, no library needed.
 */
class RemoteApi(private val pairing: Pairing) : ServerBackend {

    private val base get() = "http://${pairing.host}:${pairing.port}"

    override suspend fun servers(): List<ServerSummary> {
        val arr = JSONObject(call("GET", "/api/servers")).getJSONArray("servers")
        return List(arr.length()) { parseServer(arr.getJSONObject(it)) }
    }

    override suspend fun start(id: String) { call("POST", "/api/servers/$id/start") }
    override suspend fun stop(id: String) { call("POST", "/api/servers/$id/stop") }

    override suspend fun console(id: String): List<ConsoleLine> {
        val arr = JSONArray(call("GET", "/api/servers/$id/console"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            ConsoleLine(o.optLong("seq"), o.optString("stream", "stdout"), o.optString("text"))
        }
    }

    override suspend fun send(id: String, line: String) {
        call("POST", "/api/servers/$id/console", JSONObject().put("line", line).toString())
    }

    override suspend fun players(id: String): PlayerList {
        val o = JSONObject(call("GET", "/api/servers/$id/players"))
        val names = o.optJSONArray("players") ?: JSONArray()
        return PlayerList(o.optInt("online"), o.optInt("max"), List(names.length()) { names.getString(it) })
    }

    private suspend fun call(method: String, path: String, body: String? = null): String =
        withContext(Dispatchers.IO) {
            val conn = try {
                (URL(base + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 4_000
                    readTimeout = 8_000
                    setRequestProperty("Authorization", "Bearer ${pairing.token}")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.use { it.write(body.toByteArray()) }
                    }
                }
            } catch (e: IOException) {
                throw ApiError.Unreachable(e)
            }
            try {
                val code = try { conn.responseCode } catch (e: IOException) { throw ApiError.Unreachable(e) }
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                when {
                    code in 200..299 -> text
                    code == 401 -> throw ApiError.Unauthorized()
                    else -> throw ApiError.Http(code, errorMessage(text, code))
                }
            } finally {
                conn.disconnect()
            }
        }

    companion object {
        internal fun parseServer(o: JSONObject) = ServerSummary(
            id = o.getString("id"),
            name = o.getString("name"),
            type = o.optString("server_type", "vanilla"),
            mcVersion = o.optString("mc_version").takeIf { it.isNotEmpty() && !o.isNull("mc_version") },
            ramMb = o.optInt("ram_mb"),
            status = ServerStatus.parse(o.optString("status")),
            startedAt = if (o.isNull("started_at")) null else o.optLong("started_at"),
            needsEula = o.optBoolean("needs_eula"),
        )

        private fun errorMessage(text: String, code: Int): String =
            runCatching { JSONObject(text).getString("error") }.getOrNull() ?: "Request failed ($code)"
    }
}
