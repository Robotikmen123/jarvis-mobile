package com.jarvis.mobile.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Look up a contact's phone number by name without dialing — useful for
 * messaging tools or when the model wants to confirm before calling.
 */
object FindContactTool : JarvisTool {
    override val name = "find_contact"

    override fun declaration() = functionDecl(
        name,
        "Look up a contact by name and return their phone number(s). " +
        "Use this before send_message when the user gives a name."
    ) {
        str("name", "Contact name, partial match OK", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val q = (args["name"] as? String)?.trim().orEmpty()
        if (q.isEmpty()) return "Isim eksik."
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return "Rehber izni yok."
        }
        val cleaned = q.replace("'", " ").replace("%", "")
        val cur = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? COLLATE NOCASE",
            arrayOf("%$cleaned%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        ) ?: return "Rehber sorgusu basarisiz."
        cur.use { c ->
            if (!c.moveToFirst()) return "Rehberde \"$q\" yok."
            val out = mutableListOf<String>()
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx  = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            do {
                out += "${c.getString(nameIdx)}: ${c.getString(numIdx)}"
                if (out.size >= 5) break
            } while (c.moveToNext())
            return out.joinToString("; ")
        }
    }
}
