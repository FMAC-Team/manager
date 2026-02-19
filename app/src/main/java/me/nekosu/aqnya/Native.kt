package me.nekosu.aqnya

import android.content.Context

class Ncore {
    companion object {
        init {
            System.loadLibrary("ncore")
        }
    }

external fun helloLog();

external fun authenticate(key: String?, token: String?): Int

}