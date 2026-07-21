package com.eigenlux.roamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Unit test verifying key and placeholder parity between English and Chinese string tables.
 */
class StringsParityTest {

    private fun resFile(rel: String): File =
        listOf(File(rel), File("app/$rel")).firstOrNull { it.exists() }
            ?: error("resource not found from ${File("").absolutePath}: $rel")

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
            val zhText = zh[key] ?: return@forEach
            assertEquals(
                "placeholder mismatch for '$key' between values and values-zh",
                placeholders(enText),
                placeholders(zhText),
            )
        }
    }
}
