package com.cy.helmet.video.entity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Watermark {
    public Bitmap mBitmap;
    public int width;
    public int height;
    public int orientation;
    public int vMargin;
    public int hMargin;

    public Watermark(int width, int height, int orientation, int vmargin, int hmargin) {
        this.width = 400;
        this.height = 100;
        this.orientation = orientation;
        vMargin = vmargin;
        hMargin = hmargin;
    }


    public Bitmap getWatermarkBitmap() {

        String time = getFormatDate();

        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(mBitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawColor(Color.TRANSPARENT);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);

        Paint.FontMetrics fm = paint.getFontMetrics();
        int textWidth = (int) paint.measureText(time);
        int textHeight = (int) Math.ceil(fm.descent - fm.ascent);

        int posX = width / 2 - textWidth / 2;
        int posY = (height - textHeight) / 2 + 10;

        canvas.drawText(time, posX, posY, paint);

        return mBitmap;
    }

    private String getFormatDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return format.format(date);
    }
}
