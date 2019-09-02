package gal.udc.tfg.sergio.bikeview

import android.content.ContentValues.TAG
import android.util.Log
import android.view.Menu
import android.widget.Switch
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.*
import java.nio.charset.Charset
import kotlin.concurrent.thread


class Request {
    lateinit var rsocket: DatagramSocket

    fun start(broacast: Boolean): String {

        while (broacast){
            var recvBuf = ByteArray(15000)
            rsocket = DatagramSocket(5555)
            val packet = DatagramPacket(recvBuf, recvBuf.size)
            Log.e("INIT", "Broadcast $broacast")
            rsocket.receive(packet)
            val ipreceived = packet.address.hostAddress
            val message = String(packet.data).trim { it <= ' ' }
            Log.e("INIT", "Got UDP broadcast from $ipreceived, message: $message")
            //rsocket.close()
            if (message == "BikeView") {
                //connect(ipreceived)
                return ipreceived
            }
            //if (ips.size == 2) break
        }
        Log.e("INIT", "broadcast stop")
        try{
            rsocket.close()
        }catch (e: Exception) {
            Log.e(TAG, "Exception", e)
        }
        return ""
    }

    fun connect(ip: String): Socket{
        Log.e("INIT", "connected")
        return Socket(ip, 8000)

    }

    fun run(socket: Socket, order: String): Boolean {
        var delivered = false
        try {
            if (socket.isClosed) {
                Log.e(TAG, "socket"+socket.isBound)
                return false
            }
            else {
                socket.outputStream.write(order.toByteArray(Charset.defaultCharset()))
                Log.e("SEND", "Sent: $order")
                var responseString: String?
                val bufferReader = BufferedReader(InputStreamReader(socket.inputStream))
                responseString = bufferReader.readLine()
                Log.e("SEND", "Received: $responseString")
                delivered = delivered || (responseString != null)
                Log.e("SEND", "Delivered: $delivered")
            }

            //bufferReader.close()
            //socket.close()
        }
        catch (e: UnknownHostException) {
            Log.e(TAG, "UnknownHostException", e)
        } catch (e: IOException) {
            Log.e(TAG, "IOException", e)
            socket.close()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
        }

        Log.e("SEND", "Delivered: $delivered")
        return delivered

    }
    fun isconnected(socket: Socket): Boolean{
        return socket.isConnected
    }

    fun setbroad(broad: Boolean){
        //broacast = broad
    }
    fun stop(socket: Socket) {
        //Thread.sleep(2_00)
        //socket.outputStream.write("s".toByteArray(Charset.defaultCharset()))
        socket.close()
    }
    fun rstop(){
        //rsocket.close()
    }
}