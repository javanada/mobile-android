package com.cscd488_490.team5.i_nav;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import i_nav.Edge;
import i_nav_model.Location;
import i_nav_model.LocationObject;

public class LocationMap extends AppCompatImageView implements TimeAnimator.TimeListener {

    // map objects
    List<LocationObject> objects;
    List<Edge> edges;
    List<Edge> shortestPath;
    String canvas_image;

    // cache
    Map<String, LocationMapCacheItem> locationCache = new HashMap<>();
    Map<String, PathCacheItem> pathCache = new HashMap<>();

    int primaryX;
    int primaryY;
    int secondaryX;
    int secondaryY;

    int primaryImageX;
    int primaryImageY;
    int secondaryImageX;
    int secondaryImageY;

    int currentLocationX;
    int currentLocationY;
    int beaconRadius = 1;

    private int hour;
    private int minute;
    private int second;
    public MyObservable myObservable;

    static final double radius = 600;

    int currentWidth;
    int currentHeight;

    private static long TIMER_MSEC = 200;
    TimeAnimator mTimer;
    private long mLastTime;

    Map<String, Bitmap> objectTypes;

    // Touch Events
    private static final String TAG = "&&&-Touch";
    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    @Override
    public void onTimeUpdate(TimeAnimator timeAnimator, long l, long l1) {
        long now = System.currentTimeMillis();
        if ((now - mLastTime) < TIMER_MSEC)
            return;
        mLastTime = now;
        beaconRadius = (int) (System.currentTimeMillis() / 100) % 30;
//        Log.i("!!!", "beacon radius: " + beaconRadius);
        invalidate();
    }

    public LocationMap(Context context) {
        super(context);
        initialize();
    }

    public LocationMap(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public LocationMap(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }


    public void initialize() {

        set(3, 20, 55);
        myObservable = new MyObservable();

        mTimer = new TimeAnimator();
        mTimer.setTimeListener(this);
        mLastTime = System.currentTimeMillis();
        shortestPath = new ArrayList<Edge>();
        edges = new ArrayList<Edge>();
        objects = new ArrayList<LocationObject>();
//        mTimer.start();
        objectTypes = new HashMap<>();


//        matrix.setTranslate(1f, 1f);
//        setImageMatrix(matrix);
//        setScaleType(ScaleType.MATRIX);
//
//        this.setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//
//                Log.i(TAG, "onTouch");
//
//                if (event.getAction() == MotionEvent.ACTION_UP){
//                    return true;
//                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    savedMatrix.set(matrix);
//                    start.set(event.getX(), event.getY());
//                    Log.d(TAG, "mode=DRAG");
//                    mode = DRAG;
//                    setImageMatrix(matrix);
//                    return true;
//                }
//
//                return false;
//            }
//        });


    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }



        public MyObservable getMyObservable() {
        return myObservable;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public void set(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    int parentWidth;
    int imageWidth;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width < height) {
            setMeasuredDimension((int) (width * 1), (int) (width * 1));
        } else {
//            setMeasuredDimension((int) (height * 0.8), (int) (height * 0.8));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        currentWidth = w;
        currentHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        float scaleFactorWidth = (float)  canvas.getWidth() / 160;
//        float scaleFactorHeight = (float)  canvas.getHeight() / 240;
        float scaleFactorWidth = (float)  (currentWidth / (float) radius * 0.5);
        float scaleFactorHeight = (float)  (currentHeight / (float) radius * 0.5);

//        canvas.translate((int) (currentWidth * 0.5), (int) (currentWidth * 0.5));
//        canvas.scale(scaleFactorWidth, scaleFactorHeight);


        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        int hourVal = gregorianCalendar.get(GregorianCalendar.HOUR_OF_DAY);
        int minuteVal = gregorianCalendar.get(GregorianCalendar.MINUTE);
        int secondVal = gregorianCalendar.get(GregorianCalendar.SECOND);
        set(hourVal, minuteVal, secondVal);

        myObservable.setTime(hourVal, minuteVal, secondVal);


        canvas.drawARGB(0, 255, 255, 255);

        Paint paint = new Paint();

        Path path;
        path = new Path();


        for (LocationObject o : objects) {
//            Log.i("!!!",  "canvas.getWidth(): " + canvas.getWidth() + ", this.currentWidth: " + this.currentWidth + " ...: " + imageWidth);
            paint.setStyle(Paint.Style.FILL);

            if (o.getObject_type_id() == 4) { // primary
                paint.setColor(0xffd81eaa);
                primaryX = o.getX_coordinate();
                primaryY = o.getY_coordinate();
                primaryImageX = o.getImage_x();
                primaryImageY = o.getImage_y();

            } else if (o.getObject_type_id() == 5) { // secondary
                paint.setColor(0xff2155f2);
                secondaryX = o.getX_coordinate();
                secondaryY = o.getY_coordinate();
                secondaryImageX = o.getImage_x();
                secondaryImageY = o.getImage_y();

            } else if (o.getObject_type_id() == 2) { // door
                paint.setColor(0xffa39b28);
            } else {
                paint.setColor(0xff21f278);
            }
//            paint.setColor(Color.argb(255, 0, 0, 0));
//            int i = (findViewById(R.id.linearLayoutId)).getWidth() - this.currentWidth;
            int x = o.getImage_x() + (currentWidth - imageWidth) / 2 - 120; // accounting for admin toolbar width
//            int y = this.currentHeight - o.getImage_y();
            int y = o.getImage_y();

            int c = paint.getColor();

            paint.setColor(Color.BLACK);
            paint.setTextSize(20);
            canvas.drawText("#" + o.getObject_id(), x + 10, y, paint);

            if (objectTypes.containsKey("" + o.getObject_type_id())) {
                canvas.drawBitmap(objectTypes.get("" + o.getObject_type_id()), x - 5, y, paint);
            } else {
                paint.setColor(c);
                canvas.drawCircle(x, y, 10, paint);
            }

        }

        double x_scale = 1;
        if (Math.abs(secondaryImageX - primaryImageX) > 0) {
            x_scale = (double)(secondaryX - primaryX) / (double)(secondaryImageX - primaryImageX);
        }
        double y_scale = 1;
        if (Math.abs(secondaryImageY - primaryImageY) > 0) {
            y_scale = (double)(secondaryY - primaryY) / (double)(secondaryImageY - primaryImageY);
        }
//        Log.i("!!!", "" + primaryX + "," + primaryY + " " + primaryImageX + "," + primaryImageY + " " + secondaryX + "," + secondaryY + " " + secondaryImageX + "," + secondaryImageY);
//        Log.i("!!!", x_scale + "...." + y_scale);

        for (Edge e : edges) {
            int startX = primaryImageX + (int) (e.v1().getX() / x_scale) + (currentWidth - imageWidth) / 2 - 120; // 120 for admin offset
            int startY = primaryImageY + (int) (e.v1().getY() / y_scale);
            int endX = primaryImageX + (int) (e.v2().getX() / x_scale) + (currentWidth - imageWidth) / 2 - 120; // 120 for admin offset
            int endY = primaryImageY + (int) (e.v2().getY() / y_scale);

            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
//            canvas.drawLine(startX, currentHeight - startY, endX, currentHeight - endY, paint);
            canvas.drawLine(startX, startY, endX, endY, paint);
        }

        for (Edge e : shortestPath) {
            int startX = primaryImageX + (int) (e.v1().getX() / x_scale) + (currentWidth - imageWidth) / 2 - 120; // 120 for admin offset
            int startY = primaryImageY + (int) (e.v1().getY() / y_scale);
            int endX = primaryImageX + (int) (e.v2().getX() / x_scale) + (currentWidth - imageWidth) / 2 - 120; // 120 for admin offset
            int endY = primaryImageY + (int) (e.v2().getY() / y_scale);

            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(5);
//            canvas.drawLine(startX, currentHeight - startY, endX, currentHeight - endY, paint);
            canvas.drawLine(startX, startY, endX, endY, paint);
        }

        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        currentLocationX = primaryImageX + 20;
        currentLocationY = primaryImageY + 20;
        canvas.drawCircle(currentLocationX + (currentWidth - imageWidth) / 2, this.currentHeight - currentLocationY, Math.abs(beaconRadius), paint);

    }

    public class MyObservable extends Observable {

        public MyObservable() {

        }

        public void setTime(int hour, int minute, int second) {

            String str = "";
            if (hour < 10) {
                str += "0" + hour;
            } else {
                str += hour;
            }
            str += ":";
            if (minute < 10) {
                str += "0" + minute;
            } else {
                str += minute;
            }
            str += ":";
            if (second < 10) {
                str += "0" + second;
            } else {
                str += second;
            }
            setChanged();
            notifyObservers(str);

        }

        public int getHour() {
            return 0;
        }
    }




}
