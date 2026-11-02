package com.quizango.gallery.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quizango.gallery.data.local.database.converter.Converters
import com.quizango.gallery.data.local.database.dao.GalleryDao
import com.quizango.gallery.data.local.database.dao.VaultDao
import com.quizango.gallery.data.local.database.entity.MediaEntity
import com.quizango.gallery.data.local.database.entity.VaultEntity

@Database(
    entities = [MediaEntity::class, VaultEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun galleryDao(): GalleryDao
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var instance: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GalleryDatabase::class.java,
                    "gallery_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
}
