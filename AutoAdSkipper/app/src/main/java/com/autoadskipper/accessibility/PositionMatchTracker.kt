package com.autoadskipper.accessibility

import android.graphics.Rect
import com.autoadskipper.utils.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Layer 3 (position heuristic) is the least precise detection layer, so it
 * requires the same bounds to reappear for a package across several
 * consecutive accessibility events before [AdDetector] is allowed to act on
 * it. This class holds that small piece of rolling state.
 */
@Singleton
class PositionMatchTracker @Inject constructor() {

    private data class State(val bounds: Rect, val count: Int)

    private val state = HashMap<String, State>()

    @Synchronized
    fun confirm(packageName: String, bounds: Rect): Boolean {
        val existing = state[packageName]
        val sameSpot = existing != null && existing.bounds == bounds

        val newCount = if (sameSpot) (existing!!.count + 1) else 1
        state[packageName] = State(bounds, newCount)

        return newCount >= Constants.POSITION_MATCH_CONFIRMATIONS_REQUIRED
    }

    @Synchronized
    fun reset(packageName: String) {
        state.remove(packageName)
    }
}
