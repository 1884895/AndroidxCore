package com.yunxiao.kits

import android.app.Activity
import android.content.Context
import android.util.Log
import com.didichuxing.doraemonkit.kit.AbstractKit

open class LocalKit(var clickWithReturn: (Activity) -> Unit) : AbstractKit() {
    companion object {
        const val TAG = "LocalKit::class.java.simpleName"
    }

    override val icon: Int
        get() = R.mipmap.ic_launcher_round
    override val name: Int
        get() = R.string.build_type

    override fun onAppInit(context: Context?) {
    }

    override fun onClickWithReturn(activity: Activity): Boolean {
        Log.d(TAG, "onClickWithReturn: ")
        this.clickWithReturn(activity)
        return true
    }
}