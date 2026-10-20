package albums.gallery.photo.folder.picasa.app.web.gallery

import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.converter.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testStringListConversion() {
        val original = listOf("tag1", "tag2", "summer_2026")
        val joined = converters.fromStringList(original)
        val restored = converters.toStringList(joined)

        assertEquals(original, restored)
    }

    @Test
    fun testStringListNullAndEmpty() {
        assertEquals("", converters.fromStringList(null))
        assertTrue(converters.toStringList(null).isEmpty())
        assertTrue(converters.toStringList("").isEmpty())
    }

    @Test
    fun testIntListConversion() {
        val original = listOf(255, 128, 0, 42)
        val joined = converters.fromIntList(original)
        val restored = converters.toIntList(joined)

        assertEquals(original, restored)
    }

    @Test
    fun testIntListNullAndEmpty() {
        assertEquals("", converters.fromIntList(null))
        assertTrue(converters.toIntList(null).isEmpty())
        assertTrue(converters.toIntList("").isEmpty())
    }
}
