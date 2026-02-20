package me.nekosu.aqnya

import android.content.Context

class ncore {
    companion object {
        init {
            System.loadLibrary("ncore")
        }
    }

external fun helloLog();

external fun generateTotp( key: String):Int

external fun authenticate(key: String?, token: String?): Int

}