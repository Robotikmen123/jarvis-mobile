package com.jarvis.mobile.tools

import android.content.Context
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject

object ToolDispatcher {
    private const val TAG = "ToolDispatcher"

    private val tools: Map<String, JarvisTool> = listOf(
        OpenAppTool,
        WeatherTool,
        WebSearchTool,
        SendMessageTool,
        ReminderTool,
        YouTubeTool,
        SaveMemoryTool,
        ShutdownTool,
        // Hardware controls
        VolumeTool,
        BrightnessTool,
        FlashlightTool,
        BatteryTool,
        DndTool,
        VibrateTool,
        // Intent helpers
        CallTool,
        CalendarTool,
        TimerTool,
        AlarmTool,
        CameraTool,
        ClipboardTool,
        NotesTool,
        PhoneStatusTool,
        SettingsTool,
    ).associateBy { it.name }

    fun declarationsJson(): JsonObject {
        val arr = JsonArray()
        tools.values.forEach { arr.add(it.declaration()) }
        return JsonObject().apply { add("function_declarations", arr) }
    }

    suspend fun dispatch(ctx: Context, name: String, args: Map<String, Any?>): String {
        val tool = tools[name] ?: return "Unknown tool: $name"
        Log.i(TAG, "→ $name $args")
        return runCatching { tool.execute(ctx, args) }
            .getOrElse {
                Log.e(TAG, "Tool $name failed", it)
                "Tool $name failed: ${it.message}"
            }
    }
}
