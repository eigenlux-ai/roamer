package com.eigenlux.roamer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.eigenlux.roamer.data.Carrier
import com.eigenlux.roamer.data.ConfigCodec
import com.eigenlux.roamer.data.CountryPresets
import com.eigenlux.roamer.data.CustomRegion
import com.eigenlux.roamer.data.RegionConfig
import com.eigenlux.roamer.data.RegionRules
import com.eigenlux.roamer.data.RegionStore
import com.eigenlux.roamer.data.RoamerConfig
import com.eigenlux.roamer.ui.CustomRegionNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/** Pages that open over the main screen. */
internal sealed interface Screen {
    data object Settings : Screen
    data object AppPicker : Screen
    data object Regions : Screen
    data object AddRegion : Screen
    data object NewRegion : Screen
    data class RegionDetail(val iso: String) : Screen
    data object QuickSwitch : Screen
    data object About : Screen
}

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
    /** Incremented for every new [log] entry, so the UI can announce each one exactly once. */
    var logEvent by mutableIntStateOf(0)
        private set
    var announcedLogEvent = 0
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
    /** Pages open over the main screen, bottom to top. */
    val stack = mutableStateListOf<Screen>()

    /** Countries and regions offered on the main screen, in menu order. */
    var regions by mutableStateOf(RegionStore.loadRegions(app))
        private set

    /** Regions the user defined for codes the app does not know. */
    var customRegions by mutableStateOf(RegionStore.loadCustomRegions(app))
        private set

    /** Working copy of [regions] while the list is being edited; null outside edit mode. */
    var regionsDraft by mutableStateOf<List<RegionConfig>?>(null)
        private set

    /** Working copy of [customRegions] while the list is being edited. */
    var customsDraft by mutableStateOf<List<CustomRegion>?>(null)
        private set

    /** Every region code the app ships with: the ISO 3166-1 list plus the presets. */
    val builtInRegionCodes: Set<String> = buildSet {
        java.util.Locale.getISOCountries().mapTo(this) { it.lowercase(java.util.Locale.ROOT) }
        CountryPresets.all.mapTo(this) { it.iso }
    }

    init {
        CustomRegionNames.names = customRegions.associate { it.iso to it.name }
    }

    /** ISO codes shown in the quick-switch row, in order. */
    var quickSwitch by mutableStateOf(RegionStore.loadQuickSwitch(app, regions))
        private set

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
                post(LogState(level, r.output))
                loadAndSync()
            } finally {
                busy = false
            }
        }
    }

    private fun info(message: String) = post(LogState(LogLevel.INFO, message))

    private fun post(state: LogState) {
        log = state
        logEvent++
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
        val name = carriersOf(iso).firstOrNull().orEmpty()
        // Sequential on purpose: overlapping shell delegations drop each other's permissions.
        runOp {
            listOfNotNull(
                plan.restore.takeIf { it.isNotEmpty() }?.let { CarrierConfigController.restoreAll(ctx, it) },
                plan.override.takeIf { it.isNotEmpty() }?.let { CarrierConfigController.setOverrideAll(ctx, it, iso, name) },
            ).reduce { a, b -> a + b }
        }
    }

    fun push(screen: Screen) {
        if (stack.lastOrNull() != screen) stack.add(screen)
    }

    fun pop() {
        // Leaving the region list drops an unsaved draft.
        if (stack.removeLastOrNull() == Screen.Regions) endRegionsEdit()
    }

    /** Regions offered on the main screen: the enabled ones, in order. */
    val enabledRegions: List<RegionConfig> get() = regions.filter { it.enabled }

    /** Enabled carrier names of [iso], in order. */
    fun carriersOf(iso: String): List<String> = regions.firstOrNull { it.iso == iso }?.enabledCarriers.orEmpty()

    /** Every carrier of [iso], including presets that are turned off. */
    fun allCarriersOf(iso: String): List<Carrier> = regions.firstOrNull { it.iso == iso }?.carriers.orEmpty()

    private fun updateRegions(next: List<RegionConfig>) {
        regions = next
        RegionStore.saveRegions(ctx, next)
        // A removed region cannot stay in the quick-switch row.
        val cleaned = RegionRules.cleanQuickSwitch(quickSwitch, next)
        if (cleaned != quickSwitch) updateQuickSwitch(cleaned)
    }

    // ---- Editing the region list. Changes stay in the drafts until they are saved. ----

    val editingRegions: Boolean get() = regionsDraft != null

    val regionsDirty: Boolean
        get() = regionsDraft != null && (regionsDraft != regions || customsDraft != customRegions)

    /** Regions in the list as currently shown: the draft while editing, the saved list otherwise. */
    val listedRegions: List<RegionConfig> get() = (regionsDraft ?: regions).filter { it.enabled }

    fun isCustomRegion(iso: String): Boolean = (customsDraft ?: customRegions).any { it.iso == iso }

    private fun setDrafts(regionList: List<RegionConfig>?, customs: List<CustomRegion>?) {
        regionsDraft = regionList
        customsDraft = customs
        CustomRegionNames.names = (customs ?: customRegions).associate { it.iso to it.name }
    }

    fun startRegionsEdit() = setDrafts(regions, customRegions)

    /** Leaves edit mode; drafts that were not saved are dropped. */
    fun endRegionsEdit() = setDrafts(null, null)

    fun revertRegionsEdit() {
        if (regionsDraft != null) setDrafts(regions, customRegions)
    }

    fun saveRegionsEdit() {
        customsDraft?.let {
            if (it != customRegions) {
                customRegions = it
                RegionStore.saveCustomRegions(ctx, it)
            }
        }
        regionsDraft?.let { if (it != regions) updateRegions(RegionRules.normalizeRegions(it)) }
        setDrafts(null, null)
    }

    /** Puts a region into the list. */
    fun draftAddRegion(iso: String) {
        val draft = regionsDraft ?: return
        if (RegionRules.isValidIso(iso)) regionsDraft = RegionRules.addToList(draft, iso)
    }

    /** Takes a region out of the list; it can be added again from the Add page. */
    fun draftRemoveRegion(iso: String) {
        regionsDraft = regionsDraft?.let { RegionRules.removeFromList(it, iso) }
    }

    fun draftMoveRegion(from: Int, to: Int) {
        regionsDraft = regionsDraft?.let { RegionRules.moveInList(it, from, to) }
    }

    /** True when [iso] can be used for a new region the user defines. */
    fun isFreeRegionCode(iso: String): Boolean =
        RegionRules.isValidIso(iso) && iso !in builtInRegionCodes && !isCustomRegion(iso)

    /** Defines a new region and puts it into the list. */
    fun draftCreateCustomRegion(iso: String, name: String): Boolean {
        val customs = customsDraft ?: return false
        val cleaned = RegionRules.cleanRegionName(name) ?: return false
        if (!isFreeRegionCode(iso)) return false
        setDrafts(regionsDraft, customs + CustomRegion(iso, cleaned))
        draftAddRegion(iso)
        return true
    }

    /** Deletes a region the user defined, together with its configuration. */
    fun draftDeleteCustomRegion(iso: String) {
        val customs = customsDraft ?: return
        setDrafts(regionsDraft?.filterNot { it.iso == iso }, customs.filterNot { it.iso == iso })
    }

    fun setCarriers(iso: String, carriers: List<Carrier>) = updateRegions(
        regions.map { if (it.iso == iso) it.copy(carriers = RegionRules.normalizeCarriers(iso, carriers)) else it },
    )

    // ---- Whole-configuration operations ----

    fun exportConfig(): String = ConfigCodec.encode(RoamerConfig(regions, customRegions, quickSwitch))

    /** Replaces the configuration with an exported one. Returns false when [text] is not valid. */
    fun importConfig(text: String): Boolean {
        val config = ConfigCodec.decode(text) ?: return false
        applyConfig(config)
        return true
    }

    fun resetConfig() {
        RegionStore.clear(ctx)
        applyConfig(ConfigCodec.default(), persist = false)
    }

    private fun applyConfig(config: RoamerConfig, persist: Boolean = true) {
        customRegions = config.customRegions
        regions = config.regions
        quickSwitch = config.quickSwitch
        setDrafts(null, null)
        if (persist) {
            RegionStore.saveCustomRegions(ctx, config.customRegions)
            RegionStore.saveRegions(ctx, config.regions)
            RegionStore.saveQuickSwitch(ctx, config.quickSwitch)
        }
    }

    fun updateQuickSwitch(next: List<String>) {
        val cleaned = RegionRules.cleanQuickSwitch(next, regions)
        quickSwitch = cleaned
        RegionStore.saveQuickSwitch(ctx, cleaned)
    }

    fun setRegionMaster(on: Boolean) {
        regionMasterOn = on
        val primary = RegionLogic.primaryOf(sims)
        viewModelScope.launch(Dispatchers.IO) { LocaleOverrideController.setMaster(ctx, on, primary) }
    }
}
