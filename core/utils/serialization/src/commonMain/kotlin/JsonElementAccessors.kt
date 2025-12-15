/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.utils.serialization

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

val JsonElement.jsonObjectOrNull get() = (this as? JsonObject)

fun JsonObject.getOrFail(key: String): JsonElement {
    return get(key) ?: throw NoSuchElementException("key $key not found")
}

fun JsonObject.getIntOrFail(key: String): Int {
    val field = get(key)?.jsonPrimitive ?: throw NoSuchElementException("key $key not found")
    return field.intOrNull ?: throw IllegalStateException("Field $key is not an int: $field")
}

fun JsonObject.getString(key: String): String? {
    return get(key)?.jsonPrimitive?.content
}

fun JsonObject.getStringOrFail(key: String): String {
    return getString(key) ?: throw NoSuchElementException("key $key not found")
}

fun JsonObject.getBooleanOrFail(key: String): Boolean {
    val field = get(key)?.jsonPrimitive ?: throw NoSuchElementException("key $key not found")
    return field.booleanOrNull ?: throw IllegalStateException("Field $key is not a boolean: $field")
}
