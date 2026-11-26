package com.loka.phonefinder

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import java.net.URLEncoder

data class Normalized(
    val raw: String,
    val valid: Boolean,
    val e164: String,
    val digits: String,
    val region: String,
    val cc: Int,
    val nsn: String,
    val natDigits: String,
    val intl: String,
    val typeName: String,
    val variants: List<String>,
    val queries: List<String>,
    val combined: String
)

object PhoneUtil {
    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    val regions: List<String> get() = util.supportedRegions.toList()

    fun callingCode(region: String): Int = util.getCountryCodeForRegion(region)

    fun asciiDigits(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                in '\u0660'..'\u0669' -> sb.append('0' + (c - '\u0660'))
                in '\u06F0'..'\u06F9' -> sb.append('0' + (c - '\u06F0'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun normalize(input: String, region: String): Normalized? {
        val cleaned = asciiDigits(input).trim()
        if (cleaned.isEmpty()) return null
        val n = try {
            util.parse(cleaned, region)
        } catch (e: NumberParseException) {
            return null
        }
        val valid = util.isValidNumber(n)
        val e164 = util.format(n, PhoneNumberFormat.E164)
        val digits = e164.removePrefix("+")
        val cc = n.countryCode
        val nsn = util.getNationalSignificantNumber(n)
        val nationalFmt = util.format(n, PhoneNumberFormat.NATIONAL)
        val natDigits = nationalFmt.filter { it.isDigit() }
        val intl = util.format(n, PhoneNumberFormat.INTERNATIONAL)
        val reg = util.getRegionCodeForNumber(n) ?: region
        val typeName = util.getNumberType(n).name

        val variants = LinkedHashSet<String>()
        variants.add(e164)
        variants.add(digits)
        variants.add(natDigits)
        variants.add(nsn)
        variants.add(intl)
        variants.add(intl.replace(" ", "-"))
        variants.add(nationalFmt)
        variants.add(nationalFmt.replace(" ", "-"))
        variants.add("(+$cc) $nsn")
        variants.add("\"$e164\"")
        variants.add("\"$natDigits\"")
        variants.add("\"$digits\"")

        val queries = LinkedHashSet<String>()
        queries.add("\"$e164\"")
        queries.add("\"$natDigits\"")
        queries.add("\"$digits\"")
        listOf(
            "مين صاحب الرقم %s",
            "اسم صاحب الرقم %s",
            "رقم التليفون %s",
            "من اتصل بي %s"
        ).forEach { queries.add(it.format(natDigits)) }
        listOf(
            "Who called me %s",
            "Phone number lookup %s",
            "Caller ID %s",
            "Spam number %s",
            "Scam report %s",
            "Business phone %s"
        ).forEach { queries.add(it.format(e164)) }

        val combined = "\"$e164\" OR \"$natDigits\" OR \"$digits\""
        return Normalized(
            cleaned, valid, e164, digits, reg, cc, nsn, natDigits, intl, typeName,
            variants.toList(), queries.toList(), combined
        )
    }

    fun fillUrl(template: String, n: Normalized): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        return template
            .replace("{query}", enc(n.combined))
            .replace("{e164digits}", n.digits)
            .replace("{e164}", enc(n.e164))
            .replace("{nsn}", n.nsn)
            .replace("{national}", n.natDigits)
            .replace("{region_lc}", n.region.lowercase())
            .replace("{cc}", n.cc.toString())
    }

    fun searchUrl(engine: String, query: String): String {
        val q = URLEncoder.encode(query, "UTF-8")
        return when (engine) {
            "bing" -> "https://www.bing.com/search?q=$q"
            "ddg" -> "https://duckduckgo.com/?q=$q"
            else -> "https://www.google.com/search?q=$q"
        }
    }
}
