package com.example.imagepro;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity2 extends AppCompatActivity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        imageView = findViewById(R.id.imageView);

        // Retrieve the captured image path from the intent
        String capturedImagePath = getIntent().getStringExtra("capturedImagePath");

        // Display the image in an ImageView or process it as needed
        if (capturedImagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(capturedImagePath);
            imageView.setImageBitmap(bitmap);
        }
    }
}
