package com.example.myapplication.dropemoji;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateInterpolator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class DropEmojiView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "DropEmojiView";
    private List<Emoji> emojiList = new CopyOnWriteArrayList<>();
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private DrawThread drawThread;
    private long lastAddTime;

    public DropEmojiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setZOrderOnTop(true);
        surfaceHolder = getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        surfaceHolder.addCallback(this);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(drawThread == null) {
            drawThread = new DrawThread();
        }
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(drawThread != null) {
            drawThread.running = false;
            drawThread = null;
        }
    }

    public void addEmoji(final int bitmapResId) {
        if(System.currentTimeMillis() - lastAddTime < 500) {
            return;
        }
        lastAddTime = System.currentTimeMillis();
        emojiList.add(new Emoji(this, bitmapResId));
        postDelayed(new Runnable() {
            @Override
            public void run() {
                emojiList.add(new Emoji(DropEmojiView.this, bitmapResId));
            }
        }, 200);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                emojiList.add(new Emoji(DropEmojiView.this, bitmapResId));
            }
        }, 400);
    }

    private class DrawThread extends Thread {
        boolean running = true;
        @Override
        public void run() {
            while (true) {
                if(running) {
                    Canvas canvas = null;
                    try {
                        synchronized (surfaceHolder) {
                            canvas = surfaceHolder.lockCanvas();
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            for(Emoji emoji : emojiList) {
                                if(emoji.isFinished()) {
                                    emojiList.remove(emoji);
                                } else {
                                    emoji.onDraw(canvas, paint);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (canvas != null) {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(6);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 单个下落的表情
     * 运动轨迹：    Y轴：BounceInterpolator
     *              X轴：AccelerateInterpolator
     * 带旋转
     *
     */
    static class Emoji {
        private DropEmojiView parent;
        private Point point;
        private Bitmap bitmap;
        private Matrix matrix;
        private ValueAnimator xAnimator;
        private ValueAnimator yAnimator;
        private int fromX, fromY;
        private int toX, toY;
        private float rotateAngle;

        Emoji(DropEmojiView parent, int bitmapResId) {
            this.parent = parent;
            bitmap = BitmapFactory.decodeResource(parent.getResources(), bitmapResId);
            init();
        }

        private void init() {
            matrix = new Matrix();
            final int rotateLeft = Math.random() > 0.5 ? -1 : 1;
            fromX = (int) (parent.getWidth() / 2 + (0.5 - Math.random()) * parent.getWidth() / 2); // 控件的中间范围
            fromY = (int) (parent.getTop() - Math.random() * parent.getHeight() / 2); // 随机高度（控件上方0 ~ View高度/2）
            point = new Point(fromX, fromY);
            toX = rotateLeft < 0 ? parent.getLeft() - bitmap.getWidth() : parent.getWidth(); // 控件左右
            toY = parent.getHeight() - bitmap.getHeight();
            xAnimator = ValueAnimator.ofFloat(0f, 1f);
            xAnimator.setInterpolator(new AccelerateInterpolator());
            final int xDuration = 2500 * (toY - fromY) / parent.getHeight(); // 初始位置越高，运动时间越长
            xAnimator.setDuration(xDuration);
            xAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int rotateOnceDuration = (int) (1000 + Math.random() * 1000); // 旋转速度随机
                float speedRate = (float) (Math.random() * 0.5 + 1.2f); // 横向移动速度随机
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    point.set(calculateX(animation.getAnimatedFraction(), speedRate), point.y);
                    rotateAngle = (float) rotateLeft * animation.getAnimatedFraction() * 360 * xDuration / rotateOnceDuration;
                }
            });
            xAnimator.start();
            yAnimator = ValueAnimator.ofFloat(0f, 1f);
            yAnimator.setInterpolator(new DropEmojiBounceInterpolator());
            yAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    point.set(point.x, calculateY(animation.getAnimatedFraction()));
                }
            });
            yAnimator.setDuration(2000 * (toY - fromY) / parent.getHeight()); // 初始位置越高，运动时间越长
            yAnimator.start();
        }

        private int calculateX(float fraction, float speedRate) {
            return (int) (fromX + (toX - fromX) * fraction * speedRate);
        }

        private int calculateY(float fraction) {
            return (int) (fromY + (toY - fromY) * fraction);
        }

        public void onDraw(Canvas canvas, Paint paint) {
            matrix.setRotate(rotateAngle, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
            matrix.postTranslate(point.x, point.y);
            canvas.drawBitmap(bitmap, matrix, paint);
        }

        boolean isFinished() {
            // xAnimator比yAnimator时间长，以xAnimator结束为准
            return xAnimator != null && !xAnimator.isRunning();
        }

    }
}
