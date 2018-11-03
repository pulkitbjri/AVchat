package play.com.avwebrtc_firebase

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import play.com.avwebrtc_firebase.components.ImageTouchSlider
import java.util.*

class MainActivity : AppCompatActivity() , ImageTouchSlider.OnImageSliderChangedListener{
    override fun onChanged() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }
}
