package com.loka.phonefinder

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStore {
    private fun prefs(ctx: Context): SharedPreferences? = try {
        val mk = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx, "loka_secure", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        null
    }

    fun getKey(ctx: Context, provider: String): String =
        prefs(ctx)?.getString("key_$provider", "") ?: ""

    fun setKey(ctx: Context, provider: String, key: String): Boolean {
        val p = prefs(ctx) ?: return false
        p.edit().putString("key_$provider", key.trim()).apply()
        return true
    }

    fun clear(ctx: Context, provider: String) {
        prefs(ctx)?.edit()?.remove("key_$provider")?.apply()
    }
}
