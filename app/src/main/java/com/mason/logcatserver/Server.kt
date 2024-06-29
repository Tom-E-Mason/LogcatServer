package com.mason.logcatserver

import android.util.Log
import java.io.IOException
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
    private var logcat: Process? = null

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

    private fun runServer() {

        logcat = Runtime.getRuntime().exec("logcat")
        val input = logcat!!.inputStream

        val sockets = mutableListOf<Socket>()
        val socketsToRemove = mutableListOf<Socket>()

        val buffer = ByteArray(MAX_LOG_LINE)

        while (true) {
            val read = try {
                input.read(buffer)
            } catch (e: IOException) {
                Log.e(TAG, "Error reading from input.", e)
                break
            }

            var lastNewLine = 0
            for (i in 0 until read) {
                if (buffer[i] == '\n'.code.toByte()) {
                    lastNewLine = i
                }
            }

            val limit = lastNewLine + 1

            drainSocketQueue(sockets)
            for (socket in sockets) {
                try {
                    socket.getOutputStream().write(buffer, 0, limit)
                } catch (e: SocketException) {
                    Log.e(TAG, "Exception writing to socket.", e)
                    socket.close()
                    socketsToRemove += socket
                }
            }

            System.arraycopy(buffer, limit, buffer, 0, buffer.size - limit)

            sockets.removeAll(socketsToRemove)
            socketsToRemove.clear()
        }

        sockets.forEach {
            try {
                it.close()
            } catch (e: SocketException) {
                Log.e(TAG, "Failed to close $it.", e)
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

        logcat!!.destroy()
        serverThread!!.join()
    }

    companion object {
        private const val TAG = "Server"
        private const val PORT = 6001
        private const val MAX_LOG_LINE = 4096
    }
}
