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
import com.nova.launcher.adb.HomeTakeover
import com.nova.launcher.data.AppEntry
import com.nova.launcher.data.AppRepository
import com.nova.launcher.data.FavouritesStore
import com.nova.launcher.data.Featured
import com.nova.launcher.data.FeaturedSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeState(
    val loading: Boolean = true,
    val tvApps: List<AppEntry> = emptyList(),
    val otherApps: List<AppEntry> = emptyList(),
    val featured: List<FeaturedSlot> = Featured.entries.map { FeaturedSlot(it, null) },
    val pinned: List<AppEntry> = emptyList(),
    val pinnedPackages: Set<String> = emptySet(),
)

data class SetupState(
    val novaIsHome: Boolean = false,
    val stockHomes: List<HomeTakeover.StockHome> = emptyList(),
    val stockDisabled: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)
    private val favourites = FavouritesStore(app)
    private val takeover = HomeTakeover(app)
    private val apps = MutableStateFlow<List<AppEntry>?>(null)
    private var lastFingerprint: Set<String> = emptySet()
    private var loadJob: Job? = null

    private val _setup = MutableStateFlow(SetupState())
    val setup: StateFlow<SetupState> = _setup

    val state: StateFlow<HomeState> = combine(apps, favourites.pinned) { list, pinned ->
        val all = list.orEmpty()
        val featured = Featured.entries.map { FeaturedSlot(it, it.match(all)) }
        val featuredPkgs = featured.mapNotNull { it.app?.packageName }.toSet()
        val byPkg = all.associateBy { it.packageName }
        HomeState(
            loading = list == null,
            tvApps = all.filter { it.isTvApp },
            otherApps = all.filter { !it.isTvApp },
            featured = featured,
            pinned = pinned.filter { it !in featuredPkgs }.mapNotNull { byPkg[it] },
            pinnedPackages = pinned.toSet(),
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

    /** All PackageManager work happens off the main thread so returning Home never stutters. */
    fun refresh(force: Boolean = false) {
        if (loadJob?.isActive == true && !force) return
        loadJob = viewModelScope.launch {
            val fp = withContext(Dispatchers.IO) { runCatching { repo.fingerprint() }.getOrDefault(emptySet()) }
            if (force || fp != lastFingerprint || apps.value == null) {
                lastFingerprint = fp
                apps.value = repo.loadApps()
            }
            refreshSetup()
        }
    }

    private suspend fun refreshSetup() {
        val (isHome, stock) = withContext(Dispatchers.IO) {
            takeover.isNovaDefaultHome() to takeover.stockHomes()
        }
        _setup.value = _setup.value.copy(
            novaIsHome = isHome,
            stockHomes = stock,
            stockDisabled = stock.isNotEmpty() && stock.all { takeover.isDisabled(it.packageName) },
        )
    }

    fun launch(app: AppEntry) { repo.launch(app) }

    fun togglePinned(app: AppEntry) = favourites.toggle(app.packageName)

    fun openAppInfo(app: AppEntry) {
        repo.start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
    }

    fun findApp(featured: Featured) {
        val q = Uri.encode(featured.title)
        if (!repo.start(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$q&c=apps")))) {
            repo.start(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$q&c=apps")))
        }
    }

    fun openSettings() {
        repo.start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openDeveloperOptions() {
        if (!repo.start(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))) {
            repo.start(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) || repo.start(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openClassicHome() {
        val home = _setup.value.stockHomes.firstOrNull { !takeover.isDisabled(it.packageName) } ?: return
        HomeBypass.active = true
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            component = ComponentName(home.packageName, home.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!repo.start(intent)) HomeBypass.active = false
    }

    fun makeNovaHome() = runSetup { takeover.makeNovaHome() }

    fun restoreOriginalHome() = runSetup { takeover.restore() }

    fun clearSetupMessage() {
        _setup.value = _setup.value.copy(message = null, isError = false)
    }

    private fun runSetup(action: suspend () -> Result<String>) {
        if (_setup.value.busy) return
        _setup.value = _setup.value.copy(busy = true, message = "Connecting… if the TV asks, choose Allow (tick “Always allow”).", isError = false)
        viewModelScope.launch {
            val r = action()
            refreshSetup()
            _setup.value = _setup.value.copy(
                busy = false,
                message = r.getOrNull() ?: r.exceptionOrNull()?.message,
                isError = r.isFailure,
            )
        }
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
    }
}
