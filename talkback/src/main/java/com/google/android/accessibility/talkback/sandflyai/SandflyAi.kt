package com.google.android.accessibility.talkback.sandflyai

import android.content.Context
import android.net.Uri
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * The paid features in the private SandFly AI module (aaron-gh/sandfly-ai). Published builds
 * include the module; builds without it get null from [SandflyAi.features] and show no AI
 * features.
 */
interface SandflyAiFeatures {
    fun onServiceConnected(service: TalkBackService)

    fun onServiceDestroyed(service: TalkBackService)

    /** The name of the agent command in command lists. */
    fun agentCommandLabel(context: Context): String

    /** Runs the agent command: starts a task, or stops the running one. */
    fun startAgent(service: TalkBackService)

    /** The title of SandFly AI's settings screen in the main settings. */
    fun settingsTitle(context: Context): String

    /** The class name of the fragment of SandFly AI's settings screen. */
    val settingsFragment: String

    /** Handles a sandfly://ai/... link. Returns false when the link isn't one it knows. */
    fun handleLink(context: Context, uri: Uri): Boolean

    /** SandFly Pro's other commands for command lists: value to label. */
    fun commands(context: Context): Map<String, String>

    /** Runs the command [value] from [commands]. Returns false for any other value. */
    fun runCommand(service: TalkBackService, value: String): Boolean

    /**
     * The SandFly menu item for the command [value] from [commands]: its title and what it does,
     * run after the menu closes. Null leaves it out of the menu.
     */
    fun menuItem(service: TalkBackService, value: String): Pair<CharSequence, Runnable>?
}

object SandflyAi {
    private const val TAG = "SandflyAi"

    /** SandFly Pro's commands that have SandFly menu items. */
    const val COMMAND_RECOGNIZE_SCREEN = "SFPRO_RECOGNIZE_SCREEN"
    const val COMMAND_SCREEN_RECOGNITION_IN_APP = "SFPRO_SCREEN_RECOGNITION_IN_APP"
    private const val ENTRY = "io.github.aaron_gh.sandfly.ai.SandflyAiEntry"

    @JvmStatic
    val features: SandflyAiFeatures? by lazy {
        try {
            Class.forName(ENTRY).getDeclaredConstructor().newInstance() as SandflyAiFeatures
        } catch (e: ClassNotFoundException) {
            null
        } catch (e: ReflectiveOperationException) {
            LogUtils.e(TAG, "SandFly AI module present but not loadable: %s", e)
            null
        } catch (e: ClassCastException) {
            LogUtils.e(TAG, "SandFly AI module present but not loadable: %s", e)
            null
        }
    }

    @JvmStatic
    val isAvailable: Boolean
        get() = features != null
}
