package gal.udc.tfg.sergio.bikeview

import android.R.attr.*
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.popup.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.support.v4.content.ContextCompat
import android.view.animation.Animation
import android.view.animation.AlphaAnimation
import kotlinx.coroutines.*
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.view.*
import kotlin.properties.Delegates
import android.widget.SeekBar
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import kotlin.math.hypot
import android.support.v4.app.SupportActivity
import android.support.v4.app.SupportActivity.ExtraData
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.popup.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.PI
import kotlin.math.cos
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), SensorEventListener, TextureView.SurfaceTextureListener {

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var linerSensor: Sensor? = null
    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var light = 0
    private var bright = 50
    private var minforce = 20
    private var deviceangle: Float? = null
    private var popup: Dialog? = null
    private var rotationMatrix = FloatArray(9)
    private lateinit var ip: String
    var requester = Request()
    var sock = GlobalScope.async {requester.connect(ip)}
    var connection:Boolean by Delegates.observable(false) { _, oldValue, newValue ->
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
    private var leftchecked:Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue){
            if(leftchecked) {
                GlobalScope.async {connection = requester.run(sock.await(), "l" )}
                leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                leftButton.startAnimation(anim)
                rightchecked = false
            }
            else {
                if ((!rightchecked)&&redchecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                else if (!rightchecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                leftButton.clearAnimation()
                if(orientation == Configuration.ORIENTATION_PORTRAIT) leftButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                else leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            }
        }
    }
    private var rightchecked:Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue){
            if(rightchecked) {
                GlobalScope.async {connection = requester.run(sock.await(), "r" )}
                rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                rightButton.startAnimation(anim)
                leftchecked = false
            }
            else {
                if ((!leftchecked)&&redchecked) GlobalScope.async {connection = requester.run(sock.await(), "n" )}
                else if (!leftchecked) GlobalScope.async {connection = requester.run(sock.await(), "o" )}
                rightButton.clearAnimation()
                if(orientation == Configuration.ORIENTATION_PORTRAIT) rightButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                else rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            }
        }
    }
    private var redchecked:Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue){
            GlobalScope.async {connection = requester.run(sock.await(),
                    when {
                        leftchecked -> "l"
                        rightchecked -> "r"
                        redchecked -> "n"
                        else -> "o"
                    })
            }
            if(redchecked) {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) redButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                else redButtonSmall.setImageResource(R.drawable.ic_red_red)

            }
            else {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) redButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                else redButtonSmall.setImageResource(R.drawable.ic_red)

            }
        }
    }
    private var nightchecked:Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue){
            if (nightchecked) {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) nightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                else nightButtonSmall.setImageResource(R.drawable.ic_night_red)
            } else {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) nightButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                else nightButtonSmall.setImageResource(R.drawable.ic_night)
            }
        }
    }
    private var brakechecked:Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if(oldValue != newValue){
            if (brakechecked) {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) brakeButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                else brakeButtonSmall.setImageResource(R.drawable.ic_brake_red)
            } else {
                if(orientation == Configuration.ORIENTATION_PORTRAIT) brakeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.buttonColor))
                else brakeButtonSmall.setImageResource(R.drawable.ic_brake)
            }
        }
    }

//    val deviceListener = thread(start = true) {requester.start(true)}
    private var transmtting = false
    private val TRAMS_STATE = "transmisionState"
    private val RED_STATE = "redState"
    private val RIGHT_STATE = "rightState"
    private val LEFT_STATE = "leftState"
    private val NIGHT_STATE = "nightState"
    private val BRAKE_STATE = "brakeState"
    private var calibrate = false
    private var orientation by Delegates.notNull<Int>()
    private val anim = AlphaAnimation(0.0f, 1.0f)
    private var vrec: VideoReceiver? = null
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (nightchecked && event.sensor.type == Sensor.TYPE_LIGHT) {
            light = event.values[0].toInt()
            redchecked = light < 400
        }
        if (brakechecked) {
            if (deviceangle == null && event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR){
                if(!calibrate) popup?.show()
                else {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationMatrix = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationMatrix)
                    deviceangle =  when {
                        orientation == Configuration.ORIENTATION_PORTRAIT -> -orientationMatrix[1]
                        orientationMatrix[2] > 0 -> -orientationMatrix[2]
                        else -> -orientationMatrix[2]
                    }
                    calibrate = false
                    popup?.dismiss()
                }
            } else if(deviceangle != null && event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                val force = if (orientation == Configuration.ORIENTATION_PORTRAIT) - event.values[1] * cos(deviceangle!!) + event.values[2] * cos(PI.toFloat()/2 - deviceangle!!)
                    else event.values[0] * cos(deviceangle!!) + event.values[2] * cos(PI.toFloat()/2 - deviceangle!!)
                //redButton.text = deviceangle.toString()
                //leftButton.text = (deviceangle!!*180.toFloat()/PI).toString()
                if (force > minforce) {
                    //leftButton.text = event.values[1].toString()
                    //rightButton.text = event.values[2].toString()
                    //brakeButton.text = force.toString()
                    //rightButton.text = force.toString()
                    GlobalScope.async {
                        connection = requester.run(sock.await(), "b")
                        //delay(2000)
                        connection = requester.run(sock.await(), (bright/11).toString())
                        connection = requester.run(sock.await(), when {
                            leftchecked -> "l"
                            rightchecked -> "r"
                            redchecked -> "n"
                            else -> "o"
                        })
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        orientation = resources.configuration.orientation

        transmtting = savedInstanceState?.getBoolean(TRAMS_STATE) ?: false
        leftchecked = savedInstanceState?.getBoolean(LEFT_STATE) ?: false
        rightchecked = savedInstanceState?.getBoolean(RIGHT_STATE) ?: false
        redchecked = savedInstanceState?.getBoolean(RED_STATE) ?: false
        nightchecked = savedInstanceState?.getBoolean(NIGHT_STATE) ?: false
        brakechecked = savedInstanceState?.getBoolean(BRAKE_STATE) ?: false
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //GlobalScope.async {requester.start(true)}
        //requester.broacast = true

        GlobalScope.async {connection = requester.isconnected(sock.await())}
        if(!connection) {
            //GlobalScope.async {ip = (requester.start(true))}
            sock = GlobalScope.async {requester.connect(requester.start(true))}
            GlobalScope.async {
                connection = requester.run(sock.await(), "c")
                connection = requester.run(sock.await(),
                    when {
                        leftchecked -> "l"
                        rightchecked -> "r"
                        redchecked -> "n"
                        else -> "o"
                    })
            }
        }
        val timer = Timer("schedule", true)
        timer.schedule(0, 5000) {
            GlobalScope.async {
                connection = requester.run(sock.await(), "c")
                connection = requester.run(sock.await(),
                        when {
                            leftchecked -> "l"
                            rightchecked -> "r"
                            redchecked -> "n"
                            else -> "o"
                        })
            }
        }
        setSupportActionBar(toolbar)
        //val model = ViewModelProviders.of(this).get(Request::class.java)
        popup = Dialog(this)
        popup?.setContentView(R.layout.popup)
        val sv = videoSurface
        //sv.holder.addCallback(this)
        //sv.surfaceTextureListener
        sv.scaleX = -1f
        sv!!.surfaceTextureListener = this

        anim.duration = 500
        anim.repeatCount = Animation.INFINITE

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        linerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (transmtting) {
            GlobalScope.async { connection = requester.run(sock.await(), "v")}
            cameraButton.visibility = View.GONE
            sv.visibility = View.VISIBLE
        }

        popup?.close?.setOnClickListener {
            brakechecked = false
            popup?.dismiss()
        }

        popup?.accept?.setOnClickListener {
            calibrate = true
        }

        leftButton.setOnCheckedChangeListener { _, _->
            if (connection)  {leftchecked = !leftchecked}}

        rightButton.setOnCheckedChangeListener { _, _ ->
            if (connection) {rightchecked = !rightchecked}}

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            redButton.setOnCheckedChangeListener { _, _ ->
                if (connection) {redchecked = !redchecked}}

            nightButton.setOnCheckedChangeListener { _, _ ->
                if (connection) {nightchecked = !nightchecked}}

            brakeButton.setOnCheckedChangeListener { _, _ ->
                if (connection) {brakechecked = !brakechecked}}
        } else{
            redButtonSmall.setOnClickListener {
                if (connection) {redchecked = !redchecked}}

            nightButtonSmall.setOnClickListener {
                if (connection) {nightchecked = !nightchecked}}

            brakeButtonSmall.setOnClickListener {
                if (connection) {brakechecked = !brakechecked}}
        }

        connButton.setOnClickListener {
            GlobalScope.async {connection = requester.isconnected(sock.await())}
            if(!connection) {
                requester.start(false)
                GlobalScope.async {
                    connection = requester.run(sock.await(), "w")
                    connection = requester.run(sock.await(), "s")
                    delay(1000)
                    requester.stop(sock.await())
                }
                sock = GlobalScope.async {
                    requester = Request()
                    delay(1000)
                    requester.connect(requester.start(true))}
            }
            GlobalScope.async {
                delay(1000)
                connection = requester.run(sock.await(), "c")}
            GlobalScope.async {connection = requester.run(sock.await(),
                    when {
                        leftchecked -> "l"
                        rightchecked -> "r"
                        redchecked -> "n"
                        else -> "o"
                    })
            }
        }

        connButton.setOnLongClickListener {
            /*intent = Intent(applicationContext, MainActivity::class.java)
            val mPendingIntentId: Int = 7
            val mPendingIntent = PendingIntent.getActivity(applicationContext, mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
            val mgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            mgr?.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent)
            exitProcess(0)*/
            requester.start(false)
            GlobalScope.async {
                connection = requester.run(sock.await(), "w")
                connection = requester.run(sock.await(), "s")
                delay(1000)
                requester.stop(sock.await())
            }
            sock = GlobalScope.async {requester.connect(requester.start(true))}
            true
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
                GlobalScope.async {connection = requester.run(sock.await(), (progress/11).toString())}
                bright = progress
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.visibility = View.GONE
                brightButton.visibility = View.VISIBLE
                GlobalScope.async {connection = requester.run(sock.await(),
                        when {
                            leftchecked -> "l"
                            rightchecked -> "r"
                            else -> (bright/11).toString()
                        })
                }
            }
        })

        videoSurface.setOnClickListener {
            if(connection){
                if (transmtting) {
                    GlobalScope.async { connection = requester.run(sock.await(), "w") }
                    sv.visibility = View.GONE
                    cameraButton.visibility = View.VISIBLE
                    transmtting = false
                    GlobalScope.async {connection = requester.run(sock.await(),
                            when {
                                leftchecked -> "l"
                                rightchecked -> "r"
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
                    GlobalScope.async { connection = requester.run(sock.await(),
                            when {
                                leftchecked -> "l"
                                rightchecked -> "r"
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
        savedInstanceState?.putBoolean(LEFT_STATE, leftchecked)
        savedInstanceState?.putBoolean(RIGHT_STATE, rightchecked)
        savedInstanceState?.putBoolean(RED_STATE, redchecked)
        savedInstanceState?.putBoolean(NIGHT_STATE, nightchecked)
        savedInstanceState?.putBoolean(BRAKE_STATE, brakechecked)
        super.onSaveInstanceState(savedInstanceState)
    }


    override fun onResume() {
        super.onResume()
        //requester.broacast = true
        //GlobalScope.async {requester.start(true)}
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

        lightSensor?.also{ light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_UI)
        }
        linerSensor?.also { coordinates  ->
            sensorManager.registerListener(this, coordinates, SensorManager.SENSOR_DELAY_GAME)
        }
        rotationSensor?.also { rotation  ->
            sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME)
        }
        
        if (transmtting) {
            GlobalScope.async { connection = requester.run(sock.await(), "v")}
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        GlobalScope.async { connection = requester.run(sock.await(), "w")}
        GlobalScope.async {connection = requester.run(sock.await(),
                when {
                    leftchecked -> "l"
                    rightchecked -> "r"
                    redchecked -> "n"
                    else -> "o"
                })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requester.start(false)
        vrec?.interrupt()

        //GlobalScope.async {requester.start(false)}
        GlobalScope.async {
            connection = requester.run(sock.await(), "w")
            connection = requester.run(sock.await(), "s")
            delay(1000)
            requester.stop(sock.await())
        }
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
