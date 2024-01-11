package eu.kanade.tachiyomi.ui.browse.anime.source

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.anime.interactor.GetEnabledAnimeSources
import eu.kanade.domain.source.anime.interactor.ToggleAnimeSource
import eu.kanade.domain.source.anime.interactor.ToggleAnimeSourcePin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.anime.AnimeSourceUiModel
import eu.kanade.tachiyomi.util.system.LAST_USED_KEY
import eu.kanade.tachiyomi.util.system.PINNED_KEY
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.Pin
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

class AnimeSourcesScreenModel(
    private val preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val getEnabledAnimeSources: GetEnabledAnimeSources = Injekt.get(),
    private val toggleSource: ToggleAnimeSource = Injekt.get(),
    private val toggleSourcePin: ToggleAnimeSourcePin = Injekt.get(),
) : StateScreenModel<AnimeSourcesScreenModel.State>(State()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            getEnabledAnimeSources.subscribe()
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(Event.FailedFetchingSources)
                }
                .collectLatest(::collectLatestAnimeSources)
        }
    }

    private fun collectLatestAnimeSources(sources: List<AnimeSource>) {
        mutableState.update { state ->
            val map = TreeMap<String, MutableList<AnimeSource>> { d1, d2 ->
                // Sources without a lang defined will be placed at the end
                when {
                    d1 == LAST_USED_KEY && d2 != LAST_USED_KEY -> -1
                    d2 == LAST_USED_KEY && d1 != LAST_USED_KEY -> 1
                    d1 == PINNED_KEY && d2 != PINNED_KEY -> -1
                    d2 == PINNED_KEY && d1 != PINNED_KEY -> 1
                    d1 == "" && d2 != "" -> 1
                    d2 == "" && d1 != "" -> -1
                    else -> d1.compareTo(d2)
                }
            }
            val byLang = sources.groupByTo(map) {
                when {
                    it.isUsedLast -> LAST_USED_KEY
                    Pin.Actual in it.pin -> PINNED_KEY
                    else -> it.lang
                }
            }

            state.copy(
                isLoading = false,
                items = byLang
                    .flatMap {
                        listOf(
                            AnimeSourceUiModel.Header(it.key),
                            *it.value.map { source ->
                                AnimeSourceUiModel.Item(source)
                            }.toTypedArray(),
                        )
                    }
                    .toImmutableList(),
            )
        }
    }

    fun toggleSource(source: AnimeSource) {
        toggleSource.await(source)
    }

    fun togglePin(source: AnimeSource) {
        toggleSourcePin.await(source)
    }

    fun showSourceDialog(source: AnimeSource) {
        mutableState.update { it.copy(dialog = Dialog(source)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed interface Event {
        data object FailedFetchingSources : Event
    }

    data class Dialog(val source: AnimeSource)

    @Immutable
    data class State(
        val dialog: Dialog? = null,
        val isLoading: Boolean = true,
        val items: ImmutableList<AnimeSourceUiModel> = persistentListOf(),
    ) {
        val isEmpty = items.isEmpty()
    }
}
