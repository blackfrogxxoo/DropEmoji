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
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class DropEmojiView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "DropEmojiView";
    private static final int COLUMN_COUNT_COMMENT = 7;
    private static final int MAX_COUNT_COMMENT = 37;
    private static final int DP_PADDING_LEFT_COMMENT = 10;
    private static final int DP_PADDING_RIGHT_COMMENT = 10;
    private static final int DP_PADDING_TOP_COMMENT = 6;
    private static final int DP_PADDING_BOTTOM_COMMENT = 6;
    private static final int DP_PADDING_COMMENT_EMOJI = 0;
    private static final int DP_EMOJI_SIZE = 32;
    private static final int DP_COMMENT_ROUND_CORNER = 10;
    private static final int DURATION_COMMENT_STAY = 2000;
    private static final int DURATION_COMMENT_STAY_FULL_ALPHA = 1700;
    private static final int FULL_ALPHA = 255;
    private List<DropEmoji> dropEmojiList = new CopyOnWriteArrayList<>();
    private List<Emoji> normalEmojiList = new ArrayList<>();
    private float density;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Paint commentPaint;
    private DrawThread drawThread;
    private RectF rect = new RectF();
    private int commentBottom;
    private long lastNormalTimestamp;
    private Callback callback;
    private int baseAlpha = FULL_ALPHA;

    public DropEmojiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setZOrderOnTop(true);
        density = getResources().getDisplayMetrics().density;
        surfaceHolder = getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        surfaceHolder.addCallback(this);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        commentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        commentPaint.setColor(Color.WHITE);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(drawThread == null) {
            drawThread = new DrawThread();
        }
        drawThread.running = true;
        drawThread.start();
        baseAlpha = FULL_ALPHA;
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
        if(normalEmojiList.size() < MAX_COUNT_COMMENT && baseAlpha == FULL_ALPHA) {
            normalEmojiList.add(new Emoji(this, bitmapResId));
            lastNormalTimestamp = System.currentTimeMillis();
        }
        dropEmojiList.add(new DropEmoji(this, bitmapResId));
        if(dropEmojiList.size() == 1) { // 第一次掉三个
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    dropEmojiList.add(new DropEmoji(DropEmojiView.this, bitmapResId));
                }
            }, 200);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    dropEmojiList.add(new DropEmoji(DropEmojiView.this, bitmapResId));
                }
            }, 400);
        }
    }

    public void setCommentBottom(int bottom) {
        this.commentBottom = bottom;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }


    private class DrawThread extends Thread {
        boolean running = true;
        @Override
        public void run() {
            while (running) {
                Canvas canvas = null;
                try {
                    synchronized (surfaceHolder) {
                        canvas = surfaceHolder.lockCanvas();
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        if(normalEmojiList.size() > 0) {
                            drawCommentBar(canvas);
                        }
                        // 画掉落的Emoji
                        for(DropEmoji dropEmoji : dropEmojiList) {
                            if(dropEmoji.isFinished()) {
                                dropEmojiList.remove(dropEmoji);
                            } else {
                                dropEmoji.onDraw(canvas, paint);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();
        return normalEmojiList.size() > 0 && x > rect.left && x < rect.right && y > rect.top && y < rect.bottom;
    }

    private void drawCommentBar(Canvas canvas) {
        // 画输入框 一排7个 最多37个
        int bgAlpha = (int) (baseAlpha * 0.7f);
        commentPaint.setAlpha(bgAlpha);
        rect.left = (int) (10 * density);
        rect.bottom = commentBottom - 10 * density;
        int commentWidth = (int) (density * (DP_PADDING_LEFT_COMMENT
                + DP_PADDING_RIGHT_COMMENT
                + Math.min(7, normalEmojiList.size())
                * (DP_EMOJI_SIZE + 2 * DP_PADDING_COMMENT_EMOJI)));
        int commentHeight = (int) (density * (DP_PADDING_TOP_COMMENT
                + DP_PADDING_BOTTOM_COMMENT
                + Math.max(1, normalEmojiList.size() / COLUMN_COUNT_COMMENT
                + (normalEmojiList.size() % COLUMN_COUNT_COMMENT == 0 ? 0 : 1))
                * (DP_EMOJI_SIZE + 2 * DP_PADDING_COMMENT_EMOJI)));
        rect.right = rect.left + commentWidth;
        rect.top = rect.bottom - commentHeight;
        canvas.drawRoundRect(rect, DP_COMMENT_ROUND_CORNER * density, DP_COMMENT_ROUND_CORNER * density,  commentPaint);
        commentPaint.setAlpha(baseAlpha);
        for(int i = 0; i < normalEmojiList.size(); i ++) {
            Emoji emoji = normalEmojiList.get(i);
            int x = (int) (rect.left + DP_PADDING_LEFT_COMMENT * density
                    + i % COLUMN_COUNT_COMMENT * density * (DP_EMOJI_SIZE + 2 * DP_PADDING_COMMENT_EMOJI));
            int y = (int) (rect.top + DP_PADDING_TOP_COMMENT * density
                    + i / COLUMN_COUNT_COMMENT * density * (DP_EMOJI_SIZE + 2 * DP_PADDING_COMMENT_EMOJI));
            emoji.onDraw(canvas, x, y, density * DP_EMOJI_SIZE, commentPaint);
        }
        // 发送消息
        if(normalEmojiList.size() > 0 && callback != null) {
            long idleDuration = System.currentTimeMillis() - lastNormalTimestamp;
            if(idleDuration > DURATION_COMMENT_STAY_FULL_ALPHA && idleDuration < DURATION_COMMENT_STAY) {
                // 准备发送消息，评论框变淡，暂不能再添加
                baseAlpha = (int) ((1 - 1f * (idleDuration - DURATION_COMMENT_STAY_FULL_ALPHA)
                        / (DURATION_COMMENT_STAY - DURATION_COMMENT_STAY_FULL_ALPHA)) * FULL_ALPHA);
            } else if(System.currentTimeMillis() - lastNormalTimestamp > DURATION_COMMENT_STAY) {
                int[] bitmapResIds = new int[normalEmojiList.size()];
                for (int i = 0; i < bitmapResIds.length; i++) {
                    bitmapResIds[i] = normalEmojiList.get(i).getBitmapResId();
                }
                callback.onSend(bitmapResIds);
                normalEmojiList.clear();
                baseAlpha = FULL_ALPHA;
            }
        }
    }

    public interface Callback {
        void onSend(int[] bitmapResIds);
    }

    static class Emoji {
        private Bitmap bitmap;
        private Matrix matrix;
        private int bitmapResId;

        Emoji(DropEmojiView parent, int bitmapResId) {
            bitmap = BitmapFactory.decodeResource(parent.getResources(), bitmapResId);
            matrix = new Matrix();
            this.bitmapResId = bitmapResId;
        }

        void onDraw(Canvas canvas, int x, int y, float size, Paint paint) {
            matrix.setScale(size / bitmap.getWidth(), size / bitmap.getHeight());
            matrix.postTranslate(x, y);
            canvas.drawBitmap(bitmap, matrix, paint);
        }

        int getBitmapResId() {
            return bitmapResId;
        }
    }

    /**
     * 单个下落的表情
     * 运动轨迹：    Y轴：BounceInterpolator
     *              X轴：AccelerateInterpolator
     * 带旋转
     *
     */
    static class DropEmoji {
        private static final int BASE_X_DURATION = 3000;
        private static final int BASE_Y_DURATION = 2700;
        private static final int BASE_ROTATE_DURATION = 1500;
        private DropEmojiView parent;
        private Point point;
        private Bitmap bitmap;
        private Matrix matrix;
        private ValueAnimator xAnimator;
        private ValueAnimator yAnimator;
        private int fromX, fromY;
        private int toX, toY;
        private float rotateAngle;

        DropEmoji(DropEmojiView parent, int bitmapResId) {
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
            final int xDuration = BASE_X_DURATION * (toY - fromY) / parent.getHeight(); // 初始位置越高，运动时间越长
            xAnimator.setDuration(xDuration);
            xAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int rotateOnceDuration = (int) (BASE_ROTATE_DURATION + Math.random() * BASE_ROTATE_DURATION); // 旋转速度随机
                float speedRate = (float) (Math.random() * 0.5 + 1.2f); // 横向移动速度随机
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    point.set(calculateX(animation.getAnimatedFraction(), speedRate), point.y);
                    rotateAngle = (float) rotateLeft * animation.getAnimatedFraction() * 360 * xDuration / rotateOnceDuration;
                }
            });
            xAnimator.start();
            yAnimator = ValueAnimator.ofFloat(0f, 1f);
            yAnimator.setInterpolator(new BounceInterpolator());
            yAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    point.set(point.x, calculateY(animation.getAnimatedFraction()));
                }
            });
            yAnimator.setDuration(BASE_Y_DURATION * (toY - fromY) / parent.getHeight()); // 初始位置越高，运动时间越长
            yAnimator.start();
        }

        private int calculateX(float fraction, float speedRate) {
            return (int) (fromX + (toX - fromX) * fraction * speedRate);
        }

        private int calculateY(float fraction) {
            return (int) (fromY + (toY - fromY) * fraction);
        }

        void onDraw(Canvas canvas, Paint paint) {
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
