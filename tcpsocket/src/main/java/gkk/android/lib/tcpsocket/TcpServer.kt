package gkk.android.lib.tcpsocket

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import gkk.android.lib.tcpsocket.common.TcpConfig
import gkk.android.lib.tcpsocket.listeners.*
import gkk.android.lib.tcpsocket.threads.AcceptThread
import java.io.File
import java.net.ServerSocket
import java.net.Socket

abstract class TcpServer {
    private val TAG = TcpServer::class.java.simpleName

    private var IS_LOGGING = false
    private var PREFIX = ">>>"

    private lateinit var serverSocket: ServerSocket
    private var acceptThread: AcceptThread? = null
    private var sendThread: HandlerThread = HandlerThread(TAG)
    private val sendHandler: Handler by lazy {
        Handler(sendThread.looper)
    }
    private var connectedClientMap = hashMapOf<String, TcpClient>()

    var serverStateListener: OnServerStateListener? = null
    var messageListener: OnMessageListener? = null
    var dataListener: OnDataListener? = null
    var sendListener: OnSendListener? = null


    fun setLogging(b: Boolean, prefix: String) {
        IS_LOGGING = b
        PREFIX = prefix
    }


    fun setSendDelay(delay: Long) {
        TcpConfig.DEFAULT_SEND_DELAY = delay
    }


    fun start(port: Int) {
        if (isStarted()) return
        Thread(Runnable {
            try {
                serverSocket = ServerSocket(port)


            } catch (error: Exception) {
                error.printStackTrace()

            }
        }).start()
    }


    fun stop() {
        if (!isStarted()) return
        Thread(Runnable {
            try {
                disconnectAll()
                stopAcceptThread()
                serverSocket?.close()
                defaultServerStateListener?.onStopped()

            } catch (error: Exception) {
                defaultServerStateListener?.onError(error)
                error.printStackTrace()
            }
        }).start()
    }



    fun disconnectAll() {
        connectedClientMap.values.forEach {
            try {
                it.disconnect()

            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
        connectedClientMap.clear()
    }


    fun sendMessage(target: String, message: String) {
        try {
            connectedClientMap[target]?.sendMessage(message)
            sendListener?.onSent(target, message)

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    fun sendData(target: String, data: ByteArray) {
        try {
            connectedClientMap[target]?.sendData(data)
            sendListener?.onSent(target, "[DATA Size: ${data.size}]")

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    fun sendFile(target: String, file: File) {
        try {
            connectedClientMap[target]?.sendFile(file)
            sendListener?.onSent(target, "[FILE Size: ${file.length()}, Path: ${file.path}]")

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    fun sendBitmap(target: String, bitmap: Bitmap) {
        try {
            connectedClientMap[target]?.sendBitmap(bitmap)
            sendListener?.onSent(target, "[BITMAP Size: ${bitmap.byteCount}]")

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }



    private fun startAcceptThread() {
        if (isStarted()) return
        try {
            acceptThread = AcceptThread(serverSocket, defaultServerStateListener)
            acceptThread?.start()

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    private fun stopAcceptThread() {
        if (!isStarted()) return
        try {
            val thread = acceptThread
            acceptThread = null
            thread?.interrupt()

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    fun isStarted(): Boolean {
        return acceptThread?.isAlive ?: false
    }



    fun getClientList(): List<TcpClient> {
        return connectedClientMap.values.toList()
    }


    private fun addClientToHashMap(clientSocket: Socket) {
        try {
            var client = object : TcpClient(clientSocket) {
                override fun checkStringData(str: String): Boolean {
                    return this@TcpServer.checkStringData(str)
                }

                override fun checkByteArrayData(byteArray: ByteArray): Boolean {
                    return this@TcpServer.checkByteArrayData(byteArray)
                }
            }
            client.clientStateListener = defaultClientStateListener
            client.messageListener = defaultMessageListener
            client.dataListener = defaultDataListener
            client.initConnectedClient(clientSocket)

            connectedClientMap[clientSocket.inetAddress.hostAddress] = client

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }


    private fun removeClientFromHashMap(clientIp: String): TcpClient? {
        return connectedClientMap.remove(clientIp)
    }




    abstract fun checkStringData(str: String): Boolean

    abstract fun checkByteArrayData(byteArray: ByteArray): Boolean


    private val defaultServerStateListener = object: OnServerStateListener {
        override fun onStarted(port: Int) {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} STARTED")
            serverStateListener?.onStarted(port)
        }

        override fun onStopped() {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} STOPPED")
            serverStateListener?.onStopped()
        }

        override fun onClientConnected(socket: Socket) {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} CLIENT CONNECTED [${socket.inetAddress?.hostAddress}]")
            addClientToHashMap(socket)
            serverStateListener?.onClientConnected(socket)
        }

        override fun onClientDisconnected(ip: String) {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} CLIENT DISCONNECTED [${ip}]")
            removeClientFromHashMap(ip)
            serverStateListener?.onClientDisconnected(ip)
        }

        override fun onClientError(socket: Socket, error: Exception) {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} CLIENT ERROR [${socket.inetAddress?.hostAddress}, ${error.message}]")
            serverStateListener?.onClientError(socket, error)
        }

        override fun onError(error: Exception) {
            if (IS_LOGGING) Log.i(TAG, "${PREFIX} SERVER ERROR [${error.message}]")
            serverStateListener?.onError(error)
        }
    }


    private val defaultClientStateListener = object: OnClientStateListener {
        override fun onConnected(socket: Socket) {}

        override fun onDisconnected(ip: String) {
            defaultServerStateListener?.onClientDisconnected(ip)
        }

        override fun onError(socket: Socket, error: Exception) {

        }
    }


    private val defaultMessageListener = object: OnMessageListener {
        override fun onMessage(socket: Socket, message: String) {

        }

        override fun onError(socket: Socket, error: Exception) {

        }
    }


    private val defaultDataListener = object: OnDataListener {
        override fun onData(socket: Socket, data: ByteArray) {

        }

        override fun onError(socket: Socket, error: Exception) {

        }
    }



}