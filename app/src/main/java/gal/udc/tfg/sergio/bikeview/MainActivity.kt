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
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), SensorEventListener, TextureView.SurfaceTextureListener {


    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var light = 0
    var connection:Boolean by Delegates.observable(false) { property, oldValue, newValue ->
        //onPrepareOptionsMenu(menu)
        if(oldValue != newValue){
            if(connection) {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) connButton.setImageResource(R.drawable.ic_wifi_small)
                else connButton.setImageResource(R.drawable.ic_wifi)
            }
            else {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) connButton.setImageResource(R.drawable.ic_wifi_slash_small)
                else connButton.setImageResource(R.drawable.ic_wifi_slash)
            }
        }
    }
    var ipList: MutableList<String>  = mutableListOf()
    var sockList: MutableList<Socket> = mutableListOf()
    val requester = Request()
    var sock = GlobalScope.async {sockList}
//    val deviceListener = thread(start = true) {requester.start(true)}
    var transmtting = false
    val TRAMS_STATE = "transmisionState"
    var REQU_STATE = "requesterState"
    var redchecked = false
    var nighthecked = false
    var brakechecked = false
    var orientation by Delegates.notNull<Int>()

    private var vrec: VideoReceiver? = null
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor.type == Sensor.TYPE_LIGHT){
            light = event.values[0].toInt()
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (nightButton.isChecked) {
                    redButton.isChecked = light < 400
                }
            } else {
                if (nighthecked) {
                    if (light < 400) redButtonSmall.performClick()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transmtting = savedInstanceState?.getBoolean(TRAMS_STATE) ?: false
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //GlobalScope.async {requester.start(true)}
        //requester.broacast = true
        GlobalScope.async {ipList.add(requester.start(true))}

        if(!connection) {
            sock = GlobalScope.async {requester.connect(ipList)}
            GlobalScope.async {ipList.add(requester.start(true))}
        }
        GlobalScope.async {connection = requester.run(sock.await(), "c")}

        setSupportActionBar(toolbar)
        //val model = ViewModelProviders.of(this).get(Request::class.java)

        orientation = resources.configuration.orientation
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

        if(!connection) {
            sock = GlobalScope.async {requester.connect(ipList)}
            GlobalScope.async {ipList.add(requester.start(true))}
        }
        GlobalScope.async {connection = requester.run(sock.await(), "c")}
        GlobalScope.async {connection = requester.run(sock.await(), "c")}
        if (transmtting) {
            GlobalScope.async { connection = requester.run(sock.await(), "v")}
            cameraButton.visibility = View.GONE
            sv.visibility = View.VISIBLE
        }

        GlobalScope.async {connection = requester.run(sock.await(), "c")}


        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            leftButton.setOnCheckedChangeListener { buttonView, isChecked->
                if (connection) {
                    if (isChecked) {
                        GlobalScope.async {connection = requester.run(sock.await(), "l" )}
                        leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                        leftButton.startAnimation(anim)
                        rightButton.isChecked = false
                    } else {
                        if ((!rightButton.isChecked)&&redButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                        if (!rightButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                        leftButton.clearAnimation()
                        leftButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                    }
                }
            }

            rightButton.setOnCheckedChangeListener { buttonView, isChecked ->
                if (connection) {
                    if (isChecked) {
                        GlobalScope.async {connection = requester.run(sock.await(), "r" )}
                        rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                        rightButton.startAnimation(anim)
                        leftButton.isChecked = false
                    } else {
                        if ((!leftButton.isChecked)&&redButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                        if (!leftButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                        rightButton.clearAnimation()
                        rightButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                    }
                }
            }
            redButton.setOnCheckedChangeListener { buttonView, isChecked ->
                if (connection) {
                    redchecked=!redchecked
                    GlobalScope.async {connection = requester.run(sock.await(),
                            when {
                                leftButton.isChecked -> "l"
                                rightButton.isChecked -> "r"
                                isChecked -> "n"
                                else -> "o"
                            })
                    }
                    if (isChecked) {
                        redButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                    } else {
                        redButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                    }
                }
            }

            nightButton.setOnCheckedChangeListener { buttonView, isChecked ->

                if (connection) {
                    nighthecked=!nighthecked
                    if (isChecked) {
                        nightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                    } else {
                        nightButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                    }
                }
            }

            brakeButton.setOnCheckedChangeListener { buttonView, isChecked ->
                if (connection) {
                    brakechecked=!brakechecked
                    if (isChecked) {
                        brakeButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                        GlobalScope.async {connection = requester.run(sock.await(), "b")}
                    } else {
                        brakeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                        GlobalScope.async {connection = requester.run(sock.await(),
                                when{
                                    leftButton.isChecked -> "l"
                                    rightButton.isChecked -> "r"
                                    nightButton.isChecked  -> "n"
                                    else -> "o"
                                })
                        }
                    }
                }
            }
        } else {
            leftButton.setOnCheckedChangeListener { buttonView, isChecked->
                if (connection) {
                    if (isChecked) {
                        GlobalScope.async {connection = requester.run(sock.await(), "l" )}
                        leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                        leftButton.startAnimation(anim)
                        rightButton.isChecked = false
                    } else {
                        if ((!rightButton.isChecked)&&redchecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                        if (!rightButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                        leftButton.clearAnimation()
                        leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                    }
                }
            }

            rightButton.setOnCheckedChangeListener { buttonView, isChecked ->
                if (connection) {
                    if (isChecked) {
                        GlobalScope.async {connection = requester.run(sock.await(), "r" )}
                        rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                        rightButton.startAnimation(anim)
                        leftButton.isChecked = false
                    } else {
                        if ((!leftButton.isChecked)&&redchecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                        if (!leftButton.isChecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                        rightButton.clearAnimation()
                        rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                    }
                }
            }
            redButtonSmall.setOnClickListener {
                if (connection) {
                    redchecked=!redchecked
                    GlobalScope.async {connection = requester.run(sock.await(),
                            when {
                                leftButton.isChecked -> "l"
                                rightButton.isChecked -> "r"
                                redchecked -> "n"
                                else -> "o"
                            })}
                    if (redchecked) {
                        redButtonSmall.setImageResource(R.drawable.ic_red_red)
                    } else {
                        redButtonSmall.setImageResource(R.drawable.ic_red)
                    }
                }
            }

            nightButtonSmall.setOnClickListener {
                if (connection) {
                    nighthecked=!nighthecked
                    if (nighthecked) {
                        nightButtonSmall.setImageResource(R.drawable.ic_night_red)
                    } else {
                        nightButtonSmall.setImageResource(R.drawable.ic_night)
                    }
                }
            }

            brakeButtonSmall.setOnClickListener {
                if (connection) {
                    brakechecked=!brakechecked
                    if (brakechecked) {
                        brakeButtonSmall.setImageResource(R.drawable.ic_brake_red)
                        GlobalScope.async {connection = requester.run(sock.await(), "b")}
                    } else {
                        brakeButtonSmall.setImageResource(R.drawable.ic_brake)
                        GlobalScope.async {connection = requester.run(sock.await(),
                                when{
                                    leftButton.isChecked -> "l"
                                    rightButton.isChecked -> "r"
                                    redchecked  -> "n"
                                    else -> "o"
                                })
                        }
                    }
                }
            }
        }

        connButton.setOnClickListener {
            if(!connection) {
                sock = GlobalScope.async {requester.connect(ipList)}
                GlobalScope.async {ipList.add(requester.start(true))}
            }
            GlobalScope.async {connection = requester.run(sock.await(), "c")}
        }

        brightButton.setOnClickListener {
            if (connection) {
                brightButton.visibility = View.GONE
                seekBar.visibility = View.VISIBLE
            }
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
                        })
                }
            }
        })

        videoSurface.setOnClickListener {
            if(connection){
                if (transmtting) {
                    GlobalScope.async { connection = requester.run(sock.await(), "vs") }
                    sv.visibility = View.GONE
                    cameraButton.visibility = View.VISIBLE
                    transmtting = false
                    GlobalScope.async {connection = requester.run(sock.await(),
                            when {
                                leftButton.isChecked -> "l"
                                rightButton.isChecked -> "r"
                                redchecked -> "n"
                                else -> "o"
                            })
                    }
                }

            }

        }
        cameraButton.setOnClickListener {
            if(connection){
                if (!transmtting) {
                    GlobalScope.async { connection = requester.run(sock.await(), "v") }
                    cameraButton.visibility = View.GONE
                    sv.visibility = View.VISIBLE
                    transmtting = true
                    GlobalScope.async {connection = requester.run(sock.await(),
                            when {
                                leftButton.isChecked -> "l"
                                rightButton.isChecked -> "r"
                                redchecked -> "n"
                                else -> "o"
                            })
                    }
                }
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
        requester.start(false)
        vrec?.interrupt()
        //GlobalScope.async {requester.stop(sock.await())}
        //GlobalScope.async {requester.start(false)}
        GlobalScope.async { connection = requester.run(sock.await(), "vs2")}
        GlobalScope.async {connection = requester.run(sock.await(), "s")}
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        vrec?.stopReceiver()
        vrec?.interrupt()
        surface?.release()
        return false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        val s = Surface(surface)
        vrec = VideoReceiver(s, 5000)
        vrec?.start()
    }

}
