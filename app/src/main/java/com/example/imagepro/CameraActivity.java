package com.example.imagepro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final long CAPTURE_DELAY_MS = 5000;

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback;
    private Handler captureHandler;
    private Runnable captureRunnable;
    private TextView statusTextView; // Ajout du TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);

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

        captureHandler = new Handler();
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                captureImage();
            }
        };

        // Initialiser le TextView
        statusTextView = findViewById(R.id.statusTextView);
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
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
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

        // Attendre 5 secondes avant de capturer l'image
        captureHandler.postDelayed(captureRunnable, CAPTURE_DELAY_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        // Arrêter la capture programmée lors de la mise en pause de l'activité
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
        showToast("Image captured!");

        // Convertir la matrice OpenCV en un tableau de bytes
        byte[] imageData = convertMatToByteArray(mRgba);

        // Envoi de l'image au backend Flask
        new UploadImageTask().execute(imageData);
    }

    private class UploadImageTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected String doInBackground(byte[]... imageBytes) {
            try {
                // URL du backend Flask
                String backendUrl = "http://192.168.43.112:5000/upload";

                // Créer la demande multipart avec l'image
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "image.jpg", RequestBody.create(MediaType.parse("image/*"), imageBytes[0]))
                        .build();

                // Créer l'objet Request
                Request request = new Request.Builder()
                        .url(backendUrl)
                        .post(requestBody)
                        .build();

                // Créer l'objet OkHttpClient
                OkHttpClient client = new OkHttpClient();

                // Effectuer la demande avec OkHttp
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    // La demande a réussi
                    String responseBody = response.body().string();
                    return responseBody;
                } else {
                    // La demande a échoué
                    return "Failed to upload image to the backend. Response code: " + response.code();
                }
            } catch (Exception e) {
                // Gérer les erreurs
                e.printStackTrace();
                return "Failed to communicate with the backend: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            showToast(result);
            // Mettre à jour le TextView avec le résultat
            try {
                // Parse le JSON de la réponse
                JSONObject jsonObject = new JSONObject(result);
                String message = jsonObject.getString("message");

                // Affiche le message dans le Toast et le TextView
                showToast(message);
                statusTextView.setText(message);
            } catch (JSONException e) {
                e.printStackTrace();
                statusTextView.setText("Error parsing server response");
            }
        }
    }

    private byte[] convertMatToByteArray(Mat mat) {
        // Convertir la matrice OpenCV en un tableau de bytes
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(mat, bitmap);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
