package org.dp.facedetection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dp.facedetection.db.DBManager;
import org.dp.facedetection.db.User;
import org.dp.facedetection.face.FaceCallBack;
import org.dp.facedetection.face.FaceManager;
import org.dp.facedetection.utils.ImageUtils;
import org.dp.facedetection.utils.LogUtils;
import org.dp.facedetection.utils.ThreadUtils;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.AgastFeatureDetector;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.utils.FaceUtil;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.camrea.AutoTexturePreviewView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ToastUtils;

public class FdActivity extends CameraActivity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private File mCascadeFile2;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;
    private DetectionBasedTracker mNativeDetector;
    private DetectionBasedTracker mNativeDetectorEye;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;

    private boolean isCheck = false;


    private ImageView mImageView;
    private ImageView mImageView2;
    private TextView mTextView;
    private TextView mNumTextView;

    private int num=0;

    private Bitmap bitmap;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("detection_based_tracker");

                    try {
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");

                        FileOutputStream os = new FileOutputStream(mCascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        InputStream is1 = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                        File cascadeDir1 = getDir("cascade1", Context.MODE_PRIVATE);
                        mCascadeFile2 = new File(cascadeDir1, "haarcascade_frontalface_alt2.xml");
                        FileOutputStream os1 = new FileOutputStream(mCascadeFile2);
                        byte[] buffer1 = new byte[4096];
                        int bytesRead1;
                        while ((bytesRead1 = is1.read(buffer1)) != -1) {
                            os1.write(buffer1, 0, bytesRead1);
                        }
                        is1.close();
                        os1.close();
                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        mJavaDetectorEye = new CascadeClassifier(mCascadeFile2.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }

                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetectorEye = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
                        mNativeDetectorEye = new DetectionBasedTracker(mCascadeFile2.getAbsolutePath(), 0);

                        cascadeDir.delete();
                        cascadeDir1.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        DBManager.getInstance().init();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        setContentView(R.layout.face_detect_surface_view);
        mOpenCvCameraView = findViewById(R.id.fd_activity_surface_view);
        mImageView = findViewById(R.id.imageView);
        mImageView2 = findViewById(R.id.imageView2);
        mTextView = findViewById(R.id.textView);
        mNumTextView = findViewById(R.id.tv_num);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DBManager.getInstance().release();
        mOpenCvCameraView.disableView();
        if (bitmap != null) {
            bitmap.recycle();
        }
    }

    private Mat Matlin;
    private Mat gMatlin;
    private int absoluteFaceSize;

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();

        mRgba = new Mat(width, height, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);
        Matlin = new Mat(width, height, CvType.CV_8UC4);
        gMatlin = new Mat(width, height, CvType.CV_8UC4);
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }



    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        MatOfRect faces = new MatOfRect();
        MatOfRect faces2 = new MatOfRect();
        int rotation = mOpenCvCameraView.getDisplay().getRotation();
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
            mNativeDetectorEye.setMinFaceSize(mAbsoluteFaceSize);
        }
        if (rotation == Surface.ROTATION_0) {
            Core.rotate(mGray, gMatlin, Core.ROTATE_90_CLOCKWISE);
            Core.rotate(mRgba, Matlin, Core.ROTATE_90_CLOCKWISE);
            gMatlin.setByteSize(mGray.getByteSize());
            Matlin.setByteSize(mRgba.getByteSize());
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(gMatlin, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            if (mJavaDetectorEye != null) {
                mJavaDetectorEye.detectMultiScale(gMatlin, faces2, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            Rect[] faceArray = faces.toArray();
            Rect[] faceArray2 = faces2.toArray();
            if (faceArray.length > 0 && faceArray2.length > 0) {
                Rect bigFace = findBigFace(faceArray);
                if (bigFace != null) {
                    findFaceSuccess(Matlin, bigFace);
                    Imgproc.rectangle(Matlin, bigFace.tl(), bigFace.br(), new Scalar(0, 255, 0, 255), 1);
                }
            }
            Core.rotate(Matlin, mRgba, Core.ROTATE_90_COUNTERCLOCKWISE);
        } else if (rotation == Surface.ROTATION_90) {
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            if (mJavaDetectorEye != null) {
                mJavaDetectorEye.detectMultiScale(mGray, faces2, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            Rect[] faceArray = faces.toArray();
            Rect[] faceArray2 = faces2.toArray();
            if (faceArray.length > 0 && faceArray2.length > 0) {
                Rect bigFace = findBigFace(faceArray);
                if (bigFace != null) {
                    findFaceSuccess(mGray, bigFace);
                    Imgproc.rectangle(mGray, bigFace.tl(), bigFace.br(), new Scalar(0, 255, 0, 255), 1);
                }
            }

        } else if (rotation == Surface.ROTATION_180) {
            Core.rotate(mGray, gMatlin, Core.ROTATE_90_COUNTERCLOCKWISE);
            Core.rotate(mRgba, Matlin, Core.ROTATE_90_COUNTERCLOCKWISE);
            gMatlin.setByteSize(mGray.getByteSize());
            Matlin.setByteSize(mRgba.getByteSize());
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(gMatlin, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            if (mJavaDetectorEye != null) {
                mJavaDetectorEye.detectMultiScale(gMatlin, faces2, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            Rect[] faceArray = faces.toArray();
            Rect[] faceArray2 = faces2.toArray();
            if (faceArray.length > 0 && faceArray2.length > 0) {
                Rect bigFace = findBigFace(faceArray);
                if (bigFace != null) {
                    findFaceSuccess(mGray, bigFace);
                    Imgproc.rectangle(Matlin, bigFace.tl(), bigFace.br(), new Scalar(0, 255, 0, 255), 1);
                }
            }

            Core.rotate(Matlin, mRgba, Core.ROTATE_90_CLOCKWISE);
        } else if (rotation == Surface.ROTATION_270) {
            Core.rotate(mGray, gMatlin, Core.ROTATE_180);
            Core.rotate(mRgba, Matlin, Core.ROTATE_180);
            gMatlin.setByteSize(mGray.getByteSize());
            Matlin.setByteSize(mRgba.getByteSize());
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(gMatlin, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            if (mJavaDetectorEye != null) {
                mJavaDetectorEye.detectMultiScale(gMatlin, faces2, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            Rect[] faceArray = faces.toArray();
            Rect[] faceArray2 = faces2.toArray();
            if (faceArray.length > 0 && faceArray2.length > 0) {
                Rect bigFace = findBigFace(faceArray);
                if (bigFace != null) {
                    findFaceSuccess(gMatlin, bigFace);
                    Imgproc.rectangle(Matlin, bigFace.tl(), bigFace.br(), new Scalar(0, 255, 0, 255), 1);
                }
            }
            Core.rotate(Matlin, mRgba, Core.ROTATE_180);
        }

        return mRgba;
    }


    /**
     * 找出最大的人脸
     *
     * @param faceArray
     * @return
     */
    private Rect findBigFace(Rect[] faceArray) {
        int big = 0;
        if (faceArray != null && faceArray.length == 1) {
            return faceArray[0];
        } else if (faceArray != null && faceArray.length > 1) {
            for (int i = 0; i < faceArray.length; i++) {
                if (i != 0 && faceArray[i].width * faceArray[i].height > faceArray[big].width * faceArray[big].height) {
                    big = i;
                }
            }
            return faceArray[big];
        }
        return null;
    }


    private boolean isFirst = true;

    /**
     * 找到人脸
     */
    private void findFaceSuccess(Mat mat, Rect rect) {
        Mat submat = mat.submat(rect);
        submat.setByteSize(mat.getByteSize());
        if (isCheck) {
            return;
        }
        isCheck = true;
        if (isFirst) {
            List<User> user = DBManager.getInstance().getUser();
            LogUtils.d("mine","user"+user.size());
            if (user != null && user.size() > 0) {
                User user1 = user.get(0);
                if (user1 != null) {
                    String path = user1.getPaht();
                    if (!TextUtils.isEmpty(path)) {
                        Bitmap image = FaceUtil.getImage(this, path);
                        setmImageView(image);
                    }
                }
                isFirst = false;
            }

        }
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                FaceUtil.saveImagev2(getApplicationContext(),submat,"1234556.png");
                Bitmap image = FaceUtil.getImage(getApplicationContext(), "1234556.png");
                setmImageView2(image);
            }
        });
        FaceManager.getInstance().onFaceCheck(submat, new FaceCallBack() {
            @Override
            public void onError(String error) {
                isCheck = false;
            }

            @Override
            public void onCheckResultError(String error) {
                isCheck = false;
            }

            @Override
            public void findUserFailure(Mat mat) {
                LogUtils.d("mine", "findUserFailure");
               FaceManager.getInstance().registerFace(mat,  "12345678", "张三");
                isCheck = false;
            }

            @Override
            public void findUserSuccess(Mat mat, User user) {
                LogUtils.d("mine", "找到user");
                ToastUtils.showLong("找到你了");
                num++;
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       mNumTextView.setText("识别到"+num+"次");
                   }
               });
                isCheck = false;
            }

            @Override
            public void faceMatchNum(float match) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText("相识度"+match);
                    }
                });
            }
        });
    }


    private void setmImageView(Bitmap bitmap) {
        runOnUiThread(() -> {
            if (bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            }
        });
    }

    private void setmImageView2(Bitmap bitmap) {
        runOnUiThread(() -> {
            if (bitmap != null) {
                mImageView2.setImageBitmap(bitmap);
            }
        });
    }

    private void findUserSuccess() {
        runOnUiThread(() -> mTextView.setText("找到这个家伙了"));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50) {
            setMinFaceSize(0.5f);
        } else if (item == mItemFace40) {
            setMinFaceSize(0.4f);
        } else if (item == mItemFace30) {
            setMinFaceSize(0.3f);
        } else if (item == mItemFace20) {
            setMinFaceSize(0.2f);
        } else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
            setDetectorType(tmpDetectorType);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;
            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
                mNativeDetectorEye.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
                mNativeDetectorEye.stop();
            }
        }
    }

}