package gal.udc.tfg.sergio.bikeview

import android.util.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.net.Socket

class Device(val ip: String, val requester: Request = Request() /**,val sock: Deferred<Socket> = GlobalScope.async {requester.connect(ip)}**/) {
    fun connect(){

    }
    fun send(order: String): Boolean{
        //return GlobalScope.async {requester.run(sock.await(), order )}
        return false
    }
}