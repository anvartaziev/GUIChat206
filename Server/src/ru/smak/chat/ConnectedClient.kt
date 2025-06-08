package ru.smak.chat

import kotlinx.coroutines.*
import java.nio.channels.AsynchronousSocketChannel

class ConnectedClient(private val socket: AsynchronousSocketChannel) {

    private val communicator = Communicator(socket)
    private var userName: String? = null
    private val clientScope = CoroutineScope(Dispatchers.IO)

    init {
        communicator.start { message -> parse(message) }
    }

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
                parts.getOrNull(1)?.let { sendToAll("MSG $u $it", true) }
            }
            "LIST" -> {
                sendUserList()
            }
            "LOGOUT" -> {
                stop()
            }
        }
    }

    private fun sendOffline(){
        val u = userName ?: return
        UserRegistry.popOfflineMessages(u).forEach { (from,msg) ->
            send("MSG $from $msg")
        }
    }

    fun send(msg: String){
        clientScope.launch { communicator.sendMessage(msg) }
    }

    fun stop(){
        communicator.stop()
        userName?.let {
            connectedClients.remove(it)
            updateUserLists()
        }
    }

    private fun sendPrivate(from: String, to: String, msg: String){
        val rcpt = connectedClients[to]
        if (rcpt != null){
            rcpt.send("MSG $from $msg")
        } else {
            UserRegistry.addOfflineMessage(to, from, msg)
        }
    }

    private fun sendToAll(message: String, includeSelf: Boolean = false){
        connectedClients.values.forEach {
            if (includeSelf || it != this) it.send(message)
        }
    }

    private fun sendUserList(){
        val list = connectedClients.keys.joinToString(",")
        send("USERS $list")
    }

    private fun updateUserLists(){
        connectedClients.values.forEach { it.sendUserList() }
    }

    companion object {
        private val connectedClients = mutableMapOf<String, ConnectedClient>()
    }
}
