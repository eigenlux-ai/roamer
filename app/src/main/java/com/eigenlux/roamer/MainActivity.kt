package com.eigenlux.roamer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eigenlux.roamer.core.RegionLogic
import com.eigenlux.roamer.core.SimInfo
import com.eigenlux.roamer.data.AppLocaleStore
import com.eigenlux.roamer.data.RegionConfig
import com.eigenlux.roamer.ui.MenuItemRow
import com.eigenlux.roamer.ui.PlainButton
import com.eigenlux.roamer.ui.PrimaryButton
import com.eigenlux.roamer.ui.ScreenStack
import com.eigenlux.roamer.ui.SegmentedControl
import com.eigenlux.roamer.ui.SoftButton
import com.eigenlux.roamer.ui.ToggleSwitch
import com.eigenlux.roamer.ui.ToolbarIconButton
import com.eigenlux.roamer.ui.glassColors
import com.eigenlux.roamer.ui.glassToolbar
import com.eigenlux.roamer.ui.liquidGlass
import com.eigenlux.roamer.ui.pressScale
import com.eigenlux.roamer.ui.regionName
import com.eigenlux.roamer.ui.roamerGround
import com.eigenlux.roamer.ui.theme.RoamerCode
import com.eigenlux.roamer.ui.theme.RoamerTheme
import com.eigenlux.roamer.ui.theme.Spacing
import com.eigenlux.roamer.ui.theme.ThemeMode
import com.eigenlux.roamer.ui.theme.ThemeModeStore
import com.eigenlux.roamer.ui.theme.onSuccessContainerColor
import com.eigenlux.roamer.ui.theme.resolveDark
import com.eigenlux.roamer.ui.theme.successColor
import com.eigenlux.roamer.ui.theme.successContainerColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Main Activity host for the Roamer user interface.
 */
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

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
                    vm = vm,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it; ThemeModeStore.save(activity, it) },
                )
            }
        }
    }
}

/** Height of the floating toolbar below the status bar. */
internal val ToolbarHeight = 56.dp

private val RestoreBarHeight = 52.dp

private val RestoreBarShape = RoundedCornerShape(16.dp)

@Composable
private fun RoamerApp(
    vm: MainViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshAll()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    ScreenStack(
        stack = vm.stack,
        onBack = vm::pop,
        base = { MainScreen(vm = vm, themeMode = themeMode, onThemeModeChange = onThemeModeChange) },
    ) { screen ->
        when (screen) {
            Screen.Settings -> SettingsScreen(vm)
            Screen.Regions -> RegionsScreen(vm)
            Screen.AddRegion -> AddRegionScreen(vm)
            Screen.NewRegion -> NewRegionScreen(vm)
            is Screen.RegionDetail -> RegionDetailScreen(vm, screen.iso)
            Screen.QuickSwitch -> QuickSwitchScreen(vm)
            Screen.About -> AboutScreen(vm)
            Screen.AppPicker -> AppPickerScreen(primary = RegionLogic.primaryOf(vm.sims), onClose = vm::pop)
        }
    }
}

@Composable
private fun MainScreen(
    vm: MainViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val ctx = LocalContext.current
    val sims = vm.sims
    val busy = vm.busy
    val shizukuGranted = vm.shizukuGranted
    val overriddenCount = sims.count { it.overridden }
    val scrollState = rememberScrollState()
    val hazeState = rememberHazeState()
    val snackbarHostState = remember { SnackbarHostState() }
    var announced by remember { mutableStateOf(vm.log) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(vm.logEvent) {
        if (vm.logEvent == vm.announcedLogEvent) return@LaunchedEffect
        vm.announcedLogEvent = vm.logEvent
        val entry = vm.log
        when (entry.level) {
            LogLevel.SUCCESS -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            LogLevel.ERROR, LogLevel.WARNING -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
            LogLevel.INFO -> Unit
        }
        // At the end of the list the log section already shows the result.
        if (scrollState.value >= scrollState.maxValue) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        announced = entry
        snackbarHostState.showSnackbar(entry.message.lineSequence().first())
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val restoreBarSpace by animateDpAsState(
        targetValue = if (overriddenCount > 0) RestoreBarHeight + Spacing.md else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "restoreBarSpace",
    )
    val maskedAmount by animateFloatAsState(
        targetValue = if (overriddenCount > 0) 1f else 0f,
        animationSpec = tween(600),
        label = "maskedAmount",
    )
    val toolbarAmount by animateFloatAsState(
        targetValue = if (scrollState.value > 0) 1f else 0f,
        animationSpec = tween(180),
        label = "toolbarAmount",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .roamerGround(maskedAmount),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        top = statusBarTop + ToolbarHeight + Spacing.sm,
                        bottom = navBarBottom + Spacing.xl + restoreBarSpace,
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
            ) {
                InfoSummaryBlock(
                    shizukuAlive = vm.shizukuAlive,
                    shizukuGranted = shizukuGranted,
                    onRequestShizuku = { vm.requestShizuku() },
                    sims = sims,
                )

                if (vm.quickSwitch.isNotEmpty()) {
                    Section(stringResource(R.string.section_quick_switch)) {
                        QuickSwitchRow(
                            isos = vm.quickSwitch,
                            sims = sims,
                            enabled = !busy && shizukuGranted,
                            onPick = vm::maskAll,
                        )
                    }
                }

                Section(stringResource(R.string.section_sim_details)) {
                    if (sims.isEmpty()) {
                        Text(
                            stringResource(R.string.no_active_sim),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.xs),
                        )
                    } else {
                        sims.forEach { sim ->
                            key(sim.subId) {
                                SimCard(
                                    sim = sim,
                                    regions = vm.enabledRegions,
                                    busy = busy,
                                    shizukuGranted = shizukuGranted,
                                    onOverride = { iso, name -> vm.override(sim.subId, iso, name) },
                                    onRestore = { vm.restore(sim.subId) },
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Footnote(
                                stringResource(R.string.force_clear_hint),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
                            )
                            PlainButton(
                                text = stringResource(R.string.action_force_clear),
                                enabled = !busy && shizukuGranted,
                                onClick = { vm.forceClearAll() },
                            )
                        }
                    }
                }

                Section(stringResource(R.string.section_region_override)) {
                    GroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                // Balanced lines: in English the title wraps and would leave one word alone.
                                Text(
                                    stringResource(R.string.region_follow_primary),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineBreak = LineBreak.Heading),
                                )
                                Footnote(stringResource(R.string.region_follow_hint))
                            }
                            ToggleSwitch(
                                checked = vm.regionMasterOn,
                                enabled = shizukuGranted,
                                onCheckedChange = { vm.setRegionMaster(it) },
                            )
                        }
                        RowDivider()
                        val appsEnabled = vm.regionMasterOn && shizukuGranted
                        val contentAlpha = if (appsEnabled) 1f else 0.38f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = appsEnabled, role = Role.Button) { vm.push(Screen.AppPicker) }
                                .heightIn(min = 52.dp)
                                .padding(horizontal = Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.region_selected_apps),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                stringResource(R.string.region_selected_count, AppLocaleStore.enrolled(ctx).size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Footnote(
                        stringResource(R.string.region_honesty),
                        modifier = Modifier.padding(horizontal = Spacing.xs),
                    )
                }

                Section(stringResource(R.string.section_appearance)) {
                    val modes = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                    )
                    SegmentedControl(
                        options = modes.map { it.second },
                        selectedIndex = modes.indexOfFirst { it.first == themeMode },
                        onSelect = { onThemeModeChange(modes[it].first) },
                    )
                }

                Section(stringResource(R.string.section_log)) {
                    LogChip(vm.log)
                }
            }
        }

        Toolbar(
            hazeState = hazeState,
            amount = toolbarAmount,
            busy = busy || vm.refreshing,
            title = stringResource(R.string.app_name),
            navigation = {
                ToolbarIconButton(
                    onClick = { vm.push(Screen.Settings) },
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings))
                }
            },
        ) {
            ToolbarIconButton(onClick = { vm.refreshAll() }, enabled = !busy) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_refresh_all))
            }
        }

        AnimatedVisibility(
            visible = overriddenCount > 0,
            enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { it * 2 } +
                fadeIn(tween(160)),
            exit = slideOutVertically(tween(220)) { it * 2 } + fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottom + Spacing.md),
        ) {
            // The last non-zero count is kept while the bar leaves, so it does not flash "0".
            var shownCount by remember { mutableStateOf(overriddenCount) }
            if (overriddenCount > 0) shownCount = overriddenCount
            RestoreAllBar(
                hazeState = hazeState,
                count = shownCount,
                enabled = !busy && shizukuGranted,
                onClick = { vm.restoreAll() },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottom + Spacing.md + restoreBarSpace),
        ) { data ->
            ResultPill(hazeState = hazeState, level = announced.level, message = data.visuals.message)
        }
    }
}

/**
 * Floating toolbar. Its background turns into uniform glass by [amount] as content scrolls beneath.
 */
@Composable
internal fun Toolbar(
    hazeState: HazeState,
    amount: Float,
    busy: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().glassToolbar(hazeState, amount))
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ToolbarHeight)
                    .padding(horizontal = Spacing.xs),
            ) {
                // Centered on the bar itself; padded on both sides by the wider of the two ends,
                // so it never runs under the buttons.
                var sideWidth by remember { mutableStateOf(48.dp) }
                val density = LocalDensity.current
                fun measured(width: Int) {
                    val dp = with(density) { width.toDp() }
                    if (dp > sideWidth) sideWidth = dp
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = sideWidth + Spacing.sm),
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.onSizeChanged { measured(it.width) }) { navigation?.invoke() }
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier.onSizeChanged { measured(it.width) },
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
            below?.invoke()
        }
        AnimatedVisibility(
            visible = busy,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun RestoreAllBar(hazeState: HazeState, count: Int, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .pressScale(interaction)
            .liquidGlass(hazeState, RestoreBarShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .height(RestoreBarHeight)
            .padding(horizontal = Spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val alpha = if (enabled) 1f else 0.38f
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_mask),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha),
            modifier = Modifier.size(20.dp),
        )
        Text(
            stringResource(R.string.restore_all_count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
    }
}

@Composable
private fun ResultPill(hazeState: HazeState, level: LogLevel, message: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.lg)
            .liquidGlass(hazeState, MaterialTheme.shapes.large)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val (icon, tint) = when (level) {
            LogLevel.SUCCESS -> Icons.Filled.CheckCircle to successColor
            LogLevel.ERROR -> Icons.Filled.Close to MaterialTheme.colorScheme.error
            LogLevel.WARNING -> Icons.Filled.Warning to MaterialTheme.colorScheme.tertiary
            LogLevel.INFO -> Icons.Filled.Info to MaterialTheme.colorScheme.onSurfaceVariant
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )
        content()
    }
}

@Composable
private fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(lineBreak = LineBreak.Paragraph),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Solid grouped container of the content layer. [accent] rings it with the overridden color.
 */
@Composable
private fun GroupCard(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (accent) scheme.tertiary else scheme.outlineVariant.copy(alpha = 0.45f),
        animationSpec = tween(260),
        label = "groupCardBorder",
    )
    val borderWidth by animateDpAsState(if (accent) 1.5.dp else 1.dp, tween(260), label = "groupCardBorderWidth")
    Surface(
        modifier = modifier,
        shape = shape,
        color = scheme.surface,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun QuickSwitchRow(isos: List<String>, sims: List<SimInfo>, enabled: Boolean, onPick: (String) -> Unit) {
    val hasSim = sims.isNotEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        isos.forEach { iso ->
            val label = iso.uppercase(java.util.Locale.ROOT)
            val active = hasSim &&
                sims.all { it.countryIso.equals(iso, ignoreCase = true) } &&
                sims.any { it.overridden }
            val scheme = MaterialTheme.colorScheme
            SoftButton(
                onClick = { onPick(iso) },
                modifier = Modifier.weight(1f),
                enabled = enabled && hasSim,
                container = if (active) scheme.tertiaryContainer else glassColors.soft,
                contentColor = if (active) scheme.onTertiaryContainer else scheme.onSurface,
                contentPadding = PaddingValues(horizontal = Spacing.xs),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_mask),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(label, maxLines = 1)
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            val vendor = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            Text(
                "$vendor ${Build.MODEL}  ·  Android ${Build.VERSION.RELEASE}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ShizukuStatusBanner(alive = shizukuAlive, granted = shizukuGranted, onRequest = onRequestShizuku)
        }
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
    GroupCard(modifier = modifier, accent = sim.overridden, shape = MaterialTheme.shapes.large) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
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
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ValueCrossfade(sim.effectiveCarrier() to sim.countryIso) { (carrier, iso) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            carrier,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            countryLabel(iso),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = sim.overridden,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_mask),
                    contentDescription = stringResource(R.string.cd_overridden),
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                    modifier = Modifier
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
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
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
            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.large)
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm)
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Text(
            if (!alive) stringResource(R.string.shizuku_not_running) else stringResource(R.string.shizuku_not_authorized),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        if (alive && !granted) {
            SoftButton(onClick = onRequest) { Text(stringResource(R.string.action_grant)) }
        }
    }
}

@Composable
private fun SimCard(
    sim: SimInfo,
    regions: List<RegionConfig>,
    busy: Boolean,
    shizukuGranted: Boolean,
    onOverride: (iso: String, name: String) -> Unit,
    onRestore: () -> Unit,
) {
    var selectedIso by rememberSaveable { mutableStateOf<String?>(null) }
    // A region removed in settings can no longer be the selection.
    val selected = regions.firstOrNull { it.iso == selectedIso }
    var carrierName by rememberSaveable(selectedIso) {
        mutableStateOf(selected?.enabledCarriers?.firstOrNull().orEmpty())
    }

    GroupCard(modifier = Modifier.fillMaxWidth(), accent = sim.overridden) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(stringResource(R.string.slot_label, sim.slot), style = MaterialTheme.typography.titleLarge)
                if (sim.isDefaultSub) {
                    Badge(
                        stringResource(R.string.badge_default_sub),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = sim.overridden,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(160)),
                ) {
                    Badge(
                        stringResource(R.string.badge_overridden),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            val originalLabel = stringResource(R.string.value_original)
            val currentLabel = stringResource(R.string.value_current)
            val labelWidth = rememberTextWidth(MaterialTheme.typography.labelMedium, originalLabel, currentLabel)
            // The carrier column is as wide as its longest value, so the country name, which is
            // much longer in English, gets all the remaining width.
            val realCarrier = sim.realCarrierName.ifBlank { "-" }
            val carrierWidth = rememberTextWidth(CarrierValueStyle, realCarrier, sim.effectiveCarrier())
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ValueRow(
                    label = originalLabel,
                    labelWidth = labelWidth,
                    carrierWidth = carrierWidth,
                    country = countryLabel(sim.realCountryIso),
                    carrier = realCarrier,
                )
                ValueCrossfade(sim.effectiveCarrier() to sim.countryIso) { (carrier, iso) ->
                    ValueRow(
                        label = currentLabel,
                        labelWidth = labelWidth,
                        carrierWidth = carrierWidth,
                        country = countryLabel(iso),
                        carrier = carrier,
                    )
                }
                Column(modifier = Modifier.padding(start = labelWidth + Spacing.md)) {
                    listOf(
                        "MCC/MNC:${sim.mcc}/${sim.mnc}   subId:${sim.subId}",
                        "iccId:${mask(sim.iccId)}",
                    ).forEach { line ->
                        Text(
                            line,
                            style = RoamerCode,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Stacked rows: side by side, long country and carrier names were clipped.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                CountryMenuRow(
                    selected = selected,
                    regions = regions,
                    excludeIso = sim.realCountryIso,
                    enabled = !busy && shizukuGranted,
                    onSelect = { selectedIso = it.iso },
                )
                RowDivider()
                CarrierMenuRow(
                    value = carrierName,
                    carriers = selected?.enabledCarriers.orEmpty(),
                    enabled = !busy && shizukuGranted && selected != null,
                    onSelect = { carrierName = it },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
            ) {
                SoftButton(
                    enabled = !busy && shizukuGranted && sim.overridden,
                    onClick = onRestore,
                ) { Text(stringResource(R.string.action_restore)) }
                PrimaryButton(
                    // A region without carriers is overridden by ISO alone.
                    enabled = !busy && shizukuGranted && selected != null &&
                        (carrierName.isNotBlank() || selected.enabledCarriers.isEmpty()),
                    onClick = { selected?.let { onOverride(it.iso, carrierName) } },
                ) {
                    Text(
                        selected?.let { stringResource(R.string.action_override_to, it.iso) }
                            ?: stringResource(R.string.action_override)
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        modifier = Modifier
            .background(container, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}

private class MenuOption(val text: String, val selected: Boolean, val onClick: () -> Unit)

/**
 * Row that opens a menu: label on the left, the chosen value on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuRow(
    label: String,
    value: String,
    enabled: Boolean,
    options: List<MenuOption>,
    placeholder: String = stringResource(R.string.field_select),
) {
    var expanded by remember { mutableStateOf(false) }
    val canOpen = enabled && options.isNotEmpty()
    val alpha = if (enabled) 1f else 0.38f
    ExposedDropdownMenuBox(
        expanded = expanded && canOpen,
        onExpandedChange = { if (canOpen) expanded = it },
    ) {
        Row(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = canOpen)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(start = Spacing.lg, end = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 1,
            )
            Text(
                value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Medium,
                color = (if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface).copy(alpha = alpha),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Long values (common in English) shrink a little before they are ellipsized.
                autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                modifier = Modifier.size(20.dp),
            )
        }
        ExposedDropdownMenu(
            expanded = expanded && canOpen,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            options.forEach { option ->
                MenuItemRow(
                    text = option.text,
                    selected = option.selected,
                    onClick = { option.onClick(); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CountryMenuRow(
    selected: RegionConfig?,
    regions: List<RegionConfig>,
    excludeIso: String,
    enabled: Boolean,
    onSelect: (RegionConfig) -> Unit,
) {
    val options = regions
        .filterNot { it.iso.equals(excludeIso, ignoreCase = true) }
        .map { region ->
            MenuOption(countryLabel(region.iso), selected = region.iso == selected?.iso) { onSelect(region) }
        }
    MenuRow(
        label = stringResource(R.string.field_country),
        // Name only: with the ISO code appended, English names did not fit beside the label.
        // The code stays visible in the menu and on the Override button.
        value = selected?.let { regionName(it.iso) }.orEmpty(),
        enabled = enabled,
        options = options,
    )
}

@Composable
private fun CarrierMenuRow(
    value: String,
    carriers: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    MenuRow(
        label = stringResource(R.string.field_carrier),
        value = value,
        enabled = enabled,
        options = carriers.map { name -> MenuOption(name, selected = name == value) { onSelect(name) } },
        // Shown for a selected region that has no carriers configured.
        placeholder = stringResource(if (enabled && carriers.isEmpty()) R.string.field_not_set else R.string.field_select),
    )
}

@Composable
private fun countryLabel(iso: String): String =
    if (iso.isBlank()) "-" else "${regionName(iso)} (${iso.lowercase(java.util.Locale.ROOT)})"

private fun SimInfo.effectiveCarrier(): String =
    (if (overridden) carrierName else realCarrierName).ifBlank { "-" }

/**
 * Width of the widest of [texts] in [style], so value rows share one column in every locale
 * and font scale.
 */
@Composable
private fun rememberTextWidth(style: TextStyle, vararg texts: String): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(texts.toList(), style, density) {
        // Rounded up: a column one pixel too narrow would ellipsize its own text.
        with(density) { (texts.maxOf { measurer.measure(it, style, maxLines = 1).size.width } + 1).toDp() }
    }
}

private val CarrierValueStyle: TextStyle
    @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)

/**
 * Fades between values when an override or restore changes them.
 */
@Composable
private fun <T> ValueCrossfade(value: T, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { fadeIn(tween(220, delayMillis = 90)).togetherWith(fadeOut(tween(90))) },
        label = "value",
    ) { content(it) }
}

@Composable
private fun ValueRow(label: String, labelWidth: Dp, carrierWidth: Dp, country: String, carrier: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(labelWidth),
        )
        Text(
            country,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            carrier,
            style = CarrierValueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Capped so an unusually long carrier name cannot squeeze the country out.
            modifier = Modifier.width(carrierWidth.coerceAtMost(140.dp)),
        )
    }
}

@Composable
private fun LogChip(log: LogState) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg, icon) = when (log.level) {
        LogLevel.SUCCESS -> Triple(successContainerColor, onSuccessContainerColor, Icons.Filled.CheckCircle)
        LogLevel.ERROR -> Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Filled.Close)
        LogLevel.WARNING -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, Icons.Filled.Warning)
        LogLevel.INFO -> Triple(scheme.surfaceContainerHigh, scheme.onSurfaceVariant, Icons.Filled.Info)
    }
    val background by animateColorAsState(bg, tween(220), label = "logBackground")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.large)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
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
