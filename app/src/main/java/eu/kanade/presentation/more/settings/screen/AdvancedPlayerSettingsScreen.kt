package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.core.preference.asState
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object AdvancedPlayerSettingsScreen : SearchableSettings {
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_player_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val mpvConf = playerPreferences.mpvConf()
        val mpvInput = playerPreferences.mpvInput()

        return listOf(
            Preference.PreferenceItem.MultiLineEditTextPreference(
                pref = mpvConf,
                title = context.stringResource(MR.strings.pref_mpv_conf),
                subtitle = mpvConf.asState(scope).value
                    .lines().take(2)
                    .joinToString(
                        separator = "\n",
                        postfix = if (mpvConf.asState(scope).value.lines().size > 2) "\n..." else "",
                    ),

            ),
            Preference.PreferenceItem.MultiLineEditTextPreference(
                pref = mpvInput,
                title = context.stringResource(MR.strings.pref_mpv_input),
                subtitle = mpvInput.asState(scope).value
                    .lines().take(2)
                    .joinToString(
                        separator = "\n",
                        postfix = if (mpvInput.asState(scope).value.lines().size > 2) "\n..." else "",
                    ),
            ),
            Preference.PreferenceItem.ListPreference(
                title = context.stringResource(MR.strings.pref_debanding_title),
                pref = playerPreferences.deband(),
                entries = persistentMapOf(
                    0 to context.stringResource(MR.strings.pref_debanding_disabled),
                    1 to context.stringResource(MR.strings.pref_debanding_cpu),
                    2 to context.stringResource(MR.strings.pref_debanding_gpu),
                    3 to "YUV420P",
                ),
            ),
        )
    }
}
