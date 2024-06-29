package com.mason.logcatserver

import android.os.SystemClock
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import kotlin.concurrent.thread

class Advertiser(id: String) {

    private var socket: DatagramSocket? = null
    private var thread: Thread? = null
    private val advert = "$ADVERTISEMENT_PREFIX:$id".toByteArray()

    fun start() {

        thread = thread {
            socket = try {
                DatagramSocket(PORT)
            } catch (e: SocketException) {
                Log.e(TAG, "start Failed to create socket.", e)
                return@thread
            }

            socket?.use(::run)
        }
    }

    private fun run(sock: DatagramSocket) {

        val group = InetAddress.getByName(GROUP)
        while (true) {
            Log.i(TAG, "Sending broadcast")
            val packet = DatagramPacket(advert, advert.size, group, PORT)
            try {
                sock.send(packet)
            } catch (e: SocketException) {
                Log.e(TAG, "run Exception writing packet.", e)
                break
            }
            SystemClock.sleep(1000)
        }
    }

    fun stop() {
        socket?.close()
        thread?.let {
            it.interrupt()
            it.join()
        }
    }

    companion object {
        private const val TAG = "Advertiser"
        private const val PORT = 6000
        private const val GROUP = "224.0.24.0"
        private const val ADVERTISEMENT_PREFIX = "com.mason.logcatserver"
    }
}
