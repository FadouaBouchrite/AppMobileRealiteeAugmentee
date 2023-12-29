package com.example.imagepro;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "images")
public class ImageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String path;
    public String type;

    public ImageEntity(String path, String type) {
        this.path = path;
        this.type = type;
    }
}
