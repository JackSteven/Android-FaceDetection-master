package org.opencv.camrea;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.List;


/**
 * Time: 2019/1/24
 * Author: v_chaixiaogang
 * Description:
 */
public class CameraPreviewManagerV2 implements TextureView.SurfaceTextureListener {

    private static final String TAG = "camera_preview";


    TextureView mTextureView;
    boolean mPreviewed = false;
    private boolean mSurfaceCreated = false;
    private SurfaceTexture mSurfaceTexture;

    public static final int CAMERA_FACING_BACK = 0;

    public static final int CAMERA_FACING_FRONT = 1;

    public static final int CAMERA_USB = 2;

    public static final int CAMERA_ORBBEC = 3;

    /**
     * 垂直方向
     */
    public static final int ORIENTATION_PORTRAIT = 0;
    /**
     * 水平方向
     */
    public static final int ORIENTATION_HORIZONTAL = 1;

    /**
     * 当前相机的ID。
     */
    private int cameraFacing = CAMERA_FACING_FRONT;

    private int previewWidth;
    private int previewHeight;

    private int videoWidth;
    private int videoHeight;

    private int textureWidth;
    private int textureHeight;

    private Camera mCamera;

    private int displayOrientation = 0;
    private int cameraId = 0;
    private int mPreviewFormat = ImageFormat.NV21;
    private Mat[] mFrameChain;
    protected JavaCameraFrame[] mCameraFrame;

    /**
     * 镜像处理
     */
    private CameraDataCallbackV2 mCameraDataCallback;
    private static volatile CameraPreviewManagerV2 instance = null;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    public static CameraPreviewManagerV2 getInstance() {
        synchronized (CameraPreviewManagerV2.class) {
            if (instance == null) {
                instance = new CameraPreviewManagerV2();
            }
        }
        return instance;
    }

    public void onDestroy() {
        if (instance != null) {
            if (mCameraDataCallback != null) {
                mCameraDataCallback = null;
            }
            stopPreview();
            instance = null;
        }
    }

    public int getCameraFacing() {
        return cameraFacing;
    }

    public void setCameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
    }

    public int getDisplayOrientation() {
        return displayOrientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        this.displayOrientation = displayOrientation;
    }

    /**
     * 开启预览
     *
     * @param context
     * @param textureView
     */
    public void startPreview(Context context, TextureView textureView, int width,
                             int height, CameraDataCallbackV2 cameraDataCallback) {
        Context mContext = context;
        this.mCameraDataCallback = cameraDataCallback;
        mTextureView = textureView;
        this.previewWidth = width;
        this.previewHeight = height;
        mSurfaceTexture = mTextureView.getSurfaceTexture();
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int i, int i1) {
        Log.d("mine","openCamera");
        mSurfaceTexture = texture;
        mSurfaceCreated = true;
        textureWidth = i;
        textureHeight = i1;
        openCamera(false);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int i, int i1) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        mSurfaceCreated = false;
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {

    }


    /**
     * 关闭预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(null);
                mSurfaceCreated = false;
                mTextureView = null;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                if (mFrameChain != null) {
                    mFrameChain[0].release();
                    mFrameChain[1].release();
                }
                if (mCameraFrame != null) {
                    mCameraFrame[0].release();
                    mCameraFrame[1].release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 关闭预览
     */
    public void resetPreview() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }


    /**
     * 开启摄像头
     */
    public void openCamera(boolean isNew) {
        try {
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
                    mTextureView.setRotationY(180);
                } else {
                    mTextureView.setRotationY(0);
                }
                // 旋转90度或者270，需要调整宽高
              //  mTextureView.getSurfaceTexture().setPreviewSize(previewHeight, previewWidth);
            } else {
                boolean isRgbRevert = SingleBaseConfig.getBaseConfig().getRgbRevert();
                if (isRgbRevert) {
                    mTextureView.setRotationY(180);
                } else {
                    mTextureView.setRotationY(0);
                }
              //  mTextureView.setPreviewSize(previewWidth, previewHeight);
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

            mFrameChain = new Mat[1];
            mFrameChain[0] = new Mat(videoHeight + (videoHeight/2), videoWidth, CvType.CV_8UC1);
            //mFrameChain[1] = new Mat(videoHeight + (videoHeight/2), videoWidth, CvType.CV_8UC1);


            mCameraFrame = new JavaCameraFrame[1];
            mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], videoWidth, videoHeight);
            try {
                mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        mFrameChain[0].put(0, 0, bytes);
                        if (mCameraDataCallback != null) {
                            mCameraDataCallback.onGetCameraData(bytes, camera,
                                    videoWidth, videoHeight);
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

    public class JavaCameraFrame implements CameraBridgeViewBase.CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            if (mPreviewFormat == ImageFormat.NV21)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            else if (mPreviewFormat == ImageFormat.YV12)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4);  // COLOR_YUV2RGBA_YV12 produces inverted colors
            else
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");

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
}
