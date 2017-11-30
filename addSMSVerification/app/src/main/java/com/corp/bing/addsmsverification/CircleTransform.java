package com.corp.bing.addsmsverification;

/**
 * Created by Administrator on 2017-11-30.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

/**
 * transform bitmap circle image
 * extension bitmap transformation
 * BitmapTransformation -> Glide Library
 */
public class CircleTransform  extends BitmapTransformation {
    public CircleTransform(Context context) {
        super(context);
    }

    /**
     * Bitmap transform
     * @param pool              Object in Glide library
     * @param toTransform       Target Bitmap Image
     * @param outWidth          Bitmap's width
     * @param outHeight         Bitmap's height
     * @return                  Crop circle Bitmap image
     */
    @Override protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        return circleCrop(pool, toTransform);
    }

    /**
     * Bitmap Crop circle image
     * @param pool          Object in Glide library
     * @param source        Target Bitmap Object
     * @return              circle crop bitmap
     */
    private static Bitmap circleCrop(BitmapPool pool, Bitmap source) {
        if (source == null) return null;
        // bitmap의 가로길이와 세로 길이 중, 짧은 것을 가져옴
        // 긴 값으로 뺄 경우, 짧은 쪽에서 -가 나오거나 좌표가 가운데가 안나옴
        int size = Math.min(source.getWidth(), source.getHeight());
        // bitmap의 중심점을 구함(x, y좌표)
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        // TODO this could be acquired from the pool too
        // 구한 좌표값에 따라 bitmap을 생성
        // 정사각형 또는 완벽한 원을 만들기 위해 작은 값으로 width와 height를 대입.
        Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);
        // Params : Bitmap width, Bitmap height, Bitmap.Config
        Bitmap result = pool.get(size, size, Bitmap.Config.ARGB_8888);
        if (result == null) {
            // BitmapPool -> Bitmap
            result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
        paint.setAntiAlias(true);
        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        return result;
    }

    @Override public String getId() {
        return getClass().getName();
    }
}