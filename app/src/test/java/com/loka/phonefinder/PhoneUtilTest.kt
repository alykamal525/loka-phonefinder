package com.loka.phonefinder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilTest {
    @Test
    fun egyptianMobilePrefixesAreValid() {
        for (p in listOf("010", "011", "012", "015")) {
            val n = PhoneUtil.normalize(p + "12345678", "EG")
            assertNotNull(p, n)
            assertTrue(p, n!!.valid)
            assertEquals("+20" + p.drop(1) + "12345678", n.e164)
            assertEquals("MOBILE", n.typeName)
            assertEquals("EG", n.region)
        }
    }

    @Test
    fun egyptianInputFormsAllNormalizeTheSame() {
        val inputs = listOf(
            "01012345678", "1012345678", "10 1234 5678", "010-1234-5678", "+201012345678",
            "00201012345678", "201012345678", "(010) 1234 5678", "٠١٠١٢٣٤٥٦٧٨"
        )
        for (i in inputs) {
            val n = PhoneUtil.normalize(i, "EG")
            assertNotNull(i, n)
            assertTrue(i, n!!.valid)
            assertEquals(i, "+201012345678", n.e164)
        }
    }

    @Test
    fun invalidInputsAreRejected() {
        assertNull(PhoneUtil.normalize("", "EG"))
        assertNull(PhoneUtil.normalize("abc", "EG"))
        val shortNum = PhoneUtil.normalize("0101234", "EG")
        assertTrue(shortNum == null || !shortNum.valid)
        val badPrefix = PhoneUtil.normalize("01312345678", "EG")
        assertTrue(badPrefix == null || !badPrefix.valid)
    }

    @Test
    fun internationalNumbersWork() {
        val us = PhoneUtil.normalize("+16502530000", "EG")
        assertNotNull(us)
        assertTrue(us!!.valid)
        assertEquals("US", us.region)
        assertEquals("+16502530000", us.e164)
        val sa = PhoneUtil.normalize("0512345678", "SA")
        assertNotNull(sa)
        assertTrue(sa!!.valid)
        assertEquals("+966512345678", sa.e164)
    }

    @Test
    fun variantsContainTheExpectedForms() {
        val n = PhoneUtil.normalize("01012345678", "EG")!!
        assertTrue(n.variants.contains("+201012345678"))
        assertTrue(n.variants.contains("201012345678"))
        assertTrue(n.variants.contains("01012345678"))
        assertTrue(n.variants.contains("1012345678"))
        assertTrue(n.variants.contains("+20 10 1234 5678"))
        assertTrue(n.variants.contains("+20-10-1234-5678"))
        assertTrue(n.variants.contains("\"+201012345678\""))
        assertEquals(n.variants.size, n.variants.toSet().size)
    }

    @Test
    fun queriesAreGeneratedWithoutDuplicates() {
        val n = PhoneUtil.normalize("01012345678", "EG")!!
        assertEquals(n.queries.size, n.queries.toSet().size)
        assertTrue(n.queries.contains("\"+201012345678\""))
        assertTrue(n.queries.contains("مين صاحب الرقم 01012345678"))
        assertTrue(n.queries.contains("اسم صاحب الرقم 01012345678"))
        assertTrue(n.queries.contains("رقم التليفون 01012345678"))
        assertTrue(n.queries.contains("من اتصل بي 01012345678"))
        assertTrue(n.queries.contains("Who called me +201012345678"))
        assertTrue(n.queries.contains("Phone number lookup +201012345678"))
        assertTrue(n.queries.contains("Caller ID +201012345678"))
        assertTrue(n.queries.contains("Spam number +201012345678"))
        assertTrue(n.queries.contains("Scam report +201012345678"))
        assertTrue(n.queries.contains("Business phone +201012345678"))
        assertEquals("\"+201012345678\" OR \"01012345678\" OR \"201012345678\"", n.combined)
    }

    @Test
    fun urlTemplatesAreFilledAndEncoded() {
        val n = PhoneUtil.normalize("01012345678", "EG")!!
        assertEquals("https://wa.me/201012345678", PhoneUtil.fillUrl("https://wa.me/{e164digits}", n))
        assertEquals("https://t.me/+201012345678", PhoneUtil.fillUrl("https://t.me/+{e164digits}", n))
        assertEquals("https://www.tellows.com/num/%2B201012345678", PhoneUtil.fillUrl("https://www.tellows.com/num/{e164}", n))
        assertEquals("https://www.truecaller.com/search/eg/1012345678", PhoneUtil.fillUrl("https://www.truecaller.com/search/{region_lc}/{nsn}", n))
        val g = PhoneUtil.fillUrl("https://www.google.com/search?q={query}", n)
        assertTrue(g.startsWith("https://www.google.com/search?q=%22%2B201012345678%22"))
        assertFalse(g.contains("{"))
        assertFalse(g.contains(" "))
    }

    @Test
    fun arabicDigitsConvert() {
        assertEquals("0123456789", PhoneUtil.asciiDigits("٠١٢٣٤٥٦٧٨٩"))
        assertEquals("0123456789", PhoneUtil.asciiDigits("۰۱۲۳۴۵۶۷۸۹"))
    }
}
