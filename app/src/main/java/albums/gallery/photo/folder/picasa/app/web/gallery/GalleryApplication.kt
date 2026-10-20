package albums.gallery.photo.folder.picasa.app.web.gallery

import android.app.Application
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.GalleryDatabase
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.datastore.AppPreferences
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.mediastore.MediaStoreScanner
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepositoryImpl
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.VaultRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.VaultRepositoryImpl

class GalleryApplication : Application() {

    val database by lazy { GalleryDatabase.getInstance(this) }
    val preferences by lazy { AppPreferences(this) }
    val scanner by lazy { MediaStoreScanner(this) }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(database.galleryDao(), scanner)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(this, database.vaultDao(), database.galleryDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GalleryApplication
            private set
    }
}
