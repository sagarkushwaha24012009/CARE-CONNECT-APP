package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * HashRouter-style primary route descriptors for Care Connect's top-level NavHost.
 * Each route has both a canonical hash identifier (`#/...`) for HashRouter state tracking
 * and a URI-safe [navRoute] for AndroidX Navigation Compose `NavHost` matching.
 */
enum class PrimaryHashRoute(
    val hash: String,
    val navRoute: String
) {
    SPLASH(hash = "#/splash", navRoute = "primary/splash"),
    AUTH(hash = "#/auth", navRoute = "primary/auth"),
    ELDER_DASHBOARD(hash = "#/elder/dashboard", navRoute = "primary/elder_dashboard"),
    CARETAKER_DASHBOARD(hash = "#/caretaker/dashboard", navRoute = "primary/caretaker_dashboard");

    companion object {
        const val WORKSPACE_GRAPH_ROUTE = "primary/workspace"

        fun fromHash(rawHash: String?): PrimaryHashRoute? {
            if (rawHash.isNullOrBlank()) return null
            val normalized = normalizeHash(rawHash)
            return entries.firstOrNull { it.hash.equals(normalized, ignoreCase = true) }
        }

        fun fromNavRoute(navRoute: String?): PrimaryHashRoute? {
            if (navRoute.isNullOrBlank()) return null
            return entries.firstOrNull { it.navRoute == navRoute }
        }

        fun forRole(role: UserRole): PrimaryHashRoute {
            return when (role) {
                UserRole.ELDER -> ELDER_DASHBOARD
                UserRole.CARETAKER -> CARETAKER_DASHBOARD
            }
        }

        fun normalizeHash(rawHash: String): String {
            val trimmed = rawHash.trim()
            return when {
                trimmed.startsWith("#/") -> trimmed.lowercase()
                trimmed.startsWith("#") -> "#/" + trimmed.removePrefix("#").trimStart('/').lowercase()
                trimmed.startsWith("/") -> "#$trimmed".lowercase()
                else -> "#/$trimmed".lowercase()
            }
        }
    }
}

/**
 * Pure transition resolver mapping authentication, onboarding, and role state
 * to the canonical [PrimaryHashRoute].
 */
fun resolveTargetPrimaryHash(
    isAuthResolved: Boolean,
    isAuthenticated: Boolean,
    onboardingCompleted: Boolean,
    activeRole: UserRole,
    viewingGuide: Boolean = false
): PrimaryHashRoute {
    if (!isAuthResolved) return PrimaryHashRoute.SPLASH
    if (!isAuthenticated) {
        return if (!onboardingCompleted || viewingGuide) {
            PrimaryHashRoute.SPLASH
        } else {
            PrimaryHashRoute.AUTH
        }
    }
    return PrimaryHashRoute.forRole(activeRole)
}

/**
 * HashRouter-like state container that tracks the current hash (`#/...`),
 * exposes observable [currentHash] / [currentRoute] flows, and maintains a
 * hash history stack synchronized with the primary `NavHost`.
 */
@Stable
class HashRouterState(
    initialRoute: PrimaryHashRoute = PrimaryHashRoute.SPLASH
) {
    private val _currentHash = MutableStateFlow(initialRoute.hash)
    val currentHash: StateFlow<String> = _currentHash.asStateFlow()

    private val _currentRoute = MutableStateFlow(initialRoute)
    val currentRoute: StateFlow<PrimaryHashRoute> = _currentRoute.asStateFlow()

    private val historyStack = ArrayDeque<PrimaryHashRoute>().apply {
        addLast(initialRoute)
    }

    val historyDepth: Int
        get() = historyStack.size

    val canGoBack: Boolean
        get() = historyStack.size > 1

    fun navigate(target: PrimaryHashRoute, replace: Boolean = false) {
        if (_currentRoute.value == target) return
        if (replace && historyStack.isNotEmpty()) {
            historyStack.removeLast()
        }
        if (historyStack.lastOrNull() != target) {
            historyStack.addLast(target)
        }
        _currentRoute.value = target
        _currentHash.value = target.hash
    }

    fun navigateToHash(rawHash: String, replace: Boolean = false): PrimaryHashRoute? {
        val target = PrimaryHashRoute.fromHash(rawHash) ?: return null
        navigate(target, replace = replace)
        return target
    }

    fun syncFromNavRoute(navRoute: String?) {
        val route = PrimaryHashRoute.fromNavRoute(navRoute) ?: return
        if (_currentRoute.value != route) {
            _currentRoute.value = route
            _currentHash.value = route.hash
            val existingIndex = historyStack.indexOfLast { it == route }
            if (existingIndex >= 0) {
                while (historyStack.size - 1 > existingIndex) {
                    historyStack.removeLast()
                }
            } else {
                historyStack.addLast(route)
            }
        }
    }

    fun popBack(): PrimaryHashRoute? {
        if (historyStack.size <= 1) return null
        historyStack.removeLast()
        val previous = historyStack.last()
        _currentRoute.value = previous
        _currentHash.value = previous.hash
        return previous
    }
}

@Composable
fun rememberHashRouterState(
    initialRoute: PrimaryHashRoute = PrimaryHashRoute.SPLASH
): HashRouterState {
    return remember { HashRouterState(initialRoute) }
}
