package com.kiqa.omniservers.data

import android.content.Context

/** Remembers which desktop (or the demo) the app is paired with. Stays in app-private storage. */
class PairingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pairing", Context.MODE_PRIVATE)

    var demo: Boolean
        get() = prefs.getBoolean("demo", false)
        set(v) { prefs.edit().putBoolean("demo", v).apply() }

    var pairing: Pairing?
        get() {
            val host = prefs.getString("host", null) ?: return null
            val token = prefs.getString("token", null) ?: return null
            return Pairing(host, prefs.getInt("port", 8642), token)
        }
        set(v) {
            prefs.edit().apply {
                if (v == null) { remove("host"); remove("port"); remove("token") }
                else { putString("host", v.host); putInt("port", v.port); putString("token", v.token) }
            }.apply()
        }

    fun clear() { prefs.edit().clear().apply() }
}
