package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.AnimeExtensionReposScreen
import eu.kanade.presentation.more.settings.screen.browse.MangaExtensionReposScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val mangaReposCount by sourcePreferences.mangaExtensionRepos().collectAsState()
        val animeReposCount by sourcePreferences.animeExtensionRepos().collectAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.hideInAnimeLibraryItems(),
                        title = stringResource(MR.strings.pref_hide_in_anime_library_items),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.hideInMangaLibraryItems(),
                        title = stringResource(MR.strings.pref_hide_in_manga_library_items),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_anime_extension_repos),
                        subtitle = pluralStringResource(
                            MR.plurals.num_repos,
                            animeReposCount.size,
                            animeReposCount.size,
                        ),
                        onClick = {
                            navigator.push(AnimeExtensionReposScreen())
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_manga_extension_repos),
                        subtitle = pluralStringResource(
                            MR.plurals.num_repos,
                            mangaReposCount.size,
                            mangaReposCount.size,
                        ),
                        onClick = {
                            navigator.push(MangaExtensionReposScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_nsfw_content),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.showNsfwSource(),
                        title = stringResource(MR.strings.pref_show_nsfw_source),
                        subtitle = stringResource(MR.strings.requires_app_restart),
                        onValueChanged = {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(MR.strings.pref_category_nsfw_content),
                            )
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(MR.strings.parental_controls_info),
                    ),
                ),
            ),
        )
    }
}
