package gal.udc.tfg.sergio.bikeview

import android.content.ContentValues.TAG
import android.util.Log
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException
import java.nio.charset.Charset



class Request(private val ip: String, private val order: String) {
    fun start() : Socket{
        var socket = Socket(ip, 80)
        return socket
    }
    fun run(socket: Socket) {
        try {
            //var socket = Socket(ip, 80)
            socket.outputStream.write(order.toByteArray(Charset.defaultCharset()))
            //socket.close()
        }
        catch (e: UnknownHostException) {
            Log.e(TAG, "UnknownHostException", e)
        } catch (e: IOException) {
            Log.e(TAG, "IOException", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
        }

    }
    fun stop(socket: Socket) {
        socket.close()
    }
}