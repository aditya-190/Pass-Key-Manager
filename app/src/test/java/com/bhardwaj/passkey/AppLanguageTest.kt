package com.bhardwaj.passkey

import com.bhardwaj.passkey.domain.model.AppLanguage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * One list of languages. Arabic was once offered and translated but missing from locales_config, so picking it did
 * nothing; this fails on the JVM if the enum, the values- folders and locales_config ever disagree again. (The
 * instrumented LocalizationCoverageTest also checks each language resolves to its own strings on a device.)
 */
class AppLanguageTest {
    private val res = File("src/main/res")

    @Test
    fun `every offered language has a strings folder and nothing else does`() {
        val folders = res.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.matches(Regex("values-[a-z]{2}")) && File(it, "strings.xml").exists() }
            .map { it.name.removePrefix("values-") }
            .toSet()
        assertEquals(AppLanguage.entries.map { it.tag }.toSet() - "en", folders)
    }

    @Test
    fun `locales_config lists exactly the offered languages`() {
        val declared = Regex("""android:name="([^"]+)"""")
            .findAll(File(res, "xml/locales_config.xml").readText())
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(AppLanguage.entries.map { it.tag }.toSet(), declared)
    }

    @Test
    fun `an unknown or blank tag falls back to English`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTagOrDefault("xx"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTagOrDefault(null))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromTagOrDefault(" HI "))
    }
}
