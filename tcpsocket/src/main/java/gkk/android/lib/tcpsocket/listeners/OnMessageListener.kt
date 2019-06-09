package gkk.android.lib.tcpsocket.listeners

import java.net.Socket

interface OnMessageListener {

    fun onMessage(socket: Socket, message: String)

    fun onError(socket: Socket, error: Exception)

}