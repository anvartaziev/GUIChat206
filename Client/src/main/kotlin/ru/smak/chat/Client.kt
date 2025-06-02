package ru.smak.chat

import kotlinx.coroutines.runBlocking
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import javax.annotation.processing.Messager
import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine

class Client(
    val host: String,
    val port: Int,
) {
    private val socket = AsynchronousSocketChannel.open()
    private val communicator = Communicator(socket)

    init {
        runBlocking {
            suspendCoroutine<Void> {
                socket.connect(
                    InetSocketAddress(host, port),
                    null, ActionCompletionHandler(it)
                )
            }
            communicator.start(::parse)

            while (communicator.isRunning) {
                val userScanner = Scanner(System.`in`)
                val userInput = userScanner.nextLine()
                if (userInput.isNotBlank())
                    communicator.sendMessage(userInput)
                else
                    stop()
            }
        }
    }

    private fun parse(message: String){
        println(message)
    }

    fun stop(){
        communicator.stop()
    }
}