package `in`.sreerajp.vault_files.ui

import android.content.Context
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap

/**
 * Helpers for building share intents.
 *
 * The tricky part is Bluetooth. Android's Bluetooth send activity declares a fixed allow-list of
 * MIME types in its intent filter (image, video, audio, vcard, text, zip, pdf, MS Office, x-hwp).
 * A file whose real type is outside that list — an APK, a 7z archive, an e-book — makes the system
 * chooser drop Bluetooth, even though the Bluetooth transfer code itself handles any file fine.
 * We keep sending the correct type (other apps need it) and add Bluetooth back as an explicit
 * chooser entry when it would otherwise be missing. See [bluetoothInitialIntents].
 */

/** Fallback used when we cannot work out a file's type at all. */
const val FALLBACK_MIME_TYPE = "*/*"

/**
 * Types the platform [MimeTypeMap] does not know on every Android version. Kept small: only
 * extensions a file manager realistically runs into.
 */
private val EXTRA_MIME_TYPES = mapOf(
    "apk" to "application/vnd.android.package-archive",
    "7z" to "application/x-7z-compressed",
    "rar" to "application/vnd.rar",
    "tar" to "application/x-tar",
    "gz" to "application/gzip",
    "bz2" to "application/x-bzip2",
    "xz" to "application/x-xz",
    "md" to "text/markdown",
    "log" to "text/plain",
    "json" to "application/json",
    "yaml" to "application/x-yaml",
    "yml" to "application/x-yaml",
    "ini" to "text/plain",
    "csv" to "text/csv",
    "epub" to "application/epub+zip",
    "mobi" to "application/x-mobipocket-ebook",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "webp" to "image/webp",
    "avif" to "image/avif",
    "opus" to "audio/opus",
    "flac" to "audio/flac",
    "mkv" to "video/x-matroska",
    "torrent" to "application/x-bittorrent",
)

/**
 * Best MIME type for a file called [name]: the platform table first, then our own additions, then
 * [fallback]. The wildcard type is the right fallback for "open with" (it matches the most apps);
 * share code passes the same default because a wrong concrete type is worse than a wildcard.
 */
fun mimeTypeForName(name: String, fallback: String = FALLBACK_MIME_TYPE): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    if (ext.isEmpty()) return fallback
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        ?: EXTRA_MIME_TYPES[ext]
        ?: fallback
}

/**
 * The `type` to put on a share intent for [names]. One file keeps its own type. Many files keep the
 * shared `image/`-style family when they all agree, so image-only or video-only batches still
 * match apps that filter on it; a mixed batch falls back to the wildcard type.
 */
fun shareTypeFor(names: List<String>): String {
    if (names.isEmpty()) return FALLBACK_MIME_TYPE
    val types = names.map { mimeTypeForName(it) }
    types.distinct().singleOrNull()?.let { return it }
    val prefixes = types.map { it.substringBefore('/') }.distinct()
    val prefix = prefixes.singleOrNull() ?: return FALLBACK_MIME_TYPE
    return if (prefix == "*") FALLBACK_MIME_TYPE else "$prefix/*"
}

/**
 * An explicit Bluetooth send intent to pass as [Intent.EXTRA_INITIAL_INTENTS], or `null` when none
 * is needed.
 *
 * Returns `null` when Bluetooth already resolves for [base]'s type (we must not add a second
 * Bluetooth row) or when no Bluetooth send activity exists at all. Otherwise it copies [base] and
 * pins the component, which skips intent-filter matching entirely, and grants the Bluetooth package
 * read access to [uris] so it can read our `FileProvider` content.
 */
fun bluetoothInitialIntents(context: Context, base: Intent, uris: List<Uri>): Array<Intent>? {
    val pm = context.packageManager
    // Probe with a type Bluetooth always accepts to locate its send activity.
    val probe = Intent(base.action).setType("image/*")
    val bluetooth = pm.queryIntentActivities(probe, 0)
        .firstOrNull { it.activityInfo?.packageName?.contains("bluetooth", ignoreCase = true) == true }
        ?.activityInfo ?: return null

    val alreadyListed = pm.queryIntentActivities(base, 0)
        .any { it.activityInfo?.packageName == bluetooth.packageName }
    if (alreadyListed) return null

    uris.forEach {
        try {
            context.grantUriPermission(bluetooth.packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Not our provider (or already revoked) — the flag on the intent still covers the
            // common case, so keep going rather than losing the Bluetooth entry.
        }
    }

    val explicit = Intent(base).apply {
        setClassName(bluetooth.packageName, bluetooth.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // The chooser normally builds this itself from EXTRA_STREAM; set it here too so the
        // read grant travels with the intent when it is launched as an initial intent.
        uris.firstOrNull()?.let { first ->
            val description = ClipDescription("files", arrayOf(base.type ?: FALLBACK_MIME_TYPE))
            clipData = ClipData(description, ClipData.Item(first)).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        }
    }
    return arrayOf(explicit)
}
