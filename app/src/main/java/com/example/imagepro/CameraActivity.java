package com.example.imagepro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, RetrieveImagesAsyncTask.OnImagesRetrievedListener {
    private static final String TAG = "CameraActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // Milliseconds
    private String type=null;
    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback;
    private Handler captureHandler;
    private TextView statusTextView;


    private int clickCount = 0;
    private long lastClickTime = 0;

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

        statusTextView = findViewById(R.id.statusTextView);


        initializeCamera();
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


        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        clickCount = 0;

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "OpenCV is not loaded. Try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        // Stop the scheduled capture when the activity is paused
        captureHandler.removeCallbacksAndMessages(null);
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

    private void handleClick() {
        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            // Double click detected
            captureImage();
        } else {
            // Single click, reset the counter
            clickCount = 1;
        }

        lastClickTime = clickTime;

        // Si c'est un double-clic, lancer la capture après chaque double-clic
        if (clickCount == 2) {
            captureImage();
            clickCount = 0; // Réinitialiser le compteur après la capture
        }
    }

    private void captureImage() {
        showToast("Image captured!");

        // Convert the OpenCV matrix to a byte array
        byte[] imageData = convertMatToByteArray(mRgba);

        // Send the image to the Flask backend
        new UploadImageTask().execute(imageData);
    }

    private class UploadImageTask extends AsyncTask<byte[], Void, String> implements RetrieveImagesAsyncTask.OnImagesRetrievedListener {
        @Override
        protected String doInBackground(byte[]... bytes) {
            try {
                // URL of the Flask backend
                String backendUrl = "http://192.168.43.112:5000/upload";

                // Create the multipart request with the image
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "image.jpg", RequestBody.create(MediaType.parse("image/*"), bytes[0]))
                        .build();

                // Create the Request object
                Request request = new Request.Builder()
                        .url(backendUrl)
                        .post(requestBody)
                        .build();

                // Create the OkHttpClient object



                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();


                // Perform the request with OkHttp
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    // The request was successful
                    String responseBody = response.body().string();
                    return responseBody;


                } else {
                    // The request failed
                    return "Failed to upload image to the backend. Response code: " + response.code();
                }
            } catch (Exception e) {
                // Handle errors
                e.printStackTrace();
                return "Failed to communicate with the backend: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {

            // Update the TextView with the result
            try {
                JSONObject jsonObject = new JSONObject(result);
                String status = jsonObject.getString("status");

                if ("success".equals(status)) {
                    JSONObject gpt4Result = jsonObject.getJSONObject("gpt4_result");
                    String description = gpt4Result.getString("un_tres_petit_descriptif");

                    updateUI(description);
                } else {
                    String message = jsonObject.getString("message");
                    showToast("Échec du téléchargement de l'image : " + message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                showToast("Erreur lors de l'analyse de la réponse du serveur"+e.getMessage());
            }
        }

        private void updateUI(String description) {
            // Mise à jour des TextView avec les informations reçues
            // Supprimer les accolades et backticks du JSON
            try {
                JSONObject jsonObject = new JSONObject(description);
Log.d("Json",jsonObject+"");
                // Extract values from JSON
                String unTresPetitDescriptif = jsonObject.getString("un_tres_petit_descriptif");
                 String type = jsonObject.getString("type");
                String histoire = jsonObject.getString("histoire");

                // Display extracted values in TextView
                statusTextView.setText("Un Tres Petit Descriptif : " + unTresPetitDescriptif + "\n"
                        + "Type: " + type + "\n"
                        + "Histoire: " + histoire);
                showToast("succes");


                AppDatabase database = DatabaseInitializer.getInstance(getApplicationContext());
                ImageDao imageDao = database.imageDao();
                // Create a list of images
                List<ImageEntity> list = new ArrayList<>();
                ImageEntity image1 = new ImageEntity("file:///android_asset/images/i4-removebg-preview.png", "Rafia");
                ImageEntity image2 = new ImageEntity("file:///android_asset/images/i6-removebg-preview.png", "Chelhaouia");
                list.add(image1);
                list.add(image2);
                showToast(type);
                // Insert images into the database asynchronously
                new InsertImagesAsyncTask(imageDao).execute(list);

                // Retrieve images by type asynchronously
                new RetrieveImagesAsyncTask(imageDao, this).execute(type);





            } catch (JSONException e) {

                showToast(e.getMessage());
           }
        }

        @Override
        public void onImagesRetrieved(List<ImageEntity> images) {

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


    @Override
    public void onImagesRetrieved(List<ImageEntity> images) {
        // Handle the retrieved images here
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ImagePagerAdapter adapter = new ImagePagerAdapter(images);
        viewPager.setAdapter(adapter);
    }
}
