package com.bhardwaj.passkey.banner

import com.bhardwaj.passkey.core.SafeUrl
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

enum class AnnouncementStyle(val analyticsName: String) {
    INFO("info"),
    PROMO("promo"),
    SUCCESS("success"),
    WARNING("warning")
}

/**
 * One announcement, already validated. Built only by [AnnouncementParser] from Remote Config `announcement_json`.
 * The CTA shows only when there is a label and a target ([url] or [route]).
 */
data class Announcement(
    val id: String,
    val title: String,
    val body: String = "",
    val ctaLabel: String? = null,
    /** https only; anything else is dropped at parse time. */
    val url: String? = null,
    /** In-app destination; the app maps it (unknown routes are ignored). Wins over [url]. */
    val route: String? = null,
    val style: AnnouncementStyle = AnnouncementStyle.INFO,
    val startAtMs: Long? = null,
    val endAtMs: Long? = null,
    val minVersionCode: Long? = null,
    val maxVersionCode: Long? = null,
    val dismissible: Boolean = true
) {
    val hasCta: Boolean get() = !ctaLabel.isNullOrBlank() && (route != null || url != null)
}

/**
 * Wire format (object, or an array of objects; the first eligible one is shown):
 * ```json
 * {"id":"spring-26","title":"New levels!","body":"50 fresh puzzles are live.","cta":"Play now",
 *  "route":"daily","url":"https://example.com","style":"promo","start":"2026-11-01T00:00:00Z",
 *  "end":"2026-11-15","min_version":12,"max_version":40,"dismissible":true}
 * ```
 * `start`/`end` accept an ISO instant, an offset date-time, a date (UTC midnight) or epoch millis.
 * `style`: info | promo | success | warning (unknown values fall back to info).
 */
@Serializable
private data class AnnouncementDto(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val cta: String? = null,
    val url: String? = null,
    val route: String? = null,
    val style: String? = null,
    val start: String? = null,
    val end: String? = null,
    @SerialName("min_version") val minVersion: Long? = null,
    @SerialName("max_version") val maxVersion: Long? = null,
    val dismissible: Boolean = true
)

object AnnouncementParser {
    const val MAX_TITLE = 80
    const val MAX_BODY = 240
    const val MAX_CTA = 24

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    /** Never throws: blank, malformed or invalid entries are skipped. */
    fun parse(raw: String): List<Announcement> {
        if (raw.isBlank()) return emptyList()
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return emptyList()
        val items: List<JsonElement> =
            when (root) {
                is JsonArray -> root
                is JsonObject -> listOf(root)
                else -> emptyList()
            }
        return items.mapNotNull { item ->
            runCatching { json.decodeFromJsonElement(AnnouncementDto.serializer(), item) }.getOrNull()?.toDomain()
        }
    }

    private fun AnnouncementDto.toDomain(): Announcement? {
        val startMs = start?.let { parseTime(it) }
        val endMs = end?.let { parseTime(it) }
        val valid =
            id.isNotBlank() &&
                title.isNotBlank() &&
                '\n' !in id &&
                (start == null || startMs != null) &&
                (end == null || endMs != null) &&
                (startMs == null || endMs == null || endMs > startMs)
        if (!valid) return null
        return Announcement(
            id = id.trim(),
            title = title.trim().take(MAX_TITLE),
            body = body.trim().take(MAX_BODY),
            ctaLabel = cta?.trim()?.take(MAX_CTA)?.takeIf { it.isNotEmpty() },
            url = url?.trim()?.takeIf { SafeUrl.isHttps(it) },
            route = route?.trim()?.takeIf { it.isNotEmpty() },
            style = AnnouncementStyle.entries.find { it.analyticsName == style?.trim()?.lowercase() }
                ?: AnnouncementStyle.INFO,
            startAtMs = startMs,
            endAtMs = endMs,
            minVersionCode = minVersion,
            maxVersionCode = maxVersion,
            dismissible = dismissible
        )
    }

    internal fun parseTime(value: String): Long? {
        val text = value.trim()
        if (text.isEmpty()) return null
        text.toLongOrNull()?.let { return it }
        return runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
    }
}
