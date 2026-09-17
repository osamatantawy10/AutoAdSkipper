package com.autoadskipper.accessibility

import com.autoadskipper.data.model.DetectionLayer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers, per package, which detection layer + identifying value (a
 * resource-id or exact text) last succeeded. On the next event for that
 * package, [AdDetector] tries the cached hint first before falling back to a
 * full tree walk — this is the "cache previously detected elements" /
 * "avoid scanning the entire UI unnecessarily" requirement.
 *
 * Deliberately in-memory only (cleared when the service process restarts):
 * ad-network overlays change often enough that a persisted cache would go
 * stale and cost more than it saves.
 */
@Singleton
class DetectionCache @Inject constructor() {

    data class Hint(val layer: DetectionLayer, val value: String)

    private val cache = LinkedHashMap<String, Hint>()
    private val maxEntries = 50

    @Synchronized
    fun get(packageName: String): Hint? = cache[packageName]

    @Synchronized
    fun put(packageName: String, hint: Hint) {
        if (cache.size >= maxEntries && !cache.containsKey(packageName)) {
            val oldestKey = cache.keys.firstOrNull()
            if (oldestKey != null) cache.remove(oldestKey)
        }
        cache[packageName] = hint
    }

    @Synchronized
    fun clear() = cache.clear()
}
