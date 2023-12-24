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

import com.example.imagepro.R;

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
    private TextView statusTextView;
    private TextView descriptionTextView;
    private TextView typeTextView;
    private TextView histoireTextView;

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

        statusTextView = findViewById(R.id.statusTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        typeTextView = findViewById(R.id.typeTextView);
        histoireTextView = findViewById(R.id.histoireTextView);
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

        // Wait for 5 seconds before capturing the image
        captureHandler.postDelayed(captureRunnable, CAPTURE_DELAY_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        // Stop the scheduled capture when the activity is paused
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

        // Convert the OpenCV matrix to a byte array
        byte[] imageData = convertMatToByteArray(mRgba);

        // Send the image to the Flask backend
        new UploadImageTask().execute(imageData);
    }

    private class UploadImageTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected String doInBackground(byte[]... imageBytes) {
            try {
                // URL of the Flask backend
                String backendUrl = "http://192.168.43.112:5000/upload";

                // Create the multipart request with the image
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "image.jpg", RequestBody.create(MediaType.parse("image/*"), imageBytes[0]))
                        .build();

                // Create the Request object
                Request request = new Request.Builder()
                        .url(backendUrl)
                        .post(requestBody)
                        .build();

                // Create the OkHttpClient object
                OkHttpClient client = new OkHttpClient();

                // Perform the request with OkHttp
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    // The request was successful
                    String responseBody = response.body().string();
                    return responseBody;
                } else {
                    // The request failed
                    return "Failed to upload image to the backend. Response code: " + response.message();
                }
            } catch (Exception e) {
                // Handle errors
                e.printStackTrace();
                return "Failed to communicate with the backend: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            showToast(result);
            // Update the TextView with the result
            try {
                JSONObject jsonObject = new JSONObject(result);
                String status = jsonObject.getString("status");

                if ("success".equals(status)) {
                    JSONObject gpt4Result = jsonObject.getJSONObject("gpt4_result");
                    String description = gpt4Result.getString("un_tres_petit_descriptif");
                    String typeBabouche = gpt4Result.getString("type");
                    String histoire = gpt4Result.getString("histoire");

                    updateUI(description, typeBabouche, histoire);
                } else {
                    String message = jsonObject.getString("message");
                    showToast("Échec du téléchargement de l'image : " + message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                showToast("Erreur lors de l'analyse de la réponse du serveur");
            }
        }

        private void updateUI(String description, String typeBabouche, String histoire) {
            // Mise à jour des TextView avec les informations reçues


            // Supprimer les accolades et backticks du JSON
            String cleanDescription = description.replace("{", "").replace("}", "").replace("`", "").replace("json","");
            String cleanType = typeBabouche.replace("{", "").replace("}", "").replace("`", "").replace("json","");
            String cleanHistoire = histoire.replace("{", "").replace("}", "").replace("`", "").replace("json","");

            // Afficher les résultats propres
            statusTextView.setText("Description: " + cleanDescription + "\nType: " + cleanType + "\nHistoire: " + cleanHistoire);
        }
    }

    private byte[] convertMatToByteArray(Mat mat) {
        // Convert the OpenCV matrix to a byte array
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
