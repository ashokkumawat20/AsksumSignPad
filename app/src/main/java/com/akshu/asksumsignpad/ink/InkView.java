package com.akshu.asksumsignpad.ink;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;

public class InkView extends View {
    static final int DEFAULT_FLAGS = 3;
    public static final float DEFAULT_MAX_STROKE_WIDTH = 5.0f;
    public static final float DEFAULT_MIN_STROKE_WIDTH = 1.5f;
    public static final float DEFAULT_SMOOTHING_RATIO = 0.75f;
    static final int DEFAULT_STROKE_COLOR = -16777216;
    static final float FILTER_RATIO_ACCELERATION_MODIFIER = 0.1f;
    static final float FILTER_RATIO_MIN = 0.22f;
    @Deprecated
    public static final int FLAG_DEBUG = Integer.MIN_VALUE;
    public static final int FLAG_INTERPOLATION = 1;
    public static final int FLAG_RESPONSIVE_WIDTH = 2;
    static final float THRESHOLD_ACCELERATION = 3.0f;
    static final float THRESHOLD_VELOCITY = 7.0f;
    Bitmap bitmap;
    Canvas canvas;
    float density;
    RectF dirty;
    int flags;
    private boolean isEmpty;
    ArrayList<InkListener> listeners;
    float maxStrokeWidth;
    float minStrokeWidth;
    Paint paint;
    ArrayList<InkPoint> pointQueue;
    ArrayList<InkPoint> pointRecycle;
    float smoothingRatio;

    public interface InkListener {
        void onInkClear();

        void onInkDraw();
    }

    class InkPoint {
        float c1x;
        float c1y;
        float c2x;
        float c2y;
        long time;
        float velocity;

        /* renamed from: x */
        float f68x;

        /* renamed from: y */
        float f69y;

        InkPoint(float f, float f2, long j) {
            reset(f, f2, j);
        }

        /* access modifiers changed from: 0000 */
        public InkPoint reset(float f, float f2, long j) {
            this.f68x = f;
            this.f69y = f2;
            this.time = j;
            this.velocity = 0.0f;
            this.c1x = f;
            this.c1y = f2;
            this.c2x = f;
            this.c2y = f2;
            return this;
        }

        /* access modifiers changed from: 0000 */
        public boolean equals(float f, float f2) {
            return this.f68x == f && this.f69y == f2;
        }

        /* access modifiers changed from: 0000 */
        public float distanceTo(InkPoint inkPoint) {
            float f = inkPoint.f68x - this.f68x;
            float f2 = inkPoint.f69y - this.f69y;
            return (float) Math.sqrt((double) ((f * f) + (f2 * f2)));
        }

        /* access modifiers changed from: 0000 */
        public float velocityTo(InkPoint inkPoint) {
            return (distanceTo(inkPoint) * 1000.0f) / (((float) Math.abs(inkPoint.time - this.time)) * InkView.this.getDensity());
        }

        /* access modifiers changed from: 0000 */
        public void findControlPoints(InkPoint inkPoint, InkPoint inkPoint2) {
            if (inkPoint != null || inkPoint2 != null) {
                float smoothingRatio = InkView.this.getSmoothingRatio();
                if (inkPoint == null) {
                    this.c2x = this.f68x + (((inkPoint2.f68x - this.f68x) * smoothingRatio) / 2.0f);
                    this.c2y = this.f69y + ((smoothingRatio * (inkPoint2.f69y - this.f69y)) / 2.0f);
                } else if (inkPoint2 == null) {
                    this.c1x = this.f68x + (((inkPoint.f68x - this.f68x) * smoothingRatio) / 2.0f);
                    this.c1y = this.f69y + ((smoothingRatio * (inkPoint.f69y - this.f69y)) / 2.0f);
                } else {
                    this.c1x = (this.f68x + inkPoint.f68x) / 2.0f;
                    this.c1y = (this.f69y + inkPoint.f69y) / 2.0f;
                    this.c2x = (this.f68x + inkPoint2.f68x) / 2.0f;
                    this.c2y = (this.f69y + inkPoint2.f69y) / 2.0f;
                    float distanceTo = distanceTo(inkPoint);
                    float distanceTo2 = distanceTo / (distanceTo(inkPoint2) + distanceTo);
                    float f = this.c1x + ((this.c2x - this.c1x) * distanceTo2);
                    float f2 = this.c1y + ((this.c2y - this.c1y) * distanceTo2);
                    float f3 = this.f68x - f;
                    float f4 = this.f69y - f2;
                    float f5 = 1.0f - smoothingRatio;
                    this.c1x += ((f - this.c1x) * f5) + f3;
                    this.c1y += ((f2 - this.c1y) * f5) + f4;
                    this.c2x += f3 + ((f - this.c2x) * f5);
                    this.c2y += f4 + (f5 * (f2 - this.c2y));
                }
            }
        }
    }

    public InkView(Context context) {
        this(context, 3);
    }

    public InkView(Context context, int i) {
        super(context);
        this.pointQueue = new ArrayList<>();
        this.pointRecycle = new ArrayList<>();
        this.listeners = new ArrayList<>();
        init(i);
    }

    public InkView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public InkView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.pointQueue = new ArrayList<>();
        this.pointRecycle = new ArrayList<>();
        this.listeners = new ArrayList<>();
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.InkView, i, 0);
        int i2 = obtainStyledAttributes.getInt(R.styleable.InkView_inkFlags, 3);
        obtainStyledAttributes.recycle();
        init(i2);
    }

    private void init(int i) {
        setFlags(i);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.density = (displayMetrics.xdpi + displayMetrics.ydpi) / 2.0f;
        this.paint = new Paint();
        this.paint.setStrokeCap(Cap.ROUND);
        this.paint.setAntiAlias(true);
        setColor(-16777216);
        setMaxStrokeWidth(5.0f);
        setMinStrokeWidth(1.5f);
        setSmoothingRatio(0.75f);
        this.dirty = new RectF();
        this.isEmpty = true;
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        clear();
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        this.isEmpty = false;
        if (action == 0) {
            addPoint(getRecycledPoint(motionEvent.getX(), motionEvent.getY(), motionEvent.getEventTime()));
            Iterator it = this.listeners.iterator();
            while (it.hasNext()) {
                ((InkListener) it.next()).onInkDraw();
            }
        } else if (action == 2 && !((InkPoint) this.pointQueue.get(this.pointQueue.size() - 1)).equals(motionEvent.getX(), motionEvent.getY())) {
            addPoint(getRecycledPoint(motionEvent.getX(), motionEvent.getY(), motionEvent.getEventTime()));
        }
        if (action == 1) {
            if (this.pointQueue.size() == 1) {
                draw((InkPoint) this.pointQueue.get(0));
            } else if (this.pointQueue.size() == 2) {
                ((InkPoint) this.pointQueue.get(1)).findControlPoints((InkPoint) this.pointQueue.get(0), null);
                draw((InkPoint) this.pointQueue.get(0), (InkPoint) this.pointQueue.get(1));
            }
            this.pointRecycle.addAll(this.pointQueue);
            this.pointQueue.clear();
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas2) {
        canvas2.drawBitmap(this.bitmap, 0.0f, 0.0f, null);
        super.onDraw(canvas2);
    }

    public void setFlags(int i) {
        this.flags = i;
    }

    public void addFlags(int i) {
        this.flags = i | this.flags;
    }

    public void addFlag(int i) {
        addFlags(i);
    }

    public void removeFlags(int i) {
        this.flags = (i ^ -1) & this.flags;
    }

    public void removeFlag(int i) {
        removeFlags(i);
    }

    public boolean hasFlags(int i) {
        return (i & this.flags) > 0;
    }

    public boolean hasFlag(int i) {
        return hasFlags(i);
    }

    public void clearFlags() {
        this.flags = 0;
    }

    public void addListener(InkListener inkListener) {
        if (!this.listeners.contains(inkListener)) {
            this.listeners.add(inkListener);
        }
    }

    @Deprecated
    public void addInkListener(InkListener inkListener) {
        addListener(inkListener);
    }

    public void removeListener(InkListener inkListener) {
        this.listeners.remove(inkListener);
    }

    @Deprecated
    public void removeInkListener(InkListener inkListener) {
        removeListener(inkListener);
    }

    public void setColor(int i) {
        this.paint.setColor(i);
    }

    public void setMaxStrokeWidth(float f) {
        this.maxStrokeWidth = TypedValue.applyDimension(1, f, getResources().getDisplayMetrics());
    }

    public void setMinStrokeWidth(float f) {
        this.minStrokeWidth = TypedValue.applyDimension(1, f, getResources().getDisplayMetrics());
    }

    public float getSmoothingRatio() {
        return this.smoothingRatio;
    }

    public void setSmoothingRatio(float f) {
        this.smoothingRatio = Math.max(Math.min(f, 1.0f), 0.0f);
    }

    public boolean isViewEmpty() {
        return this.isEmpty;
    }

    public void clear() {
        if (this.bitmap != null) {
            this.bitmap.recycle();
        }
        this.bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        this.canvas = new Canvas(this.bitmap);
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            ((InkListener) it.next()).onInkClear();
        }
        invalidate();
        this.isEmpty = true;
    }

    public Bitmap getBitmap() {
        return getBitmap(0);
    }

    public Bitmap getBitmap(int i) {
        Bitmap createBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        Canvas canvas2 = new Canvas(createBitmap);
        if (i != 0) {
            canvas2.drawColor(i);
        }
        canvas2.drawBitmap(this.bitmap, 0.0f, 0.0f, null);
        return createBitmap;
    }
    public Bitmap getSignatureBitmap() {
        Bitmap originalBitmap = getTransparentSignatureBitmap();
        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        return whiteBgBitmap;
    }
    public Bitmap getTransparentSignatureBitmap() {
        ensureSignatureBitmap();
        return this.bitmap;
    }

    private void ensureSignatureBitmap() {
        if (this.bitmap == null) {
            this.bitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                    Config.ARGB_8888);
            canvas = new Canvas(this.bitmap);
        }
    }
    public void drawBitmap(Bitmap bitmap2, float f, float f2, Paint paint2) {
        this.canvas.drawBitmap(bitmap2, f, f2, paint2);
        invalidate();
    }

    /* access modifiers changed from: 0000 */
    public float getDensity() {
        return this.density;
    }

    /* access modifiers changed from: 0000 */
    public void addPoint(InkPoint inkPoint) {
        this.pointQueue.add(inkPoint);
        int size = this.pointQueue.size();
        if (size == 1) {
            int size2 = this.pointRecycle.size();
            inkPoint.velocity = size2 > 0 ? ((InkPoint) this.pointRecycle.get(size2 - 1)).velocityTo(inkPoint) / 2.0f : 0.0f;
            this.paint.setStrokeWidth(computeStrokeWidth(inkPoint.velocity));
        } else if (size == 2) {
            InkPoint inkPoint2 = (InkPoint) this.pointQueue.get(0);
            inkPoint.velocity = inkPoint2.velocityTo(inkPoint);
            inkPoint2.velocity += inkPoint.velocity / 2.0f;
            inkPoint2.findControlPoints(null, inkPoint);
            this.paint.setStrokeWidth(computeStrokeWidth(inkPoint2.velocity));
        } else if (size == 3) {
            InkPoint inkPoint3 = (InkPoint) this.pointQueue.get(0);
            InkPoint inkPoint4 = (InkPoint) this.pointQueue.get(1);
            inkPoint4.findControlPoints(inkPoint3, inkPoint);
            inkPoint.velocity = inkPoint4.velocityTo(inkPoint);
            draw(inkPoint3, inkPoint4);
            this.pointRecycle.add(this.pointQueue.remove(0));
        }
    }

    /* access modifiers changed from: 0000 */
    public InkPoint getRecycledPoint(float f, float f2, long j) {
        if (this.pointRecycle.size() != 0) {
            return ((InkPoint) this.pointRecycle.remove(0)).reset(f, f2, j);
        }
        InkPoint inkPoint = new InkPoint(f, f2, j);
        return inkPoint;
    }

    /* access modifiers changed from: 0000 */
    public float computeStrokeWidth(float f) {
        if (hasFlags(2)) {
            return this.maxStrokeWidth - ((this.maxStrokeWidth - this.minStrokeWidth) * Math.min(f / THRESHOLD_VELOCITY, 1.0f));
        }
        return this.maxStrokeWidth;
    }

    /* access modifiers changed from: 0000 */
    public void draw(InkPoint inkPoint) {
        this.paint.setStyle(Style.FILL);
        this.canvas.drawCircle(inkPoint.f68x, inkPoint.f69y, this.paint.getStrokeWidth() / 2.0f, this.paint);
        invalidate();
    }

    /* access modifiers changed from: 0000 */
    public void draw(InkPoint inkPoint, InkPoint inkPoint2) {
        InkPoint inkPoint3 = inkPoint;
        InkPoint inkPoint4 = inkPoint2;
        this.dirty.left = Math.min(inkPoint3.f68x, inkPoint4.f68x);
        this.dirty.right = Math.max(inkPoint3.f68x, inkPoint4.f68x);
        this.dirty.top = Math.min(inkPoint3.f69y, inkPoint4.f69y);
        this.dirty.bottom = Math.max(inkPoint3.f69y, inkPoint4.f69y);
        this.paint.setStyle(Style.STROKE);
        float min = Math.min(((Math.abs((inkPoint4.velocity - inkPoint3.velocity) / ((float) (inkPoint4.time - inkPoint3.time))) * FILTER_RATIO_ACCELERATION_MODIFIER) / THRESHOLD_ACCELERATION) + FILTER_RATIO_MIN, 1.0f);
        float computeStrokeWidth = computeStrokeWidth(inkPoint4.velocity);
        float strokeWidth = this.paint.getStrokeWidth();
        float f = (computeStrokeWidth * min) + ((1.0f - min) * strokeWidth);
        float f2 = f - strokeWidth;
        if (hasFlags(1)) {
            int sqrt = (int) (Math.sqrt(Math.pow((double) (inkPoint4.f68x - inkPoint3.f68x), 2.0d) + Math.pow((double) (inkPoint4.f69y - inkPoint3.f69y), 2.0d)) / 5.0d);
            float f3 = 1.0f / ((float) (sqrt + 1));
            float f4 = f3 * f3;
            float f5 = f4 * f3;
            float f6 = f3 * THRESHOLD_ACCELERATION;
            float f7 = f4 * THRESHOLD_ACCELERATION;
            float f8 = f4 * 6.0f;
            float f9 = 6.0f * f5;
            float f10 = (inkPoint3.f68x - (inkPoint3.c2x * 2.0f)) + inkPoint4.c1x;
            float f11 = (inkPoint3.f69y - (inkPoint3.c2y * 2.0f)) + inkPoint4.c1y;
            float f12 = (((inkPoint3.c2x - inkPoint4.c1x) * THRESHOLD_ACCELERATION) - inkPoint3.f68x) + inkPoint4.f68x;
            float f13 = f;
            float f14 = (((inkPoint3.c2y - inkPoint4.c1y) * THRESHOLD_ACCELERATION) - inkPoint3.f69y) + inkPoint4.f69y;
            float f15 = ((inkPoint3.c2x - inkPoint3.f68x) * f6) + (f10 * f7) + (f12 * f5);
            float f16 = ((inkPoint3.c2y - inkPoint3.f69y) * f6) + (f7 * f11) + (f5 * f14);
            float f17 = f12 * f9;
            float f18 = (f10 * f8) + f17;
            float f19 = f14 * f9;
            float f20 = (f11 * f8) + f19;
            float f21 = inkPoint3.f68x;
            int i = 0;
            float f22 = inkPoint3.f69y;
            float f23 = f21;
            float f24 = f18;
            float f25 = f20;
            while (true) {
                int i2 = i + 1;
                if (i >= sqrt) {
                    break;
                }
                float f26 = f23 + f15;
                float f27 = f22 + f16;
                float f28 = f2;
                this.paint.setStrokeWidth(((((float) i2) * f2) / ((float) sqrt)) + strokeWidth);
                float f29 = f27;
                int i3 = i2;
                this.canvas.drawLine(f23, f22, f26, f27, this.paint);
                f15 += f24;
                f16 += f25;
                f24 += f17;
                f25 += f19;
                this.dirty.left = Math.min(this.dirty.left, f26);
                this.dirty.right = Math.max(this.dirty.right, f26);
                f22 = f29;
                this.dirty.top = Math.min(this.dirty.top, f22);
                this.dirty.bottom = Math.max(this.dirty.bottom, f22);
                f23 = f26;
                i = i3;
                f2 = f28;
            }
            this.paint.setStrokeWidth(f13);
            InkPoint inkPoint5 = inkPoint2;
            this.canvas.drawLine(f23, f22, inkPoint5.f68x, inkPoint5.f69y, this.paint);
        } else {
            this.canvas.drawLine(inkPoint3.f68x, inkPoint3.f69y, inkPoint4.f68x, inkPoint4.f69y, this.paint);
            this.paint.setStrokeWidth(f);
        }
        invalidate((int) (this.dirty.left - (this.maxStrokeWidth / 2.0f)), (int) (this.dirty.top - (this.maxStrokeWidth / 2.0f)), (int) (this.dirty.right + (this.maxStrokeWidth / 2.0f)), (int) (this.dirty.bottom + (this.maxStrokeWidth / 2.0f)));
    }
}
