package com.jarvis.mobile.tools

import android.content.Context
import com.google.gson.JsonObject

interface JarvisTool {
    val name: String
    fun declaration(): JsonObject
    suspend fun execute(ctx: Context, args: Map<String, Any?>): String
}

fun functionDecl(name: String, description: String, builder: ParamsBuilder.() -> Unit = {}): JsonObject {
    val params = ParamsBuilder().apply(builder).build()
    return JsonObject().apply {
        addProperty("name", name)
        addProperty("description", description)
        add("parameters", params)
    }
}

class ParamsBuilder {
    private val props = JsonObject()
    private val required = mutableListOf<String>()

    fun str(name: String, description: String, required: Boolean = false) {
        props.add(name, JsonObject().apply {
            addProperty("type", "STRING")
            addProperty("description", description)
        })
        if (required) this.required += name
    }

    fun int(name: String, description: String, required: Boolean = false) {
        props.add(name, JsonObject().apply {
            addProperty("type", "INTEGER")
            addProperty("description", description)
        })
        if (required) this.required += name
    }

    fun bool(name: String, description: String, required: Boolean = false) {
        props.add(name, JsonObject().apply {
            addProperty("type", "BOOLEAN")
            addProperty("description", description)
        })
        if (required) this.required += name
    }

    fun build(): JsonObject = JsonObject().apply {
        addProperty("type", "OBJECT")
        add("properties", props)
        if (required.isNotEmpty()) {
            val arr = com.google.gson.JsonArray()
            required.forEach { arr.add(it) }
            add("required", arr)
        }
    }
}
