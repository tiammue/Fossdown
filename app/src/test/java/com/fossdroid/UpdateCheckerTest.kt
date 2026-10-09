package com.fossdroid

import com.fossdroid.update.UpdateChecker
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun normalizeStripsVPrefix() {
        assertEquals("1.2.3", UpdateChecker.normalizeVersion("v1.2.3"))
        assertEquals("1.2.3", UpdateChecker.normalizeVersion("V1.2.3"))
        assertEquals("1.0.0", UpdateChecker.normalizeVersion(" 1.0.0 "))
    }

    @Test
    fun compareSemVerOrdersVersions() {
        assertTrue(UpdateChecker.compareSemVer("1.1.0", "1.0.9") > 0)
        assertTrue(UpdateChecker.compareSemVer("1.0.0", "1.0.1") < 0)
        assertEquals(0, UpdateChecker.compareSemVer("v2.0.0", "2.0.0"))
        assertTrue(UpdateChecker.compareSemVer("2.0", "1.9.9") > 0)
    }

    @Test
    fun parseReleasePicksApkAsset() {
        val json = JSONObject(
            """
            {
              "tag_name": "v1.2.0",
              "html_url": "https://github.com/tiammue/Fossdroid/releases/tag/v1.2.0",
              "body": "Fixes",
              "assets": [
                {
                  "name": "notes.txt",
                  "browser_download_url": "https://example.com/notes.txt"
                },
                {
                  "name": "Fossdroid-1.2.0.apk",
                  "browser_download_url": "https://example.com/Fossdroid-1.2.0.apk"
                }
              ]
            }
            """.trimIndent()
        )
        val update = UpdateChecker.parseRelease(json)
        assertEquals("1.2.0", update.versionName)
        assertEquals("v1.2.0", update.tagName)
        assertEquals("https://example.com/Fossdroid-1.2.0.apk", update.apkDownloadUrl)
    }
}
