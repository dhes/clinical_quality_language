package org.opencds.cqf.cql.engine.util

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

@OptIn(ExperimentalNativeApi::class)
actual fun identityHashCode(value: Any): Int {
    return value.identityHashCode()
}
