package ru.smak.chat

import kotlinx.coroutines.*
import java.nio.channels.AsynchronousSocketChannel

/**
 * Represents a single connected client and handles all communication with it.
 */
class ConnectedClient(private val socket: AsynchronousSocketChannel) {

    private val communicator = Communicator(socket)
    private var userName: String? = null
    private val clientScope = CoroutineScope(Dispatchers.IO)

    init {
        // Start listening for incoming messages from this client
        communicator.start { message -> parse(message) }
    }

    /**
     * Parse a single command from the client and execute it.
     */
    private fun parse(message: String) {
        val parts = message.trim().split(" ", limit = 2)
        when(parts[0]){
            "REGISTER" -> {
                val p = parts.getOrNull(1)?.split(" ", limit = 2)
                if (p != null && p.size == 2){
                    if (UserRegistry.register(p[0], p[1])) {
                        userName = p[0]
                        connectedClients[p[0]] = this
                        send("LOGINOK")
                        sendOffline()
                        updateUserLists()
                    } else send("LOGINFAIL already_registered")
                } else send("LOGINFAIL bad_format")
            }
            "LOGIN" -> {
                val p = parts.getOrNull(1)?.split(" ", limit = 2)
                if (p != null && p.size == 2){
                    if (UserRegistry.check(p[0], p[1]) && !connectedClients.containsKey(p[0])) {
                        userName = p[0]
                        connectedClients[p[0]] = this
                        send("LOGINOK")
                        sendOffline()
                        updateUserLists()
                    } else send("LOGINFAIL wrong")
                } else send("LOGINFAIL bad_format")
            }
            "MSG" -> {
                val u = userName ?: return
                val p = parts.getOrNull(1)?.split(" ", limit = 2) ?: return
                if (p.size == 2) sendPrivate(u, p[0], p[1])
            }
            "BROADCAST" -> {
                val u = userName ?: return
                // Broadcast to other users; we already display our own message locally
                parts.getOrNull(1)?.let { sendToAll("MSG $u $it", false) }
            }
            "LIST" -> {
                sendUserList()
            }
            "LOGOUT" -> {
                stop()
            }
        }
    }

    /** Send all queued offline messages to the client once they log in. */
    private fun sendOffline(){
        val u = userName ?: return
        UserRegistry.popOfflineMessages(u).forEach { (from,msg) ->
            send("MSG $from $msg")
        }
    }

    /** Enqueue a message to be sent asynchronously. */
    fun send(msg: String){
        clientScope.launch { communicator.sendMessage(msg) }
    }

    /** Stop communication and remove client from the list of connected users. */
    fun stop(){
        communicator.stop()
        userName?.let {
            connectedClients.remove(it)
            updateUserLists()
        }
    }

    /**
     * Deliver a private message. If recipient is offline it will be saved for
     * later delivery.
     */
    private fun sendPrivate(from: String, to: String, msg: String){
        val rcpt = connectedClients[to]
        if (rcpt != null){
            rcpt.send("MSG $from $msg")
        } else {
            UserRegistry.addOfflineMessage(to, from, msg)
        }
    }

    /** Broadcast a message to all connected clients. */
    private fun sendToAll(message: String, includeSelf: Boolean = false){
        connectedClients.values.forEach {
            if (includeSelf || it != this) it.send(message)
        }
    }

    /** Send list of currently connected users to this client. */
    private fun sendUserList(){
        val list = connectedClients.keys.joinToString(",")
        send("USERS $list")
    }

    /** Inform every client that the set of online users has changed. */
    private fun updateUserLists(){
        connectedClients.values.forEach { it.sendUserList() }
    }

    companion object {
        private val connectedClients = mutableMapOf<String, ConnectedClient>()
    }
}
