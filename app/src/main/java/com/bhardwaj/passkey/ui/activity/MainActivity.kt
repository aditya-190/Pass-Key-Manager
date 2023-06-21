package com.bhardwaj.passkey.ui.activity

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bhardwaj.passkey.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(binding.root)
        getLanguage()
    }

    private fun getLanguage() {
        changeLanguage(getSharedPreferences("settings", MODE_PRIVATE).getString("language", "en").toString())
    }

    fun changeLanguage(languageId: String) {
        val locale = Locale(languageId)
        Locale.setDefault(locale)
        val configuration = this@MainActivity.resources.configuration
        configuration.setLocale(locale)
        baseContext.resources.updateConfiguration(configuration, baseContext.resources.displayMetrics)
        getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", locale.language).apply()
    }
}