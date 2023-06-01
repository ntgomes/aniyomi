package eu.kanade.presentation.library.manga

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.components.CheckboxItem
import eu.kanade.presentation.components.HeadingItem
import eu.kanade.presentation.components.RadioItem
import eu.kanade.presentation.components.SortItem
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.components.TriStateItem
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.manga.MangaLibrarySettingsScreenModel
import eu.kanade.tachiyomi.widget.toTriStateFilter
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.TriStateFilter
import tachiyomi.domain.library.manga.model.MangaLibrarySort
import tachiyomi.domain.library.manga.model.sort
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.display

@Composable
fun MangaLibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    screenModel: MangaLibrarySettingsScreenModel,
    activeCategoryIndex: Int,
) {
    val state by screenModel.state.collectAsState()
    val category by remember(activeCategoryIndex) {
        derivedStateOf { state.categories[activeCategoryIndex] }
    }

    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(R.string.action_filter),
            stringResource(R.string.action_sort),
            stringResource(R.string.action_display),
        ),
    ) { contentPadding, page ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> FilterPage(
                    screenModel = screenModel,
                )
                1 -> SortPage(
                    category = category,
                    screenModel = screenModel,
                )
                2 -> DisplayPage(
                    category = category,
                    screenModel = screenModel,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    screenModel: MangaLibrarySettingsScreenModel,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloadedManga().collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_downloaded),
        state = if (downloadedOnly) {
            TriStateFilter.ENABLED_IS
        } else {
            filterDownloaded.toTriStateFilter()
        },
        enabled = !downloadedOnly,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterDownloadedManga) },
    )
    val filterUnread by screenModel.libraryPreferences.filterUnread().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_unread),
        state = filterUnread.toTriStateFilter(),
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterUnread) },
    )
    val filterStarted by screenModel.libraryPreferences.filterStartedManga().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_started),
        state = filterStarted.toTriStateFilter(),
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterStartedManga) },
    )
    val filterBookmarked by screenModel.libraryPreferences.filterBookmarkedManga().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_bookmarked),
        state = filterBookmarked.toTriStateFilter(),
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterBookmarkedManga) },
    )
    val filterCompleted by screenModel.libraryPreferences.filterCompletedManga().collectAsState()
    TriStateItem(
        label = stringResource(R.string.completed),
        state = filterCompleted.toTriStateFilter(),
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterCompletedManga) },
    )

    when (screenModel.trackServices.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = screenModel.trackServices[0]
            val filterTracker by screenModel.libraryPreferences.filterTrackedManga(service.id.toInt()).collectAsState()
            TriStateItem(
                label = stringResource(R.string.action_filter_tracked),
                state = filterTracker.toTriStateFilter(),
                onClick = { screenModel.toggleTracker(service.id.toInt()) },
            )
        }
        else -> {
            HeadingItem(R.string.action_filter_tracked)
            screenModel.trackServices.map { service ->
                val filterTracker by screenModel.libraryPreferences.filterTrackedManga(service.id.toInt()).collectAsState()
                TriStateItem(
                    label = stringResource(service.nameRes()),
                    state = filterTracker.toTriStateFilter(),
                    onClick = { screenModel.toggleTracker(service.id.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SortPage(
    category: Category,
    screenModel: MangaLibrarySettingsScreenModel,
) {
    val sortingMode = category.sort.type
    val sortDescending = !category.sort.isAscending

    listOf(
        R.string.action_sort_alpha to MangaLibrarySort.Type.Alphabetical,
        R.string.action_sort_total to MangaLibrarySort.Type.TotalChapters,
        R.string.action_sort_last_read to MangaLibrarySort.Type.LastRead,
        R.string.action_sort_last_manga_update to MangaLibrarySort.Type.LastUpdate,
        R.string.action_sort_unread_count to MangaLibrarySort.Type.UnreadCount,
        R.string.action_sort_latest_chapter to MangaLibrarySort.Type.LatestChapter,
        R.string.action_sort_chapter_fetch_date to MangaLibrarySort.Type.ChapterFetchDate,
        R.string.action_sort_date_added to MangaLibrarySort.Type.DateAdded,
    ).map { (titleRes, mode) ->
        SortItem(
            label = stringResource(titleRes),
            sortDescending = sortDescending.takeIf { sortingMode == mode },
            onClick = {
                val isTogglingDirection = sortingMode == mode
                val direction = when {
                    isTogglingDirection -> if (sortDescending) MangaLibrarySort.Direction.Ascending else MangaLibrarySort.Direction.Descending
                    else -> if (sortDescending) MangaLibrarySort.Direction.Descending else MangaLibrarySort.Direction.Ascending
                }
                screenModel.setSort(category, mode, direction)
            },
        )
    }
}

@Composable
private fun ColumnScope.DisplayPage(
    category: Category,
    screenModel: MangaLibrarySettingsScreenModel,
) {
    HeadingItem(R.string.action_display_mode)
    listOf(
        R.string.action_display_grid to LibraryDisplayMode.CompactGrid,
        R.string.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
        R.string.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
        R.string.action_display_list to LibraryDisplayMode.List,
    ).map { (titleRes, mode) ->
        RadioItem(
            label = stringResource(titleRes),
            selected = category.display == mode,
            onClick = { screenModel.setDisplayMode(category, mode) },
        )
    }

    HeadingItem(R.string.badges_header)
    val downloadBadge by screenModel.libraryPreferences.downloadBadge().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_download_badge),
        checked = downloadBadge,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::downloadBadge)
        },
    )
    val localBadge by screenModel.libraryPreferences.localBadge().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_local_badge),
        checked = localBadge,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::localBadge)
        },
    )
    val languageBadge by screenModel.libraryPreferences.languageBadge().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_language_badge),
        checked = languageBadge,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::languageBadge)
        },
    )

    HeadingItem(R.string.tabs_header)
    val categoryTabs by screenModel.libraryPreferences.categoryTabs().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_show_tabs),
        checked = categoryTabs,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::categoryTabs)
        },
    )
    val categoryNumberOfItems by screenModel.libraryPreferences.categoryNumberOfItems().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_show_number_of_items),
        checked = categoryNumberOfItems,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::categoryNumberOfItems)
        },
    )

    HeadingItem(R.string.other_header)
    val showContinueReadingButton by screenModel.libraryPreferences.showContinueViewingButton().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_show_continue_reading_button),
        checked = showContinueReadingButton,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::showContinueViewingButton)
        },
    )
}
