package com.shirulot.myshader

import android.app.Application
import com.pixpark.gpupixel.GPUPixel

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        GPUPixel.Init(this)
    }
}