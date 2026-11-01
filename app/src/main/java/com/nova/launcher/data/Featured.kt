package com.nova.launcher.data

/**
 * The four hero favourites. Apps are matched against what is actually installed using
 * label and package keywords; nothing depends on an exact package ID.
 */
enum class Featured(
    val title: String,
    val tagline: String,
    private val labelKeys: List<String>,
    private val packageKeys: List<String>,
    private val exclude: List<String> = emptyList(),
) {
    NUVIO("Nuvio", "Your streaming universe", listOf("nuvio"), listOf("nuvio")),
    YOUTUBE(
        "YouTube", "Watch what moves you", listOf("youtube"), listOf("youtube"),
        exclude = listOf("kids", "music", "studio", "unplugged", "youtube tv", "create"),
    ),
    NETFLIX("Netflix", "Stories without end", listOf("netflix"), listOf("netflix")),
    PRIME_VIDEO(
        "Prime Video", "Cinema, delivered", listOf("prime video", "primevideo"),
        listOf("primevideo", "amazonvideo"),
    );

    fun match(apps: List<AppEntry>): AppEntry? = apps.mapNotNull { app ->
        val l = app.label.lowercase()
        val p = app.packageName.lowercase()
        if (exclude.any { l.contains(it) || p.contains(it.replace(" ", "")) }) return@mapNotNull null
        val score = when {
            labelKeys.any { l == it } -> 100
            labelKeys.any { l.startsWith(it) } -> 80
            labelKeys.any { l.contains(it) } -> 60
            packageKeys.any { p.contains(it) } -> 40
            else -> 0
        }
        if (score == 0) null else app to score + (if (app.isTvApp) 5 else 0)
    }.maxByOrNull { it.second }?.first
}

data class FeaturedSlot(val featured: Featured, val app: AppEntry?)
