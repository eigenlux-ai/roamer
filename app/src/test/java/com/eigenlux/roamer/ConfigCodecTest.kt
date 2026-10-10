package com.eigenlux.roamer

import com.eigenlux.roamer.data.Carrier
import com.eigenlux.roamer.data.ConfigCodec
import com.eigenlux.roamer.data.CustomRegion
import com.eigenlux.roamer.data.RegionConfig
import com.eigenlux.roamer.data.RoamerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for exporting and importing the configuration.
 */
class ConfigCodecTest {

    @Test
    fun `the default configuration round-trips unchanged`() {
        val default = ConfigCodec.default()
        assertEquals(default, ConfigCodec.decode(ConfigCodec.encode(default)))
    }

    @Test
    fun `an edited configuration round-trips unchanged`() {
        val config = RoamerConfig(
            regions = listOf(
                RegionConfig("zz", listOf(Carrier("My \"Carrier\""))),
                RegionConfig("us", listOf(Carrier("Mint"), Carrier("AT&T"), Carrier("T-Mobile", enabled = false), Carrier("Verizon"))),
                RegionConfig("jp", listOf(Carrier("NTT docomo"), Carrier("au"), Carrier("SoftBank"), Carrier("Rakuten Mobile")), enabled = false),
            ),
            customRegions = listOf(CustomRegion("zz", "测试地区")),
            quickSwitch = listOf("us", "zz"),
        )
        assertEquals(config, ConfigCodec.decode(ConfigCodec.encode(config)))
    }

    @Test
    fun `files that are not a Roamer config are rejected`() {
        assertNull(ConfigCodec.decode("not json"))
        assertNull(ConfigCodec.decode("{}"))
        assertNull(ConfigCodec.decode("""{"format":"roamer-config","version":99,"regions":[]}"""))
        assertNull(ConfigCodec.decode("""{"format":"roamer-config","version":1}"""))
    }

    @Test
    fun `invalid entries are dropped and the rest is normalized`() {
        val decoded = ConfigCodec.decode(
            """{"format":"roamer-config","version":1,
               "regions":[{"iso":"US","carriers":[{"name":"  Verizon "},{"name":""},{"name":"Verizon"}]},{"iso":"toolong"},{"iso":"us"}],
               "customRegions":[{"iso":"q1","name":"Bad"},{"iso":"zz","name":" Test "}],
               "quickSwitch":["us","nope","us"]}""",
        )!!
        assertEquals(
            listOf(RegionConfig("us", listOf(Carrier("Verizon"), Carrier("T-Mobile"), Carrier("AT&T")))),
            decoded.regions,
        )
        assertEquals(listOf(CustomRegion("zz", "Test")), decoded.customRegions)
        assertEquals(listOf("us"), decoded.quickSwitch)
    }
}
