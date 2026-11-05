package com.spicetify.mobile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.spicetify.mobile.ui.AppViewModel
import com.spicetify.mobile.ui.SpicetifyNavHost
import com.spicetify.mobile.ui.theme.SpicetifyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        handleDeepLink(intent)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val playback by viewModel.playback.collectAsStateWithLifecycle()

            SpicetifyTheme(
                settings = settings,
                artworkAccent = playback.artworkAccent,
            ) {
                // Surface fixes LocalContentColor so bare Text() calls stay legible on dark backgrounds.
                Surface(color = MaterialTheme.colorScheme.background) {
                    SpicetifyNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "spicetify-mobile" && data.host == "callback") {
            viewModel.handleAuthRedirect(data)
        }
    }
}
