/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package org.opencv.camrea;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.TextureView;
import android.widget.FrameLayout;


/**
 * 基于 系统TextureView实现的预览View。
 *
 * @Time: 2019/1/28
 * @Author: v_chaixiaogang
 */
public class AutoTexturePreviewViewV2 extends TextureView {


    private int videoWidth = 0;
    private int videoHeight = 0;


    private int previewWidth = 0;
    private int previewHeight = 0;
    private static int scale = 2;

    public static float circleRadius;
    public static float circleX;
    public static float circleY;

    private float[] pointXY = new float[3];


    public AutoTexturePreviewViewV2(Context context) {
        super(context);
        init();
    }

    public AutoTexturePreviewViewV2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoTexturePreviewViewV2(Context context, AttributeSet attrs,
                                    int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private Handler handler = new Handler(Looper.getMainLooper());

    private void init() {
        setWillNotDraw(false);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        previewWidth = getWidth();
        previewHeight = getHeight();

        if (videoWidth == 0 || videoHeight == 0 || previewWidth == 0 || previewHeight == 0) {
            return;
        }

        if (previewWidth * videoHeight > previewHeight * videoWidth) {
            int scaledChildHeight = videoHeight * previewWidth / videoWidth;
            layout(0, (previewHeight - scaledChildHeight) / scale,
                    previewWidth, (previewHeight + scaledChildHeight) / scale);
        } else {
            int scaledChildWidth = videoWidth * previewHeight / videoHeight;
          layout((previewWidth - scaledChildWidth) / scale, 0,
                    (previewWidth + scaledChildWidth) / scale, previewHeight);

        }


    }


    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewSize(int width, int height) {
        if (this.videoWidth == width && this.videoHeight == height) {
            return;
        }
        this.videoWidth = width;
        this.videoHeight = height;
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });

    }


}
