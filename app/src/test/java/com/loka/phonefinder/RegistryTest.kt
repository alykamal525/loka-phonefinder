package com.loka.phonefinder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RegistryTest {
    private val data: Registry.Data by lazy {
        val f = File("../registry/providers.json")
        assertTrue("registry file missing at " + f.absolutePath, f.exists())
        Registry.parse(f.readText(), "test")
    }

    private val known = setOf("query", "e164digits", "e164", "nsn", "national", "region_lc", "cc")

    @Test
    fun registryParsesAndIsNotEmpty() {
        assertTrue(data.providers.size >= 30)
    }

    @Test
    fun idsAreUnique() {
        assertEquals(data.providers.size, data.providers.map { it.id }.toSet().size)
    }

    @Test
    fun requiredDiscoveryListIsPresent() {
        val names = data.providers.map { it.name.lowercase() }
        for (r in listOf(
            "truecaller", "getcontact", "hiya", "sync.me", "callapp", "eyecon", "whoscall",
            "tellows", "numbuster", "should i answer", "callersmart", "unknownphone", "whocallsme"
        )) {
            assertTrue(r, names.any { it.contains(r) })
        }
        for (r in listOf("facebook", "instagram", "telegram", "whatsapp", "linkedin", "google", "bing", "yahoo", "duckduckgo")) {
            assertTrue(r, names.any { it.contains(r) })
        }
    }

    @Test
    fun urlsAreHttpsAndTemplatesUseKnownPlaceholders() {
        val ph = Regex("\\{([a-z0-9_]+)\\}")
        for (p in data.providers) {
            assertTrue(p.id + " homepage", p.homepage.startsWith("https://"))
            if (p.url.isNotEmpty()) {
                assertTrue(p.id + " url", p.url.startsWith("https://"))
                for (m in ph.findAll(p.url)) assertTrue(p.id + " " + m.value, m.groupValues[1] in known)
            }
            assertTrue(p.id + " modes", p.modes.isNotEmpty() && p.modes.all { it in setOf("quick", "deep", "engines") })
            assertTrue(p.id + " category", p.category in setOf("engine", "caller_id", "egypt", "social"))
            assertTrue(p.id + " notes", p.noteEn.isNotEmpty() && p.noteAr.isNotEmpty())
            assertEquals(p.id, "active", p.status)
        }
    }

    @Test
    fun everyTemplateFillsCompletely() {
        val n = PhoneUtil.normalize("01012345678", "EG")!!
        for (p in data.providers) {
            val t = if (p.url.isNotEmpty()) p.url else p.homepage
            val u = PhoneUtil.fillUrl(t, n)
            assertTrue(p.id, !u.contains("{") && !u.contains(" "))
        }
    }

    @Test
    fun everyModeHasProviders() {
        for (m in listOf("quick", "deep", "engines")) {
            assertTrue(m, data.providers.any { m in it.modes })
        }
        assertTrue(data.providers.filter { "engines" in it.modes }.all { it.category == "engine" })
    }
}
