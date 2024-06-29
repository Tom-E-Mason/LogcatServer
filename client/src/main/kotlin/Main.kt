package com.mason

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.Socket

private const val ADVERTISER_PORT = 6000
private const val SERVER_PORT = 6001
private const val GROUP = "224.0.24.0"

fun main() {
    println("Hello World!")
    val socket = discover() ?: return

    socket.getInputStream().bufferedReader().use {
        var line: String? = it.readLine()
        while (line != null) {
            println(line)
            line = it.readLine()
        }
    }

    println("Socket closed.")
}

fun discover(): Socket? {
    val discoverySocket = MulticastSocket(ADVERTISER_PORT)
    val group = InetAddress.getByName(GROUP)
    discoverySocket.joinGroup(group)

    while (true) {
        val buf = ByteArray(256)
        val packet = DatagramPacket(buf, buf.size)
        discoverySocket.receive(packet)

        val data = String(packet.data.copyOfRange(0, packet.length))
        println("Received: $data from ${packet.socketAddress}.")

        val socket = Socket(packet.address, SERVER_PORT)
        if (!socket.isConnected) {
            println("Socket not connect.")
            return null
        }

        return socket
    }
}
