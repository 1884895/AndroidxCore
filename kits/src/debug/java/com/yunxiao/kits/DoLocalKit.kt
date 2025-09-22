package com.yunxiao.kits

import android.app.Application
import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.didichuxing.doraemonkit.DoKit
import com.didichuxing.doraemonkit.kit.AbstractKit
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.Interceptor

class DoLocalKit {
    companion object {
        fun doLocalKit(context: Application, vararg localKit: AbstractKit) {
            Stetho.initializeWithDefaults(context)
            DoKit.Builder(context)
                .productId("72d72a52148ae3f31ac82ba381141d0e")
                .customKits(localKit.toMutableList())
                .build()
        }

        fun localInterceptor(context: Context): List<Interceptor>? {
            return arrayListOf(StethoInterceptor(), ChuckerInterceptor.Builder(context).build())
        }
    }
}