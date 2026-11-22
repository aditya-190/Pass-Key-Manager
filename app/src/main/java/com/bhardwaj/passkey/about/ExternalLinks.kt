package com.bhardwaj.passkey.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Intents used by About, Legal and the banner. Each returns false when no app can handle it. */
object ExternalLinks {
    fun openUrl(context: Context, url: String): Boolean = start(context, Intent(Intent.ACTION_VIEW, url.toUri()))

    fun share(context: Context, text: String, chooserTitle: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        return start(context, Intent.createChooser(send, chooserTitle))
    }

    fun email(context: Context, address: String, subject: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
