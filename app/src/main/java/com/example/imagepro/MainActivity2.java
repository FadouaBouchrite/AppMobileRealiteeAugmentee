package com.example.imagepro;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class   MainActivity2 extends AppCompatActivity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        imageView = findViewById(R.id.imageView);

        // Retrieve the captured image data from the intent
        Mat capturedImage = (Mat) getIntent().getSerializableExtra("capturedImage");

        // Display the image in an ImageView or process it as needed
        if (capturedImage != null) {
            Bitmap bitmap = Bitmap.createBitmap(capturedImage.cols(), capturedImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(capturedImage, bitmap);
            imageView.setImageBitmap(bitmap);
        }
    }
}
