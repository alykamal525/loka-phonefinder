package com.loka.phonefinder

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

data class Hit(val title: String, val snippet: String, val url: String, val query: String) {
    val host: String
        get() = try {
            URI(url).host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
}

data class NamesResult(
    val state: String,
    val status: CardStatus,
    val hits: List<Hit>,
    val detail: String,
    val provider: String,
    val checkedAt: Long
)

class ApiException(val code: Int, message: String) : Exception(message)

object SearchApi {
    private fun clean(s: String): String =
        s.replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"").replace("&amp;", "&").replace("&#x27;", "'").replace("&#39;", "'")
            .replace("&lt;", "<").replace("&gt;", ">")
            .trim()

    fun parseSerper(json: String, query: String): List<Hit> {
        val arr = JSONObject(json).optJSONArray("organic") ?: return emptyList()
        val out = ArrayList<Hit>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val url = o.optString("link")
            if (url.isEmpty()) continue
            out.add(Hit(clean(o.optString("title")), clean(o.optString("snippet")), url, query))
        }
        return out
    }

    fun parseBrave(json: String, query: String): List<Hit> {
        val arr = JSONObject(json).optJSONObject("web")?.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Hit>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val url = o.optString("url")
            if (url.isEmpty()) continue
            out.add(Hit(clean(o.optString("title")), clean(o.optString("description")), url, query))
        }
        return out
    }

    /** Keeps only hits whose title or snippet really contains a form of the number. */
    fun mentions(hits: List<Hit>, n: Normalized): List<Hit> {
        val forms = n.variants.filter { !it.startsWith("\"") }.distinct()
        val regexes = forms.map { Regex("(?<!\\d)" + Regex.escape(it) + "(?!\\d)") }
        return hits.filter { h ->
            val text = h.title + " " + h.snippet
            regexes.any { it.containsMatchIn(text) }
        }.distinctBy { it.url }
    }

    fun fetch(provider: String, key: String, query: String): List<Hit> {
        val c: HttpURLConnection
        if (provider == "brave") {
            val u = URL(
                "https://api.search.brave.com/res/v1/web/search?q=" +
                    URLEncoder.encode(query, "UTF-8") + "&count=20"
            )
            c = u.openConnection() as HttpURLConnection
            c.connectTimeout = 10000
            c.readTimeout = 15000
            c.setRequestProperty("X-Subscription-Token", key)
            c.setRequestProperty("Accept", "application/json")
        } else {
            c = URL("https://google.serper.dev/search").openConnection() as HttpURLConnection
            c.connectTimeout = 10000
            c.readTimeout = 15000
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("X-API-KEY", key)
            c.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("q", query).put("num", 20).put("gl", "eg").toString()
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = c.responseCode
        if (code != 200) throw ApiException(code, "HTTP $code")
        val txt = c.inputStream.bufferedReader().use { it.readText() }
        return if (provider == "brave") parseBrave(txt, query) else parseSerper(txt, query)
    }
}
