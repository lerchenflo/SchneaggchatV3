package org.lerchenflo.schneaggchatv3mp.testutil

import eu.anifantakis.lib.ksafe.KSafe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Stand-in for a constructor dependency that the code path under test never uses. Every call
 * on it throws, so a test fails loudly as soon as the path starts using it (then replace the
 * stub with a real object or a fake). Saves writing big fakes for interfaces like
 * `UserRepository` that a send-path test never touches.
 *
 * Works for interfaces only (`java.lang.reflect.Proxy`), which is why it lives in `jvmTest`.
 */
inline fun <reified T : Any> unusedDependency(): T = unusedDependency(T::class.java)

fun <T : Any> unusedDependency(type: Class<T>): T {
    require(type.isInterface) { "${type.name} is not an interface; unusedDependency() only stubs interfaces" }

    val handler = InvocationHandler { proxy, method, args ->
        when {
            method.name == "toString" && method.parameterCount == 0 -> "unusedDependency<${type.simpleName}>"
            method.name == "hashCode" && method.parameterCount == 0 -> System.identityHashCode(proxy)
            method.name == "equals" && method.parameterCount == 1 -> proxy === args?.get(0)
            else -> throw UnsupportedOperationException(
                "${type.simpleName}.${method.name}() was called, but this test stubbed ${type.simpleName} " +
                    "as unused. Give the test a real object or a fake for it."
            )
        }
    }

    return type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type), handler))
}

/**
 * A [KSafe] whose constructor never ran: it touches no files and no key store, and every call on
 * it fails. `Preferencemanager` needs one, but only its token/own-id methods use it. Use this only
 * for code paths that never read or write secure storage.
 */
fun unusableKSafe(): KSafe {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
    return allocateInstance.invoke(unsafe, KSafe::class.java) as KSafe
}
