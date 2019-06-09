package gkk.android.lib.tcpsocket

import android.app.admin.DeviceAdminInfo
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import gkk.android.lib.tcpsocket.common.TcpConfig
import gkk.android.lib.tcpsocket.listeners.OnClientStateListener
import gkk.android.lib.tcpsocket.listeners.OnDataListener
import gkk.android.lib.tcpsocket.listeners.OnMessageListener
import gkk.android.lib.tcpsocket.listeners.OnSendListener
import gkk.android.lib.tcpsocket.threads.ReceiveThread
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Socket

abstract class TcpClient() {
    private val TAG = TcpClient::class.java.simpleName

    private var IS_LOGGING = false
    private var PREFIX = ">>>"

    private lateinit var socket: Socket
    private lateinit var bos: BufferedOutputStream
    private var receiveThread: ReceiveThread? = null
    private var sendThread: HandlerThread = HandlerThread(TAG)
    private val sendHandler: Handler by lazy {
        Handler(sendThread.looper)
    }

    var clientStateListener: OnClientStateListener? = null
    var messageListener: OnMessageListener? = null
    var dataListener: OnDataListener? = null
    var sendListener: OnSendListener? = null

    var ipAddress = ""


    constructor(socket: Socket): this() {
        try {
            this.socket = socket
            ipAddress = socket.inetAddress.hostAddress
            TcpConfig.port = socket.port
            bos = BufferedOutputStream(DataOutputStream(socket.getOutputStream()))

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    fun setLogging(b: Boolean, prefix: String?) {
        IS_LOGGING = b
        PREFIX = prefix ?: ""
    }


    fun setSendDelay(delay: Long) {
        TcpConfig.DEFAULT_SEND_DELAY = delay
    }


    fun getPort(): Int {
        return socket.port
    }


    fun isConnected(): Boolean {
        if (!::socket.isInitialized) return false
        return socket.isConnected
    }




    fun initConnectedClient(socket: Socket) {
        setLogging(false, null)
        startReceiveThread(socket)
    }



    fun connect(ip: String, port: Int) {
        Thread(Runnable {
            try {
                this.ipAddress = ip
                TcpConfig.port = port
                socket = Socket(ipAddress, port)
                bos = BufferedOutputStream(DataOutputStream(socket.getOutputStream()))
                startReceiveThread(socket)
                startSendThread()

                if (isConnected()) defaultClientStateListener.onConnected(socket)

            } catch (error: Exception) {
                error.printStackTrace()
            }
        }).start()
    }


    fun disconnect() {
        Thread(Runnable {
            try {
                if (!isConnected()) return@Runnable
                stopReceiveThread()
                stopSendThread()
                bos.close()

                // socket.close() 및 onDisconnected() 호출하지 말것.
                // ReceiveThread 가 종료될때 호출함.

            } catch (error: Exception) {
                error.printStackTrace()
            }
        }).start()
    }



    fun sendMessage(message: String) {
        if (!isConnected()) return
        sendHandler.post {
            try {
                Thread.sleep(TcpConfig.DEFAULT_SEND_DELAY)
                bos.write(message.toByteArray())
                bos.flush()
                defaultSendListener?.onSent(ipAddress, message)

            } catch (error: Exception) {
                defaultClientStateListener?.onError(socket, error)
                error.printStackTrace()
            }
        }
    }



    fun sendData(data: ByteArray) {
        if (!isConnected()) return
        sendHandler.post {
            try {
                Thread.sleep(TcpConfig.DEFAULT_SEND_DELAY)
                bos.write(data)
                bos.flush()
                defaultSendListener?.onSent(ipAddress, "[DATA Size: ${data.size}]")

            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
    }



    fun sendFile(file: File) {
        if (!isConnected()) return
        sendHandler.post {
            try {
                Thread.sleep(TcpConfig.DEFAULT_SEND_DELAY)
                if (file.length() > Int.MAX_VALUE) {
                    Log.e(TAG, "${PREFIX} ERROR > 파일크기가 2GB를 초과했습니다.")
                }
                bos.write(file.readBytes())
                bos.flush()
                defaultSendListener?.onSent(ipAddress, "[FILE Size: ${file.length()}, Path: ${file.path}]")

            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
    }



    fun sendBitmap(bitmap: Bitmap) {
        if (!isConnected()) return
        sendHandler.post {
            var baos = ByteArrayOutputStream()
            try {
                Thread.sleep(TcpConfig.DEFAULT_SEND_DELAY)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                bos.write(baos.toByteArray())
                bos.flush()
                defaultSendListener?.onSent(ipAddress, "[BITMAP Size: ${bitmap.byteCount}]")

            } catch (error: Exception) {
                error.printStackTrace()
            } finally {
                baos.close()
            }
        }
    }



    private fun startSendThread() {
        try {
            sendThread.start()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    private fun stopSendThread() {
        try {
            sendThread.quit()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    private fun startReceiveThread(socket: Socket) {
        if (receiveThread == null) {
            receiveThread = object : ReceiveThread(socket) {
                override fun checkStringData(str: String): Boolean {
                    return this@TcpClient.checkStringData(str)
                }

                override fun checkByteArrayData(byteArray: ByteArray): Boolean {
                    return this@TcpClient.checkByteArrayData(byteArray)
                }
            }
            receiveThread?.clientStateListener = defaultClientStateListener
            receiveThread?.messageListener = defaultMessageLister
            receiveThread?.dataListener = defaultDataListener
        }
        receiveThread?.start()
    }


    private fun stopReceiveThread() {
        if (receiveThread == null) return
        val thread = receiveThread
        receiveThread = null
        thread?.interrupt()
    }



    abstract fun checkStringData(str: String): Boolean

    abstract fun checkByteArrayData(byteArray: ByteArray): Boolean


    private val defaultClientStateListener = object: OnClientStateListener {
        override fun onConnected(socket: Socket) {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} CONNECTED [${socket.inetAddress?.hostAddress}]")
            clientStateListener?.onConnected(socket)
        }

        override fun onDisconnected(ip: String) {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} DISCONNECTED [$ip]")
            clientStateListener?.onDisconnected(ip)
        }

        override fun onError(socket: Socket, error: Exception) {
            if (IS_LOGGING) Log.e(TAG, "${PREFIX} STATE ERROR [${socket.inetAddress?.hostAddress}]: ${error.message}")
            clientStateListener?.onError(socket, error)
        }
    }


    private val defaultMessageLister = object: OnMessageListener {
        override fun onMessage(socket: Socket, message: String) {
            if (IS_LOGGING) Log.d(TAG, "${PREFIX} RECEIVED MESSAGE [${socket.inetAddress?.hostAddress}]: $message")
            messageListener?.onMessage(socket, message)
        }

        override fun onError(socket: Socket, error: Exception) {
            if (IS_LOGGING) Log.e(TAG, "${PREFIX} MESSAGE ERROR [${socket.inetAddress?.hostAddress}]: ${error.message}")
            messageListener?.onError(socket, error)
        }
    }


    private val defaultDataListener = object: OnDataListener {
        override fun onData(socket: Socket, data: ByteArray) {
            if (IS_LOGGING) Log.d(TAG, "${PREFIX} RECEIVED DATA [${socket.inetAddress?.hostAddress}]: SIZE = ${data.size}")
            dataListener?.onData(socket, data)
        }

        override fun onError(socket: Socket, error: Exception) {
            if (IS_LOGGING) Log.e(TAG, "${PREFIX} DATA ERROR [${socket.inetAddress?.hostAddress}]: ${error.message}")
            dataListener?.onError(socket, error)
        }
    }


    private val defaultSendListener = object: OnSendListener {
        override fun onSent(target: String, message: String) {
            if (IS_LOGGING) Log.w(TAG, "${PREFIX} SENT MESSAGE : $message")
            sendListener?.onSent(target, message)
        }

    }

}