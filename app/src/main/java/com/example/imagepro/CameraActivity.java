package com.example.imagepro;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final long CAPTURE_INTERVAL_MS = 5000; // 5 seconds

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback;
    private Handler captureHandler;
    private Runnable captureRunnable;
    private Mat capturedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Request camera permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);

        // Initialize OpenCV
        mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.i(TAG, "OpenCV Is loaded");
                        mOpenCvCameraView.enableView();
                    }
                    default: {
                        super.onManagerConnected(status);
                    }
                    break;
                }
            }
        };

        // Initialize the capture handler and runnable
        captureHandler = new Handler();
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                captureImage();
                showToast("Image captured!");
                // Schedule the next capture after the specified interval
                captureHandler.postDelayed(this, CAPTURE_INTERVAL_MS);
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                showToast("Camera permission denied");
            }
        }
    }

    private void initializeCamera() {
        mOpenCvCameraView = findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "OpenCV is not loaded. Try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }

        // Start capturing images every 5 seconds
        captureHandler.postDelayed(captureRunnable, CAPTURE_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        // Stop capturing when the activity is paused
        captureHandler.removeCallbacks(captureRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        return mRgba;
    }

    private void captureImage() {
        // Copy the current frame to the capturedImage Mat
        capturedImage = new Mat();
        mRgba.copyTo(capturedImage);

        // Save the captured image to a file (optional)
        saveImageToFile(capturedImage);
    }

    private void saveImageToFile(Mat image) {
        // Create a directory for storing images
        File storageDir = new File(Environment.getExternalStorageDirectory(), "CapturedImages");
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        // Save the image with a timestamp in the filename
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "IMG_" + timestamp + ".jpg";
        File imageFile = new File(storageDir, filename);
        Imgcodecs.imwrite(imageFile.getAbsolutePath(), image);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}



