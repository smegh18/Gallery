package albums.gallery.photo.folder.picasa.app.web.gallery

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.GalleryDatabase
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.datastore.AppPreferences
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.mediastore.MediaStoreScanner
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepositoryImpl
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.VaultRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.VaultRepositoryImpl
import albums.gallery.photo.folder.picasa.app.web.gallery.data.ads.AdManager

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
            .components {
                add(VideoFrameDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AdManager.initialize(this)
    }

    companion object {
        lateinit var instance: GalleryApplication
            private set
    }
}
