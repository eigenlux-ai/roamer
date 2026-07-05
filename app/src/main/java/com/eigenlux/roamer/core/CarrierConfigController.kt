package com.eigenlux.roamer.core

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.eigenlux.roamer.R

/**
 * App-side orchestrator: enumerate SIMs (public API) + trigger privileged instrumentation for
 * override/restore, and poll the actual effective state to confirm success/failure.
 *
 * Privileged writes and capture-if-absent both live in [PrivilegedOverrideInstrumentation]
 * (App uid, same process, attached with NO_RESTART via [InstrumentationTrigger], not killing the UI).
 * This class does not touch hidden privileged APIs directly.
 */
object CarrierConfigController {

    private const val TAG = "RoamerCtl"

    private const val POLL_TIMEOUT_MS = 5000L
    private const val POLL_INTERVAL_MS = 120L

    /**
     * Operation result. [total]/[succeeded] carry the batch N/M semantics: the UI uses them to
     * distinguish "all succeeded / partially succeeded / all failed", avoiding the contradiction of
     * "logs report failure yet the card shows overridden". A single-SIM operation has total=1.
     */
    data class Result(
        val ok: Boolean,
        val output: String,
        val total: Int = 1,
        val succeeded: Int = if (ok) 1 else 0,
    ) {
        /** Batch partial success: the success count is between 0 and the total (neither all-success nor all-failure). */
        val partial: Boolean get() = succeeded in 1 until total
    }

    /** Enumerate the currently active SIMs (public API, no privilege needed; ICCID goes through a separate privileged binder direct read, see below). */
    @SuppressLint("MissingPermission")
    fun loadSims(ctx: Context): List<SimInfo> {
        val sm = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subs = runCatching { sm.activeSubscriptionInfoList }.getOrNull().orEmpty()
        // A non-privileged App reading SubscriptionInfo.iccId gets it redacted to empty by the system
        // (requires USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER); read the true value via Shizuku binder direct
        // read (shell identity), gracefully degrading to empty on failure.
        val iccIds = runCatching { PrivilegedSubscriptionReader.readIccIds() }.getOrDefault(emptyMap())
        // The subscription an app's no-arg TelephonyManager reads bind to (getDefaultSubscriptionId).
        // Only surface it when there are >= 2 SIMs, where it disambiguates which card apps actually read;
        // on a single SIM the badge would be redundant noise.
        val defaultSubId = if (subs.size >= 2) {
            runCatching { SubscriptionManager.getDefaultSubscriptionId() }
                .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        } else {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
        return subs.map { info ->
            val subId = info.subscriptionId
            val tm = tmFor(ctx, subId)
            val op = tm.simOperator.orEmpty()          // MCCMNC, real and not overridable
            val effectiveIso = tm.simCountryIso.orEmpty()
            val curCarrier = tm.simOperatorName.ifBlank { info.carrierName?.toString().orEmpty() }
            // Whether overridden = derived at runtime, no longer relying on a saved flag: if the real
            // ISO (derived from the immutable MCC) differs from the currently effective ISO, the country
            // code has been overridden. Name-only overrides (not UI-reachable, and auto-reverting on
            // clear) are not part of this signal.
            val realIso = MccUtil.countryFromMcc(op)
            val overridden = realIso.isNotBlank() && effectiveIso.isNotBlank() &&
                !realIso.equals(effectiveIso, ignoreCase = true)
            // Real carrier name: getSimCarrierIdName comes from the Carrier-ID database (matched by SIM
            // identity), immune to the carrier_name override → still returns the real name while an
            // override is in effect, for the "original value" display; when unavailable, in the
            // non-overridden state fall back to the current name.
            val realCarrier = runCatching { tm.simCarrierIdName?.toString().orEmpty() }
                .getOrDefault("").ifBlank { if (!overridden) curCarrier else "" }
            SimInfo(
                slot = info.simSlotIndex + 1,
                subId = subId,
                carrierName = curCarrier,
                countryIso = effectiveIso,
                mcc = op.take(3),
                mnc = if (op.length > 3) op.substring(3) else "",
                iccId = iccIds[subId] ?: info.iccId.orEmpty(),
                realCountryIso = realIso,
                realCarrierName = realCarrier,
                overridden = overridden,
                isDefaultSub = subId == defaultSubId,
            )
        }
    }

    /** Override a single SIM = single-element batch (convenience alias). */
    fun setOverride(ctx: Context, subId: Int, iso: String?, name: String?): Result =
        setOverrideAll(ctx, listOf(subId), iso, name)

    /** Restore a single SIM = single-element batch (convenience alias). */
    fun restore(ctx: Context, subId: Int): Result = restoreAll(ctx, listOf(subId))

    /**
     * Override multiple SIMs: **one** instrumentation processes all subIds (internally a single shell
     * delegation + sequential loop, no concurrency), then polls each one to confirm simCountryIso took
     * effect. All SIMs share the same iso/name.
     */
    fun setOverrideAll(ctx: Context, subIds: List<Int>, iso: String?, name: String?): Result {
        if (!ShizukuManager.hasPermission()) return Result(false, ctx.getString(R.string.err_shizuku_unavailable))
        if (subIds.isEmpty()) return Result(false, ctx.getString(R.string.err_no_target_sim))
        val wantIso = iso?.takeIf { it.isNotBlank() }?.lowercase()
        val extras = buildMap {
            put("action", "set"); put("subId", subIds.joinToString(","))
            iso?.takeIf { it.isNotBlank() }?.let { put("iso", it) }
            name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
        }
        return runCatching {
            InstrumentationTrigger.trigger(ctx, extras)
            // Use the **actually effective** simCountryIso as the source of truth: first wait until all
            // are in place or timeout, then verify each SIM's final state to produce the N/M semantics
            pollUntil { wantIso == null || subIds.all { effectiveIso(ctx, it) == wantIso } }
            val done = subIds.filter { wantIso == null || effectiveIso(ctx, it) == wantIso }
            summarize(ctx, ctx.getString(R.string.op_override), subIds, done, "iso='${wantIso ?: "-"}' name='${name ?: "-"}'")
        }.getOrElse { fail(ctx, ctx.getString(R.string.op_override), it) }
    }

    /**
     * Restore multiple SIMs: **one** instrumentation processes all subIds (single shell delegation +
     * sequential loop). Each SIM's real ISO is derived at runtime from the immutable MCC; success is
     * judged by each SIM's simCountryIso returning to its true value.
     * Key: batching avoids the process-level stopDelegateShellPermissionIdentity clobbering each other
     * when multiple instrumentations are triggered concurrently (which would cause the secondary SIM's
     * carrier to fail to restore during "restore all"). The sticky revert of the carrier name is not a
     * criterion; the UI uses the immune realCarrierName.
     */
    fun restoreAll(ctx: Context, subIds: List<Int>): Result {
        if (!ShizukuManager.hasPermission()) return Result(false, ctx.getString(R.string.err_shizuku_unavailable))
        if (subIds.isEmpty()) return Result(false, ctx.getString(R.string.err_no_target_sim))
        val realIsos = subIds.associateWith { MccUtil.countryFromMcc(tmFor(ctx, it).simOperator.orEmpty()) }
        return runCatching {
            InstrumentationTrigger.trigger(ctx, mapOf("action" to "restore", "subId" to subIds.joinToString(",")))
            val settled = { subId: Int -> val ri = realIsos[subId].orEmpty(); ri.isBlank() || effectiveIso(ctx, subId) == ri }
            pollUntil { subIds.all(settled) }
            val done = subIds.filter(settled)
            summarize(ctx, ctx.getString(R.string.op_restore), subIds, done, ctx.getString(R.string.detail_iso_restored))
        }.getOrElse { fail(ctx, ctx.getString(R.string.op_restore), it) }
    }

    /**
     * Summarize the batch result into a [Result] with N/M semantics. [succeeded] = the list of subIds
     * verified to have actually taken effect. All succeeded → SUCCESS copy; partial → list the SIMs
     * that did not take effect; all failed → FAIL. A single SIM (total=1) degrades to concise single-SIM copy.
     */
    private fun summarize(ctx: Context, verb: String, subIds: List<Int>, succeeded: List<Int>, detail: String): Result {
        val total = subIds.size
        val n = succeeded.size
        val failed = subIds - succeeded.toSet()
        // total == 1 is fully handled here (single-SIM copy); every branch below is therefore reached
        // only when total >= 2. That invariant is what lets the English batch strings hardcode the plural
        // noun ("%2$d SIMs") without a <plurals> rule. If a single-SIM path is ever routed into an all_*
        // branch, revisit this and switch those strings to plurals.
        val output = when {
            total == 1 -> if (n == 1) ctx.getString(R.string.result_single_success, verb, detail)
                else ctx.getString(R.string.result_single_timeout, verb, POLL_TIMEOUT_MS.toInt(), detail)
            n == total -> ctx.getString(R.string.result_all_success, verb, total, detail)
            n == 0 -> ctx.getString(R.string.result_all_fail, verb, total, detail)
            else -> ctx.getString(R.string.result_partial, verb, n, total, failed.joinToString(","))
        }
        return Result(ok = n == total, output = output, total = total, succeeded = n)
    }

    /** The actually effective country code (lowercase), i.e. getSimCountryIso — the value the user actually sees. */
    private fun effectiveIso(ctx: Context, subId: Int): String =
        runCatching { tmFor(ctx, subId).simCountryIso.orEmpty().lowercase() }.getOrDefault("")

    /** Poll until pred holds or timeout. Runs on the IO thread, so a blocking sleep is harmless. */
    private inline fun pollUntil(pred: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (pred()) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun tmFor(ctx: Context, subId: Int): TelephonyManager =
        (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .createForSubscriptionId(subId)

    private fun fail(ctx: Context, op: String, t: Throwable): Result {
        Log.e(TAG, "$op failed", t)
        val cause = generateSequence(t) { it.cause }.last()
        return Result(false, ctx.getString(R.string.result_fail, op, cause.message ?: cause.javaClass.simpleName))
    }
}
