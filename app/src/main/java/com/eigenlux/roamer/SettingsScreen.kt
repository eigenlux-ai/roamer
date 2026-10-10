package com.eigenlux.roamer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.eigenlux.roamer.data.Carrier
import com.eigenlux.roamer.data.OpenSourceLicenses
import com.eigenlux.roamer.data.RegionRules
import com.eigenlux.roamer.ui.CheckCircle
import com.eigenlux.roamer.ui.ChevronBack
import com.eigenlux.roamer.ui.CircleGlyphButton
import com.eigenlux.roamer.ui.ConfirmDialog
import com.eigenlux.roamer.ui.EditableRow
import com.eigenlux.roamer.ui.GroupRow
import com.eigenlux.roamer.ui.PlainButton
import com.eigenlux.roamer.ui.RowPosition
import com.eigenlux.roamer.ui.SearchField
import com.eigenlux.roamer.ui.Tag
import com.eigenlux.roamer.ui.ToggleSwitch
import com.eigenlux.roamer.ui.ToolbarIconButton
import com.eigenlux.roamer.ui.regionName
import com.eigenlux.roamer.ui.rememberReorderState
import com.eigenlux.roamer.ui.roamerGround
import com.eigenlux.roamer.ui.theme.RoamerCode
import com.eigenlux.roamer.ui.theme.Spacing
import com.eigenlux.roamer.ui.theme.destructiveColor
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base layout shared by the settings pages: the ground, a scrolling grouped list, and the
 * floating toolbar with a back chevron.
 */
@Composable
private fun SettingsPage(
    title: String,
    onClose: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    below: (@Composable () -> Unit)? = null,
    action: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var headerHeight by remember { mutableStateOf(Dp.Unspecified) }
    val toolbarAmount by animateFloatAsState(
        targetValue = if (listState.canScrollBackward) 1f else 0f,
        animationSpec = tween(180),
        label = "toolbarAmount",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState).roamerGround()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentPadding = PaddingValues(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = (if (headerHeight == Dp.Unspecified) statusBarTop + ToolbarHeight else headerHeight) + Spacing.md,
                    bottom = navBarBottom + Spacing.xl,
                ),
                content = content,
            )
        }
        Toolbar(
            hazeState = hazeState,
            amount = toolbarAmount,
            busy = false,
            title = title,
            modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
            navigation = {
                ToolbarIconButton(onClick = onClose, tint = MaterialTheme.colorScheme.onSurface) {
                    Icon(
                        ChevronBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
            below = below,
            actions = action,
        )
    }
}

private fun LazyListScope.sectionHeader(key: String, text: String) {
    item(key = key) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.animateItem().padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
        )
    }
}

private fun LazyListScope.footnote(key: String, text: String) {
    item(key = key) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.animateItem().padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm),
        )
    }
}

/** Explanatory text above a list. */
private fun LazyListScope.hint(key: String, text: String) {
    item(key = key) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.animateItem().padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
        )
    }
}

private fun LazyListScope.gap(key: String) {
    item(key = key) { Spacer(Modifier.animateItem().height(Spacing.xl)) }
}

@Composable
private fun RowScope.RowTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}

/**
 * Title of a row for something that is either a preset or added by the user. Entries the user
 * added carry the Custom tag; entries that are turned off are dimmed.
 */
@Composable
private fun RowScope.TaggedTitle(text: String, custom: Boolean, enabled: Boolean = true) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (custom) Tag(stringResource(R.string.tag_custom))
    }
}

@Composable
private fun OffLabel() {
    Text(
        stringResource(R.string.carrier_off),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IsoCode(iso: String) {
    Text(
        iso.uppercase(Locale.ROOT),
        style = RoamerCode,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

/**
 * Toolbar actions of an editable list. Outside edit mode: Edit. In edit mode: Done while nothing
 * has changed; once the draft differs, Revert (drops the changes) and Save.
 */
@Composable
private fun EditActions(
    editing: Boolean,
    dirty: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onRevert: () -> Unit,
    onSave: () -> Unit,
) {
    when {
        !editing -> PlainButton(text = stringResource(R.string.action_edit), onClick = onEdit)
        dirty -> {
            PlainButton(
                text = stringResource(R.string.action_revert),
                onClick = onRevert,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PlainButton(text = stringResource(R.string.action_save), onClick = onSave)
        }
        else -> PlainButton(text = stringResource(R.string.action_done), onClick = onDone)
    }
}

/**
 * Settings page: the region list, the quick-switch selection, and export, import, and reset of
 * the whole configuration.
 */
@Composable
internal fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    // Result of the last export, import, or reset, shown under the group.
    var status by remember { mutableStateOf<Int?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = vm.exportConfig()
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }.isSuccess
            }
            status = if (ok) R.string.config_exported else R.string.config_export_failed
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openInputStream(uri)!!.use { input ->
                        // A config is a few kilobytes; anything far larger is not one.
                        input.readNBytes(MAX_CONFIG_BYTES + 1).takeIf { it.size <= MAX_CONFIG_BYTES }?.decodeToString()
                    }
                }.getOrNull()
            }
            status = if (text != null && vm.importConfig(text)) R.string.config_imported else R.string.config_import_failed
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.config_reset_title),
            message = stringResource(R.string.config_reset_message),
            confirmLabel = stringResource(R.string.action_reset),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                confirmReset = false
                vm.resetConfig()
                status = R.string.config_reset_done
            },
            onDismiss = { confirmReset = false },
        )
    }

    val footer = stringResource(R.string.config_footer)
    val statusText = status?.let { stringResource(it) }

    SettingsPage(title = stringResource(R.string.settings_title), onClose = vm::pop) {
        item(key = "regions") {
            GroupRow(position = RowPosition.FIRST, onClick = { vm.push(Screen.Regions) }) {
                RowTitle(stringResource(R.string.settings_regions))
                Text(
                    vm.enabledRegions.size.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Chevron()
            }
        }
        item(key = "quick") {
            GroupRow(position = RowPosition.LAST, onClick = { vm.push(Screen.QuickSwitch) }) {
                RowTitle(stringResource(R.string.settings_quick_switch))
                Text(
                    vm.quickSwitch.joinToString(" ") { it.uppercase(Locale.ROOT) },
                    style = RoamerCode,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Chevron()
            }
        }
        gap("#configGap")
        item(key = "export") {
            GroupRow(position = RowPosition.FIRST, onClick = { exporter.launch(CONFIG_FILE_NAME) }) {
                RowTitle(stringResource(R.string.config_export))
            }
        }
        item(key = "import") {
            GroupRow(
                position = RowPosition.MIDDLE,
                // Some file managers report JSON as plain text or as an unknown type.
                onClick = { importer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
            ) { RowTitle(stringResource(R.string.config_import)) }
        }
        item(key = "reset") {
            GroupRow(position = RowPosition.LAST, onClick = { confirmReset = true }) {
                Text(
                    stringResource(R.string.config_reset),
                    style = MaterialTheme.typography.bodyLarge,
                    color = destructiveColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        footnote("#configFooter", statusText ?: footer)
        gap("#aboutGap")
        item(key = "about") {
            GroupRow(position = RowPosition.SINGLE, onClick = { vm.push(Screen.About) }) {
                RowTitle(stringResource(R.string.settings_about))
                Chevron()
            }
        }
    }
}

/**
 * About page: the app's icon, name, and version, then the libraries it is built with.
 */
@Composable
internal fun AboutScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val info = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull() }
    val icon = remember {
        runCatching { ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap(288, 288).asImageBitmap() }.getOrNull()
    }
    val version = stringResource(R.string.about_version, info?.versionName.orEmpty(), info?.longVersionCode ?: 0L)
    val appName = stringResource(R.string.app_name)
    val sourceLabel = stringResource(R.string.about_source)
    val licensesHeader = stringResource(R.string.about_licenses)
    val footer = stringResource(R.string.about_footer, OpenSourceLicenses.APP_LICENSE)
    fun open(url: String) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    SettingsPage(title = stringResource(R.string.settings_about), onClose = vm::pop) {
        item(key = "#identity") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)),
                    )
                }
                Text(
                    appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Spacing.md),
                )
                Text(version, style = RoamerCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "#source") {
            GroupRow(position = RowPosition.SINGLE, onClick = { open(OpenSourceLicenses.SOURCE_URL) }) {
                RowTitle(sourceLabel)
                Chevron()
            }
        }
        footnote("#sourceFooter", footer)
        gap("#licensesGap")
        sectionHeader("#licensesHeader", licensesHeader)
        val libraries = OpenSourceLicenses.libraries
        itemsIndexed(libraries, key = { _, lib -> lib.name }) { index, lib ->
            GroupRow(position = RowPosition.of(index, libraries.size), onClick = { open(lib.url) }) {
                RowTitle(lib.name)
                Text(
                    lib.license,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Chevron()
            }
        }
    }
}

private const val CONFIG_FILE_NAME = "roamer-config.json"
private const val MAX_CONFIG_BYTES = 1_000_000

/**
 * The countries and regions offered on the main screen. Edit mode adds, removes, and reorders
 * them; outside edit mode a row opens that region's configuration.
 */
@Composable
internal fun RegionsScreen(vm: MainViewModel) {
    val listState = rememberLazyListState()
    val editing = vm.editingRegions
    val regions = vm.listedRegions
    val reorder = rememberReorderState(listState, regions, { it.iso }, vm::draftMoveRegion)
    fun startEditing() {
        // The Add row is inserted above the first region. A lazy list keeps its first visible
        // item in place across insertions, which would leave the new row scrolled out of view,
        // so when the list is at the top it is told to stay at index 0 instead.
        val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        if (atTop) listState.requestScrollToItem(0)
        vm.startRegionsEdit()
    }
    val deleteLabel = stringResource(R.string.cd_delete)
    val reorderLabel = stringResource(R.string.cd_reorder)
    val addLabel = stringResource(R.string.regions_add)
    val footer = stringResource(R.string.regions_footer)
    val empty = stringResource(R.string.regions_empty)
    val editHint = stringResource(R.string.regions_edit_hint)

    SettingsPage(
        title = stringResource(R.string.settings_regions),
        onClose = vm::pop,
        listState = listState,
        action = {
            EditActions(
                editing = editing,
                dirty = vm.regionsDirty,
                onEdit = ::startEditing,
                onDone = vm::endRegionsEdit,
                onRevert = vm::revertRegionsEdit,
                onSave = vm::saveRegionsEdit,
            )
        },
    ) {
        // One key for both texts, so the list stays put when the mode changes.
        hint("#hint", if (editing) editHint else if (regions.isEmpty()) empty else footer)
        if (editing) {
            item(key = "#add") {
                GroupRow(
                    position = RowPosition.SINGLE,
                    onClick = { vm.push(Screen.AddRegion) },
                    modifier = Modifier.animateItem(),
                ) {
                    CircleGlyphButton(plus = true, label = addLabel, onClick = { vm.push(Screen.AddRegion) })
                    Text(
                        addLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            gap("#addGap")
        }
        itemsIndexed(regions, key = { _, region -> region.iso }) { index, region ->
            EditableRow(
                rowKey = region.iso,
                position = RowPosition.of(index, regions.size),
                editing = editing,
                reorder = reorder,
                onDelete = { vm.draftRemoveRegion(region.iso) },
                deleteLabel = deleteLabel,
                reorderLabel = reorderLabel,
                onClick = { vm.push(Screen.RegionDetail(region.iso)) },
                trailing = { Chevron() },
            ) {
                TaggedTitle(regionName(region.iso), custom = vm.isCustomRegion(region.iso))
                IsoCode(region.iso)
            }
        }
    }
}

/**
 * Page for adding countries and regions. Regions the user defined have their own section: they
 * always stay listed there, with a toggle for whether they are in use, and can be deleted for
 * good. Built-in regions below are added with one tap and stay in place, marked as added, until
 * the page is opened again.
 */
@Composable
internal fun AddRegionScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val listed = vm.listedRegions.mapTo(HashSet()) { it.iso }
    val customs = vm.customsDraft ?: vm.customRegions
    val builtIn = remember(ctx.resources.configuration) {
        vm.builtInRegionCodes.map { it to regionName(ctx, it) }.sortedBy { it.second.lowercase(Locale.ROOT) }
    }
    val q = query.trim().lowercase(Locale.ROOT)
    fun matches(iso: String, name: String) =
        q.isEmpty() || name.lowercase(Locale.ROOT).contains(q) || iso.contains(q)
    val shownCustoms = customs.filter { matches(it.iso, it.name) }
    // Regions already in the list when the page opened are left out. One added during this
    // visit stays where it is, marked as added, so the list does not jump and the tap can be
    // undone; it is gone the next time the page opens.
    val listedAtEntry = remember { listed.toSet() }
    val candidates = builtIn.filter { (iso, name) -> iso !in listedAtEntry && matches(iso, name) }
    val addLabel = stringResource(R.string.action_add)
    val deleteLabel = stringResource(R.string.cd_delete)
    val customLabel = stringResource(R.string.regions_add_custom)
    val customHeader = stringResource(R.string.custom_region_title)
    val builtInHeader = stringResource(R.string.regions_built_in)
    val addedLabel = stringResource(R.string.region_added)
    val emptyText = stringResource(R.string.add_region_empty)

    SettingsPage(
        title = stringResource(R.string.regions_add),
        onClose = vm::pop,
        below = {
            Box(modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm)) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.add_region_search),
                )
            }
        },
    ) {
        item(key = "#custom") {
            GroupRow(
                position = RowPosition.SINGLE,
                onClick = { vm.push(Screen.NewRegion) },
                modifier = Modifier.animateItem(),
            ) {
                Text(
                    customLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Chevron()
            }
        }
        gap("#customGap")
        if (shownCustoms.isNotEmpty()) {
            sectionHeader("#customHeader", customHeader)
            itemsIndexed(shownCustoms, key = { _, c -> "c:${c.iso}" }) { index, custom ->
                val inUse = custom.iso in listed
                GroupRow(
                    position = RowPosition.of(index, shownCustoms.size),
                    modifier = Modifier.animateItem(),
                ) {
                    CircleGlyphButton(
                        plus = false,
                        label = deleteLabel,
                        color = destructiveColor,
                        onClick = { vm.draftDeleteCustomRegion(custom.iso) },
                    )
                    TaggedTitle(custom.name, custom = false, enabled = inUse)
                    IsoCode(custom.iso)
                    ToggleSwitch(
                        checked = inUse,
                        onCheckedChange = { on ->
                            if (on) vm.draftAddRegion(custom.iso) else vm.draftRemoveRegion(custom.iso)
                        },
                    )
                }
            }
            gap("#builtInGap")
        }
        if (candidates.isNotEmpty()) sectionHeader("#builtInHeader", builtInHeader)
        itemsIndexed(candidates, key = { _, c -> c.first }) { index, (iso, name) ->
            val added = iso in listed
            val toggle = { if (added) vm.draftRemoveRegion(iso) else vm.draftAddRegion(iso) }
            GroupRow(
                position = RowPosition.of(index, candidates.size),
                onClick = toggle,
                modifier = Modifier.animateItem(),
            ) {
                if (added) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        CheckCircle(checked = true)
                    }
                } else {
                    CircleGlyphButton(plus = true, label = addLabel, onClick = toggle)
                }
                TaggedTitle(name, custom = false, enabled = !added)
                IsoCode(iso)
                if (added) {
                    Text(
                        addedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (candidates.isEmpty() && shownCustoms.isEmpty()) footnote("#empty", emptyText)
    }
}

/**
 * Form for defining a region the app does not know: a two-letter code and a name.
 */
@Composable
internal fun NewRegionScreen(vm: MainViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val iso = code.trim().lowercase(Locale.ROOT)
    val codeValid = RegionRules.isValidIso(iso)
    val codeFree = vm.isFreeRegionCode(iso)
    val canAdd = codeFree && RegionRules.cleanRegionName(name) != null
    val codeLabel = stringResource(R.string.custom_code)
    val codeHint = stringResource(R.string.custom_code_hint)
    val nameLabel = stringResource(R.string.custom_name)
    val footer = stringResource(R.string.custom_footer)
    val taken = stringResource(R.string.custom_code_taken)

    SettingsPage(
        title = stringResource(R.string.custom_region_title),
        onClose = vm::pop,
        action = {
            PlainButton(
                text = stringResource(R.string.action_add),
                enabled = canAdd,
                onClick = {
                    // Back to the Add page, where the new region now shows in its own section.
                    if (vm.draftCreateCustomRegion(iso, name)) vm.pop()
                },
            )
        },
    ) {
        item(key = "code") {
            GroupRow(position = RowPosition.FIRST) {
                FieldLabel(codeLabel)
                InlineField(
                    value = code,
                    onValueChange = { input -> code = input.filter { it.isLetter() && it.code < 128 }.take(2) },
                    placeholder = codeHint,
                    style = RoamerCode,
                )
            }
        }
        item(key = "name") {
            GroupRow(position = RowPosition.LAST) {
                FieldLabel(nameLabel)
                InlineField(
                    value = name,
                    onValueChange = { name = it.take(RegionRules.MAX_REGION_NAME_LENGTH) },
                    placeholder = nameLabel,
                )
            }
        }
        footnote("#footer", if (codeValid && !codeFree) taken else footer)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * Borderless single-line text field for a row, aligned to the end like the row's value.
 */
@Composable
private fun RowScope.InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    onDone: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = Modifier.weight(1f),
        decorationBox = { field ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) Text(placeholder, style = style, color = scheme.onSurfaceVariant)
                field()
            }
        },
    )
}

/**
 * Configuration of one country or region. For now it holds the carrier list: preset carriers
 * can be turned on or off, and the user's own carriers can be added and removed.
 */
@Composable
internal fun RegionDetailScreen(vm: MainViewModel, iso: String) {
    val saved = vm.allCarriersOf(iso)
    // Working copy while editing; null outside edit mode. Leaving the page drops it.
    var draftCarriers by remember { mutableStateOf<List<Carrier>?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val editing = draftCarriers != null
    val carriers = draftCarriers ?: saved
    val dirty = draftCarriers != null && draftCarriers != saved
    val reorder = rememberReorderState(listState, carriers, { it.name }) { from, to ->
        if (from in carriers.indices && to in carriers.indices) {
            draftCarriers = carriers.toMutableList().apply { add(to, removeAt(from)) }
        }
    }
    val cleanedName = RegionRules.cleanCarrier(newName)
    val canAdd = cleanedName != null && carriers.none { it.name == cleanedName }
    fun addCarrier() {
        if (cleanedName != null && canAdd) {
            draftCarriers = carriers + Carrier(cleanedName)
            newName = ""
        }
    }
    val deleteLabel = stringResource(R.string.cd_delete)
    val reorderLabel = stringResource(R.string.cd_reorder)
    val header = stringResource(R.string.region_carriers)
    val footer = stringResource(R.string.region_carriers_footer)
    val empty = stringResource(R.string.region_carriers_empty)
    val placeholder = stringResource(R.string.field_carrier)
    val addLabel = stringResource(R.string.action_add)

    SettingsPage(
        title = regionName(iso),
        onClose = vm::pop,
        listState = listState,
        action = {
            EditActions(
                editing = editing,
                dirty = dirty,
                onEdit = { draftCarriers = saved },
                onDone = { draftCarriers = null },
                onRevert = { draftCarriers = saved; newName = "" },
                onSave = {
                    draftCarriers?.let { vm.setCarriers(iso, it) }
                    draftCarriers = null
                    newName = ""
                },
            )
        },
    ) {
        if (carriers.isNotEmpty()) sectionHeader("#header", header)
        itemsIndexed(carriers, key = { _, carrier -> carrier.name }) { index, carrier ->
            val preset = RegionRules.isPresetCarrier(iso, carrier.name)
            EditableRow(
                rowKey = carrier.name,
                position = RowPosition.of(index, carriers.size),
                editing = editing,
                reorder = reorder,
                // Presets can only be turned off; carriers the user added can be removed.
                onDelete = if (preset) null else ({ draftCarriers = carriers - carrier }),
                deleteLabel = deleteLabel,
                reorderLabel = reorderLabel,
                trailing = { if (!carrier.enabled) OffLabel() },
                editTrailing = if (preset) {
                    {
                        ToggleSwitch(
                            checked = carrier.enabled,
                            onCheckedChange = { on ->
                                draftCarriers = carriers.map { if (it.name == carrier.name) it.copy(enabled = on) else it }
                            },
                        )
                    }
                } else {
                    null
                },
            ) { TaggedTitle(carrier.name, custom = !preset, enabled = carrier.enabled) }
        }
        footnote("#footer", if (carriers.none { it.enabled }) empty else footer)
        if (editing) {
            gap("#addGap")
            item(key = "#add") {
                GroupRow(position = RowPosition.SINGLE, modifier = Modifier.animateItem()) {
                    InlineField(
                        value = newName,
                        onValueChange = { newName = it.take(RegionRules.MAX_CARRIER_LENGTH) },
                        placeholder = placeholder,
                        onDone = ::addCarrier,
                    )
                    PlainButton(text = addLabel, enabled = canAdd, onClick = ::addCarrier)
                }
            }
        }
    }
}

/**
 * Chooses which regions appear in the quick-switch row of the main screen, and in what order.
 */
@Composable
internal fun QuickSwitchScreen(vm: MainViewModel) {
    val listState = rememberLazyListState()
    // The page is always editable; changes stay in this draft until they are saved.
    var quick by remember { mutableStateOf(vm.quickSwitch) }
    val dirty = quick != vm.quickSwitch
    val available = vm.enabledRegions.map { it.iso }.filterNot { it in quick }
    val full = quick.size >= RegionRules.MAX_QUICK_SWITCH
    val reorder = rememberReorderState(listState, quick, { "q:$it" }) { from, to ->
        if (from in quick.indices && to in quick.indices) {
            quick = quick.toMutableList().apply { add(to, removeAt(from)) }
        }
    }
    val deleteLabel = stringResource(R.string.cd_delete)
    val reorderLabel = stringResource(R.string.cd_reorder)
    val addLabel = stringResource(R.string.action_add)
    val shownHeader = stringResource(R.string.quick_shown)
    val availableHeader = stringResource(R.string.quick_available)
    val footer = stringResource(R.string.quick_footer, RegionRules.MAX_QUICK_SWITCH)
    val emptyText = stringResource(R.string.quick_empty)
    val fullText = stringResource(R.string.quick_full)

    SettingsPage(
        title = stringResource(R.string.settings_quick_switch),
        onClose = vm::pop,
        listState = listState,
        action = {
            if (dirty) {
                PlainButton(
                    text = stringResource(R.string.action_revert),
                    onClick = { quick = vm.quickSwitch },
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PlainButton(
                    text = stringResource(R.string.action_save),
                    onClick = { vm.updateQuickSwitch(quick); quick = vm.quickSwitch },
                )
            }
        },
    ) {
        sectionHeader("#shown", shownHeader)
        itemsIndexed(quick, key = { _, iso -> "q:$iso" }) { index, iso ->
            EditableRow(
                rowKey = "q:$iso",
                position = RowPosition.of(index, quick.size),
                editing = true,
                reorder = reorder,
                onDelete = { quick = quick - iso },
                deleteLabel = deleteLabel,
                reorderLabel = reorderLabel,
            ) {
                RowTitle(regionName(iso))
                IsoCode(iso)
            }
        }
        footnote("#shownFooter", if (quick.isEmpty()) emptyText else footer)
        if (available.isNotEmpty()) {
            gap("#gap")
            sectionHeader("#available", availableHeader)
            itemsIndexed(available, key = { _, iso -> "a:$iso" }) { index, iso ->
                GroupRow(
                    position = RowPosition.of(index, available.size),
                    onClick = if (full) null else ({ quick = quick + iso }),
                    modifier = Modifier.animateItem(),
                ) {
                    CircleGlyphButton(
                        plus = true,
                        label = addLabel,
                        enabled = !full,
                        onClick = { quick = quick + iso },
                    )
                    RowTitle(regionName(iso))
                    IsoCode(iso)
                }
            }
            if (full) footnote("#full", fullText)
        }
    }
}
