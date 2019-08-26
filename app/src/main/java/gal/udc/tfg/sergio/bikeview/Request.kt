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
    var socks: MutableList<Socket> = mutableListOf()
    //var broacast = true
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
                return ipreceived
            }
            //if (ips.size == 2) break
        }
        Log.e("INIT", "Stop")
        rsocket.close()
        return ""
    }

    fun connect(ips: MutableList<String>): MutableList<Socket>{
        Log.e("CONN", "Number of ip ${ips.size}")
        //if (ips.size == 0) GlobalScope.async { start(true) }
        ips.forEach {
            Log.e("CONN", "ip ${it}")
            socks.add(Socket(it, 8000))
        }
        Log.e("CONN", "Number of sockets ${socks.size}")
        return socks
    }

    fun run(sockets: MutableList<Socket>, order: String): Boolean {
        var delivered = false
        sockets.forEach {
            try {
                if (it.isClosed) {
                    Log.e(TAG, "socket"+it.isBound)
                    return false
                }
                else {
                    it.outputStream.write(order.toByteArray(Charset.defaultCharset()))
                    Log.e("SEND", "Sent: $order")
                    var responseString: String?
                    val bufferReader = BufferedReader(InputStreamReader(it.inputStream))
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
                it.close()
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Exception", e)
            }
        }
        Log.e("SEND", "Delivered: $delivered")
        return delivered

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