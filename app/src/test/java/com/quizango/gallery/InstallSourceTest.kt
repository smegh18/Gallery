package com.quizango.gallery

import com.quizango.gallery.data.local.InstallSource
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallSourceTest {
    @Test
    fun organicOnlyWhenPlaySaysSo() {
        assertEquals(InstallSource.ORGANIC, InstallSource.classify("utm_source=google-play&utm_medium=organic"))
        assertEquals(InstallSource.CAMPAIGN, InstallSource.classify("utm_source=fb&utm_medium=cpc&utm_campaign=x"))
        assertEquals(InstallSource.CAMPAIGN, InstallSource.classify(null)) // sideloaded / debug
    }
}
