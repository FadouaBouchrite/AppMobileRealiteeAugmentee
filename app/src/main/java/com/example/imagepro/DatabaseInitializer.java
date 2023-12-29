package com.example.imagepro;



import android.content.Context;
import android.util.Log;

import androidx.room.Room;

// Assurez-vous qu'il n'y a qu'une seule instance de la base de données Room dans l'application.
public class DatabaseInitializer {
    private static AppDatabase database;

    public static AppDatabase getInstance(Context applicationContext) {
        if (database == null) {
            synchronized (AppDatabase.class) {
                if (database == null) {
                    database = Room.databaseBuilder(applicationContext,
                                    AppDatabase.class, "images_catalog.db")
                            .build();
                }
            }
        }
        return database;
    }
}