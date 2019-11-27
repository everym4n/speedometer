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

    private static final float STROKE_WIDTH = 50f;
    private static final float SCALE_RADIUS = 300f;
    private static final RectF SCALE_RECT = new RectF(STROKE_WIDTH / 2, STROKE_WIDTH / 2, 2 * SCALE_RADIUS, 2 * SCALE_RADIUS);

    private static final float SCALE_FONT_SIZE = 40f;
    private static final float CURRENT_SPEED_FONT_SIZE = 60f;

    public static final float SCALE_START_ANGLE = -210f;
    public static final float SCALE_SWEEP_ANGLE = 240f;
    public static final int ZERO_SPEED = 0;

    private static final float ARROW_CIRCLE_RADIUS = 40f;
    public static final float ARROW_BOTTOM_WIDTH = 20f;
    public static final float ARROW_RADIUS = 200f;
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


    public static final float CENTER_X = SCALE_RECT.left + SCALE_RADIUS;
    public static final float CENTER_Y = SCALE_RECT.top + SCALE_RADIUS;


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
        TEXT_SCALE_SPEED_PAINT.setTextSize(SCALE_FONT_SIZE);

        TEXT_CURRENT_SPEED_PAINT.setColor(textColor);
        TEXT_CURRENT_SPEED_PAINT.setStyle(Paint.Style.FILL);
        TEXT_CURRENT_SPEED_PAINT.setTextSize(CURRENT_SPEED_FONT_SIZE);
    }

    private void configureScale() {
        SCALE_PAINT.setStyle(Paint.Style.STROKE);
        SCALE_PAINT.setStrokeWidth(STROKE_WIDTH);

        SweepGradient shader = new SweepGradient(CENTER_X, CENTER_Y,
                new int[]{highSpeedColor,lowSpeedColor, lowSpeedColor, mediumSpeedColor, highSpeedColor},
                new float[]{0.1f, 0.2f, 0.4f, 0.8f, 1.0f});
        SCALE_PAINT.setShader(shader);
    }

    private void configureArrow() {
        ARROW_PAINT.setColor(arrowColor);
        ARROW_PAINT.setStyle(Paint.Style.FILL);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawArc(SCALE_RECT, SCALE_START_ANGLE, SCALE_SWEEP_ANGLE, false, SCALE_PAINT);
        drawText(canvas);
        drawArrow(canvas);
    }


    private void drawText(Canvas canvas) {
        //Текст по центру
        final String speedText = formatString(currentSpeed);
        getTextBounds(speedText, TEXT_CURRENT_SPEED_PAINT);

        float x = CENTER_X - textBounds.width() / 2f - textBounds.left;
        float y = CENTER_Y + getPointByAngle(SCALE_RADIUS, SCALE_START_ANGLE).y;
        canvas.drawText(speedText, x, y, TEXT_CURRENT_SPEED_PAINT);

        //Текст в начале шкалы
        final String zeroSpeedText = formatString(ZERO_SPEED);
        getTextBounds(zeroSpeedText, TEXT_SCALE_SPEED_PAINT);
        PointF startPoint = getPointByAngle(SCALE_RADIUS, SCALE_START_ANGLE);
        x = CENTER_X + startPoint.x - textBounds.width() / 2.0f;
        y = CENTER_Y + startPoint.y + textBounds.height();
        canvas.drawText(zeroSpeedText, x, y, TEXT_SCALE_SPEED_PAINT);

        //Текст в конце шкалы
        final String maxSpeedText = formatString(maxSpeed);
        getTextBounds(maxSpeedText, TEXT_SCALE_SPEED_PAINT);
        PointF endPoint = getPointByAngle(SCALE_RADIUS, SCALE_START_ANGLE + SCALE_SWEEP_ANGLE);
        x = CENTER_X + endPoint.x - textBounds.width() / 2.0f;
        y = CENTER_Y + endPoint.y + textBounds.height();
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
        canvas.drawCircle(CENTER_X, CENTER_Y, ARROW_CIRCLE_RADIUS, ARROW_PAINT);
        Path path = new Path();
        float currentAngle = SCALE_START_ANGLE + SCALE_SWEEP_ANGLE * currentSpeed / maxSpeed;
        PointF point = getPointByAngle(ARROW_BOTTOM_WIDTH, currentAngle - 90);
        point.x += CENTER_X;
        point.y += CENTER_Y;
        path.moveTo(point.x, point.y);
        PointF point2 = getPointByAngle(ARROW_BOTTOM_WIDTH, currentAngle + 90);
        point2.x += CENTER_X;
        point2.y += CENTER_Y;
        path.lineTo(point2.x, point2.y);

        PointF point3 = getPointByAngle(ARROW_RADIUS, currentAngle);
        point3.x += CENTER_X;
        point3.y += CENTER_Y;
        path.lineTo(point3.x, point3.y);
        path.close();
        canvas.drawPath(path, ARROW_PAINT);
    }
}



