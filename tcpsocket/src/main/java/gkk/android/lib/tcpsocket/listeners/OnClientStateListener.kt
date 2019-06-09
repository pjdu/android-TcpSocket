package gkk.android.lib.tcpsocket.listeners

import java.net.Socket

interface OnClientStateListener {

    fun onConnected(socket: Socket)

    fun onDisconnected(ip: String)

    fun onError(socket: Socket, error: Exception)
}