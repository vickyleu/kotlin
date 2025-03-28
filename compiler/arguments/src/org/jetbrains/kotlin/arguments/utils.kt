/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import org.jetbrains.kotlin.arguments.types.BooleanType

val BooleanType.Companion.defaultFalse: BooleanType
    get() = BooleanType(
        isNullable = false.asReleaseDependent(),
        defaultValue = false.asReleaseDependent()
    )

val BooleanType.Companion.defaultTrue: BooleanType
    get() = BooleanType(
        isNullable = false.asReleaseDependent(),
        defaultValue = true.asReleaseDependent()
    )

val BooleanType.Companion.defaultNull: BooleanType
    get() = BooleanType(
        isNullable = true.asReleaseDependent(),
        defaultValue = null.asReleaseDependent()
    )
