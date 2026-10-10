package com.eigenlux.roamer.core

import android.os.IBinder
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper

/**
 * Shared entry point for hidden-API exemptions and Shizuku-wrapped system service interfaces.
 */
internal object SystemServices {

    init {
        // Swallowed so JVM unit tests can load classes that depend on this object.
        runCatching { HiddenApiBypass.addHiddenApiExemptions("L", "I") }
    }

    /**
     * Forces the exemption initializer to run before reflective hidden-API access.
     */
    fun ensureHiddenApiAccess() = Unit

    /**
     * Resolves [serviceName] from `ServiceManager` and returns its [interfaceName] proxy,
     * with calls routed through Shizuku so they carry the shell identity.
     */
    fun shizukuInterface(serviceName: String, interfaceName: String): Any {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, serviceName) as IBinder
        return Class.forName("$interfaceName\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(binder))!!
    }
}
