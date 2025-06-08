package ru.smak.chat

import java.io.File
import java.util.Base64

/**
 * Keeps persistent information about registered users and their undelivered
 * private messages. Data is stored in simple text files so that the server can
 * be restarted without losing information.
 */
object UserRegistry {
    /** File where "login:password" pairs are stored. */
    private val usersFile = File("users.txt")
    /** File with offline messages in format user|from|base64(message). */
    private val offlineFile = File("offline.txt")
    private val users = mutableMapOf<String, String>()
    private val offline = mutableMapOf<String, MutableList<Pair<String,String>>>()

    init {
        // Load registered users
        if (usersFile.exists()) {
            usersFile.forEachLine {
                val parts = it.split(":", limit = 2)
                if (parts.size == 2) users[parts[0]] = parts[1]
            }
        }
        // Load saved offline messages
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
    /** Register a new user. Returns false if login already exists. */
    fun register(login: String, password: String): Boolean {
        val l = login.trim()
        val p = password.trim()
        if (l in users) return false
        users[l] = p
        saveUsers()
        return true
    }

    @Synchronized
    /** Verify user credentials. */
    fun check(login: String, password: String): Boolean {
        val l = login.trim()
        val p = password.trim()
        return users[l] == p
    }

    @Synchronized
    /** Store a private message for delivery when user comes online. */
    fun addOfflineMessage(user: String, from: String, msg: String) {
        offline.computeIfAbsent(user) { mutableListOf() }.add(from to msg)
        saveOffline()
    }

    @Synchronized
    /** Retrieve and remove all queued offline messages for a user. */
    fun popOfflineMessages(user: String): List<Pair<String,String>> {
        val list = offline.remove(user) ?: emptyList()
        saveOffline()
        return list
    }

    /** Persist the user list to disk. */
    private fun saveUsers() {
        usersFile.printWriter().use { pw ->
            users.forEach { (u,p) -> pw.println("$u:$p") }
        }
    }

    /** Persist queued offline messages to disk. */
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
