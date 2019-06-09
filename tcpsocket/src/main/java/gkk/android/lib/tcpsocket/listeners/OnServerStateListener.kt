package gkk.android.lib.tcpsocket.listeners

import java.net.Socket

interface OnServerStateListener {

    fun onStarted(port: Int)

    fun onStopped()

    fun onClientConnected(socket: Socket)

    fun onClientDisconnected(ip: String)

    fun onClientError(socket: Socket, error: Exception)

    fun onError(error: Exception)
}