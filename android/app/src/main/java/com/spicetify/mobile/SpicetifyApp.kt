package com.spicetify.mobile

import android.app.Application

class SpicetifyApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
