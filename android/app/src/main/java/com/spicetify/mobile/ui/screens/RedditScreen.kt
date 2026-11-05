package com.spicetify.mobile.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.spicetify.mobile.ui.components.AppBackground
import com.spicetify.mobile.ui.components.DetailHeader

private const val REDDIT_URL = "https://www.reddit.com/r/spotify/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RedditScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val current = webView
        if (current != null && current.canGoBack()) {
            current.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            DetailHeader(
                title = "Reddit",
                subtitle = "r/spotify · Custom App",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Recargar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(REDDIT_URL)),
                                )
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Abrir en el navegador",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = WebViewClient()
                        webView = this
                        loadUrl(REDDIT_URL)
                    }
                },
                update = { view ->
                    webView = view
                },
            )
        }
    }
}
