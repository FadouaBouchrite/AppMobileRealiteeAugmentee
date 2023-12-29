package com.example.imagepro;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ImageDao {
    @Insert
    void insertImage(ImageEntity image);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertImages(List<ImageEntity> images);

    @Query("SELECT * FROM images WHERE type = :type")
    List<ImageEntity> getImagesByType(String type);
}