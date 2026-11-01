package com.nova.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nova.launcher.data.AppEntry
import com.nova.launcher.data.AppRepository
import com.nova.launcher.data.FavouritesStore
import com.nova.launcher.data.Featured
import com.nova.launcher.data.FeaturedSlot
import com.nova.launcher.service.HomeButtonService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val featured: List<FeaturedSlot> = Featured.entries.map { FeaturedSlot(it, null) },
    val pinned: List<AppEntry> = emptyList(),
    val pinnedPackages: Set<String> = emptySet(),
    /** True when NOVA is neither the default Home app nor has its Home button service on. */
    val showHomeSetup: Boolean = false,
    val hasClassicHome: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)
    private val favourites = FavouritesStore(app)
    private val apps = MutableStateFlow<List<AppEntry>?>(null)
    private val homeInfo = MutableStateFlow(false to false)
    private var lastFingerprint: Set<String> = emptySet()
    private var loadJob: Job? = null

    val state: StateFlow<HomeState> = combine(apps, favourites.pinned, homeInfo) { list, pinned, home ->
        val all = list.orEmpty()
        val featured = Featured.entries.map { FeaturedSlot(it, it.match(all)) }
        val featuredPkgs = featured.mapNotNull { it.app?.packageName }.toSet()
        val byPkg = all.associateBy { it.packageName }
        HomeState(
            loading = list == null,
            apps = all,
            featured = featured,
            pinned = pinned.filter { it !in featuredPkgs }.mapNotNull { byPkg[it] },
            pinnedPackages = pinned.toSet() + featuredPkgs,
            showHomeSetup = home.first,
            hasClassicHome = home.second,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh(force = true)
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(app, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        refresh(force = true)
    }

    fun refresh(force: Boolean = false) {
        homeInfo.value = computeHomeInfo()
        val fp = runCatching { repo.fingerprint() }.getOrDefault(emptySet())
        if (!force && fp == lastFingerprint && apps.value != null) return
        lastFingerprint = fp
        loadJob?.cancel()
        loadJob = viewModelScope.launch { apps.value = repo.loadApps() }
    }

    fun launch(app: AppEntry) { repo.launch(app) }

    fun togglePinned(app: AppEntry) = favourites.toggle(app.packageName)

    fun openAppInfo(app: AppEntry) {
        repo.start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
    }

    /** Opens the store page search for a favourite that isn't installed yet. */
    fun findApp(featured: Featured) {
        val q = Uri.encode(featured.title)
        repo.start(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$q&c=apps"))) ||
            repo.start(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$q&c=apps")))
    }

    fun openSettings() {
        repo.start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openHomeSetup() {
        repo.start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) || repo.start(Intent(Settings.ACTION_SETTINGS))
    }

    /** Hands over to the TV's original launcher (fallback). */
    fun openClassicHome() {
        val home = repo.otherHomeApps().firstOrNull() ?: return
        HomeBypass.active = true
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            component = ComponentName(home.activityInfo.packageName, home.activityInfo.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!repo.start(intent)) HomeBypass.active = false
    }

    private fun computeHomeInfo(): Pair<Boolean, Boolean> {
        val ctx = getApplication<Application>()
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val defaultHome = runCatching {
            ctx.packageManager.resolveActivity(homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.getOrNull()
        val isDefault = defaultHome == ctx.packageName
        val serviceOn = HomeButtonService.isEnabled(ctx)
        val hasClassic = runCatching { repo.otherHomeApps().isNotEmpty() }.getOrDefault(false)
        return (!isDefault && !serviceOn) to hasClassic
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
    }
}
