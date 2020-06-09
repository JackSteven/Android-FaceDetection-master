package org.dp.facedetection.face;

import android.text.TextUtils;

import org.dp.facedetection.BaseApplication;
import org.dp.facedetection.db.DBManager;
import org.dp.facedetection.db.User;
import org.dp.facedetection.utils.LogUtils;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.utils.FaceUtil;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceManager {

    private static FaceManager manager;

    private static List<User> mUserList;


    private ExecutorService es = Executors.newSingleThreadExecutor();

    public static FaceManager getInstance() {
        synchronized (FaceManager.class) {
            if (manager == null) {
                manager = new FaceManager();
                updateUser();
            }
            return manager;
        }

    }

    public void onFaceCheck(Mat mat, FaceCallBack callBack) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                if (mUserList != null && mUserList.size() > 0) {
                    for (User user : mUserList) {
                        if (user != null) {
                           String path = user.getPaht();
                            if (!TextUtils.isEmpty(path)) {
                                String pathFile1 = FaceUtil.getFilePath(BaseApplication.getContext(), path);
                                Mat imread = Imgcodecs.imread(pathFile1);
                                float match = FaceUtil.match(mat, imread);
                                if (match > 0.6) {
                                    if (callBack != null) {
                                        callBack.findUserSuccess(mat, user);
                                    }
                                    return;
                                }else {
                                    if (callBack != null) {
                                        callBack.faceMatchNum(match);
                                    }
                                }
                            }
                        }
                    }
                    if (callBack != null) {
                        callBack.findUserFailure(mat);
                    }
                } else {
                    if (callBack != null) {
                        callBack.findUserFailure(mat);
                    }
                }
            }
        });
    }

    public void registerFace(Mat mat,  String userId, String name) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        es.submit(new Runnable() {
            @Override
            public void run() {
                if (mUserList != null && mUserList.size() > 0) {
                    for (User user : mUserList) {
                        if (user != null && userId.equals(user.getUserId())) {
                            LogUtils.d("mine", "用户已存在");
                            return;
                        }
                    }
                }
                User user = new User();
                user.setUserId(userId);
                user.setCtime(System.currentTimeMillis());
                user.setName(name);
                String path = userId + name + ".png";
                boolean isS = FaceUtil.saveImagev2(BaseApplication.getContext(), mat, path);
                if (isS){
                    user.setPaht(path);
                    DBManager.getInstance().addUser(user);
                    updateUser();
                }


            }
        });
    }


    public static void updateUser() {
        mUserList = DBManager.getInstance().getUser();
    }

}
