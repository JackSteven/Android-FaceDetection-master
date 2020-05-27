package org.dp.facedetection

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.acitivity_face.*
import org.dp.facedetection.utils.FaceOnDrawTexturViewUtil
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.camrea.CameraPreviewManager
import org.opencv.camrea.SingleBaseConfig
import org.opencv.core.*
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceActivity : AppCompatActivity(){


    companion object {
        /**
         * 图片越大，性能消耗越大，也可以选择640*480， 1280*720
         */
         val PREFER_WIDTH = SingleBaseConfig.getBaseConfig().rgbAndNirWidth
         val PERFER_HEIGH = SingleBaseConfig.getBaseConfig().rgbAndNirHeight
    }

    lateinit var faceDetect: FaceDetect
    private val FACE_RECT_COLOR =  Scalar(0.0, 255.0, 0.0, 255.0)
    private val ex2: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    var paint:Paint ?=null
    var rectF:RectF ?=null
    var isCheack=false
    private var mCascadeFile: File? = null
    private var mCascadeFile2: File? = null
    private var mJavaDetector: CascadeClassifier? = null
    private var mJavaDetector2: CascadeClassifier? = null
    private val mRelativeFaceSize = 0.2f
    private var mAbsoluteFaceSize = 0
    private var mRgba: Mat? = null
    private var mGray: Mat? = null

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS ->{

                    // load cascade file from application resources
                    val isRead = resources.openRawResource(R.raw.lbpcascade_frontalface)
                    val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
                    mCascadeFile = File(cascadeDir, "haarcascade_frontalface_default.xml")
                    var os = FileOutputStream(mCascadeFile)
                    var buffer = ByteArray(4096)
                    var bytesRead: Int=0
                    while (isRead.read(buffer).also({ bytesRead = it }) != -1) {
                        os.write(buffer, 0, bytesRead)
                    }
                    isRead.close()
                    os.close()
                    mJavaDetector = CascadeClassifier(mCascadeFile!!.absolutePath)
                    if (mJavaDetector?.empty()!!) {
                        mJavaDetector = null
                    }


                    val is1 =
                        resources.openRawResource(R.raw.haarcascade_frontalface_alt2)
                    val cascadeDir1 =
                        getDir("cascade1", Context.MODE_PRIVATE)
                    mCascadeFile2 = File(cascadeDir1, "haarcascade_frontalface_alt2.xml")
                    val os1 = FileOutputStream(mCascadeFile2)
                    val buffer1 = ByteArray(4096)
                    var bytesRead1: Int
                    while (is1.read(buffer1).also { bytesRead1 = it } != -1) {
                        os1.write(buffer1, 0, bytesRead1)
                    }
                    is1.close()
                    os1.close()
                    mJavaDetector2 = CascadeClassifier(mCascadeFile2!!.absolutePath)
                    if (mJavaDetector2?.empty()!!) {
                        mJavaDetector2 = null
                    }

                    cascadeDir.delete()
                    cascadeDir1.delete()
                    mGray = Mat()
                    mRgba = Mat()
                    startCamera()
                }
                else -> {
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.acitivity_face)
        faceDetect = FaceDetect()
        mFaceDetectImageView.isOpaque = false
        mFaceDetectImageView.keepScreenOn = true
        paint= Paint()
        paint?.strokeWidth=2f
        rectF= RectF()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        }else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRgba?.release()
        mGray?.release()
    }

    /**
     * 设置视频流的来源
     */
    protected fun startCamera() {
        CameraPreviewManager.getInstance().cameraFacing = 1
        //相机预览
        CameraPreviewManager.getInstance().startPreview(
            this,
            mAutoCameraPreviewView,
           PREFER_WIDTH,
           PERFER_HEIGH
        ) { fra, data, camera, width, height ->
            checkDatas(fra)
        }
    }

    fun checkDatas( fra:CameraPreviewManager.JavaCameraFrame){
        if (isCheack){
            return
        }
        ex2.submit {
            mRgba=fra.rgba()
            mGray=fra.gray()
            if (mAbsoluteFaceSize == 0) {
                val height: Int = fra.gray().rows()
                if (Math.round(height * mRelativeFaceSize) > 0) {
                    mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize)
                }
            }
            val faces = MatOfRect()
            val facesEye = MatOfRect()
            if (mJavaDetector != null) {
                mJavaDetector!!.detectMultiScale(
                    mGray,
                    faces,
                    1.1,
                    2,
                    2,
                    Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()),
                    Size(30.0, 30.0)
                )
            }
            if (mJavaDetector2 != null) {
                mJavaDetector2!!.detectMultiScale(
                    mGray,
                    facesEye,
                    1.1,
                    2,
                    2,
                    Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()),
                    Size(30.0, 30.0)
                )
            }
            val facesArray = faces.toArray()
            val facesArray2 = facesEye.toArray()
            if (facesArray.size > 0 && facesArray2.size>0) {
                Log.d("mine","faces.size"+(fra.rgba()==null))
                for (i in facesArray.indices) {
                    val rect = facesArray[i]
                    showFrame(facesArray[i])
                    Imgproc.rectangle(
                        mRgba,
                        facesArray[i].tl(),
                        facesArray[i].br(),
                        FACE_RECT_COLOR,
                        30
                    )
                }
            }else{
                showFrame(null)
            }
        }


        /*isCheack=true
        ex2.submit {
            val facesArray =
                fra.rgba()?.nativeObjAddr?.let { faceDetect.faceDetect(it) }
            if (facesArray!=null && facesArray.size>0){
                for (face in facesArray) {
                    Log.d("mine","人脸"+face.faceRect.x+"_"+face.faceRect.y+"_"+face.faceRect.width+"_"+face.faceRect.height+"__"+face.faceAngle+"_"+face.faceConfidence)
                     showFrame(facesArray)
                    isCheack=false
                }
            }else{
                showFrame(null)
                isCheack=false
            }
        }*/

    }

    /**
     * 绘制人脸框
     */
    private fun showFrame(rect: Rect?) {
        if (!isFinishing) {
            runOnUiThread {
                val canvas: Canvas = mFaceDetectImageView.lockCanvas()
                if (canvas == null) {
                    mFaceDetectImageView.unlockCanvasAndPost(canvas)
                    return@runOnUiThread
                }

                if (rect == null) {
                    // 清空canvas
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    mFaceDetectImageView.unlockCanvasAndPost(canvas)
                    return@runOnUiThread
                }
                rectF?.set(FaceOnDrawTexturViewUtil.getFaceRectTwo(rect))
                // 检测图片的坐标和显示的坐标不一样，需要转换。
              /*  FaceOnDrawTexturViewUtil.mapFromOriginalRect(
                    rectF,
                    mAutoCameraPreviewView, rect
                )*/
                paint?.setColor(
                    resources.getColor(
                        R.color.color_5fe4ef
                    )
                )
                paint?.setStyle(Paint.Style.STROKE)
                // 绘制框

                rectF?.let { paint?.let { it1 ->
                    canvas.drawRect(it,it1)
                } }
                mFaceDetectImageView.unlockCanvasAndPost(canvas)
            }
        }
    }



}