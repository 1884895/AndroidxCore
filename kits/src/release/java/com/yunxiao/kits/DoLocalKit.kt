package com.yunxiao.kits

import android.app.Application
import android.content.Context
import okhttp3.Interceptor

class DoLocalKit {

    companion object {
        fun doLocalKit(context: Application, vararg localKit: LocalKit) {

        }

        fun localInterceptor(context: Context): List<Interceptor>? {
            return null
        }
    }
}