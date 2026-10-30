package org.cqframework.cql.shared

import kotlin.annotation.AnnotationTarget.*

@Target(CLASS, PROPERTY, FUNCTION, FILE) actual annotation class JsOnlyExport

actual typealias BigDecimal = BigDecimalJs

actual typealias RoundingMode = RoundingModeJs

actual typealias QName = QNameJs
