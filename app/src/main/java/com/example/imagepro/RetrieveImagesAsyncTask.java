package com.example.imagepro;


import android.os.AsyncTask;

import java.util.List;
public class RetrieveImagesAsyncTask extends AsyncTask<String, Void, List<ImageEntity>> {
    private ImageDao imageDao;
    private OnImagesRetrievedListener listener;

    public RetrieveImagesAsyncTask(ImageDao imageDao, OnImagesRetrievedListener listener) {
        this.imageDao = imageDao;
        this.listener = listener;
    }

    @Override
    protected List<ImageEntity> doInBackground(String... strings) {
        return imageDao.getImagesByType(strings[0]);
    }

    @Override
    protected void onPostExecute(List<ImageEntity> result) {
        super.onPostExecute(result);
        // Pass the result to the listener
        listener.onImagesRetrieved(result);
    }

    // Define the callback interface
    public interface OnImagesRetrievedListener {
        void onImagesRetrieved(List<ImageEntity> images);
    }
}