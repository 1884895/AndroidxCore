package com.haofenshu.AndroidxCore

import android.app.Application
import com.haofenshu.lnkscreen.initKioskMode

class AndroidXCore : Application() {
    override fun onCreate() {
        super.onCreate()
        initKioskMode()
    }
}