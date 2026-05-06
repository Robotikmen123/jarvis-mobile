package com.jarvis.mobile.live

import android.content.Context
import com.jarvis.mobile.memory.MemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Welcome {

    suspend fun build(ctx: Context): String = withContext(Dispatchers.IO) {
        val address = MemoryStore.getValue(ctx, "preferences", "address_preference") ?: "efendim"
        "Hosgeldiniz $address."
    }
}
