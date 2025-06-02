package ru.smak.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import kotlin.coroutines.suspendCoroutine

class Client(
    val host: String,
    val port: Int,
) {
    private val socket = AsynchronousSocketChannel.open()
    private val communicator = Communicator(socket)
    private val clientScope = CoroutineScope(Dispatchers.IO)
    private val messageListeners = mutableListOf<(String)->Unit>()
    fun addMessageListener(listener: (String)->Unit){
        messageListeners.add(listener)
    }
    fun removeMessageListener(listener: (String)->Unit){
        messageListeners.remove(listener)
    }

    init {
        runBlocking {
            suspendCoroutine<Void> {
                socket.connect(
                    InetSocketAddress(host, port),
                    null, ActionCompletionHandler(it)
                )
            }
            communicator.start(::parse)
        }
    }

    private fun parse(message: String){
        messageListeners.forEach { it(message) }
    }

    fun stop(){
        communicator.stop()
    }
    fun sendMessage(message: String)=clientScope.launch {
        if (message.isNotBlank())
            communicator.sendMessage(message)
    }
}