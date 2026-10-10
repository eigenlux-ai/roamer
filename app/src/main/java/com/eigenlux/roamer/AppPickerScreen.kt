package com.eigenlux.roamer

import android.content.pm.ApplicationInfo
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.eigenlux.roamer.core.LocaleOverrideController
import com.eigenlux.roamer.core.RegionLogic
import com.eigenlux.roamer.data.AppLocaleStore
import com.eigenlux.roamer.ui.CheckCircle
import com.eigenlux.roamer.ui.ControlShape
import com.eigenlux.roamer.ui.ToolbarIconButton
import com.eigenlux.roamer.ui.SoftButton
import com.eigenlux.roamer.ui.glassColors
import com.eigenlux.roamer.ui.roamerGround
import com.eigenlux.roamer.ui.theme.RoamerCode
import com.eigenlux.roamer.ui.theme.Spacing
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val iconCache = LruCache<String, ImageBitmap>(256)

private data class AppEntry(val pkg: String, val label: String, val isSystem: Boolean)

/**
 * App picker interface for selecting target applications to mirror SIM region settings.
 */
@Composable
fun AppPickerScreen(primary: RegionLogic.PrimaryState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var enrolled by remember { mutableStateOf(AppLocaleStore.enrolled(ctx)) }

    androidx.compose.runtime.LaunchedEffect(showSystem) {
        apps = null
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val snap = AppLocaleStore.enrolled(ctx)
            runCatching {
                pm.getInstalledApplications(0)
                    .asSequence()
                    .filter { it.packageName != ctx.packageName }
                    .filter { showSystem || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .map { AppEntry(it.packageName, pm.getApplicationLabel(it).toString(), (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0) }
                    .sortedWith(compareByDescending<AppEntry> { it.pkg in snap }.thenBy { it.label.lowercase() })
                    .toList()
            }.getOrDefault(emptyList())
        }
    }

    BackHandler(onBack = onClose)

    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val toolbarAmount by animateFloatAsState(
        targetValue = if (listState.canScrollBackward) 1f else 0f,
        animationSpec = tween(180),
        label = "toolbarAmount",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState).roamerGround()) {
            val list = apps
            val q = query.trim().lowercase(Locale.ROOT)
            val filtered = when {
                list == null -> null
                q.isBlank() -> list
                else -> list.filter {
                    it.label.lowercase(Locale.ROOT).contains(q) || it.pkg.lowercase(Locale.ROOT).contains(q)
                }
            }
            when {
                filtered == null -> CenterHint {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(Spacing.md))
                    Text(stringResource(R.string.app_picker_loading), style = MaterialTheme.typography.bodyMedium)
                }
                filtered.isEmpty() -> CenterHint {
                    Text(
                        stringResource(R.string.app_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    contentPadding = PaddingValues(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        top = headerHeight + Spacing.sm,
                        bottom = navBarBottom + Spacing.xl,
                    ),
                ) {
                    items(filtered, key = { it.pkg }) { app ->
                        AppRow(
                            app = app,
                            checked = enrolled.contains(app.pkg),
                            onCheckedChange = { checked ->
                                enrolled = enrolled.toMutableSet().apply { if (checked) add(app.pkg) else remove(app.pkg) }
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (checked) LocaleOverrideController.enroll(ctx, app.pkg, primary)
                                        else LocaleOverrideController.unenroll(ctx, app.pkg)
                                    }
                                    // The operation can be refused; show what was actually stored.
                                    val stored = AppLocaleStore.isEnrolled(ctx, app.pkg)
                                    enrolled = if (stored) enrolled + app.pkg else enrolled - app.pkg
                                }
                            },
                        )
                    }
                }
            }
        }

        Toolbar(
            hazeState = hazeState,
            amount = toolbarAmount,
            busy = false,
            title = stringResource(R.string.app_picker_title),
            modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
            navigation = {
                ToolbarIconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(32.dp),
                    )
                }
            },
            below = {
                Column(
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = stringResource(R.string.app_picker_search),
                    )
                    val scheme = MaterialTheme.colorScheme
                    SoftButton(
                        onClick = { showSystem = !showSystem },
                        container = if (showSystem) scheme.primaryContainer else glassColors.soft,
                        contentColor = if (showSystem) scheme.onPrimaryContainer else scheme.onSurface,
                    ) { Text(stringResource(R.string.app_picker_show_system)) }
                }
            },
        )
    }
}

/**
 * Search field of the glass header.
 */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    val scheme = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                modifier = Modifier
                    .background(glassColors.soft, ControlShape)
                    .heightIn(min = 44.dp)
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                    }
                    field()
                }
            }
        },
    )
}

@Composable
private fun AppRow(app: AppEntry, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        AppIcon(app.pkg)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.pkg, style = RoamerCode, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CheckCircle(checked = checked)
    }
}

@Composable
private fun AppIcon(pkg: String) {
    val ctx = LocalContext.current
    var bitmap by remember(pkg) { mutableStateOf(iconCache.get(pkg)) }
    androidx.compose.runtime.LaunchedEffect(pkg) {
        if (bitmap != null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }?.also { iconCache.put(pkg, it) }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun CenterHint(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}
