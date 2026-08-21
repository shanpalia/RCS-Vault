package com.example

import com.example.data.update.UpdateManager
import com.example.data.update.VersionInfo
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun `verify official update server URLs`() {
        assertEquals(
            "https://shanpalia.github.io/WebsitePaliaAPK_V.2/",
            UpdateManager.UPDATE_SERVER_BASE_URL
        )
        assertEquals(
            "https://shanpalia.github.io/WebsitePaliaAPK_V.2/rcs-vault/version.json",
            UpdateManager.VERSION_JSON_URL
        )
    }

    @Test
    fun `verify version json parsing structure`() {
        val sampleJson = """
            {
              "versionCode": 2,
              "versionName": "1.1.0",
              "apkUrl": "https://shanpalia.github.io/WebsitePaliaAPK_V.2/rcs-vault/rcs-vault-1.1.0.apk",
              "releaseNotes": [
                "Improved automatic backup",
                "Improved media backup",
                "Improved PDF reports",
                "Bug fixes"
              ],
              "forceUpdate": false
            }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val versionCode = json.optInt("versionCode")
        val versionName = json.optString("versionName")
        val apkUrl = json.optString("apkUrl")
        val forceUpdate = json.optBoolean("forceUpdate")
        val notesArray = json.optJSONArray("releaseNotes")
        val releaseNotes = mutableListOf<String>()
        if (notesArray != null) {
            for (i in 0 until notesArray.length()) {
                releaseNotes.add(notesArray.optString(i))
            }
        }

        val versionInfo = VersionInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            releaseNotes = releaseNotes,
            forceUpdate = forceUpdate
        )

        assertEquals(2, versionInfo.versionCode)
        assertEquals("1.1.0", versionInfo.versionName)
        assertEquals("https://shanpalia.github.io/WebsitePaliaAPK_V.2/rcs-vault/rcs-vault-1.1.0.apk", versionInfo.apkUrl)
        assertEquals(4, versionInfo.releaseNotes.size)
        assertFalse(versionInfo.forceUpdate)
        assertTrue(versionInfo.releaseNotes.contains("Improved automatic backup"))
    }
}
