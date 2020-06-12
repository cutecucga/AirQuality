package com.lanhnh.airquality.extentions

import android.os.SystemClock
import android.view.View

fun View.safeClick(blockInMillis: Long = 1000L, onClick: (View) -> Unit) {
    var lastClickTime: Long = 0
    this.setOnClickListener {
        if (SystemClock.elapsedRealtime() - lastClickTime < blockInMillis) return@setOnClickListener
        lastClickTime = SystemClock.elapsedRealtime()
        onClick(this)
    }
}
