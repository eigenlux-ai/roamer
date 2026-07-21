package com.eigenlux.roamer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eigenlux.roamer.core.CarrierConfigController
import com.eigenlux.roamer.core.LocaleOverrideController
import com.eigenlux.roamer.core.RegionLogic
import com.eigenlux.roamer.core.ShizukuManager
import com.eigenlux.roamer.core.SimInfo
import com.eigenlux.roamer.data.AppLocaleStore
import com.eigenlux.roamer.data.CarrierPresets
import com.eigenlux.roamer.data.CountryPreset
import com.eigenlux.roamer.data.CountryPresets
import com.eigenlux.roamer.ui.theme.RoamerCode
import com.eigenlux.roamer.ui.theme.RoamerTheme
import com.eigenlux.roamer.ui.theme.Spacing
import com.eigenlux.roamer.ui.theme.ThemeMode
import com.eigenlux.roamer.ui.theme.ThemeModeStore
import com.eigenlux.roamer.ui.theme.resolveDark
import com.eigenlux.roamer.ui.theme.successColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Main Activity host for the Roamer user interface.
 */
class MainActivity : ComponentActivity() {

    private val requestPhoneState =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPhoneState.launch(Manifest.permission.READ_PHONE_STATE)
        }
        enableEdgeToEdge()
        val activity = this
        setContent {
            var themeMode by remember { mutableStateOf(ThemeModeStore.load(activity)) }
            val dark = themeMode.resolveDark()
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(activity.window, view).run {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            RoamerTheme(darkTheme = dark) {
                RoamerApp(
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it; ThemeModeStore.save(activity, it) },
                )
            }
        }
    }
}

private enum class LogLevel { INFO, SUCCESS, ERROR, WARNING }
private data class LogState(val level: LogLevel, val message: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoamerApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var sims by remember { mutableStateOf<List<SimInfo>>(emptyList()) }
    val readyMsg = stringResource(R.string.log_ready)
    var log by remember { mutableStateOf(LogState(LogLevel.INFO, readyMsg)) }
    var busy by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val opMutex = remember { Mutex() }
    var shizukuGranted by remember { mutableStateOf(ShizukuManager.hasPermission()) }
    var regionMasterOn by remember { mutableStateOf(AppLocaleStore.isMasterOn(ctx)) }
    var showAppPicker by remember { mutableStateOf(false) }
    var shizukuAlive by remember { mutableStateOf(ShizukuManager.isBinderAlive()) }

    fun refreshShizuku() {
        shizukuAlive = ShizukuManager.isBinderAlive()
        shizukuGranted = ShizukuManager.hasPermission()
    }

    fun refreshAll() {
        refreshShizuku()
        scope.launch {
            if (busy || opMutex.isLocked) return@launch
            refreshing = true
            val loaded = withContext(Dispatchers.IO) {
                runCatching { CarrierConfigController.loadSims(ctx) }.getOrNull()
            }
            if (loaded != null) {
                sims = loaded
                withContext(Dispatchers.IO) {
                    runCatching { LocaleOverrideController.sync(ctx, RegionLogic.primaryOf(loaded)) }
                }
            }
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { refreshAll() }
    LaunchedEffect(shizukuGranted) { if (shizukuGranted) refreshAll() }
    DisposableEffect(lifecycleOwner) {
        val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> refreshShizuku() }
        val aliveListener = Shizuku.OnBinderReceivedListener { refreshShizuku() }
        val deadListener = Shizuku.OnBinderDeadListener { refreshShizuku() }
        Shizuku.addRequestPermissionResultListener(permListener)
        Shizuku.addBinderReceivedListenerSticky(aliveListener)
        Shizuku.addBinderDeadListener(deadListener)
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshAll()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(permListener)
            Shizuku.removeBinderReceivedListener(aliveListener)
            Shizuku.removeBinderDeadListener(deadListener)
            lifecycleOwner.lifecycle.removeObserver(obs)
        }
    }

    fun run(block: () -> CarrierConfigController.Result) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val r = opMutex.withLock { withContext(Dispatchers.IO) { block() } }
                val level = when {
                    r.ok -> LogLevel.SUCCESS
                    r.partial -> LogLevel.WARNING
                    else -> LogLevel.ERROR
                }
                log = LogState(level, r.output)
                val loaded = withContext(Dispatchers.IO) {
                    runCatching { CarrierConfigController.loadSims(ctx) }.getOrNull()
                }
                if (loaded != null) {
                    sims = loaded
                    withContext(Dispatchers.IO) {
                        runCatching { LocaleOverrideController.sync(ctx, RegionLogic.primaryOf(loaded)) }
                    }
                }
            } finally {
                busy = false
            }
        }
    }

    fun runRestoreAll() {
        val subIds = sims.filter { it.overridden }.map { it.subId }
        if (subIds.isEmpty()) {
            log = LogState(LogLevel.INFO, ctx.getString(R.string.nothing_to_restore))
            return
        }
        run { CarrierConfigController.restoreAll(ctx, subIds) }
    }

    fun runMaskAll(iso: String) {
        if (sims.isEmpty()) {
            log = LogState(LogLevel.INFO, ctx.getString(R.string.no_sim_to_mask))
            return
        }
        val subIds = sims.filterNot { it.realCountryIso.equals(iso, ignoreCase = true) }.map { it.subId }
        if (subIds.isEmpty()) {
            log = LogState(LogLevel.INFO, ctx.getString(R.string.already_masked, iso.uppercase()))
            return
        }
        val name = CarrierPresets.forCountry(iso).firstOrNull().orEmpty()
        run { CarrierConfigController.setOverrideAll(ctx, subIds, iso, name) }
    }

    if (showAppPicker) {
        AppPickerScreen(
            primary = RegionLogic.primaryOf(sims),
            onClose = { showAppPicker = false },
        )
    } else Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { refreshAll() }, enabled = !busy) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_refresh_all))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (busy || refreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
            InfoSummaryBlock(
                shizukuAlive = shizukuAlive,
                shizukuGranted = shizukuGranted,
                onRequestShizuku = { ShizukuManager.requestPermission(); refreshShizuku() },
                sims = sims,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                val hasSim = sims.isNotEmpty()
                val overriddenCount = sims.count { it.overridden }
                val compactPad = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.section_quick_switch), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    listOf("us" to "US", "jp" to "JP", "kr" to "KR", "cn" to "CN", "hk" to "HK").forEach { (iso, label) ->
                        val active = hasSim &&
                            sims.all { it.countryIso.equals(iso, ignoreCase = true) } &&
                            sims.any { it.overridden }
                        val enabled = !busy && shizukuGranted && hasSim
                        val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_mask),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(label)
                        }
                        if (active) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                enabled = enabled,
                                onClick = { runMaskAll(iso) },
                                shape = RoundedCornerShape(Spacing.md),
                                contentPadding = compactPad,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                                content = content,
                            )
                        } else {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = enabled,
                                onClick = { runMaskAll(iso) },
                                shape = RoundedCornerShape(Spacing.md),
                                contentPadding = compactPad,
                                content = content,
                            )
                        }
                    }
                }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.section_sim_details), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Button(
                        enabled = !busy && shizukuGranted && overriddenCount > 0,
                        onClick = { runRestoreAll() },
                        shape = RoundedCornerShape(Spacing.md),
                        contentPadding = compactPad,
                    ) {
                        Text(
                            if (overriddenCount > 0) stringResource(R.string.restore_all_count, overriddenCount)
                            else stringResource(R.string.restore_all)
                        )
                    }
                }
                if (sims.isEmpty()) {
                    Text(stringResource(R.string.no_active_sim), style = MaterialTheme.typography.bodySmall)
                } else {
                    sims.forEach { sim ->
                        key(sim.subId) {
                            SimCard(
                                sim = sim,
                                busy = busy,
                                shizukuGranted = shizukuGranted,
                                onOverride = { iso, name ->
                                    run { CarrierConfigController.setOverride(ctx, sim.subId, iso, name) }
                                },
                                onRestore = { run { CarrierConfigController.restore(ctx, sim.subId) } },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.section_region_override), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(stringResource(R.string.region_follow_primary), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.region_follow_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = regionMasterOn,
                        enabled = shizukuGranted,
                        onCheckedChange = { on ->
                            regionMasterOn = on
                            scope.launch(Dispatchers.IO) {
                                LocaleOverrideController.setMaster(ctx, on, RegionLogic.primaryOf(sims))
                            }
                        },
                    )
                }
                Surface(
                    onClick = { showAppPicker = true },
                    enabled = regionMasterOn && shizukuGranted,
                    shape = RoundedCornerShape(Spacing.md),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.region_selected_apps), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text(
                            stringResource(R.string.region_selected_count, AppLocaleStore.enrolled(ctx).size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    stringResource(R.string.region_honesty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.section_appearance), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                    )
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        ) { Text(label) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.section_log), style = MaterialTheme.typography.titleSmall)
                LogChip(log)
            }
            }
        }
    }
}

@Composable
private fun InfoSummaryBlock(
    shizukuAlive: Boolean,
    shizukuGranted: Boolean,
    onRequestShizuku: () -> Unit,
    sims: List<SimInfo>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val vendor = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        Text(
            "$vendor ${Build.MODEL}  ·  Android ${Build.VERSION.RELEASE}",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        ShizukuStatusBanner(alive = shizukuAlive, granted = shizukuGranted, onRequest = onRequestShizuku)
        if (sims.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                sims.forEach { sim ->
                    MiniSimTile(sim = sim, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun MiniSimTile(sim: SimInfo, modifier: Modifier = Modifier) {
    val borderColor by animateColorAsState(
        targetValue = if (sim.overridden) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(220),
        label = "miniTileBorder",
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(Spacing.md),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_sim_landscape),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.slot_label, sim.slot),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    (if (sim.overridden) sim.carrierName else sim.realCarrierName).ifBlank { "-" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    countryLabel(sim.countryIso),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (sim.overridden) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_mask),
                    contentDescription = stringResource(R.string.cd_overridden),
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = 0.dp)
                        .size(80.dp)
                        .rotate(-18f),
                )
            }
        }
    }
}

@Composable
private fun ShizukuStatusBanner(alive: Boolean, granted: Boolean, onRequest: () -> Unit) {
    if (alive && granted) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = successColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(R.string.shizuku_authorized),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(Spacing.md))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            if (!alive) stringResource(R.string.shizuku_not_running) else stringResource(R.string.shizuku_not_authorized),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        if (alive && !granted) {
            OutlinedButton(onClick = onRequest, shape = RoundedCornerShape(Spacing.md)) { Text(stringResource(R.string.action_grant)) }
        }
    }
}

@Composable
private fun SimCard(
    sim: SimInfo,
    busy: Boolean,
    shizukuGranted: Boolean,
    onOverride: (iso: String, name: String) -> Unit,
    onRestore: () -> Unit,
) {
    var selectedCountry by remember { mutableStateOf<CountryPreset?>(null) }
    var carrierName by remember(selectedCountry) {
        mutableStateOf(selectedCountry?.let { CarrierPresets.forCountry(it.iso).firstOrNull() }.orEmpty())
    }

    val borderColor by animateColorAsState(
        targetValue = if (sim.overridden) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(220),
        label = "simCardBorder",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(stringResource(R.string.slot_label, sim.slot), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(
                    "subId:${sim.subId}   iccId:${mask(sim.iccId)}",
                    style = RoamerCode,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ValueRow(
                    label = stringResource(R.string.value_original),
                    country = countryLabel(sim.realCountryIso),
                    carrier = sim.realCarrierName.ifBlank { "-" },
                )
                Text(
                    "MCC/MNC:${sim.mcc}/${sim.mnc}",
                    style = RoamerCode,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = ValueLabelWidth + Spacing.md),
                )
            }
            ValueRow(
                label = stringResource(R.string.value_current),
                country = countryLabel(sim.countryIso),
                carrier = (if (sim.overridden) sim.carrierName else sim.realCarrierName).ifBlank { "-" },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionLabel(stringResource(R.string.section_override_to))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CountryDropdown(
                    selected = selectedCountry,
                    excludeIso = sim.realCountryIso,
                    enabled = !busy && shizukuGranted,
                    onSelect = { selectedCountry = it },
                    modifier = Modifier.weight(0.6f),
                )
                CarrierDropdown(
                    value = carrierName,
                    countryIso = selectedCountry?.iso.orEmpty(),
                    enabled = !busy && shizukuGranted && selectedCountry != null,
                    onSelect = { carrierName = it },
                    modifier = Modifier.weight(0.4f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (sim.isDefaultSub) {
                    Text(
                        stringResource(R.string.badge_default_sub),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
                if (sim.overridden) {
                    Text(
                        stringResource(R.string.badge_overridden),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    enabled = !busy && shizukuGranted && sim.overridden,
                    onClick = onRestore,
                    shape = RoundedCornerShape(Spacing.md),
                ) { Text(stringResource(R.string.action_restore)) }
                Button(
                    enabled = !busy && shizukuGranted && selectedCountry != null && carrierName.isNotBlank(),
                    onClick = { selectedCountry?.let { onOverride(it.iso, carrierName) } },
                    shape = RoundedCornerShape(Spacing.md),
                ) {
                    Text(
                        selectedCountry?.let { stringResource(R.string.action_override_to, it.iso) }
                            ?: stringResource(R.string.action_override)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(
    selected: CountryPreset?,
    excludeIso: String,
    enabled: Boolean,
    onSelect: (CountryPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(excludeIso) {
        CountryPresets.all.filterNot { it.iso.equals(excludeIso, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.let { countryLabel(it.iso) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.field_country), maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(countryLabel(preset.iso)) },
                    onClick = { onSelect(preset); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarrierDropdown(
    value: String,
    countryIso: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(countryIso) { CarrierPresets.forCountry(countryIso) }
    val canOpen = enabled && options.isNotEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded && canOpen,
        onExpandedChange = { if (canOpen) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.field_carrier), maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && canOpen) },
            shape = RoundedCornerShape(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded && canOpen, onDismissRequest = { expanded = false }) {
            options.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(name); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun countryLabel(iso: String): String = when {
    iso.isBlank() -> "-"
    else -> CountryPresets.byIso(iso)?.let { "${stringResource(it.nameRes)} (${it.iso})" } ?: iso
}

private val ValueLabelWidth = 44.dp

@Composable
private fun ValueRow(label: String, country: String, carrier: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(ValueLabelWidth),
        )
        Text(
            country,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            carrier,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LogChip(log: LogState) {
    val (bg, fg, icon) = when (log.level) {
        LogLevel.SUCCESS -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "✓",
        )
        LogLevel.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "✕",
        )
        LogLevel.WARNING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "!",
        )
        LogLevel.INFO -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "·",
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(Spacing.md))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(icon, color = fg, style = MaterialTheme.typography.labelLarge)
            Text(
                when (log.level) {
                    LogLevel.SUCCESS -> stringResource(R.string.log_level_success)
                    LogLevel.ERROR -> stringResource(R.string.log_level_error)
                    LogLevel.WARNING -> stringResource(R.string.log_level_partial)
                    LogLevel.INFO -> stringResource(R.string.log_level_info)
                },
                color = fg,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(log.message, style = RoamerCode, color = fg)
    }
}

private fun mask(s: String): String =
    if (s.isBlank()) "-" else if (s.length <= 6) s else "${s.take(4)}…${s.takeLast(2)}"
