package com.example.imagepro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class CarouselActivity extends AppCompatActivity implements RetrieveImagesAsyncTask.OnImagesRetrievedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carousel);
        AppDatabase database = DatabaseInitializer.getInstance(getApplicationContext());
        ImageDao imageDao = database.imageDao();
        // Create a list of images
        List<ImageEntity> list = new ArrayList<>();
        ImageEntity image1 = new ImageEntity("file:///android_asset/images/i4-removebg-preview.png", "shlha");
        ImageEntity image2 = new ImageEntity("file:///android_asset/images/i6-removebg-preview.png", "shlha");
        list.add(image1);
        list.add(image2);
        // Insert images into the database asynchronously
        new InsertImagesAsyncTask(imageDao).execute(list);

        // Retrieve images by type asynchronously
        new RetrieveImagesAsyncTask(imageDao, this).execute("shlha");
    }

    @Override
    public void onImagesRetrieved(List<ImageEntity> images) {
        // Handle the retrieved images here
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ImagePagerAdapter adapter = new ImagePagerAdapter(images);
        viewPager.setAdapter(adapter);
    }

}