package gkk.android.lib.tcpsocket.threads

import gkk.android.lib.tcpsocket.listeners.OnServerStateListener
import java.net.ServerSocket

class AcceptThread(var serverSocket: ServerSocket,
                   var serverStateListener: OnServerStateListener): Thread() {
    private val TAG = AcceptThread::class.java.simpleName

    override fun run() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    var clientSocket = serverSocket.accept()
                    serverStateListener.onClientConnected(clientSocket)

                } catch (error: Exception) {
                    error.printStackTrace()
                    serverStateListener.onError(error)
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
            serverStateListener.onError(error)
        }
    }
}