package com.loka.phonefinder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchApiTest {
    private val n = PhoneUtil.normalize("01012345678", "EG")!!

    @Test
    fun serperParsesOrganicResults() {
        val json = """{"organic":[{"title":"Cairo Dental Clinic","link":"https://example.com/a","snippet":"Call +20 10 1234 5678 today"},{"title":"x","link":"","snippet":"skip"}]}"""
        val hits = SearchApi.parseSerper(json, "q")
        assertEquals(1, hits.size)
        assertEquals("Cairo Dental Clinic", hits[0].title)
        assertEquals("example.com", hits[0].host)
    }

    @Test
    fun braveParsesAndStripsTags() {
        val json = """{"web":{"results":[{"title":"Shop <strong>01012345678</strong>","url":"https://www.shop.example/p","description":"Tel &amp; fax"}]}}"""
        val hits = SearchApi.parseBrave(json, "q")
        assertEquals(1, hits.size)
        assertEquals("Shop 01012345678", hits[0].title)
        assertEquals("Tel & fax", hits[0].snippet)
        assertEquals("shop.example", hits[0].host)
    }

    @Test
    fun emptyOrMissingResultsAreEmpty() {
        assertTrue(SearchApi.parseSerper("{}", "q").isEmpty())
        assertTrue(SearchApi.parseBrave("{}", "q").isEmpty())
    }

    @Test
    fun mentionsKeepOnlyHitsThatContainTheNumber() {
        val hits = listOf(
            Hit("Clinic", "Call 01012345678 now", "https://a.example/1", "q"),
            Hit("Other", "No number here", "https://b.example/2", "q"),
            Hit("Longer", "Call 010123456789 now", "https://c.example/3", "q"),
            Hit("Intl", "Reach us on +20 10 1234 5678", "https://d.example/4", "q"),
            Hit("Dup", "01012345678", "https://a.example/1", "q2")
        )
        val m = SearchApi.mentions(hits, n)
        assertEquals(listOf("https://a.example/1", "https://d.example/4"), m.map { it.url })
    }
}
