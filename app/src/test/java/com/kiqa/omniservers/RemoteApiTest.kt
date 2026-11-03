package com.kiqa.omniservers

import com.kiqa.omniservers.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

/** Runs the client against a tiny fake of the Mac's API, same paths and JSON shapes. */
class RemoteApiTest {
    private lateinit var socket: ServerSocket
    private val seen = mutableListOf<String>()
    private var lastBody = ""
    private val goodToken = "cp_good"

    private fun api(token: String = goodToken) = RemoteApi(Pairing("127.0.0.1", socket.localPort, token))

    private fun route(method: String, path: String, auth: String?): Pair<Int, String> = when {
        auth != "Bearer $goodToken" -> 401 to """{"error":"unauthorized"}"""
        path == "/api/servers" -> 200 to """{"servers":[
            {"id":"a1","name":"Survival","server_type":"paper","mc_version":"1.21.4","ram_mb":4096,"status":"running","pid":123,"started_at":1700000000,"needs_eula":false},
            {"id":"b2","name":"Fresh","server_type":"vanilla","mc_version":null,"ram_mb":2048,"status":"stopped","pid":null,"started_at":null,"needs_eula":true}]}"""
        path.endsWith("/console") && method == "GET" -> 200 to """[{"server_id":"a1","seq":5,"stream":"stdout","level":"stdout","text":"hello"},{"server_id":"a1","seq":6,"stream":"stderr","level":"stderr","text":"oops"}]"""
        path.endsWith("/console") -> 200 to """{"ok":true}"""
        path.endsWith("/players") -> 200 to """{"online":2,"max":20,"players":["Kiqa","Dex"]}"""
        path.endsWith("/start") -> 400 to """{"error":"EULA not accepted"}"""
        path.endsWith("/stop") -> 200 to """{"ok":true}"""
        else -> 404 to """{"error":"not found"}"""
    }

    @Before fun up() {
        socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val c = try { socket.accept() } catch (e: Exception) { return@thread }
                c.use {
                    val input = it.getInputStream().buffered()
                    fun line(): String {
                        val sb = StringBuilder()
                        while (true) { val b = input.read(); if (b < 0 || b == '\n'.code) break; if (b != '\r'.code) sb.append(b.toChar()) }
                        return sb.toString()
                    }
                    val (method, path) = line().split(" ").let { p -> p[0] to p[1] }
                    val headers = mutableMapOf<String, String>()
                    while (true) { val h = line(); if (h.isEmpty()) break; headers[h.substringBefore(":").lowercase()] = h.substringAfter(":").trim() }
                    val len = headers["content-length"]?.toInt() ?: 0
                    lastBody = String(ByteArray(len).also { buf -> var n = 0; while (n < len) n += input.read(buf, n, len - n) })
                    seen += "$method $path"
                    val (code, body) = route(method, path, headers["authorization"])
                    val bytes = body.toByteArray()
                    it.getOutputStream().apply {
                        write("HTTP/1.1 $code X\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes); flush()
                    }
                }
            }
        }
    }

    @After fun down() { socket.close() }

    @Test fun listsServersWithAllFields() = runBlocking {
        val list = api().servers()
        assertEquals(2, list.size)
        val a = list[0]
        assertEquals("a1", a.id); assertEquals("Survival", a.name); assertEquals("paper", a.type)
        assertEquals("1.21.4", a.mcVersion); assertEquals(4096, a.ramMb)
        assertEquals(ServerStatus.Running, a.status); assertEquals(1700000000L, a.startedAt)
        val b = list[1]
        assertNull(b.mcVersion); assertNull(b.startedAt); assertTrue(b.needsEula)
        assertEquals(ServerStatus.Stopped, b.status)
    }

    @Test fun sendsTheBearerToken() = runBlocking {
        api().servers()
        assertEquals(listOf("GET /api/servers"), seen)
    }

    @Test fun wrongTokenIsUnauthorized() {
        try { runBlocking { api("nope").servers() }; fail("should throw") }
        catch (e: ApiError.Unauthorized) { /* expected */ }
    }

    @Test fun readsConsoleAndPlayers() = runBlocking {
        val lines = api().console("a1")
        assertEquals(listOf(5L, 6L), lines.map { it.seq })
        assertEquals(LogLevel.Error, lines[1].level)
        val p = api().players("a1")
        assertEquals(PlayerList(2, 20, listOf("Kiqa", "Dex")), p)
    }

    @Test fun sendPostsAJsonLine() = runBlocking {
        api().send("a1", "say hi")
        assertEquals("POST /api/servers/a1/console", seen.last())
        assertEquals("say hi", org.json.JSONObject(lastBody).getString("line"))
    }

    @Test fun stopWorks() = runBlocking {
        api().stop("a1")
        assertEquals("POST /api/servers/a1/stop", seen.last())
    }

    @Test fun serverErrorsCarryTheDesktopMessage() {
        try { runBlocking { api().start("a1") }; fail("should throw") }
        catch (e: ApiError.Http) { assertEquals(400, e.code); assertEquals("EULA not accepted", e.message) }
    }

    @Test fun deadPortIsUnreachable() {
        val closed = RemoteApi(Pairing("127.0.0.1", 1, "x"))
        try { runBlocking { closed.servers() }; fail("should throw") }
        catch (e: ApiError.Unreachable) { /* expected */ }
    }
}
