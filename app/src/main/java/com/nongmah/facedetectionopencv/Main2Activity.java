package com.nongmah.facedetectionopencv;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.nongmah.facedetectionopencv.lib.PortraitCameraBridgeViewBase;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main2Activity extends AppCompatActivity implements PortraitCameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = Main2Activity.class.getSimpleName();

    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private int mCameraId = PortraitCameraBridgeViewBase.CAMERA_ID_BACK;

    private final String _TAG = "ProcessedCameraActivity:";

    private PortraitCameraBridgeViewBase mOpenCvCameraView;

    Main2Activity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mOpenCvCameraView = (PortraitCameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(mCameraId);

        findViewById(R.id.btnSwapCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        String TAG = new StringBuilder(_TAG).append("onResume").toString();
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initiation");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback);
        } else {
            Log.i(TAG, "OpenCV library found inside package. Using it");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        String TAG = new StringBuilder(_TAG).append("onPause").toString();
        Log.i(TAG, "Disabling a camera view");

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        String TAG = new StringBuilder(_TAG).append("onDestroy").toString();
        Log.i(TAG, "Disabling a camera view");

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        super.onDestroy();
    }

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            String TAG = new StringBuilder(_TAG).append("onManagerConnected").toString();

            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");


                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }



                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        String TAG = new StringBuilder(_TAG).append("onCameraViewStarted").toString();

        Log.i(TAG, "OpenCV CameraView Started");
    }

    @Override
    public void onCameraViewStopped() {
        String TAG = new StringBuilder(_TAG).append("onCameraViewStarted").toString();

        Log.i(TAG, "OpenCV CameraView Stopped");
    }

    @Override
    public Mat onCameraFrame(PortraitCameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Mat rgba = inputFrame.rgba();
//
//        return rgba;

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }


        MatOfRect faces = new MatOfRect();

//        Core.flip(mRgba.t(), mRgba, 1);
//        Core.flip(mGray.t(), mGray, 1);

        if (mDetectorType == JAVA_DETECTOR) {
            Log.d(TAG, "111");
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            Log.d(TAG, "222");
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

//        mRgba = inputFrame.rgba();
//        mGray = inputFrame.gray();

        return mRgba;
    }

    private void swapCamera() {
        mOpenCvCameraView.disableView();
        if (mCameraId == PortraitCameraBridgeViewBase.CAMERA_ID_BACK) {
            mCameraId = PortraitCameraBridgeViewBase.CAMERA_ID_FRONT;
        } else {
            mCameraId = PortraitCameraBridgeViewBase.CAMERA_ID_BACK;
        }
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }
}