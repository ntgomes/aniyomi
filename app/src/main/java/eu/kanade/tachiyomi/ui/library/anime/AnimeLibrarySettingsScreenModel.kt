package eu.kanade.tachiyomi.ui.library.anime

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.TriState
import tachiyomi.core.preference.getAndSet
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.category.anime.interactor.SetAnimeDisplayMode
import tachiyomi.domain.category.anime.interactor.SetSortModeForAnimeCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeLibrarySettingsScreenModel(
    val preferences: BasePreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val setAnimeDisplayMode: SetAnimeDisplayMode = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForAnimeCategory = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
) : ScreenModel {

    val trackers
        get() = trackerManager.trackers.filter { it.isLoggedIn }

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<TriState>) {
        preference(libraryPreferences).getAndSet {
            it.next()
        }
    }

    fun toggleTracker(id: Int) {
        toggleFilter { libraryPreferences.filterTrackedAnime(id) }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setAnimeDisplayMode.await(mode)
    }

    fun setSort(
        category: Category?,
        mode: AnimeLibrarySort.Type,
        direction: AnimeLibrarySort.Direction,
    ) {
        screenModelScope.launchIO {
            setSortModeForCategory.await(category, mode, direction)
        }
    }
}
