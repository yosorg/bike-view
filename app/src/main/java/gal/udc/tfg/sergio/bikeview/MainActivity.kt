package gal.udc.tfg.sergio.bikeview

import android.R.attr.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.longToast
import org.jetbrains.anko.uiThread
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.support.v4.content.ContextCompat
import android.view.animation.Animation
import android.view.animation.AlphaAnimation
import kotlinx.coroutines.*
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.PersistableBundle
import org.jetbrains.anko.toast
import java.net.InetAddress
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.*
import android.widget.Switch
import gal.udc.tfg.sergio.bikeview.R.attr.*
import kotlin.properties.Delegates
import android.os.Build
import android.widget.ImageButton
import android.widget.SeekBar
import java.net.Socket


class MainActivity : AppCompatActivity(), SensorEventListener, SurfaceHolder.Callback, TextureView.SurfaceTextureListener {


    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var light = 0
    var connection:Boolean by Delegates.observable(false) { property, oldValue, newValue ->
        //onPrepareOptionsMenu(menu)
        if(oldValue != newValue){
            if(connection) connButton.setImageResource(R.drawable.ic_wifi)
            else connButton.setImageResource(R.drawable.ic_wifi_slash)
        }
    }
    private var ips: MutableList<String> = mutableListOf()
    val requester = Request()
    var sock = GlobalScope.async {requester.connect()}
    var transmtting = false
    val TRAMS_STATE = "transmisionState"
    var REQU_STATE = "requesterState"



    private var mRec: UdpReceiverDecoderThread? = null
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor.type == Sensor.TYPE_LIGHT){
            light = event.values[0].toInt()
            if (nightButton.isChecked) {
                redButton.isChecked = light < 400
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transmtting = savedInstanceState?.getBoolean(TRAMS_STATE) ?: false
        setContentView(R.layout.activity_main)
        //GlobalScope.async {requester.start(true)}
        //requester.broacast = true
        //sock = GlobalScope.async {requester.connect()}
        setSupportActionBar(toolbar)
        //val model = ViewModelProviders.of(this).get(Request::class.java)

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        }
        val sv = videoSurface
        //sv.holder.addCallback(this)
        //sv.surfaceTextureListener
        sv.scaleX = -1f
        sv!!.surfaceTextureListener = this

        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 500
        anim.repeatCount = Animation.INFINITE

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (transmtting) {
            GlobalScope.async { connection = requester.run(sock.await(), "v")}
        }

        GlobalScope.async {connection = requester.run(sock.await(), "c")}

        leftButton.setOnCheckedChangeListener { buttonView, isChecked->
            if (isChecked) {
                GlobalScope.async {connection = requester.run(sock.await(), "l" )}
                if (connection) {
                    leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                    leftButton.startAnimation(anim)
                }
                rightButton.isChecked = false
            } else {
                if ((!rightButton.isChecked)&&redButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                if (!rightButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                leftButton.clearAnimation()
                leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            }
        }

        rightButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                GlobalScope.async {connection = requester.run(sock.await(), "r" )}
                if (connection) {
                    rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                    rightButton.startAnimation(anim)
                }
                leftButton.isChecked = false
            } else {
                if ((!leftButton.isChecked)&&redButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                if (!leftButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                rightButton.clearAnimation()
                rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            }
        }

        redButton.setOnCheckedChangeListener { buttonView, isChecked ->
            GlobalScope.async {connection = requester.run(sock.await(),
                    when {
                        leftButton.isChecked -> "l"
                        rightButton.isChecked -> "r"
                        isChecked -> "n"
                        else -> "o"
                    })}
            if (isChecked&&connection) {
                redButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            } else {
                redButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.background_light))
            }
        }

        nightButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked&&connection) {
                nightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            } else {
                nightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.background_light))
            }
        }

        brakeButton.setOnCheckedChangeListener { buttonView, isChecked ->
            GlobalScope.async {connection = requester.run(sock.await(), if (isChecked) "b" else "o")}
        }

        connButton.setOnClickListener {
            if(!connection) {
                sock = GlobalScope.async {requester.connect()}
            }
            GlobalScope.async {connection = requester.run(sock.await(), "c")}
        }

        brightButton.setOnClickListener {
            brightButton.visibility = View.GONE
            seekBar.visibility = View.VISIBLE
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                GlobalScope.async {connection = requester.run(sock.await(), progress.toString())}
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.visibility = View.GONE
                brightButton.visibility = View.VISIBLE
                GlobalScope.async {connection = requester.run(sock.await(),
                        when {
                            leftButton.isChecked -> "l"
                            rightButton.isChecked -> "r"
                            else -> progress.toString()
                        })}
            }
        })

        videoSurface.setOnClickListener {
            if (!transmtting) {
                GlobalScope.async { connection = requester.run(sock.await(), "v") }
                transmtting = true
            } else {
                GlobalScope.async { connection = requester.run(sock.await(), "vs") }
                transmtting = false
            }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle?) {

        savedInstanceState?.putBoolean(TRAMS_STATE, transmtting)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        //requester.broacast = true
        //GlobalScope.async {requester.start(true)}

        lightSensor?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (transmtting) {
            GlobalScope.async { connection = requester.run(sock.await(), "v")}
        }
    }



    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(this)
        GlobalScope.async { connection = requester.run(sock.await(), "vs1")}
    }

    override fun onDestroy() {
        super.onDestroy()
        //GlobalScope.async {requester.stop(sock.await())}
        //GlobalScope.async {requester.start(false)}
        GlobalScope.async {connection = requester.run(sock.await(), "s")}
    }

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.bright -> {
                seekBar.visibility = View.VISIBLE
            }
            R.id.connect -> {
                if(!connection) {
                    sock = GlobalScope.async {requester.connect()}
                }
                GlobalScope.async {connection = requester.run(sock.await(), "c")}
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (connection) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) connButton.setImageResource(R.drawable.ic_wifi)
            else {
                invalidateOptionsMenu()
                menu?.findItem(R.id.connect)?.setIcon(R.drawable.ic_wifi)
            }
        } else {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) connButton.setImageResource(R.drawable.ic_wifi_slash)
            else {
                invalidateOptionsMenu()
                menu?.findItem(R.id.connect)?.setIcon(R.drawable.ic_wifi_slash)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }*/

    override fun surfaceCreated(holder: SurfaceHolder) {
        //Log.d("UDP", "created")
        mRec = UdpReceiverDecoderThread(holder.surface, 5000)
        mRec?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        //Log.d("UDP", "surface changed")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        //Log.d("UDP", "surface destroyed")
        GlobalScope.launch { connection = requester.run(sock.await(), "vs1")}

        mRec?.interrupt()
        mRec = null
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        mRec?.interrupt()
        mRec = null
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        val s = Surface(surface)
        mRec = UdpReceiverDecoderThread(s, 5000)
        mRec?.start()
    }

}
