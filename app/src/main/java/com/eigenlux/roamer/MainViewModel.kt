package com.eigenlux.roamer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eigenlux.roamer.core.CarrierConfigController
import com.eigenlux.roamer.core.LocaleOverrideController
import com.eigenlux.roamer.core.RegionLogic
import com.eigenlux.roamer.core.ShizukuManager
import com.eigenlux.roamer.core.SimInfo
import com.eigenlux.roamer.data.AppLocaleStore
import com.eigenlux.roamer.data.CarrierPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

internal enum class LogLevel { INFO, SUCCESS, ERROR, WARNING }
internal data class LogState(val level: LogLevel, val message: String)

/**
 * Holds the main screen state and runs SIM operations, so both survive configuration changes.
 */
internal class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()

    var sims by mutableStateOf<List<SimInfo>>(emptyList())
        private set
    var log by mutableStateOf(LogState(LogLevel.INFO, app.getString(R.string.log_ready)))
        private set
    var busy by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var shizukuAlive by mutableStateOf(ShizukuManager.isBinderAlive())
        private set
    var shizukuGranted by mutableStateOf(ShizukuManager.hasPermission())
        private set
    var regionMasterOn by mutableStateOf(AppLocaleStore.isMasterOn(app))
        private set
    var showAppPicker by mutableStateOf(false)

    private val opMutex = Mutex()

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> onShizukuChanged() }
    private val aliveListener = Shizuku.OnBinderReceivedListener { onShizukuChanged() }
    private val deadListener = Shizuku.OnBinderDeadListener { onShizukuChanged() }

    init {
        Shizuku.addRequestPermissionResultListener(permListener)
        Shizuku.addBinderReceivedListenerSticky(aliveListener)
        Shizuku.addBinderDeadListener(deadListener)
    }

    override fun onCleared() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        Shizuku.removeBinderReceivedListener(aliveListener)
        Shizuku.removeBinderDeadListener(deadListener)
    }

    /**
     * Re-reads Shizuku status. Returns true when permission was just gained.
     */
    private fun refreshShizuku(): Boolean {
        shizukuAlive = ShizukuManager.isBinderAlive()
        val granted = ShizukuManager.hasPermission()
        val gained = granted && !shizukuGranted
        shizukuGranted = granted
        return gained
    }

    private fun onShizukuChanged() {
        if (refreshShizuku()) reloadSims()
    }

    fun requestShizuku() {
        ShizukuManager.requestPermission()
        onShizukuChanged()
    }

    fun refreshAll() {
        refreshShizuku()
        reloadSims()
    }

    private fun reloadSims() {
        viewModelScope.launch {
            if (busy || opMutex.isLocked) return@launch
            refreshing = true
            loadAndSync()
            refreshing = false
        }
    }

    private suspend fun loadAndSync() {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { CarrierConfigController.loadSims(ctx) }.getOrNull()
        } ?: return
        sims = loaded
        withContext(Dispatchers.IO) {
            runCatching { LocaleOverrideController.sync(ctx, RegionLogic.primaryOf(loaded)) }
        }
    }

    private fun runOp(block: () -> CarrierConfigController.Result) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val r = opMutex.withLock { withContext(Dispatchers.IO) { block() } }
                val level = when {
                    r.ok -> LogLevel.SUCCESS
                    r.partial -> LogLevel.WARNING
                    else -> LogLevel.ERROR
                }
                log = LogState(level, r.output)
                loadAndSync()
            } finally {
                busy = false
            }
        }
    }

    private fun info(message: String) {
        log = LogState(LogLevel.INFO, message)
    }

    fun override(subId: Int, iso: String, name: String) =
        runOp { CarrierConfigController.setOverride(ctx, subId, iso, name) }

    fun restore(subId: Int) = runOp { CarrierConfigController.restore(ctx, subId) }

    fun restoreAll() {
        val subIds = sims.filter { it.overridden }.map { it.subId }
        if (subIds.isEmpty()) return info(ctx.getString(R.string.nothing_to_restore))
        runOp { CarrierConfigController.restoreAll(ctx, subIds) }
    }

    fun forceClearAll() {
        val subIds = sims.map { it.subId }
        if (subIds.isEmpty()) return info(ctx.getString(R.string.no_active_sim))
        runOp { CarrierConfigController.clearAll(ctx, subIds) }
    }

    /**
     * Switches every SIM to [iso]: overrides the foreign ones and restores a home SIM that is overridden.
     */
    fun maskAll(iso: String) {
        if (sims.isEmpty()) return info(ctx.getString(R.string.no_sim_to_mask))
        val plan = RegionLogic.maskPlan(sims, iso)
        if (plan.restore.isEmpty() && plan.override.isEmpty()) {
            return info(ctx.getString(R.string.already_masked, iso.uppercase()))
        }
        val name = CarrierPresets.forCountry(iso).firstOrNull().orEmpty()
        // Sequential on purpose: overlapping shell delegations drop each other's permissions.
        runOp {
            listOfNotNull(
                plan.restore.takeIf { it.isNotEmpty() }?.let { CarrierConfigController.restoreAll(ctx, it) },
                plan.override.takeIf { it.isNotEmpty() }?.let { CarrierConfigController.setOverrideAll(ctx, it, iso, name) },
            ).reduce { a, b -> a + b }
        }
    }

    fun setRegionMaster(on: Boolean) {
        regionMasterOn = on
        val primary = RegionLogic.primaryOf(sims)
        viewModelScope.launch(Dispatchers.IO) { LocaleOverrideController.setMaster(ctx, on, primary) }
    }
}
