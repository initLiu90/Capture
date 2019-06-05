package com.lzp.capture;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;

public final class CaptureUtil {
    private static final String TAG = CaptureUtil.class.getSimpleName();

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
     * @param width 最终生成截图的宽度 * @param height 最终生成截图的高度 * @param scrollView * @param bg scrollview background may be null * @param file save capture image * @param quality compress quality * @return
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
        return capture(listView, width, height, bg, file, quality, null);
    }

    public static boolean capture(ListView listView, final int width, final int height, @Nullable Bitmap bg, @NonNull File file, int quality, @Nullable Logo logo) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter != null) {
            try {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);

                final int itemCount = adapter.getCount();

                //step1：计算所有item的高度
                int totalHeight = 0;
                for (int pos = 0; pos < itemCount; pos++) {
                    View itemView = adapter.getView(pos, null, listView);
                    setupListItemView(itemView, listView);

                    totalHeight += itemView.getHeight();
                }
                if (logo != null) {
                    totalHeight += logo.marginTop + logo.height + logo.marginBottom;
                }

                //计算缩放比例
                float scale = (height * 1.0F) / totalHeight;

                //step2：绘制背景
                if (bg != null) {
                    drawBg(width, height, scale, canvas, bg);
                }

                //step3：对canvas缩放
                scaleCanvas(width, canvas, scale);

                //step4：绘制每一个item
                int saveCount = canvas.save();
                for (int pos = 0; pos < itemCount; pos++) {
                    View itemView = adapter.getView(pos, null, listView);
                    setupListItemView(itemView, listView);

                    doCapture(itemView, canvas);

                    canvas.translate(0, itemView.getHeight());
                }
                canvas.restoreToCount(saveCount);

                //step5：draw logo
                if (logo != null) {
                    drawLogo(listView, logo, canvas, totalHeight);
                }

                //step6：保存bitmap到文件
                saveBitmapToFile(bitmap, file, quality);
                recycleBitmap(bitmap);

                return true;
            } catch (OutOfMemoryError outOfMemoryError) {
                Log.e(TAG, "capture error", outOfMemoryError);
            }
        }
        return false;
    }

    /**
     * for LinearLayoutManager vertical
     * 截图时不能截取Item之间的Divider，需要改进。
     * @param recyclerView
     * @param width
     * @param height
     * @param bg
     * @param file
     * @param quality
     * @return
     */
    public static boolean capture(RecyclerView recyclerView, final int width, final int height, @Nullable Bitmap bg, @NonNull File file, int quality) {
        return capture(recyclerView, width, height, bg, file, quality, null);
    }

    /**
     * for LinearLayoutManager vertical
     *
     * 截图时不能截取Item之间的Divider，需要改进。
     * @param recyclerView
     * @param width
     * @param height
     * @param bg
     * @param file
     * @param quality
     * @param logo
     * @return
     */
    public static boolean capture(RecyclerView recyclerView, final int width, final int height, @Nullable Bitmap bg, @NonNull File file, int quality, @Nullable Logo logo) {
        if (recyclerView != null && recyclerView.getAdapter() != null) {
            final int itemCount = recyclerView.getAdapter().getItemCount();

            int totalHeight = 0;
            for (int pos = 0; pos < itemCount; pos++) {
                RecyclerView.ViewHolder holder = recyclerView.getAdapter().onCreateViewHolder(recyclerView, recyclerView.getAdapter().getItemViewType(pos));
                setupRecyclerViewItemView(holder, recyclerView);

                totalHeight += holder.itemView.getHeight();
            }
            if (logo != null) {
                totalHeight += logo.marginTop + logo.height + logo.marginBottom;
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);

            float scale = (height * 1.0F) / totalHeight;

            if (bg != null) {
                drawBg(width, height, scale, canvas, bg);
            }

            scaleCanvas(width, canvas, scale);

            int saveCount = canvas.save();
            for (int pos = 0; pos < itemCount; pos++) {
                RecyclerView.ViewHolder holder = recyclerView.getAdapter().onCreateViewHolder(recyclerView, recyclerView.getAdapter().getItemViewType(pos));
                recyclerView.getAdapter().bindViewHolder(holder, pos);
                setupRecyclerViewItemView(holder, recyclerView);

                doCapture(holder.itemView, canvas);

                canvas.translate(0, holder.itemView.getHeight());
            }
            canvas.restoreToCount(saveCount);

            if (logo != null) {
                drawLogo(recyclerView, logo, canvas, totalHeight);
            }

            saveBitmapToFile(bitmap, file, quality);
            recycleBitmap(bitmap);

            return true;
        }
        return false;
    }

    private static void setupRecyclerViewItemView(RecyclerView.ViewHolder holder, RecyclerView recyclerView) {
        final ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        final RecyclerView.LayoutParams rvLayoutParams;
        if (lp == null) {
            rvLayoutParams = (RecyclerView.LayoutParams) recyclerView.getLayoutManager().generateDefaultLayoutParams();
            holder.itemView.setLayoutParams(rvLayoutParams);
        } else if ((lp instanceof RecyclerView.LayoutParams && recyclerView.getLayoutManager().checkLayoutParams((RecyclerView.LayoutParams) lp))) {
            rvLayoutParams = (RecyclerView.LayoutParams) recyclerView.getLayoutManager().generateLayoutParams(lp);
            holder.itemView.setLayoutParams(rvLayoutParams);
        }

        recyclerView.getLayoutManager().measureChildWithMargins(holder.itemView, 0, 0);

        int left, top, right, bottom;
        left = recyclerView.getLayoutManager().getPaddingLeft();

        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams();
        final int decoratedMeasurementInOther = recyclerView.getLayoutManager().getDecoratedMeasuredWidth(holder.itemView) + params.leftMargin
                + params.rightMargin;
        right = left + decoratedMeasurementInOther;
        top = 0;
        bottom = top + holder.itemView.getMeasuredHeight();

        recyclerView.getLayoutManager().layoutDecoratedWithMargins(holder.itemView, left, top, right, bottom);
    }

    /**
     * 测量以及布局itemView
     *
     * @param itemView
     * @param listView
     */
    private static void setupListItemView(View itemView, ListView listView) {
        AbsListView.LayoutParams p = (AbsListView.LayoutParams) itemView.getLayoutParams();
        if (p == null) {
            p = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        }
        final int itemWidthSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.EXACTLY),
                listView.getPaddingLeft() + listView.getPaddingRight(), p.width);
        final int lpHeight = p.height;
        final int itemHeightSpec;
        if (lpHeight > 0) {
            itemHeightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY);
        } else {
            itemHeightSpec = View.MeasureSpec.makeMeasureSpec(listView.getMeasuredHeight(), View.MeasureSpec.UNSPECIFIED);
        }
        itemView.measure(itemWidthSpec, itemHeightSpec);

        final int w = itemView.getMeasuredWidth();
        final int h = itemView.getMeasuredHeight();

        int itemViewLeft = listView.getListPaddingLeft();
        final int itemViewRight = itemViewLeft + w;
        final int itemViewTop = 0;
        final int itemViewBottom = itemViewTop + h;

        itemView.layout(itemViewLeft, itemViewTop, itemViewRight, itemViewBottom);
    }


    private static void drawBg(final int width, final int height, float scale, Canvas canvas, Bitmap bg) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(bg, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        //因为做了缩放，所以内容区域应该小于最终图片的宽度
        // 因为只有内容区域有背景，所以这里计算出内容区域的的起始位置，只在这片区域绘制背景
        int left = (int) (width / 2 - width * scale / 2);
        int right = (int) (width * scale + left);
        int top = 0;
        int bottom = height;
        canvas.drawRect(left, top, right, bottom, paint);
    }

    private static void scaleCanvas(final int width, Canvas canvas, float scale) {
        canvas.scale(scale, scale, width / 2, 0);
    }

    /**
     * @param captureView
     * @param logo
     * @param canvas      canvas 缩放过但是没有translate
     * @param totalHeight scrollView.getHeight+scrollRange+logo.margionTop+logo.Height+logo.marginBottom
     */
    private static void drawLogo(View captureView, Logo logo, Canvas canvas, final int totalHeight) {
        if (logo != null) {
            int left = captureView.getWidth() / 2 - logo.width / 2;
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

    //    private static void scrollCapture(View parentView, Canvas canvas, ScrollView scrollView, int scrollRange) {
//        //需要截屏的高度
//        int totalDistance = scrollView.getHeight() + scrollRange;
//        //已经截过的高度
//        int consumed = 0;
//        int saveCount = canvas.save();
//        doCapture(parentView, canvas);
//        consumed += scrollView.getHeight();
//        while (consumed < totalDistance) {
//            //剩余的高度
//            int dy = totalDistance - consumed;
//            if (dy > scrollView.getHeight()) {
//                dy = scrollView.getHeight();
//            }
//            scrollView.scrollBy(0, dy);
//            canvas.translate(0, dy);
//            doCapture(parentView, canvas);
//            consumed += dy;
//        }
//        canvas.restoreToCount(saveCount);
//    }

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