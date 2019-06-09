package gkk.android.lib.tcpsocket.listeners

interface OnSendListener {

    fun onSent(target: String, message: String)
}