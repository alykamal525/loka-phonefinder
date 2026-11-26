package com.loka.phonefinder

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("loka", MODE_PRIVATE)
        val lang = prefs.getString("lang", "ar") ?: "ar"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        super.onCreate(savedInstanceState)
        setContent {
            LokaTheme {
                val vm: MainViewModel = viewModel()
                App(prefs, vm)
            }
        }
    }
}
