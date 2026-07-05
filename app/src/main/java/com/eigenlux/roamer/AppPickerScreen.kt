package com.eigenlux.roamer

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.util.Locale
import com.eigenlux.roamer.core.LocaleOverrideController
import com.eigenlux.roamer.core.RegionLogic
import com.eigenlux.roamer.data.AppLocaleStore
import com.eigenlux.roamer.ui.theme.RoamerCode
import com.eigenlux.roamer.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One installed app in the picker (icon is loaded lazily per row). */
private data class AppEntry(val pkg: String, val label: String, val isSystem: Boolean)

/**
 * Full-screen app picker for the region-override "selected apps" list. Checking an app enrolls it
 * (captures baseline + applies the current target); unchecking restores its baseline. Enumeration
 * needs QUERY_ALL_PACKAGES (declared in the manifest); without it the system only returns a subset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(primary: RegionLogic.PrimaryState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var enrolled by remember { mutableStateOf(AppLocaleStore.enrolled(ctx)) }

    // Load installed apps off the main thread; re-run when the system-apps filter toggles.
    androidx.compose.runtime.LaunchedEffect(showSystem) {
        apps = null
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            // Snapshot the enrolled set at load time so the "enrolled on top" order stays stable while the
            // user checks/unchecks (avoids rows jumping away mid-tap); re-entering the picker re-sorts.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_picker_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = Spacing.lg)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.app_picker_search)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilterChip(
                    selected = showSystem,
                    onClick = { showSystem = !showSystem },
                    label = { Text(stringResource(R.string.app_picker_show_system)) },
                )
            }

            val list = apps
            when {
                list == null -> CenterHint {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(Spacing.md))
                    Text(stringResource(R.string.app_picker_loading), style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    val q = query.trim().lowercase(Locale.ROOT)
                    val filtered = if (q.isBlank()) list
                        else list.filter {
                            it.label.lowercase(Locale.ROOT).contains(q) || it.pkg.lowercase(Locale.ROOT).contains(q)
                        }
                    if (filtered.isEmpty()) {
                        CenterHint { Text(stringResource(R.string.app_picker_empty), style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filtered, key = { it.pkg }) { app ->
                                AppRow(
                                    app = app,
                                    checked = enrolled.contains(app.pkg),
                                    onCheckedChange = { checked ->
                                        enrolled = enrolled.toMutableSet().apply { if (checked) add(app.pkg) else remove(app.pkg) }
                                        scope.launch(Dispatchers.IO) {
                                            if (checked) LocaleOverrideController.enroll(ctx, app.pkg, primary)
                                            else LocaleOverrideController.unenroll(ctx, app.pkg)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        AppIcon(app.pkg)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.pkg, style = RoamerCode, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Lazily load and render an app icon (48dp); blank placeholder while loading / on failure. */
@Composable
private fun AppIcon(pkg: String) {
    val ctx = LocalContext.current
    var bitmap by remember(pkg) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(pkg) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
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
