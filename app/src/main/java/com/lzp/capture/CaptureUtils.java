package com.lzp.capture;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ListViewCompat;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;

public final class CaptureUtils {
    private static final String TAG = CaptureUtils.class.getSimpleName();

    public static final boolean capture(@NonNull View view, @NonNull File file, int quality) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);

            doCapture(view, canvas);

            saveBitmapToFile(bitmap, file, quality);

            recycleBitmap(bitmap);

            return true;
        } catch (OutOfMemoryError error) {
            Log.e(TAG, "capture error", error);
        }
        return false;
    }

    public static final boolean capture(@NonNull ScrollView scrollView, final int width, final int height, @Nullable Bitmap bg, @NonNull File file, int quality) {
        return capture(scrollView, width, height, bg, file, quality, null);
    }

    /**
     * @param width      最终生成截图的宽度
     * @param height     最终生成截图的高度
     * @param scrollView
     * @param bg         scrollview background may be null
     * @param file       save capture image
     * @param quality    compress quality
     * @return
     */
    public static final boolean capture(@NonNull ScrollView scrollView, final int width, final int height, @Nullable Bitmap bg, @NonNull File file, int quality, @Nullable Logo logo) {
        View view = scrollView.getChildAt(0);
        if (view != null) {
            //获取scrollView可滚动的范围
            int scrollRange = computeScrollViewVerticalScrollRange(scrollView);
            int totalHeight = scrollView.getHeight() + scrollRange;
            if (logo != null) {
                totalHeight += logo.marginTop + logo.height + logo.marginBottom;
            }

            try {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);

                //计算缩放比例
                float scale = (height * 1.0F) / totalHeight;

                //因为如果背景是渐变的话，截屏出来的图片是一屏一个渐变，背景有问题，所以这里需要单独绘制背景
                if (bg != null) {
                    drawBg(width, height, scale, canvas, bg);
                }

                scaleCanvas(width, canvas, scale);

                doCapture(view, canvas);

                if (logo != null) {
                    drawLogo(scrollView, logo, canvas, totalHeight);
                }

                saveBitmapToFile(bitmap, file, quality);
                recycleBitmap(bitmap);
                return true;
            } catch (OutOfMemoryError outOfMemoryError) {
                Log.e(TAG, "capture error", outOfMemoryError);
            }
        }
        return false;
    }


    public static boolean capture(ListView listView, final int width, final int height, @Nullable Bitmap bg, @NonNull File file, int quality) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);

        listView.setSelection(0);

        int index = 0;

        listView.draw(canvas);
        File tmp = new File(file.getParentFile().getAbsolutePath() + "/" + (index++) + "_" + file.getName());
        saveBitmapToFile(bitmap, tmp, quality);

        while (ListViewCompat.canScrollList(listView, 1)) {
            ListViewCompat.scrollListBy(listView, listView.getHeight());
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            listView.draw(canvas);
            tmp = new File(file.getParentFile().getAbsolutePath() + "/" + (index++) + "_" + file.getName());
            saveBitmapToFile(bitmap, tmp, quality);
        }


        return false;
    }

    private static void drawBg(final int width, final int height, float scale, Canvas canvas, Bitmap bg) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(bg, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        //因为做了缩放，所以内容区域应该小于最终图片的宽度
        //因为只有内容区域有背景，所以这里计算出内容区域的的起始位置，只在这片区域绘制背景
        int left = (int) (width / 2 - width * scale / 2);
        int right = (int) (width * scale + left);
        int top = 0;
        int bottom = height;

        canvas.drawRect(left, top, right, bottom, paint);
    }

    private static void scaleCanvas(final int width, Canvas canvas, float scale) {
        canvas.scale(scale, scale, width / 2, 0);
    }

    private static void scrollCapture(View parentView, Canvas canvas, ScrollView scrollView, int scrollRange) {
        //需要截屏的高度
        int totalDistance = scrollView.getHeight() + scrollRange;
        //已经截过的高度
        int consumed = 0;

        int saveCount = canvas.save();

        doCapture(parentView, canvas);
        consumed += scrollView.getHeight();

        while (consumed < totalDistance) {
            //剩余的高度
            int dy = totalDistance - consumed;
            if (dy > scrollView.getHeight()) {
                dy = scrollView.getHeight();
            }
            scrollView.scrollBy(0, dy);

            canvas.translate(0, dy);

            doCapture(parentView, canvas);
            consumed += dy;
        }

        canvas.restoreToCount(saveCount);
    }

    /**
     * @param scrollView
     * @param logo
     * @param canvas
     * @param totalHeight scrollView.getHeight+scrollRange+logo.margionTop+logo.Height+logo.marginBottom
     */
    private static void drawLogo(ScrollView scrollView, Logo logo, Canvas canvas, final int totalHeight) {
        if (logo != null) {
            int left = scrollView.getWidth() / 2 - logo.width / 2;
            int right = left + logo.width;
            int bottom = totalHeight - logo.marginBottom;
            int top = bottom - logo.height;
            logo.drawable.setBounds(left, top, right, bottom);
            logo.drawable.draw(canvas);
        }
    }

    private static void doCapture(View root, Canvas canvas) {
        root.draw(canvas);
    }

    private static int computeScrollViewVerticalScrollRange(ScrollView scrollView) {
        final int count = scrollView.getChildCount();
        final int contentHeight = scrollView.getHeight() - scrollView.getPaddingBottom() - scrollView.getPaddingTop();
        if (count == 0) {
            return 0;
        }
        int scrollRange = scrollView.getChildAt(0).getBottom();
        return scrollRange - contentHeight;
    }

    private static String saveBitmapToFile(Bitmap bitmap, File file, int quality) {
        ByteArrayOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            bos.flush();

            fos = new FileOutputStream(file);
            bos.writeTo(fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            safeClose(bos);
            safeClose(fos);
        }
        return file.getAbsolutePath();
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    public static final class Logo {
        public final Drawable drawable;
        public final int width;
        public final int height;
        public final int marginTop;
        public final int marginBottom;

        public Logo(@NonNull Drawable drawable, int width, int height, int marginTop, int marginBottom) {
            this.drawable = drawable;
            this.width = width;
            this.height = height;
            this.marginBottom = marginBottom;
            this.marginTop = marginTop;
        }
    }
}
