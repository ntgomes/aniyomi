package eu.kanade.tachiyomi.data.backup.restore

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupDecoder
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtension
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.restore.restorers.AnimeRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.CategoriesRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.ExtensionsRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.MangaRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.PreferenceRestorer
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
    private val isSync: Boolean,

    private val categoriesRestorer: CategoriesRestorer = CategoriesRestorer(),
    private val preferenceRestorer: PreferenceRestorer = PreferenceRestorer(context),
    private val animeRestorer: AnimeRestorer = AnimeRestorer(),
    private val mangaRestorer: MangaRestorer = MangaRestorer(),
    private val extensionsRestorer: ExtensionsRestorer = ExtensionsRestorer(context),
) {

    private var restoreAmount = 0
    private var restoreProgress = 0
    private val errors = mutableListOf<Pair<Date, String>>()

    /**
     * Mapping of source ID to source name from backup data
     */
    private var animeSourceMapping: Map<Long, String> = emptyMap()
    private var mangaSourceMapping: Map<Long, String> = emptyMap()

    suspend fun restore(uri: Uri, options: RestoreOptions) {
        val startTime = System.currentTimeMillis()

        restoreFromFile(uri, options)

        val time = System.currentTimeMillis() - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(
            time,
            errors.size,
            logFile.parent,
            logFile.name,
            isSync,
        )
    }

    private suspend fun restoreFromFile(uri: Uri, options: RestoreOptions) {
        val backup = BackupDecoder(context).decode(uri)

        // Store source mapping for error messages
        val backupAnimeMaps = backup.backupAnimeSources + backup.backupBrokenAnimeSources.map { it.toBackupSource() }
        mangaSourceMapping = backupAnimeMaps.associate { it.sourceId to it.name }
        val backupMangaMaps = backup.backupSources + backup.backupBrokenSources.map { it.toBackupSource() }
        mangaSourceMapping = backupMangaMaps.associate { it.sourceId to it.name }

        if (options.library) {
            restoreAmount += backup.backupManga.size + backup.backupAnime.size + 2 // +2 for anime and manga categories
        }
        if (options.appSettings) {
            restoreAmount += 1
        }
        if (options.sourceSettings) {
            restoreAmount += 1
        }
        if (options.extensions) {
            restoreAmount += 1
        }

        coroutineScope {
            if (options.library) {
                restoreCategories(
                    backupAnimeCategories = backup.backupAnimeCategories,
                    backupMangaCategories = backup.backupCategories,
                )
            }
            if (options.appSettings) {
                restoreAppPreferences(backup.backupPreferences)
            }
            if (options.sourceSettings) {
                restoreSourcePreferences(backup.backupSourcePreferences)
            }
            if (options.library) {
                restoreAnime(backup.backupAnime, backup.backupAnimeCategories)
                restoreManga(backup.backupManga, backup.backupCategories)
            }
            if (options.extensions) {
                restoreExtensions(backup.backupExtensions)
            }

            // TODO: optionally trigger online library + tracker update
        }
    }

    private fun CoroutineScope.restoreCategories(
        backupAnimeCategories: List<BackupCategory>,
        backupMangaCategories: List<BackupCategory>,
    ) = launch {
        ensureActive()
        categoriesRestorer.restoreAnimeCategories(backupAnimeCategories)
        categoriesRestorer.restoreMangaCategories(backupMangaCategories)

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.categories),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreAnime(
        backupAnimes: List<BackupAnime>,
        backupAnimeCategories: List<BackupCategory>,
    ) = launch {
        animeRestorer.sortByNew(backupAnimes)
            .forEach {
                ensureActive()

                try {
                    animeRestorer.restoreAnime(it, backupAnimeCategories)
                } catch (e: Exception) {
                    val sourceName = animeSourceMapping[it.source] ?: it.source.toString()
                    errors.add(Date() to "${it.title} [$sourceName]: ${e.message}")
                }

                restoreProgress += 1
                notifier.showRestoreProgress(it.title, restoreProgress, restoreAmount, isSync)
            }
    }

    private fun CoroutineScope.restoreManga(
        backupMangas: List<BackupManga>,
        backupMangaCategories: List<BackupCategory>,
    ) = launch {
        mangaRestorer.sortByNew(backupMangas)
            .forEach {
                ensureActive()

                try {
                    mangaRestorer.restoreManga(it, backupMangaCategories)
                } catch (e: Exception) {
                    val sourceName = mangaSourceMapping[it.source] ?: it.source.toString()
                    errors.add(Date() to "${it.title} [$sourceName]: ${e.message}")
                }

                restoreProgress += 1
                notifier.showRestoreProgress(it.title, restoreProgress, restoreAmount, isSync)
            }
    }

    private fun CoroutineScope.restoreAppPreferences(preferences: List<BackupPreference>) = launch {
        ensureActive()
        preferenceRestorer.restoreAppPreferences(preferences)

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.app_settings),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreSourcePreferences(preferences: List<BackupSourcePreferences>) = launch {
        ensureActive()
        preferenceRestorer.restoreSourcePreferences(preferences)

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.source_settings),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreExtensions(extensions: List<BackupExtension>) = launch {
        ensureActive()
        extensionsRestorer.restoreExtensions(extensions)

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.source_settings),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }

    private fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("tachiyomi_restore.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }
}
