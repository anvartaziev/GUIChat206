package ru.smak.chat

import java.io.PrintWriter
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.*

class Communicator(
    private val socket: AsynchronousSocketChannel
) {
    var isRunning = false
        private set
    private var parse: ((String)->Unit)? = null
    private val communicatorScope = CoroutineScope(Dispatchers.IO)

    private fun startMessageAccepting(){
        communicatorScope.launch {
            while (isRunning){
                try {
                    var capacity = Int.SIZE_BYTES
                    repeat(2) {
                        val buf = ByteBuffer.allocate(capacity)
                        suspendCoroutine {
                            socket.read(buf, null, ActionCompletionHandler(it))
                        }
                        buf.flip()
                        if (it == 0) capacity = buf.getInt()
                        else {
                            val message = Charsets.UTF_8.decode(buf).toString()
                            parse?.invoke(message)
                        }
                        buf.clear()
                    }
                } catch (_: Throwable){
                    break
                }
            }
        }
    }

    suspend fun sendMessage(message: String){
        val ba = message.toByteArray()
        val buf = ByteBuffer.allocate(ba.size + Int.SIZE_BYTES)
        buf.putInt(ba.size)
        buf.put(ba)
        buf.flip()
        suspendCoroutine {
            socket.write(buf, null, ActionCompletionHandler(it))
        }
        buf.clear()
    }

    fun start(parser: (String)->Unit){
        if (!socket.isOpen) throw Exception("Connection closed")
        parse = parser
        if (!isRunning){
            isRunning = true
            startMessageAccepting()
        }
    }

    fun stop(){
        isRunning = false
        socket.close()
    }

}