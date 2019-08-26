package gal.udc.tfg.sergio.bikeview

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.io.IOException
import android.util.Log
import java.nio.ByteBuffer
import java.net.SocketException
import java.net.DatagramPacket
import java.net.DatagramSocket


internal class VideoReceiver(surface: Surface, var port: Int) : Thread() {
    var nalu_search_state = 0
    var nalu_data: ByteArray
    var nalu_data_position: Int = 0
    var NALU_MAXLEN = 1024 * 1024

    private var decoder: MediaCodec? = null
    private var format: MediaFormat? = null
    var s: DatagramSocket? = null
    init {
        nalu_data = ByteArray(NALU_MAXLEN)
        nalu_data_position = 0

        try {
            decoder = MediaCodec.createDecoderByType("video/avc")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        format = MediaFormat.createVideoFormat("video/avc", 1920, 1080)
        decoder?.configure(format, surface, null, 0)
        decoder?.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        decoder?.start()

    }

    override fun run() {
        val server_port = this.port
        val message = ByteArray(1500)
        val p = DatagramPacket(message, message.size)

        try {
            s?.reuseAddress = true
            s = DatagramSocket(server_port)
        } catch (e: SocketException) {
            e.printStackTrace()
        }

        while (!interrupted() && s != null) {
            try {
                s?.receive(p)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                parseDatagram(message, p.length)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
    fun stopReceiver(){
        s?.close()
        s?.disconnect()

        decoder?.flush()
        decoder?.stop()
        decoder?.release()
    }
    private fun feedDecoder(n: ByteArray, len: Int) {

        while (decoder != null) {
            val bi = MediaCodec.BufferInfo()
            val outputBufferIndex = decoder!!.dequeueOutputBuffer(bi, 0)
            if (outputBufferIndex >= 0) {
                //val outputBuffer = decoder!!.getOutputBuffer(outputBufferIndex)
                decoder!!.releaseOutputBuffer(outputBufferIndex, true)
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d("UDP", "output format changed")
            }


            val inputBufferIndex = decoder!!.dequeueInputBuffer(200)
            if (inputBufferIndex >= 0) {
                val inputBuffer = decoder!!.getInputBuffer(inputBufferIndex)
                inputBuffer!!.put(n, 0, len)
                decoder!!.queueInputBuffer(inputBufferIndex, 0, len, 0, 0)
                break
            }
        }


    }

    private fun interpretNalu(n: ByteArray, len: Int) {
        feedDecoder(n, len)
    }

    private fun parseDatagram(p: ByteArray, plen: Int) {
        var i = 0

        while (i < plen) {
            nalu_data[nalu_data_position++] = p[i]
            if (nalu_data_position == NALU_MAXLEN - 1) {
                Log.d("UDP", "Nalu overflow")
                nalu_data_position = 0
            }

            when (nalu_search_state) {
                0, 1, 2 -> if (p[i].toInt() == 0)
                    nalu_search_state++
                else
                    nalu_search_state = 0

                3 -> {
                    if (p[i].toInt() == 1) {
                        nalu_data[0] = 0
                        nalu_data[1] = 0
                        nalu_data[2] = 0
                        nalu_data[3] = 1
                        interpretNalu(nalu_data, nalu_data_position - 4)
                        nalu_data_position = 4
                    }
                    nalu_search_state = 0
                }

                else -> {
                }
            }
            ++i
        }
    }
}