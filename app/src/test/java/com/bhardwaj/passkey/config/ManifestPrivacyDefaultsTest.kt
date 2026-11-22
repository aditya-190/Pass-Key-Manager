package com.bhardwaj.passkey.config

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Nothing may be collected before the user chooses, and the app must never declare an advertising ID.
 * These defaults live in XML where no compiler checks them, so this test pins every one.
 */
class ManifestPrivacyDefaultsTest {
    private val android = "http://schemas.android.com/apk/res/android"
    private val tools = "http://schemas.android.com/tools"

    private val manifest = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(File("src/main/AndroidManifest.xml"))

    private fun elements(tag: String): List<Element> {
        val nodes = manifest.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private val metaData: Map<String, String> =
        elements("meta-data").associate { it.getAttributeNS(android, "name") to it.getAttributeNS(android, "value") }

    @Test
    fun `every collection and consent default is off`() {
        listOf(
            "firebase_analytics_collection_enabled",
            "firebase_crashlytics_collection_enabled",
            "firebase_performance_collection_enabled",
            "google_analytics_adid_collection_enabled",
            "google_analytics_default_allow_analytics_storage",
            "google_analytics_default_allow_ad_storage",
            "google_analytics_default_allow_ad_user_data",
            "google_analytics_default_allow_ad_personalization_signals"
        ).forEach { key -> assertEquals(key, "false", metaData[key]) }
    }

    @Test
    fun `automatic screen reporting is off so screen_view comes only from navigation`() {
        assertEquals("false", metaData["google_analytics_automatic_screen_reporting_enabled"])
    }

    @Test
    fun `advertising ID permissions are stripped from the merged manifest`() {
        val removed = elements("uses-permission")
            .filter { it.getAttributeNS(tools, "node") == "remove" }
            .map { it.getAttributeNS(android, "name") }
            .toSet()
        listOf(
            "com.google.android.gms.permission.AD_ID",
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION"
        ).forEach { assertTrue(it, it in removed) }
    }
}
