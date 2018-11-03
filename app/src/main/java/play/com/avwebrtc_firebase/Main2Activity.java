package play.com.avwebrtc_firebase;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import static java.security.AccessController.getContext;

public class Main2Activity extends AppCompatActivity {

    FloatingActionButton call;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        call=findViewById(R.id.call);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);

        int h=displayMetrics.heightPixels;
        RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams1.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams1.bottomMargin = h/9;
        call.setLayoutParams(layoutParams1);


        call.setOnTouchListener((v, event) -> {
//            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
            int width = v.getWidth();
            float xPos = event.getRawX();
            float yPos = event.getRawY();

            if (event.getAction()== MotionEvent.ACTION_MOVE)
            {
                    layoutParams1.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                    layoutParams.topMargin= (int) (event.getRawY());
                    Log.i("", "onCreate: "+layoutParams.topMargin+"     "+h+"    "+convertPxToDp(Main2Activity.this,h));
                    if (layoutParams.topMargin>=h/2 && layoutParams.topMargin<=3*h/4)
                    {
                        call.setLayoutParams(layoutParams);

                    }


            }
            return false;
        });
    }
    public float convertPxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public float convertDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
