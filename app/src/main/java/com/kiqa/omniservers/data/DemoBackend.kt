package com.kiqa.omniservers.data

import kotlinx.coroutines.delay

/**
 * A pretend desktop, so the app can be tried (and screenshotted) without any server.
 * Starting takes a couple of seconds like the real thing, then the console fills with
 * plausible lines and players come and go.
 */
class DemoBackend(private val clock: () -> Long = { System.currentTimeMillis() / 1000 }) : ServerBackend {

    private class Sim(
        val id: String, val name: String, val type: String, val version: String, val ramMb: Int,
        var status: ServerStatus, var startedAt: Long?, var changedAt: Long,
        val lines: MutableList<ConsoleLine> = mutableListOf(), var nextSeq: Long = 1,
        val maxPlayers: Int = 20, var roster: List<String> = emptyList(),
    )

    private val now get() = clock()

    private val sims = mutableListOf(
        Sim("demo-survival", "Survival with friends", "paper", "1.21.4", 4096, ServerStatus.Running, now - 2 * 3600 - 14 * 60, 0,
            roster = listOf("Kiqa", "Mira_Builds", "Dex")),
        Sim("demo-creative", "Creative plots", "fabric", "1.21.1", 6144, ServerStatus.Stopped, null, 0),
        Sim("demo-pocket", "Pocket edition", "bedrock", "1.21", 2048, ServerStatus.Stopped, null, 0),
    ).also { list ->
        list.first().apply {
            add("[10:02:11] [Server thread/INFO]: Starting minecraft server version 1.21.4")
            add("[10:02:14] [Server thread/INFO]: Preparing spawn area: 100%")
            add("[10:02:15] [Server thread/INFO]: Done (3.412s)! For help, type \"help\"")
            add("[10:14:40] [Server thread/INFO]: Kiqa joined the game")
            add("[10:21:03] [Server thread/WARN]: Can't keep up! Is the server overloaded? Running 2104ms behind")
            add("[10:36:52] [Server thread/INFO]: Mira_Builds joined the game")
        }
    }

    private fun Sim.add(text: String, stream: String = "stdout") { lines += ConsoleLine(nextSeq++, stream, text) }

    /** Moves "Starting"/"Stopping" along once enough time has passed. */
    private fun tick(s: Sim) {
        val elapsed = now - s.changedAt
        if (s.status == ServerStatus.Starting && elapsed >= 3) {
            s.status = ServerStatus.Running
            s.startedAt = now
            s.add("[00:00:03] [Server thread/INFO]: Done (2.9s)! For help, type \"help\"")
            s.roster = if (s.name.startsWith("Creative")) listOf("Dex") else emptyList()
        } else if (s.status == ServerStatus.Stopping && elapsed >= 2) {
            s.status = ServerStatus.Stopped
            s.startedAt = null
            s.roster = emptyList()
            s.add("[Server] Server stopped")
        }
    }

    private fun find(id: String) = sims.first { it.id == id }.also(::tick)

    override suspend fun servers(): List<ServerSummary> {
        delay(60)
        return sims.map { s ->
            tick(s)
            ServerSummary(s.id, s.name, s.type, s.version, s.ramMb, s.status, s.startedAt, needsEula = false)
        }
    }

    override suspend fun start(id: String) {
        val s = find(id)
        if (s.status == ServerStatus.Stopped) {
            s.status = ServerStatus.Starting; s.changedAt = now
            s.add("[Server] Starting ${s.name}…")
        }
    }

    override suspend fun stop(id: String) {
        val s = find(id)
        if (s.status == ServerStatus.Running) {
            s.status = ServerStatus.Stopping; s.changedAt = now
            s.add("> stop")
            s.add("[Server thread/INFO]: Stopping the server")
        }
    }

    override suspend fun console(id: String): List<ConsoleLine> = find(id).lines.takeLast(300)

    override suspend fun send(id: String, line: String) {
        val s = find(id)
        s.add("> $line")
        if (s.status != ServerStatus.Running) return
        when (line.trim().split(" ").first()) {
            "list" -> s.add("[Server thread/INFO]: There are ${s.roster.size} of a max of ${s.maxPlayers} players online: ${s.roster.joinToString(", ")}")
            "save-all" -> s.add("[Server thread/INFO]: Saved the game")
            "say" -> s.add("[Server thread/INFO]: [Server] ${line.removePrefix("say").trim()}")
            else -> s.add("[Server thread/INFO]: Done")
        }
    }

    override suspend fun players(id: String): PlayerList {
        val s = find(id)
        return PlayerList(s.roster.size, s.maxPlayers, s.roster)
    }
}
