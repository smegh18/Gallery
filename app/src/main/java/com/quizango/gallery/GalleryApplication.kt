package com.quizango.gallery

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.quizango.gallery.data.local.database.GalleryDatabase
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.local.mediastore.MediaStoreScanner
import com.quizango.gallery.data.local.mediastore.MediaStoreThumbnailFetcher
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.MediaRepositoryImpl
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.data.repository.VaultRepositoryImpl
import com.quizango.gallery.ui.theme.applyNightMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleryApplication : Application(), ImageLoaderFactory {

    val database by lazy { GalleryDatabase.getInstance(this) }
    val preferences by lazy { AppPreferences(this) }
    val scanner by lazy { MediaStoreScanner(this) }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(database.galleryDao(), scanner)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(this, database.vaultDao(), database.galleryDao())
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .components {
                add(MediaStoreThumbnailFetcher.Factory(this@GalleryApplication))
                add(VideoFrameDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Decrypted vault copies only live while the app runs
        CoroutineScope(Dispatchers.IO).launch { vaultRepository.clearViewCache() }
        CoroutineScope(Dispatchers.Main.immediate).launch {
            preferences.themeMode.collect { mode ->
                applyNightMode(mode)
            }
        }
    }

    companion object {
        lateinit var instance: GalleryApplication
            private set
    }
}
