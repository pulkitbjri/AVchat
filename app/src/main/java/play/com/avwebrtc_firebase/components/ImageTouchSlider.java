package play.com.avwebrtc_firebase.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import play.com.avwebrtc_firebase.R;

public class ImageTouchSlider extends RelativeLayout implements View.OnTouchListener {

private Context mContext;

private ImageView mImage;
private int mScreenWidthInPixel;
private int mScreenWidthInDp;
private float mDensity;

private int mPaddingInDp = 15;
private int mPaddingInPixel;

private int mLengthOfSlider;

public interface OnImageSliderChangedListener{
    void onChanged();
}

private OnImageSliderChangedListener mOnImageSliderChangedListener;

public ImageTouchSlider(Context context) {
    super(context);
    mContext = context;
    createView();
}

public ImageTouchSlider(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
    createView();
}

public ImageTouchSlider(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mContext = context;
    createView();
}

public void createView() {
    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.image_touch_slider, this, true);

    mImage = (ImageView) findViewById(R.id.slider);
    mImage.setOnTouchListener(this);

    WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    Display display = manager.getDefaultDisplay();
    DisplayMetrics outMetrics = new DisplayMetrics ();
    display.getMetrics(outMetrics);

    mDensity  = getResources().getDisplayMetrics().density;
    float dpWidth  = outMetrics.widthPixels / mDensity;
    mScreenWidthInPixel = outMetrics.widthPixels;
    mScreenWidthInDp = (int) (mScreenWidthInPixel / mDensity);

    mLengthOfSlider = (int) (mScreenWidthInDp - mPaddingInDp*2);
}

@Override
public boolean onTouch(View v, MotionEvent event) {
    LayoutParams layoutParams = (LayoutParams) v.getLayoutParams();
    int width = v.getWidth();
    float xPos = event.getRawX();

    switch(event.getAction()) {
    case MotionEvent.ACTION_DOWN:
        // You can add some clicked reaction here.
        break;
    case MotionEvent.ACTION_MOVE:
        if(xPos < (mScreenWidthInPixel - width - mPaddingInDp*mDensity) && xPos > mPaddingInDp*mDensity) {
            mOnImageSliderChangedListener.onChanged();
            layoutParams.leftMargin = (int) xPos - width / 2;
            mImage.setLayoutParams(layoutParams);
        }
        break;
    case MotionEvent.ACTION_UP:
        break;
    default:
        break;
    }

    return true;
}

public void setOnImageSliderChangedListener(OnImageSliderChangedListener listener) {
    mOnImageSliderChangedListener = listener;
}

}