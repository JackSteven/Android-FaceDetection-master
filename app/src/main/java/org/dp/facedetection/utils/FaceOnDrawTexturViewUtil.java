package org.dp.facedetection.utils;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.TextureView;

import org.dp.facedetection.Face;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.camrea.AutoTexturePreviewView;


/**
 * Created by ShiShuaiFeng on 2019/6/5.
 */

public class FaceOnDrawTexturViewUtil {


    private FaceOnDrawTexturViewUtil() {
    }

    /**
     * 通过中心点坐标（x，y） 和 width ，绘制Rect
     *
     * @return
     */
    public static Rect getFaceRectTwo(org.opencv.core.Rect rect1) {
        Rect rect = new Rect();
        rect.top = (int) (rect1.tl().y);
        rect.left = (int) (rect1.tl().x);
        rect.right = (int) (rect1.br().x);
        rect.bottom = (int) (rect1.br().y);
        return rect;
    }

    /**
     * 通过中心点坐标（x，y） 和 width ，绘制Rect
     *
     * @param faceInfo
     * @return
     */
    public static Rect getFaceRectTwo(Face faceInfo) {
        Rect rect = new Rect();
        rect.top = (int) (faceInfo.faceRect.y);
        rect.left = (int) (faceInfo.faceRect.x);
        rect.right = (int) (faceInfo.faceRect.x+faceInfo.faceRect.width);
        rect.bottom = (int) (faceInfo.faceRect.y+faceInfo.faceRect.height);
        return rect;
    }



    public static void mapFromOriginalRect(RectF rectF,
                                           AutoTexturePreviewView textureView,
                                           org.opencv.core.Rect rect1) {
        int selfWidth = textureView.getWidth();
        int selfHeight = textureView.getHeight();
        Matrix matrix = new Matrix();
        if (selfWidth * rect1.width > selfHeight * rect1.width) {
            int targetHeight = rect1.height * selfWidth / rect1.width;
            int delta = (targetHeight - selfHeight) / 2;
            float ratio = 1.0f * selfWidth / rect1.width;
            matrix.postScale(ratio, ratio);
            matrix.postTranslate(0, -delta);
        } else {
            int targetWith =rect1.width * selfHeight / rect1.height;
            int delta = (targetWith - selfWidth) / 2;
            float ratio = 1.0f * selfHeight / rect1.height;
            matrix.postScale(ratio, ratio);
            matrix.postTranslate(-delta, 0);
        }
        matrix.mapRect(rectF);

    }


}
