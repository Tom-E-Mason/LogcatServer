package com.mason.logcatserver

import android.os.SystemClock
import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.time.LocalDateTime
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class Server {

    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private var logcatThread: Thread? = null
    private val socketQueue = ArrayBlockingQueue<Socket>(16)

    fun start() {
        Log.i(TAG, "start")
        acceptThread = thread {
            runServer()
        }
        logcatThread = thread {
            runLogcat()
        }
    }

    private fun runServer() {

        serverSocket = ServerSocket(PORT)
        if (!serverSocket!!.isBound) {
            serverSocket!!.bind(null)
        }
        while (true) {

            val socket = try {
                serverSocket!!.accept()
            } catch (e: SocketException) {
                Log.e(TAG, "runServer Exception waiting for accept.", e)
                break
            }

            if (!socketQueue.offer(socket)) {
                Log.e(TAG, "Failed to queue socket.")
                socket.close()
            }
        }
    }

    private fun runLogcat() {

        val logcat = Runtime.getRuntime().exec("logcat")
        val reader = logcat.inputStream.bufferedReader()

        val sockets = mutableListOf<Socket>()
        while (true) {
            drainSocketQueue(sockets)

            val line = reader.readLine() + '\n'

            for (socket in sockets) {
                try {
                    socket.getOutputStream().write(line.toByteArray())
                } catch (e: SocketException) {
                    Log.e(TAG, "Exception writing to socket.", e)
                    socket.close()
                }
            }

            SystemClock.sleep(1000)
        }
    }

    private fun drainSocketQueue(sockets: MutableList<Socket>) {
        var socket = socketQueue.poll()
        while (socket != null) {
            sockets.add(socket)
            Log.i(TAG, "drainSocketQueue New socket in logcat thread: $socket.")
            socket = socketQueue.poll()
        }
    }

    fun stop() {
        Log.i(TAG, "stop")
        serverSocket!!.close()
        acceptThread!!.join()
    }

    companion object {
        private const val TAG = "Server"
        private const val PORT = 6001
    }
}