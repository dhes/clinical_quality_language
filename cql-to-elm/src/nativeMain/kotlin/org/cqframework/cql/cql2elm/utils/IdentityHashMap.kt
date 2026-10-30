package org.cqframework.cql.cql2elm.utils

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

/** A key compared by reference. */
private class IdentityKey<K : Any>(val key: K) {
    override fun equals(other: Any?): Boolean = other is IdentityKey<*> && other.key === key

    @OptIn(ExperimentalNativeApi::class) override fun hashCode(): Int = key.identityHashCode()
}

internal actual class IdentityHashMap<K : Any, V : Any> actual constructor() {
    private val backingMap = LinkedHashMap<IdentityKey<K>, V>()

    actual operator fun get(key: K): V? = backingMap[IdentityKey(key)]

    actual operator fun set(key: K, value: V) {
        backingMap[IdentityKey(key)] = value
    }

    actual operator fun iterator(): Iterator<Map.Entry<K, V>> =
        backingMap.entries
            .map { (k, v) ->
                object : Map.Entry<K, V> {
                    override val key: K = k.key
                    override val value: V = v
                }
            }
            .iterator()

    actual fun getOrPut(key: K, defaultValue: () -> V): V =
        backingMap.getOrPut(IdentityKey(key), defaultValue)
}
