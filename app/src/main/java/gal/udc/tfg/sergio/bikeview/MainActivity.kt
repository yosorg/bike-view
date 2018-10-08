package gal.udc.tfg.sergio.bikeview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.longToast
import org.jetbrains.anko.uiThread
import android.R.attr.button
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.animation.Animation
import android.view.animation.AlphaAnimation
import gal.udc.tfg.sergio.bikeview.R.attr.color
import kotlinx.coroutines.experimental.delay


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ip = "192.168.43.251"
        val sock = async {Request(ip, "l").start() }
        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 500
        anim.repeatCount = Animation.INFINITE

        leftButton.setOnCheckedChangeListener { buttonView, isChecked->
            async {Request(ip, if (isChecked) "l" else "o").run(sock.await())}
            if (isChecked) {
                leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                leftButton.startAnimation(anim)
                rightButton.isChecked = false
            } else {
                leftButton.clearAnimation()
                leftButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.background_light))
            }

        }
        rightButton.setOnCheckedChangeListener { buttonView, isChecked ->
            async {Request(ip, if (isChecked) "r" else "o").run(sock.await())}
            if (isChecked) {
                rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                rightButton.startAnimation(anim)
                leftButton.isChecked = false
            } else {
                rightButton.clearAnimation()
                rightButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.background_light))
            }
        }
        redButton.setOnCheckedChangeListener { buttonView, isChecked ->
            async {Request(ip, if (isChecked) "n" else "o").run(sock.await())}
        }
        nightButton.setOnCheckedChangeListener { buttonView, isChecked ->
            async {Request(ip, if (isChecked) "n" else "o").run(sock.await())}
        }
        brakeButton.setOnCheckedChangeListener { buttonView, isChecked ->
            async {Request(ip, if (isChecked) "b" else "o").run(sock.await())}
        }
    }
}
    