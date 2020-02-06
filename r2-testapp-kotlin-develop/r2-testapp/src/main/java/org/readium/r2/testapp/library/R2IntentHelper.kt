package org.readium.r2.testapp.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.readium.r2.shared.Publication.EXTENSION
import org.readium.r2.testapp.CatalogActivity


class R2IntentHelper {
    fun catalogActivityIntent(context: Context?, uri: Uri, extension: EXTENSION): Intent {
        val i = Intent(context, CatalogActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i.putExtra(EXTENSION, extension.value)
        i.putExtra(URI, uri.toString())
        return i
    }

    companion object {
        const val URI = "URI"
        const val EXTENSION = "EXTENSION"
    }
}
