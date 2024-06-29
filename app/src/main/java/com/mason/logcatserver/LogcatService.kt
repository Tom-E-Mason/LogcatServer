package com.mason.logcatserver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LogcatService : Service() {

    private val server = Server()
    private val advertiser = Advertiser(Build.MODEL)
    private val _started = MutableStateFlow(false)

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "onStartCommand")
        _started.value = true
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return LogcatServiceClient()
    }

    inner class LogcatServiceClient : Binder() {

        val started: StateFlow<Boolean>
            get() = _started

        fun start() {
            Log.i(TAG, "start")
            startService(Intent(this@LogcatService, LogcatService::class.java))
            server.start()
            advertiser.start()
        }

        fun stop() {
            Log.i(TAG, "stop")
            server.stop()
            advertiser.stop()
            _started.value = false
            stopService(Intent(this@LogcatService, LogcatService::class.java))
        }
    }

    companion object {
        private const val TAG = "LogcatService"
    }
}
