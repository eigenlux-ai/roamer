package com.eigenlux.roamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Executable guard for the i18n contract: the default (English) and Chinese string tables must stay
 * in sync. Android's MissingTranslation lint only fails the release build (lintVital), so a plain JVM
 * test makes "both strings.xml carry the same keys, with matching format placeholders" a fast, always-on
 * constraint — adding a string to one file and forgetting the other turns red here instead of at release.
 */
class StringsParityTest {

    private fun resFile(rel: String): File =
        listOf(File(rel), File("app/$rel")).firstOrNull { it.exists() }
            ?: error("resource not found from ${File("").absolutePath}: $rel")

    /** Parse `<string name="x">…</string>` entries into name -> raw text. */
    private fun parseStrings(rel: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resFile(rel))
        val nodes = doc.getElementsByTagName("string")
        return buildMap {
            for (i in 0 until nodes.length) {
                val el = nodes.item(i)
                val name = el.attributes.getNamedItem("name")?.nodeValue ?: continue
                put(name, el.textContent)
            }
        }
    }

    /** Positional format specifiers, e.g. `%1$s`, `%2$d` — order-independent, compared as a sorted set. */
    private fun placeholders(text: String): List<String> =
        Regex("%\\d+\\$[a-zA-Z]").findAll(text).map { it.value }.sorted().toList()

    private val en = parseStrings("src/main/res/values/strings.xml")
    private val zh = parseStrings("src/main/res/values-zh/strings.xml")

    @Test
    fun `both locales define the same keys`() {
        val onlyEn = en.keys - zh.keys
        val onlyZh = zh.keys - en.keys
        assertTrue("keys missing from values-zh: $onlyEn", onlyEn.isEmpty())
        assertTrue("keys missing from values (English default): $onlyZh", onlyZh.isEmpty())
    }

    @Test
    fun `format placeholders match per key`() {
        en.forEach { (key, enText) ->
            val zhText = zh[key] ?: return@forEach // key parity covered by the other test
            assertEquals(
                "placeholder mismatch for '$key' between values and values-zh",
                placeholders(enText),
                placeholders(zhText),
            )
        }
    }
}
