package eu.kanade.tachiyomi.ui.deeplink.manga

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import eu.kanade.tachiyomi.ui.deeplink.DeepLinkScreenType
import eu.kanade.tachiyomi.ui.main.MainActivity

class DeepLinkMangaActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.apply {
            flags = flags or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(MainActivity.INTENT_SEARCH_TYPE, DeepLinkScreenType.MANGA.toString())
            setClass(applicationContext, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
