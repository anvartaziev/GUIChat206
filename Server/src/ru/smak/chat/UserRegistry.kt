package ru.smak.chat

import java.io.File
import java.util.Base64

object UserRegistry {
    private val usersFile = File("users.txt")
    private val offlineFile = File("offline.txt")
    private val users = mutableMapOf<String, String>()
    private val offline = mutableMapOf<String, MutableList<Pair<String,String>>>()

    init {
        if (usersFile.exists()) {
            usersFile.forEachLine {
                val parts = it.split(":", limit = 2)
                if (parts.size == 2) users[parts[0]] = parts[1]
            }
        }
        if (offlineFile.exists()) {
            offlineFile.forEachLine {
                val parts = it.split("|", limit = 3)
                if (parts.size == 3) {
                    val msg = String(Base64.getDecoder().decode(parts[2]))
                    offline.computeIfAbsent(parts[0]) { mutableListOf() }.
                        add(parts[1] to msg)
                }
            }
        }
    }

    @Synchronized
    fun register(login: String, password: String): Boolean {
        if (login in users) return false
        users[login] = password
        saveUsers()
        return true
    }

    @Synchronized
    fun check(login: String, password: String): Boolean =
        users[login] == password

    @Synchronized
    fun addOfflineMessage(user: String, from: String, msg: String) {
        offline.computeIfAbsent(user) { mutableListOf() }.add(from to msg)
        saveOffline()
    }

    @Synchronized
    fun popOfflineMessages(user: String): List<Pair<String,String>> {
        val list = offline.remove(user) ?: emptyList()
        saveOffline()
        return list
    }

    private fun saveUsers() {
        usersFile.printWriter().use { pw ->
            users.forEach { (u,p) -> pw.println("$u:$p") }
        }
    }

    private fun saveOffline() {
        offlineFile.printWriter().use { pw ->
            offline.forEach { (u, msgs) ->
                msgs.forEach { (from, msg) ->
                    val enc = Base64.getEncoder().encodeToString(msg.toByteArray())
                    pw.println("$u|$from|$enc")
                }
            }
        }
    }
}
