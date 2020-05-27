package org.opencv.android;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup.LayoutParams;

import org.opencv.BuildConfig;
import org.opencv.camrea.AutoTexturePreviewView;
import org.opencv.camrea.AutoTexturePreviewViewV2;
import org.opencv.camrea.CameraPreviewManager;
import org.opencv.camrea.SingleBaseConfig;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */
public class JavaCamera3View extends CameraBridgeViewBase implements PreviewCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "JavaCameraView";

    private byte mBuffer[];
    private Mat[] mFrameChain;
    private int mChainIdx = 0;
    private Thread mThread;
    private boolean mStopThread;
    private int cameraId = 0;
    protected Camera mCamera;
    protected JavaCameraFrame[] mCameraFrame;
    private int mPreviewFormat = ImageFormat.NV21;


    private int videoWidth;
    private int videoHeight;


    private int previewWidth=640;
    private int previewHeight=480;

    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;


    public static final int CAMERA_USB = 2;

    public static final int CAMERA_ORBBEC = 3;

    /**
     * 当前相机的ID。
     */
    private int cameraFacing = CAMERA_FACING_FRONT;
    private AutoTexturePreviewViewV2 mSurfaceTexture;
    public static class JavaCameraSizeAccessor implements ListItemAccessor {

        @Override
        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        @Override
        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }



    public JavaCamera3View(Context context, int cameraId) {
        super(context, cameraId);
    }

    public JavaCamera3View(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize java camera");
        boolean result = true;
        synchronized (this) {
            mCamera = null;
            openCamera(true);

            /* Now set camera parameters */
            try {


                mFrameWidth = previewWidth;
                mFrameHeight = previewHeight;
                if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT)) {
                    mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
                } else {
                    mScale = 0;
                }

                if (mFpsMeter != null) {
                    mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                }

                int size = mFrameWidth * mFrameHeight;
                size  = size * ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat()) / 8;
                mBuffer = new byte[size];

                mCamera.addCallbackBuffer(mBuffer);
                mCamera.setPreviewCallbackWithBuffer(this);

                mFrameChain = new Mat[2];
                mFrameChain[0] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
                mFrameChain[1] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);

                AllocateCache();

                mCameraFrame = new JavaCameraFrame[2];
                mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight);
                mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 开启摄像头
     */
    public void openCamera(boolean isNew) {
        try {
            if (mSurfaceTexture==null){
                mSurfaceTexture=new AutoTexturePreviewViewV2(getContext());
            }
            if (mCamera == null || isNew) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == cameraFacing) {
                        cameraId = i;
                    }
                }
                mCamera = Camera.open(cameraId);
            }

            // 摄像头图像预览角度
            int cameraRotation = SingleBaseConfig.getBaseConfig().getVideoDirection();
            switch (cameraFacing) {
                case CAMERA_FACING_FRONT:
                    mCamera.setDisplayOrientation(cameraRotation);
                    break;
                case CAMERA_FACING_BACK:
                    mCamera.setDisplayOrientation(cameraRotation);
                    break;
                case CAMERA_USB:
                    mCamera.setDisplayOrientation(cameraRotation);
                    break;
                default:
                    break;
            }

            if (cameraRotation == 90 || cameraRotation == 270) {
                boolean isRgbRevert = SingleBaseConfig.getBaseConfig().getRgbRevert();
                if (isRgbRevert) {
                    mSurfaceTexture.setRotationY(180);
                } else {
                    mSurfaceTexture.setRotationY(0);
                }
                // 旋转90度或者270，需要调整宽高
                mSurfaceTexture.setPreviewSize(previewHeight, previewWidth);
            } else {
                boolean isRgbRevert = SingleBaseConfig.getBaseConfig().getRgbRevert();
                if (isRgbRevert) {
                    mSurfaceTexture.setRotationY(180);
                } else {
                    mSurfaceTexture.setRotationY(0);
                }
                mSurfaceTexture.setPreviewSize(previewWidth, previewHeight);
            }
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizeList = params.getSupportedPreviewSizes(); // 获取所有支持的camera尺寸
            final Camera.Size optionSize = getOptimalPreviewSize(sizeList, previewWidth,
                    previewHeight); // 获取一个最为适配的camera.size
            if (optionSize.width == previewWidth && optionSize.height == previewHeight) {
                videoWidth = previewWidth;
                videoHeight = previewHeight;
            } else {
                videoWidth = optionSize.width;
                videoHeight = optionSize.height;
            }
            params.setPreviewSize(videoWidth, videoHeight);
            mPreviewFormat = params.getPreviewFormat();
            mCamera.setParameters(params);
            try {
                mCamera.setPreviewTexture(mSurfaceTexture.getSurfaceTexture());
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        synchronized (this) {
                            mFrameChain[mChainIdx].put(0, 0, bytes);
                            mCameraFrameReady = true;
                            this.notify();
                        }
                        if (mCamera != null) {
                            mCamera.addCallbackBuffer(mBuffer);
                        }
                    }
                });
                mCamera.startPreview();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * 解决预览变形问题
     *
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double aspectTolerance = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > aspectTolerance) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    protected void releaseCamera() {
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);

                mCamera.release();
            }
            mCamera = null;
            if (mFrameChain != null) {
                mFrameChain[0].release();
                mFrameChain[1].release();
            }
            if (mCameraFrame != null) {
                mCameraFrame[0].release();
                mCameraFrame[1].release();
            }
        }
    }

    private boolean mCameraFrameReady = false;

    @Override
    protected boolean connectCamera(int width, int height) {

        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height)) {
            return false;
        }

        mCameraFrameReady = false;

        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        mStopThread = false;
        mThread = new Thread(new CameraWorker());
        mThread.start();

        return true;
    }

    @Override
    protected void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Waiting for thread");
            if (mThread != null) {
                mThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread =  null;
        }

        /* Now release camera */
        releaseCamera();

        mCameraFrameReady = false;
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        synchronized (this) {
            mFrameChain[mChainIdx].put(0, 0, frame);
            mCameraFrameReady = true;
            this.notify();
        }
        if (mCamera != null) {
            mCamera.addCallbackBuffer(mBuffer);
        }
    }

    public class JavaCameraFrame implements CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            if (mPreviewFormat == ImageFormat.NV21) {
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            } else if (mPreviewFormat == ImageFormat.YV12) {
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4);  // COLOR_YUV2RGBA_YV12 produces inverted colors
            } else {
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");
            }

            return mRgba;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        public void release() {
            mRgba.release();
        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    };

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do {
                boolean hasFrame = false;
                synchronized (JavaCamera3View.this) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            JavaCamera3View.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady)
                    {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame) {
                    if (!mFrameChain[1 - mChainIdx].empty()) {
                        deliverAndDrawFrame(mCameraFrame[1 - mChainIdx]);
                    }
                }
            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }
}
