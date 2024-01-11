package eu.kanade.tachiyomi.ui.browse.anime.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.AnimeExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.anime.extension.details.AnimeExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun animeExtensionsTab(
    extensionsScreenModel: AnimeExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by extensionsScreenModel.state.collectAsState()

    return TabContent(
        titleRes = MR.strings.label_anime_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = Icons.Outlined.Translate,
                onClick = {
                    navigator.push(
                        eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionFilterScreen(),
                    )
                },
            ),
        ),
        content = { contentPadding, _ ->
            AnimeExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension ->
                    when (extension) {
                        is AnimeExtension.Available -> extensionsScreenModel.installExtension(
                            extension,
                        )
                        else -> extensionsScreenModel.uninstallExtension(extension)
                    }
                },
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onOpenWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { navigator.push(AnimeExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustExtension(it) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
            )
        },
    )
}
