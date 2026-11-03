package com.kiqa.omniservers.data

enum class ServerStatus {
    Stopped, Starting, Running, Stopping, Crashed;

    val isBusy get() = this == Starting || this == Stopping
    val isOn get() = this == Running || this == Starting

    companion object {
        /** The desktop apps send lower-case words; anything unknown is shown as stopped. */
        fun parse(raw: String?): ServerStatus = when (raw?.lowercase()) {
            "starting" -> Starting
            "running" -> Running
            "stopping" -> Stopping
            "crashed" -> Crashed
            else -> Stopped
        }
    }
}

data class ServerSummary(
    val id: String,
    val name: String,
    val type: String,
    val mcVersion: String?,
    val ramMb: Int,
    val status: ServerStatus,
    /** Unix seconds, null while the server is off. */
    val startedAt: Long?,
    val needsEula: Boolean,
) {
    /** "Paper 1.21.4" or just "Paper" when the version is unknown. */
    val software: String
        get() = type.replaceFirstChar { it.uppercase() } + (mcVersion?.let { " $it" } ?: "")

    val ramLabel: String
        get() = if (ramMb % 1024 == 0) "${ramMb / 1024} GB" else "$ramMb MB"
}

data class ConsoleLine(val seq: Long, val stream: String, val text: String) {
    val level: LogLevel get() = LogLevel.of(text, stream)
}

enum class LogLevel {
    Info, Warn, Error, Command;

    companion object {
        fun of(text: String, stream: String): LogLevel = when {
            text.startsWith(">") -> Command
            "ERROR]" in text || "/ERROR" in text || "Exception" in text || stream == "stderr" -> Error
            "WARN]" in text || "/WARN" in text -> Warn
            else -> Info
        }
    }
}

data class PlayerList(val online: Int, val max: Int, val players: List<String>) {
    companion object { val Empty = PlayerList(0, 0, emptyList()) }
}

/** Everything the app needs to reach a desktop OMNIServers. */
data class Pairing(val host: String, val port: Int, val token: String) {
    val label get() = "$host:$port"

    companion object {
        /**
         * Reads the JSON the desktop app shows for pairing: {"host":"…","port":8642,"token":"cp_…"}.
         * Returns null for anything that is not that, so the UI can say "that doesn't look right".
         */
        fun parse(text: String): Pairing? = runCatching {
            val o = org.json.JSONObject(text.trim())
            val host = o.getString("host").trim()
            val port = o.optInt("port", 8642)
            val token = o.getString("token").trim()
            if (host.isEmpty() || token.isEmpty() || port !in 1..65535) null else Pairing(host, port, token)
        }.getOrNull()
    }
}

sealed class ApiError(message: String) : Exception(message) {
    class Unreachable(cause: Throwable?) : ApiError("Can't reach your computer") { init { initCause(cause) } }
    class Unauthorized : ApiError("The pairing token was rejected")
    class Http(val code: Int, msg: String) : ApiError(msg)
}

/** What the screens talk to: a real desktop over HTTP, or the built-in demo. */
interface ServerBackend {
    suspend fun servers(): List<ServerSummary>
    suspend fun start(id: String)
    suspend fun stop(id: String)
    suspend fun console(id: String): List<ConsoleLine>
    suspend fun send(id: String, line: String)
    suspend fun players(id: String): PlayerList
}
