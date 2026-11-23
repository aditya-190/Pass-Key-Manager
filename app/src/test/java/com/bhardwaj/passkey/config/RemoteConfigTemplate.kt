package com.bhardwaj.passkey.config

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** The deployable `<project>/firebase/remoteconfig.template.json` as `name -> (default, valueType)`. */
object RemoteConfigTemplate {
    val params: Map<String, Pair<String, String>> by lazy {
        // Unit tests run with the module dir (app/) as the working dir.
        val root = Json.parseToJsonElement(File("../firebase/remoteconfig.template.json").readText()).jsonObject
        val params = mutableMapOf<String, JsonObject>()
        root["parameters"]?.jsonObject?.forEach { (k, v) -> params[k] = v.jsonObject }
        root["parameterGroups"]?.jsonObject?.values?.forEach { group ->
            group.jsonObject["parameters"]?.jsonObject?.forEach { (k, v) -> params[k] = v.jsonObject }
        }
        params.mapValues { (_, v) ->
            v.getValue("defaultValue").jsonObject.getValue("value").jsonPrimitive.content to
                v.getValue("valueType").jsonPrimitive.content
        }
    }

    /** Every key is in the template with the in-app default and the matching Remote Config type. */
    fun assertMatches(keys: List<ConfigKey<*>>) {
        keys.forEach { key ->
            assertTrue("${key.name} missing from remoteconfig.template.json", key.name in params)
            val (value, type) = params.getValue(key.name)
            val expectedType =
                when (key) {
                    is ConfigKey.LongKey -> "NUMBER"
                    is ConfigKey.BoolKey -> "BOOLEAN"
                    is ConfigKey.StringKey -> "STRING"
                }
            assertEquals(key.name, expectedType, type)
            assertEquals(key.name, key.default.toString(), value)
        }
    }
}
