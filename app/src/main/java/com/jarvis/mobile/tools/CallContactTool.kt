package com.jarvis.mobile.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Call someone by their contact name. Looks the number up in the system
 * Contacts provider and opens the dialer pre-filled. Prefers the mobile
 * number when the contact has multiple.
 */
object CallContactTool : JarvisTool {
    override val name = "call_contact"

    override fun declaration() = functionDecl(
        name,
        "Find a contact by name and open the dialer with their number. " +
        "Use this whenever the user says 'call <name>' / '<isim>i ara'. " +
        "Only fall back to call_number when the user provides a raw phone number."
    ) {
        str("name", "Contact name; partial match is OK (case-insensitive).", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val q = (args["name"] as? String)?.trim().orEmpty()
        if (q.isEmpty()) return "Isim eksik."
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return "Rehber izni yok. JARVIS'i acip izin verir misin? (Settings → Apps → JARVIS → Permissions → Contacts)"
        }
        val match = lookup(ctx, q) ?: return "Rehberde \"$q\" bulunamadi."
        val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(match.number)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            ctx.startActivity(dial)
            "Cevirici acildi: ${match.name} (${match.number})"
        }.getOrElse { "Cevirici acilamadi: ${it.message}" }
    }

    private data class Match(val name: String, val number: String)

    private fun lookup(ctx: Context, query: String): Match? {
        val cleaned = query.replace("'", " ").replace("%", "")
        val proj = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
        )
        val cur = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            proj,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? COLLATE NOCASE",
            arrayOf("%$cleaned%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        ) ?: return null
        cur.use { c ->
            if (!c.moveToFirst()) return null
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx  = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)

            // First pass: prefer TYPE_MOBILE.
            do {
                if (c.getInt(typeIdx) == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                    return Match(c.getString(nameIdx), c.getString(numIdx))
                }
            } while (c.moveToNext())
            // Fallback: first row regardless of type.
            c.moveToFirst()
            return Match(c.getString(nameIdx), c.getString(numIdx))
        }
    }
}
