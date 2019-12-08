package com.andreveryman.speedometer;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SpeedoMeterView extends View {

    private static final Paint ARROW_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint SCALE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint TEXT_SCALE_SPEED_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint TEXT_CURRENT_SPEED_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final float STROKE_WIDTH_MULTIPLIER = 0.1f;
    public static final float ARROW_RADIUS_MULTIPLIER = 0.98f;
    public static final float ARROW_CIRCLE_RADIUS_MULTIPLIER = 0.1f;
    public static final float SCALE_FONT_SIZE_MULTIPLIER = 0.1f;
    public static final float CURRENT_SPEED_FONT_SIZE_MULTIPLIER = 0.3f;
    public static final float SCALE_START_ANGLE = -210f;
    public static final float SCALE_SWEEP_ANGLE = 240f;
    public static final int ZERO_SPEED = 0;

    private   float strokeWidth = 50f;
    private   float scaleRadius = 300f;
    private   RectF scaleRect = new RectF(strokeWidth / 2, strokeWidth / 2, 2 * scaleRadius, 2 * scaleRadius);
    //Границы отрисовываемого содержимого с учетом паддинга
    private RectF viewRect = new RectF(0,0,0,0);

    private  float scaleFontSize = 40f;
    private float currentSpeedFontSize = 60f;



    private  float arrowCircleRadius = 40f;
    public  float arrowBottomWidth = 20f;
    public  float arrowRadius = 200f;
    public static final int MAX_SPEED_DEFAULT = 300;

    private Rect textBounds = new Rect();
    private int currentSpeed;
    private int maxSpeed;

    @ColorInt
    private int textColor;

    @ColorInt
    private int arrowColor;

    @ColorInt
    private int lowSpeedColor;
    @ColorInt
    private int mediumSpeedColor;
    @ColorInt
    private int highSpeedColor;


    public   float centerX = scaleRect.left + scaleRadius;
    public   float centerY = scaleRect.top + scaleRadius;
    private float[] positions;


    public SpeedoMeterView(Context context) {
        this(context, null, R.attr.speedometerStyle);
    }

    public SpeedoMeterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.speedometerStyle);
    }

    public SpeedoMeterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, defStyleAttr, attrs);
    }

    public int getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(int currentSpeed) {
        this.currentSpeed = currentSpeed;
        invalidate();
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
        invalidate();
    }

    private String formatString(int progress) {
        return String.format("%d km/h", progress);
    }

    private void getTextBounds(@NonNull String progressString, Paint paint) {
        paint.getTextBounds(progressString, 0, progressString.length(), textBounds);
    }

    private void init(@NonNull Context context, int defStyleAttr, @Nullable AttributeSet attrs) {
        extractAttributes(context, defStyleAttr, attrs);
        configureArrow();
        configureScale();
        configureText();
    }


    private void extractAttributes(@NonNull Context context, int defStyleAttr, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            final Resources.Theme theme = context.getTheme();
            final TypedArray typedArray = theme.obtainStyledAttributes(attrs,
                    R.styleable.SpeedoMeterView, defStyleAttr, 0);
            try {
                maxSpeed = typedArray.getInt(R.styleable.SpeedoMeterView_maxSpeed, MAX_SPEED_DEFAULT);
                currentSpeed = typedArray.getInt(R.styleable.SpeedoMeterView_currentSpeed, 0);

                textColor = typedArray.getColor(R.styleable.SpeedoMeterView_textColor, Color.BLACK);
                arrowColor = typedArray.getColor(R.styleable.SpeedoMeterView_arrowColor, Color.RED);
                lowSpeedColor = typedArray.getColor(R.styleable.SpeedoMeterView_lowSpeedColor, Color.RED);
                mediumSpeedColor = typedArray.getColor(R.styleable.SpeedoMeterView_mediumSpeedColor, Color.YELLOW);
                highSpeedColor = typedArray.getColor(R.styleable.SpeedoMeterView_highSpeedColor, Color.GREEN);


            } finally {
                typedArray.recycle();
            }
        }
    }


    private void configureText() {
        TEXT_SCALE_SPEED_PAINT.setColor(textColor);
        TEXT_SCALE_SPEED_PAINT.setStyle(Paint.Style.FILL);
        TEXT_SCALE_SPEED_PAINT.setTextSize(scaleFontSize);

        TEXT_CURRENT_SPEED_PAINT.setColor(textColor);
        TEXT_CURRENT_SPEED_PAINT.setStyle(Paint.Style.FILL);
        TEXT_CURRENT_SPEED_PAINT.setTextSize(currentSpeedFontSize);
    }

    private void configureScale() {
        SCALE_PAINT.setStyle(Paint.Style.STROKE);
        SCALE_PAINT.setStrokeWidth(strokeWidth);


    }

    private void configureArrow() {
        ARROW_PAINT.setColor(arrowColor);
        ARROW_PAINT.setStyle(Paint.Style.FILL);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);


        //Считаем доступное пространство с учетом отступов
        int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int heightSize = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();


        //если никаких ограничений нет, берем размер экрана в качестве ограничения
        if(widthSize <= 0 && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)
            widthSize = getResources().getDisplayMetrics().widthPixels - getPaddingLeft() - getPaddingRight();
        if(heightSize <= 0 && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED)
            heightSize = getResources().getDisplayMetrics().heightPixels - getPaddingTop() - getPaddingBottom();




        //Требуемый размер это сплюснутый квадрат, так как низ дуги не отрисовывается
        //Если высота больше ширины, то считаем что view займет всю доступную ширину, а высоту считаем
        //если ширина больше высоты, то наборот
        int requestedWidth = 0;
        int requestedHeight = 0;
        if(heightSize>=widthSize){
            requestedWidth = widthSize;
            requestedHeight = getExpectedHeightFromWidth(widthSize);
        }else{
            requestedHeight = heightSize;
            requestedWidth = getExpectedWidthFromHeight(heightSize);
        }

        requestedWidth += getPaddingLeft() + getPaddingRight();
        requestedHeight += getPaddingTop() + getPaddingBottom();
        if(requestedWidth<getSuggestedMinimumWidth()) requestedWidth = getSuggestedMinimumWidth();
        if(requestedHeight<getSuggestedMinimumHeight())requestedHeight = getSuggestedMinimumHeight();

        requestedWidth = resolveSize(requestedWidth,widthMeasureSpec);
        requestedHeight = resolveSize(requestedHeight,heightMeasureSpec);

        setMeasuredDimension(requestedWidth, requestedHeight);
    }




    public int getExpectedHeightFromWidth(float width){
        return  (int) ( width/2+
                    + getPointByAngle(width/2,SCALE_START_ANGLE).y
                   + width*SCALE_FONT_SIZE_MULTIPLIER
                  + width*STROKE_WIDTH_MULTIPLIER/2);

    }


    public int getExpectedWidthFromHeight(float height){
        return (int) (height / ( Math.sin(Math.toRadians(SCALE_START_ANGLE))/2
                        + 0.5f
                        + SCALE_FONT_SIZE_MULTIPLIER
                        + STROKE_WIDTH_MULTIPLIER/2
                        ));
    }




    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //Масштабируем элементы для отрисовки спидометра с учетом доступного пространства

        //Получаем пространство где можно рисовать с учетом отступов
        viewRect.left = getPaddingLeft();
        viewRect.right = w - getPaddingRight();
        viewRect.top = getPaddingTop();
        viewRect.bottom = h - getPaddingBottom();
        float size;

        //Считаем диаметр шкалы основываясь на измерениях доступного пространства
        if(viewRect.height()>=viewRect.width()){
            size =viewRect.width();
        }else{
            size = (getExpectedWidthFromHeight(viewRect.height()));
        }


        //Считаем толщину шкалы, радиус шкалы, прямоугольник в котором она отрисовывается
        strokeWidth = size * STROKE_WIDTH_MULTIPLIER;
        SCALE_PAINT.setStrokeWidth(strokeWidth);
        scaleRadius = (size - strokeWidth) / 2;
        scaleRect.left = viewRect.left+strokeWidth/2;
        scaleRect.top = viewRect.top+strokeWidth/2;
        scaleRect.right = scaleRect.left + scaleRadius*2;
        scaleRect.bottom = scaleRect.top+scaleRadius*2;

        //Центр шкалы
        centerY = scaleRect.top+scaleRadius;
        centerX = scaleRect.left+scaleRadius;

        //Радиус стрелки, круга в центре, толщина стрелки
        arrowRadius = (scaleRadius - strokeWidth/2)* ARROW_RADIUS_MULTIPLIER;
        arrowCircleRadius = scaleRadius * ARROW_CIRCLE_RADIUS_MULTIPLIER;
        arrowBottomWidth = arrowCircleRadius /2;

        //Шрифты спидометра
        scaleFontSize = size * SCALE_FONT_SIZE_MULTIPLIER;
        currentSpeedFontSize = arrowRadius * CURRENT_SPEED_FONT_SIZE_MULTIPLIER;
        TEXT_CURRENT_SPEED_PAINT.setTextSize(currentSpeedFontSize);
        TEXT_SCALE_SPEED_PAINT.setTextSize(scaleFontSize);

        //Задание шейдера с учетом нового центра
        SweepGradient shader = new SweepGradient(centerX, centerY,
                new int[]{highSpeedColor,lowSpeedColor, lowSpeedColor, mediumSpeedColor, highSpeedColor},
                positions);
        SCALE_PAINT.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawArc(scaleRect, SCALE_START_ANGLE, SCALE_SWEEP_ANGLE, false, SCALE_PAINT);
        drawText(canvas);
        drawArrow(canvas);
    }


    private void drawText(Canvas canvas) {
        //Текст по центру
        final String speedText = formatString(currentSpeed);
        getTextBounds(speedText, TEXT_CURRENT_SPEED_PAINT);

        float x = centerX - textBounds.width() / 2f - textBounds.left;
        float y = centerY + getPointByAngle(scaleRadius, SCALE_START_ANGLE).y;
        canvas.drawText(speedText, x, y, TEXT_CURRENT_SPEED_PAINT);

        //Текст в начале шкалы
        final String zeroSpeedText = formatString(ZERO_SPEED);
        getTextBounds(zeroSpeedText, TEXT_SCALE_SPEED_PAINT);
        PointF startPoint = getPointByAngle(scaleRadius, SCALE_START_ANGLE);
        x = scaleRect.left;
        y = centerY + startPoint.y + +strokeWidth/2+ textBounds.height();
        canvas.drawText(zeroSpeedText, x, y, TEXT_SCALE_SPEED_PAINT);

        //Текст в конце шкалы
        final String maxSpeedText = formatString(maxSpeed);
        getTextBounds(maxSpeedText, TEXT_SCALE_SPEED_PAINT);
        PointF endPoint = getPointByAngle(scaleRadius, SCALE_START_ANGLE + SCALE_SWEEP_ANGLE);
        x = scaleRect.right - textBounds.width();
        y = centerY + endPoint.y + strokeWidth/2+textBounds.height();
        canvas.drawText(maxSpeedText, x, y, TEXT_SCALE_SPEED_PAINT);
    }


    //Получить координаты точки на шкале по углу в градусах
    private PointF getPointByAngle(float radius, float angle) {
        float x = (float) (radius * Math.cos(Math.toRadians(angle)));
        float y = (float) (radius * Math.sin(Math.toRadians(angle)));
        return new PointF(x, y);
    }

    //Рисование стрелки
    private void drawArrow(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, arrowCircleRadius, ARROW_PAINT);
        Path path = new Path();
        float currentAngle = SCALE_START_ANGLE + SCALE_SWEEP_ANGLE * currentSpeed / maxSpeed;
        PointF point = getPointByAngle(arrowBottomWidth, currentAngle - 90);
        point.x += centerX;
        point.y += centerY;
        path.moveTo(point.x, point.y);
        PointF point2 = getPointByAngle(arrowBottomWidth, currentAngle + 90);
        point2.x += centerX;
        point2.y += centerY;
        path.lineTo(point2.x, point2.y);

        PointF point3 = getPointByAngle(arrowRadius, currentAngle);
        point3.x += centerX;
        point3.y += centerY;
        path.lineTo(point3.x, point3.y);
        path.close();
        canvas.drawPath(path, ARROW_PAINT);
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final SpeedometerSavedState savedState = (SpeedometerSavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentSpeed = savedState.currentSpeed;
    }


    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        SpeedometerSavedState speedometerSavedState = new SpeedometerSavedState(super.onSaveInstanceState());
        speedometerSavedState.currentSpeed = this.currentSpeed;
        return speedometerSavedState;
    }


    private static class SpeedometerSavedState extends BaseSavedState implements Parcelable{
        private int currentSpeed;
        SpeedometerSavedState(Parcelable parcelable){
            super(parcelable);
        }

        SpeedometerSavedState(Parcel source) {
            super(source);
            currentSpeed = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(currentSpeed);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SpeedometerSavedState> CREATOR = new Creator<SpeedometerSavedState>() {
            @Override
            public SpeedometerSavedState createFromParcel(Parcel in) {
                return new SpeedometerSavedState(in);
            }

            @Override
            public SpeedometerSavedState[] newArray(int size) {
                return new SpeedometerSavedState[size];
            }
        };
    }
}



