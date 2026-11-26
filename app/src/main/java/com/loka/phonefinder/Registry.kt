package com.loka.phonefinder

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Registry {
    const val REMOTE_URL =
        "https://raw.githubusercontent.com/alykamal525/loka-phonefinder/main/registry/providers.json"

    data class Data(
        val version: String,
        val updated: String,
        val source: String,
        val providers: List<Provider>
    )

    fun parse(json: String, source: String): Data {
        val o = JSONObject(json)
        val arr = o.getJSONArray("providers")
        val list = ArrayList<Provider>()
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val modes = ArrayList<String>()
            val m = p.optJSONArray("modes")
            if (m != null) for (j in 0 until m.length()) modes.add(m.getString(j))
            list.add(
                Provider(
                    id = p.getString("id"),
                    name = p.getString("name"),
                    category = p.optString("category"),
                    type = p.optString("type"),
                    url = p.optString("url"),
                    homepage = p.optString("homepage"),
                    noteEn = p.optString("note_en"),
                    noteAr = p.optString("note_ar"),
                    egypt = p.optString("egypt"),
                    status = p.optString("status", "active"),
                    lastChecked = p.optString("lastChecked"),
                    lastResult = p.optString("lastResult"),
                    modes = modes
                )
            )
        }
        return Data(o.optString("version"), o.optString("updated"), source, list)
    }

    private fun bundled(ctx: Context): Data =
        parse(ctx.assets.open("providers.json").bufferedReader().use { it.readText() }, "bundled")

    fun load(ctx: Context): Data {
        val b = bundled(ctx)
        val f = File(ctx.filesDir, "providers_cache.json")
        if (f.exists()) {
            try {
                val c = parse(f.readText(), "cache")
                if (c.updated >= b.updated) return c
            } catch (e: Exception) {
            }
        }
        return b
    }

    fun fetchRemote(ctx: Context): Data? {
        return try {
            val c = URL(REMOTE_URL).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            if (c.responseCode != 200) return null
            val txt = c.inputStream.bufferedReader().use { it.readText() }
            val d = parse(txt, "remote")
            if (d.providers.isEmpty()) return null
            File(ctx.filesDir, "providers_cache.json").writeText(txt)
            d
        } catch (e: Exception) {
            null
        }
    }
}
