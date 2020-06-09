package org.dp.facedetection;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class OpencvUtils {


    public void postData(byte[] data, int width, int height, int cameraId) {
        ByteBuffer byteBuffer = encodeValue(data);
        if (byteBuffer != null) {
            Mat src = new Mat(height * 3 / 2, width, CvType.CV_8UC1, byteBuffer);
            Imgproc.cvtColor(src, src, Imgproc.COLOR_YUV2RGBA_NV21);
            if (cameraId == 1) {
                //逆时针旋转90度
                Core.rotate(src, src, Core.ROTATE_90_COUNTERCLOCKWISE);
                //1：水平翻转   0：垂直翻转
                Core.flip(src, src, 1);
            } else {
                //顺时针旋转90度
                Core.rotate(src, src, Core.ROTATE_90_CLOCKWISE);
            }
            Mat gray = new Mat();
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.equalizeHist(gray, gray);
        }
    }


    public ByteBuffer encodeValue(byte[] value) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(value.length);
        byteBuffer.clear();
        byteBuffer.get(value, 0, value.length);
        return byteBuffer;
    }
}
