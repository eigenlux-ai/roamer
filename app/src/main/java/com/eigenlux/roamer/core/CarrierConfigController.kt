package com.eigenlux.roamer.core

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.eigenlux.roamer.R

/**
 * Orchestrates SIM enumeration, triggers instrumentation operations for override/restore,
 * and polls telephony state to confirm applied changes.
 */
object CarrierConfigController {

    private const val TAG = "RoamerCtl"

    private const val POLL_TIMEOUT_MS = 5000L
    private const val POLL_INTERVAL_MS = 120L

    /**
     * Result status for carrier config operations.
     */
    data class Result(
        val ok: Boolean,
        val output: String,
        val total: Int = 1,
        val succeeded: Int = if (ok) 1 else 0,
    ) {
        val partial: Boolean get() = succeeded in 1 until total

        /**
         * Merges the results of two consecutive operations into one report.
         */
        operator fun plus(other: Result): Result = Result(
            ok = ok && other.ok,
            output = "$output\n${other.output}",
            total = total + other.total,
            succeeded = succeeded + other.succeeded,
        )
    }

    /**
     * Enumerates active SIM slots and constructs corresponding [SimInfo] instances.
     */
    @SuppressLint("MissingPermission")
    fun loadSims(ctx: Context): List<SimInfo> {
        val sm = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subs = runCatching { sm.activeSubscriptionInfoList }.getOrNull().orEmpty()
        val iccIds = runCatching { PrivilegedSubscriptionReader.readIccIds() }.getOrDefault(emptyMap())
        val defaultSubId = if (subs.size >= 2) {
            runCatching { SubscriptionManager.getDefaultSubscriptionId() }
                .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        } else {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
        return subs.map { info ->
            val subId = info.subscriptionId
            val tm = tmFor(ctx, subId)
            val op = tm.simOperator.orEmpty()
            val effectiveIso = tm.simCountryIso.orEmpty()
            val curCarrier = tm.simOperatorName.ifBlank { info.carrierName?.toString().orEmpty() }
            val realIso = MccUtil.countryFromMcc(op)
            val overridden = realIso.isNotBlank() && effectiveIso.isNotBlank() &&
                !realIso.equals(effectiveIso, ignoreCase = true)
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

    fun setOverride(ctx: Context, subId: Int, iso: String?, name: String?): Result =
        setOverrideAll(ctx, listOf(subId), iso, name)

    fun restore(ctx: Context, subId: Int): Result = restoreAll(ctx, listOf(subId))

    /**
     * Triggers override operation for multiple SIM subscriptions and polls for state confirmation.
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
        val op = ctx.getString(R.string.op_override)
        return runCatching {
            val outcome = InstrumentationTrigger.trigger(ctx, extras)
            if (outcome?.ok == false) return@runCatching rejected(ctx, op, subIds, outcome)
            val settled = { subId: Int -> wantIso == null || effectiveIso(ctx, subId) == wantIso }
            val done = awaitSettled(subIds, outcome, settled)
            summarize(ctx, op, subIds, done, "iso='${wantIso ?: "-"}' name='${name ?: "-"}'", outcome)
        }.getOrElse { fail(ctx, op, it) }
    }

    /**
     * Triggers restore operation for multiple SIM subscriptions and polls until real ISO is reflected.
     */
    fun restoreAll(ctx: Context, subIds: List<Int>): Result =
        revertAll(ctx, subIds, "restore", R.string.op_restore, R.string.detail_iso_restored)

    /**
     * Writes back the real values and clears the override layer of multiple SIM subscriptions
     * unconditionally. Recovers SIMs that [restoreAll] cannot target because they are not detected
     * as overridden.
     */
    fun clearAll(ctx: Context, subIds: List<Int>): Result =
        revertAll(ctx, subIds, "clear", R.string.op_clear, R.string.detail_layer_cleared)

    private fun revertAll(ctx: Context, subIds: List<Int>, action: String, opRes: Int, detailRes: Int): Result {
        if (!ShizukuManager.hasPermission()) return Result(false, ctx.getString(R.string.err_shizuku_unavailable))
        if (subIds.isEmpty()) return Result(false, ctx.getString(R.string.err_no_target_sim))
        val realIsos = subIds.associateWith { MccUtil.countryFromMcc(tmFor(ctx, it).simOperator.orEmpty()) }
        val op = ctx.getString(opRes)
        return runCatching {
            val outcome = InstrumentationTrigger.trigger(ctx, mapOf("action" to action, "subId" to subIds.joinToString(",")))
            if (outcome?.ok == false) return@runCatching rejected(ctx, op, subIds, outcome)
            val settled = { subId: Int -> val ri = realIsos[subId].orEmpty(); ri.isBlank() || effectiveIso(ctx, subId) == ri }
            val done = awaitSettled(subIds, outcome, settled)
            summarize(ctx, op, subIds, done, ctx.getString(detailRes), outcome)
        }.getOrElse { fail(ctx, op, it) }
    }

    /**
     * Polls until every SIM the instrumentation did not report as failed satisfies [settled],
     * then returns the SIMs that did.
     */
    private inline fun awaitSettled(
        subIds: List<Int>,
        outcome: InstrumentationOutcome?,
        settled: (Int) -> Boolean,
    ): List<Int> {
        val candidates = subIds - outcome?.failures?.keys.orEmpty()
        pollUntil { candidates.all(settled) }
        return candidates.filter(settled)
    }

    private fun rejected(ctx: Context, op: String, subIds: List<Int>, outcome: InstrumentationOutcome): Result =
        Result(
            ok = false,
            output = ctx.getString(R.string.result_fail, op, outcome.error.orEmpty()),
            total = subIds.size,
            succeeded = 0,
        )

    private fun summarize(
        ctx: Context,
        verb: String,
        subIds: List<Int>,
        succeeded: List<Int>,
        detail: String,
        outcome: InstrumentationOutcome?,
    ): Result {
        val total = subIds.size
        val n = succeeded.size
        val failed = subIds - succeeded.toSet()
        val output = when {
            total == 1 -> if (n == 1) ctx.getString(R.string.result_single_success, verb, detail)
                else ctx.getString(R.string.result_single_timeout, verb, POLL_TIMEOUT_MS.toInt(), detail)
            n == total -> ctx.getString(R.string.result_all_success, verb, total, detail)
            n == 0 -> ctx.getString(R.string.result_all_fail, verb, total, detail)
            else -> ctx.getString(R.string.result_partial, verb, n, total, failed.joinToString(","))
        }
        val reasons = outcome?.failures.orEmpty().entries.joinToString("") { "\nsubId=${it.key}: ${it.value}" }
        return Result(ok = n == total, output = output + reasons, total = total, succeeded = n)
    }

    private fun effectiveIso(ctx: Context, subId: Int): String =
        runCatching { tmFor(ctx, subId).simCountryIso.orEmpty().lowercase() }.getOrDefault("")

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
