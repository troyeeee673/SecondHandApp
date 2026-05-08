package com.example.jianlou.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class CircleImageView extends androidx.appcompat.widget.AppCompatImageView {
    private Paint paint;
    private Matrix matrix;
    private float radius;

    public CircleImageView(Context context) {
        super(context);
        init();
    }

    public CircleImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        matrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            super.onDraw(canvas);
            return;
        }

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap == null) {
            super.onDraw(canvas);
            return;
        }

        // 创建圆形BitmapShader
        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        // 计算缩放比例，使图片居中显示
        float scale;
        int bSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int size = Math.min(getWidth(), getHeight());
        scale = size * 1.0f / bSize;

        matrix.setScale(scale, scale);
        matrix.postTranslate((getWidth() - bSize * scale) / 2, (getHeight() - bSize * scale) / 2);

        shader.setLocalMatrix(matrix);
        paint.setShader(shader);

        // 绘制圆形
        radius = size / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
    }
}