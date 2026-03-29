package me.nekosu.aqnya

import android.content.Context

class ncore {
    companion object {
        init {
            System.loadLibrary("ncore")
        }
    }

    external fun helloLog()

    external fun ctl(value: Int): Int

    external fun adduid(value: Int): Int

    external fun deluid(value: Int): Int

    external fun hasuid(value: Int): Int

    external fun addRule(
        path: String,
        statusBits: Long,
    ): Int

    external fun delRule(path: String): Int
}
