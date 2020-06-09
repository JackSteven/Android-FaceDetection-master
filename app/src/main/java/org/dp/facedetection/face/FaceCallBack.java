package org.dp.facedetection.face;

import org.dp.facedetection.db.User;
import org.opencv.core.Mat;

public interface FaceCallBack {

    void onError(String error);

    void onCheckResultError(String error);

    /**
     * 本地搜索不到用户
     */
    void findUserFailure(Mat mat);

    /**
     * 本地搜索用户成功
     */
    void findUserSuccess(Mat mat, User user);

    void faceMatchNum( float match);
}
