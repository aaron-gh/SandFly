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
}

object SandflyAi {
    private const val TAG = "SandflyAi"
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
