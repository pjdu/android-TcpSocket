package gkk.android.lib.tcpsocket.threads

import gkk.android.lib.tcpsocket.listeners.OnClientStateListener
import gkk.android.lib.tcpsocket.listeners.OnDataListener
import gkk.android.lib.tcpsocket.listeners.OnMessageListener
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.net.Socket

abstract class ReceiveThread(var socket: Socket): Thread() {
    private val TAG = ReceiveThread::class.java.simpleName

    private var bis = BufferedInputStream(DataInputStream(socket.getInputStream()))
    private var baos = ByteArrayOutputStream()
    private var ip = socket.inetAddress.hostAddress ?: ""
    private var isCalledString = false
    private var isCalledByteArray = false
    private var bufferSize = 1024

    var clientStateListener: OnClientStateListener? = null
    var messageListener: OnMessageListener? = null
    var dataListener: OnDataListener? = null



    override fun run() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                var buffer = ByteArray(bufferSize)
                var dataSize = bis.read(buffer, 0, buffer.size)
                if (dataSize == -1) break

                baos.write(buffer, 0, dataSize)
                baos.flush()

                if (messageListener != null) {
                    var str = String(baos.toByteArray())
                    if (checkStringData(str)) {
                        messageListener?.onMessage(socket, str)
                        isCalledString = true
                    }
                }

                if (dataListener != null) {
                    var byteArray = baos.toByteArray()
                    if (checkByteArrayData(byteArray)) {
                        dataListener?.onData(socket, byteArray)
                        isCalledByteArray = true
                    }
                }

                if (isCalledString || isCalledByteArray) {
                    baos.reset()
                    isCalledString = false
                    isCalledByteArray = false
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
        } finally {
            bis.close()
            baos.close()
            socket.close()
            clientStateListener?.onDisconnected(this.ip)
        }
    }



    abstract fun checkStringData(str: String): Boolean


    abstract fun checkByteArrayData(byteArray: ByteArray): Boolean
}