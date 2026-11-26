package com.loka.phonefinder

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("loka", MODE_PRIVATE).getString("lang", "ar") ?: "ar"
        val loc = Locale(lang)
        Locale.setDefault(loc)
        val cfg = Configuration(newBase.resources.configuration)
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        super.attachBaseContext(newBase.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("loka", MODE_PRIVATE)
        setContent {
            LokaTheme {
                val vm: MainViewModel = viewModel()
                App(prefs, vm)
            }
        }
    }
}
