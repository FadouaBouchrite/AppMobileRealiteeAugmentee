package com.example.imagepro;


import android.os.AsyncTask;

import java.util.List;

public class InsertImagesAsyncTask extends AsyncTask<List<ImageEntity>, Void, Void> {
    private ImageDao imageDao;

    InsertImagesAsyncTask(ImageDao imageDao) {
        this.imageDao = imageDao;
    }

    @Override
    protected Void doInBackground(List<ImageEntity>... lists) {
        imageDao.insertImages(lists[0]);
        return null;
    }
}
