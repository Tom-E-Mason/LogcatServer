package com.mason.logcatserver

import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class Server {

    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private var serverThread: Thread? = null
    private val socketQueue = ArrayBlockingQueue<Socket>(16)
    private val logQueue = ArrayBlockingQueue<String>(2048)

    fun start() {
        Log.i(TAG, "start")
        acceptThread = thread {
            runAcceptThread()
        }
        serverThread = thread {
            runServer()
        }
    }

    private fun runAcceptThread() {

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
        var line = reader.readLine()
        while (line != null) {
            logQueue.put(line)
            line = reader.readLine()
        }
    }

    private fun runServer() {

        thread { runLogcat() }

        val sockets = mutableListOf<Socket>()
        while (true) {
            val line = logQueue.take() + '\n'
            drainSocketQueue(sockets)
            for (socket in sockets) {
                try {
                    socket.getOutputStream().write(line.toByteArray())
                } catch (e: SocketException) {
                    Log.e(TAG, "Exception writing to socket.", e)
                    socket.close()
                }
            }
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