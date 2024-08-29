package com.wintercruel.puremusic.tools;

import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class BrightnessTransformation extends BitmapTransformation {

    private final float brightnessMultiplier;

    public BrightnessTransformation(float brightnessMultiplier) {
        this.brightnessMultiplier = brightnessMultiplier;
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        Bitmap.Config config = toTransform.getConfig() != null ? toTransform.getConfig() : Bitmap.Config.ARGB_8888;
        Bitmap bitmap = pool.get(toTransform.getWidth(), toTransform.getHeight(), config);

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[]{
                brightnessMultiplier, 0, 0, 0, 0,
                0, brightnessMultiplier, 0, 0, 0,
                0, 0, brightnessMultiplier, 0, 0,
                0, 0, 0, 1, 0
        });

        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(toTransform, 0, 0, paint);

        return bitmap;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(("brightness" + brightnessMultiplier).getBytes());
    }
}
