package me.nekosu.aqnya

import android.content.Context

class ncore {
    companion object {
        init {
            System.loadLibrary("ncore")
        }
    }

external fun helloLog();

external fun ctl(value: Int): Int

}