package com.eigenlux.roamer.core

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import java.lang.reflect.InvocationTargetException
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Outcome of one [PrivilegedOverrideInstrumentation] run.
 *
 * @property ok False when the run failed as a whole.
 * @property error Failure message of the run, or null when [ok].
 * @property failures Per-subscription failure messages keyed by subId.
 */
data class InstrumentationOutcome(
    val ok: Boolean,
    val error: String?,
    val failures: Map<Int, String>,
)

/**
 * Triggers [PrivilegedOverrideInstrumentation] using `IActivityManager.startInstrumentation`
 * via Shizuku with `INSTR_FLAG_NO_RESTART` to attach to the active process without restarting the UI.
 */
object InstrumentationTrigger {

    const val EXTRA_TOKEN = "token"

    private const val RESULT_TIMEOUT_MS = 15_000L

    // The instrumentation attaches to this process, so its outcome is handed back in memory.
    private val pending = ConcurrentHashMap<String, CompletableFuture<InstrumentationOutcome>>()

    init {
        SystemServices.ensureHiddenApiAccess()
    }

    /**
     * Launches instrumentation with the specified extras bundle and waits for its outcome.
     * Returns null if no outcome arrives in time; callers then fall back to polling telephony state.
     */
    fun trigger(context: Context, extras: Map<String, String>): InstrumentationOutcome? {
        val token = UUID.randomUUID().toString()
        val future = CompletableFuture<InstrumentationOutcome>()
        pending[token] = future
        try {
            start(context, extras + (EXTRA_TOKEN to token))
            return try {
                future.get(RESULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                null
            }
        } finally {
            pending.remove(token)
        }
    }

    /**
     * Delivers the outcome of the run identified by [token] to the waiting [trigger] call.
     */
    fun report(token: String?, outcome: InstrumentationOutcome) {
        token?.let { pending[it] }?.complete(outcome)
    }

    private fun start(context: Context, extras: Map<String, String>) {
        val args = Bundle().apply { extras.forEach { (k, v) -> putString(k, v) } }
        val am = SystemServices.shizukuInterface(Context.ACTIVITY_SERVICE, "android.app.IActivityManager")

        val component = ComponentName(context, PrivilegedOverrideInstrumentation::class.java)
        val flags = instrFlag("INSTR_FLAG_DISABLE_HIDDEN_API_CHECKS", 1 shl 1) or
            instrFlag("INSTR_FLAG_NO_RESTART", 1 shl 3)

        val watcherClass = Class.forName("android.app.IInstrumentationWatcher")
        val connectionClass = Class.forName("android.app.IUiAutomationConnection")
        val uiAutomationConnection = Class.forName("android.app.UiAutomationConnection")
            .getConstructor().newInstance()

        val method = am.javaClass.getMethod(
            "startInstrumentation",
            ComponentName::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            Bundle::class.java,
            watcherClass,
            connectionClass,
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        val started = try {
            method.invoke(am, component, null, flags, args, null, uiAutomationConnection, 0, null)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
        check(started != false) { "startInstrumentation returned false" }
    }

    private fun instrFlag(name: String, fallback: Int): Int = runCatching {
        ActivityManager::class.java.getField(name).getInt(null)
    }.getOrDefault(fallback)
}
