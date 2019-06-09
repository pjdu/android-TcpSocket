package gkk.android.lib.tcpsocket.listeners

import java.net.Socket

interface OnDataListener {

    fun onData(socket: Socket, data: ByteArray)

    fun onError(socket: Socket, error: Exception)

}