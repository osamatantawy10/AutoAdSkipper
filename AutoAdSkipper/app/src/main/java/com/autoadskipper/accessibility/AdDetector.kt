package com.autoadskipper.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.autoadskipper.data.model.DetectionLayer
import com.autoadskipper.utils.Constants

/**
 * Pure(ish) logic for finding a "skip ad" style node within an accessibility
 * node tree. Kept separate from the Service class so it can be unit tested
 * without a running Android service.
 *
 * Layers, tried in order, cheapest/most-precise first:
 *  1. TEXT               — label matches a known skip phrase (any language).
 *  2. RESOURCE_ID          — the node's view-id contains a known skip-button id fragment.
 *  3. CLOSE_EXPLICIT      — text explicitly references an ad ("Close Ad") — low
 *                          ambiguity, acted on directly like layers 1-2.
 *  3b. CLOSE_GENERIC       — text/content-description is a generic word ("Close",
 *                          "OK", "Continue", "Done") that appears constantly in
 *                          totally unrelated Android UI (permission dialogs,
 *                          onboarding, ToS screens...). NEVER acted on by text
 *                          alone — only when [hasAdContextEvidence] finds real
 *                          corroborating evidence (see below).
 *  4. POSITION            — (opt-in, "High" sensitivity) a clickable, button-sized
 *                          node sits in a screen corner, scoped to a recognized
 *                          ad-SDK container when found, and only trusted once it
 *                          repeats across consecutive events.
 *  5. VISUAL_AI            — (stub, see SkipButtonVisualDetector) reserved for V3.
 *
 * Confidence gating for generic close/dismiss text (the fix for "clicks
 * almost any X button"): a candidate needs a score >=
 * Constants.CLOSE_CONFIDENCE_THRESHOLD, built from:
 *   - the candidate's bounds sit inside a recognized ad-SDK container      (+3)
 *   - ad-indicator text ("Sponsored", "Advertisement"...) found nearby     (+2)
 *   - the candidate sits in a typical ad-close corner position            (+1)
 * A bare generic word with none of these present is never clicked.
 *
 * Performance: layers 1-3 are evaluated together in a single tree walk
 * (see [scanForMatch]) rather than several separate full traversals.
 */
object AdDetector {

    data class Match(
        val node: AccessibilityNodeInfo,
        val layer: DetectionLayer,
        val identifier: String
    )

    fun findSkipNode(
        root: AccessibilityNodeInfo,
        packageName: String,
        sensitivity: String,
        cache: DetectionCache,
        positionTracker: PositionMatchTracker,
        screenBounds: Rect,
        allowGenericMatches: Boolean
    ): Match? {
        cache.get(packageName)?.let { hint ->
            findByHint(root, hint)?.let { node ->
                return Match(node, hint.layer, hint.value)
            }
        }

        val adContainerBounds = findAdContainerBounds(root)

        scanForMatch(root, adContainerBounds, screenBounds, allowGenericMatches)?.let { (node, layer, identifier) ->
            if (layer == DetectionLayer.TEXT || layer == DetectionLayer.RESOURCE_ID) {
                cache.put(packageName, DetectionCache.Hint(layer, identifier))
            }
            return Match(node, layer, identifier)
        }

        // Position heuristic now requires a recognized ad-SDK container to
        // be present. Previously it fell back to searching the WHOLE screen
        // corner, which meant "any small clickable thing in the corner" —
        // a significant remaining source of random clicks on ordinary UI.
        if (sensitivity == Constants.Sensitivity.HIGH && adContainerBounds != null && allowGenericMatches) {
            findByPosition(root, screenBounds, adContainerBounds)?.let { node ->
                val bounds = Rect().also { node.getBoundsInScreen(it) }
                if (positionTracker.confirm(packageName, bounds)) {
                    return Match(node, DetectionLayer.POSITION, bounds.flattenToString())
                }
            }
        }

        return null
    }

    /**
     * Single breadth-first traversal checking text, resource-id, and
     * close/dismiss criteria per node. Text (layer 1) always wins if found.
     * Resource-id and explicit-close matches are recorded as soon as seen.
     * Generic close/dismiss candidates are recorded as a lower-priority
     * fallback ONLY once they clear the confidence check.
     */
    private fun scanForMatch(
        root: AccessibilityNodeInfo,
        adContainerBounds: Rect?,
        screenBounds: Rect,
        allowGenericMatches: Boolean
    ): Triple<AccessibilityNodeInfo, DetectionLayer, String>? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        var fallback: Triple<AccessibilityNodeInfo, DetectionLayer, String>? = null

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Explicit ad-naming text wins outright.
            if (matchesExplicitSkipPhrase(node)) {
                findClickableSelfOrAncestor(node)?.let { clickable ->
                    val label = node.text?.toString() ?: node.contentDescription?.toString() ?: "skip"
                    return Triple(clickable, DetectionLayer.TEXT, label)
                }
            }

            // Bare "Skip"-style text is only a candidate — it must clear the
            // ad-context confidence check, exactly like generic close text.
            if (fallback == null && allowGenericMatches && matchesGenericSkipPhrase(node)) {
                findClickableSelfOrAncestor(node)?.let { clickable ->
                    if (confidenceScore(clickable, adContainerBounds, screenBounds) >= Constants.CLOSE_CONFIDENCE_THRESHOLD) {
                        val label = node.text?.toString() ?: node.contentDescription?.toString() ?: "skip"
                        fallback = Triple(clickable, DetectionLayer.TEXT_GENERIC_CONFIRMED, label)
                    }
                }
            }

            if (fallback == null) {
                val viewId = node.viewIdResourceName
                if (viewId != null && Constants.SKIP_RESOURCE_ID_KEYWORDS.any { viewId.contains(it, ignoreCase = true) }) {
                    findClickableSelfOrAncestor(node)?.let { clickable ->
                        fallback = Triple(clickable, DetectionLayer.RESOURCE_ID, viewId)
                    }
                }
            }

            if (fallback == null) {
                val text = node.text?.toString()?.trim()
                val desc = node.contentDescription?.toString()?.trim()

                if (text != null && Constants.AD_EXPLICIT_CLOSE_PHRASES.any { it.equals(text, ignoreCase = true) }) {
                    findClickableSelfOrAncestor(node)?.let { clickable ->
                        fallback = Triple(clickable, DetectionLayer.CLOSE_EXPLICIT, text)
                    }
                } else {
                    val isGenericTextMatch = text != null && Constants.GENERIC_CLOSE_PHRASES.any { it.equals(text, ignoreCase = true) }
                    val isGenericDescMatch = desc != null && Constants.CLOSE_CONTENT_DESCRIPTION_KEYWORDS.any {
                        desc.lowercase().contains(it)
                    }
                    if (allowGenericMatches && (isGenericTextMatch || isGenericDescMatch)) {
                        findClickableSelfOrAncestor(node)?.let { clickable ->
                            val confidence = confidenceScore(clickable, adContainerBounds, screenBounds)
                            if (confidence >= Constants.CLOSE_CONFIDENCE_THRESHOLD) {
                                fallback = Triple(clickable, DetectionLayer.CLOSE_GENERIC_CONFIRMED, text ?: desc ?: "close")
                            }
                        }
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return fallback
    }

    /**
     * Scores how much evidence exists that [node] is genuinely part of an
     * advertisement, for gating ambiguous generic close/dismiss text. See
     * class doc for the scoring breakdown.
     */
    private fun confidenceScore(node: AccessibilityNodeInfo, adContainerBounds: Rect?, screenBounds: Rect): Int {
        var score = 0
        val bounds = Rect().also { node.getBoundsInScreen(it) }

        if (adContainerBounds != null && adContainerBounds.contains(bounds)) {
            score += Constants.CONFIDENCE_AD_CONTAINER_BOUNDS
        }

        if (hasNearbyAdIndicatorText(node)) {
            score += Constants.CONFIDENCE_NEARBY_AD_TEXT
        }

        if (isInCornerPosition(bounds, screenBounds)) {
            score += Constants.CONFIDENCE_CORNER_POSITION
        }

        return score
    }

    /**
     * Looks at [node]'s parent's subtree (siblings, one level up) for text
     * or content-description containing an ad-indicator phrase like
     * "Sponsored" or "Advertisement" — corroborating evidence for ambiguous
     * generic close/dismiss text when no recognized ad-SDK container class
     * is present (common with custom/WebView-rendered ad creatives).
     */
    private fun hasNearbyAdIndicatorText(node: AccessibilityNodeInfo): Boolean {
        val parent = node.parent ?: return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(parent)
        var visited = 0

        while (queue.isNotEmpty() && visited < 60) {
            val current = queue.removeFirst()
            visited++
            val text = (current.text?.toString() ?: current.contentDescription?.toString())?.lowercase()
            if (text != null && Constants.AD_INDICATOR_TEXT_KEYWORDS.any { text.contains(it) }) {
                return true
            }
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    private fun isInCornerPosition(bounds: Rect, screenBounds: Rect): Boolean {
        val zoneWidth = screenBounds.width() * Constants.POSITION_ZONE_WIDTH_FRACTION
        val zoneHeight = screenBounds.height() * Constants.POSITION_ZONE_HEIGHT_FRACTION
        val inTopRight = bounds.right >= screenBounds.right - zoneWidth && bounds.top <= screenBounds.top + zoneHeight
        val inBottomRight = bounds.right >= screenBounds.right - zoneWidth && bounds.bottom >= screenBounds.bottom - zoneHeight
        val isButtonSized = bounds.width() in 20..400 && bounds.height() in 20..200
        return (inTopRight || inBottomRight) && isButtonSized
    }

    private fun findByHint(root: AccessibilityNodeInfo, hint: DetectionCache.Hint): AccessibilityNodeInfo? {
        return when (hint.layer) {
            DetectionLayer.RESOURCE_ID -> {
                val matches = root.findAccessibilityNodeInfosByViewId(hint.value)
                matches?.firstOrNull { it.isClickable && it.isEnabled }
                    ?: matches?.firstOrNull()?.let { findClickableSelfOrAncestor(it) }
            }
            DetectionLayer.TEXT -> {
                val matches = root.findAccessibilityNodeInfosByText(hint.value)
                matches?.firstNotNullOfOrNull { candidate ->
                    if (matchesExplicitSkipPhrase(candidate)) findClickableSelfOrAncestor(candidate) else null
                }
            }
            // Explicit/generic close and position hints are intentionally
            // NOT cached — caching an ad-context decision risks acting on a
            // stale confidence judgement after the screen has changed.
            else -> null
        }
    }

    /**
     * Looks for a node whose class name matches a known ad-SDK fragment.
     * Used both to scope the position heuristic and as the strongest single
     * signal in generic-close confidence scoring.
     */
    private fun findAdContainerBounds(root: AccessibilityNodeInfo): Rect? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val className = node.className?.toString()
            if (className != null && Constants.AD_SDK_CLASS_KEYWORDS.any { className.contains(it, ignoreCase = true) }) {
                return Rect().also { node.getBoundsInScreen(it) }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findByPosition(root: AccessibilityNodeInfo, screenBounds: Rect, adContainerBounds: Rect?): AccessibilityNodeInfo? {
        val searchBounds = adContainerBounds ?: screenBounds
        val zoneWidth = searchBounds.width() * Constants.POSITION_ZONE_WIDTH_FRACTION
        val zoneHeight = searchBounds.height() * Constants.POSITION_ZONE_HEIGHT_FRACTION

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        val bounds = Rect()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isClickable && node.isEnabled) {
                node.getBoundsInScreen(bounds)
                val withinContainer = adContainerBounds == null || adContainerBounds.contains(bounds)
                val inTopRightCorner = bounds.right >= searchBounds.right - zoneWidth && bounds.top <= searchBounds.top + zoneHeight
                val inBottomRightCorner = bounds.right >= searchBounds.right - zoneWidth &&
                    bounds.bottom >= searchBounds.bottom - zoneHeight
                val isButtonSized = bounds.width() in 40..400 && bounds.height() in 30..200

                if (withinContainer && (inTopRightCorner || inBottomRightCorner) && isButtonSized) {
                    return node
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * True if the node's text/content-description explicitly names an ad
     * ("Skip Ad"). Safe to act on directly — essentially no ambiguity.
     */
    fun matchesExplicitSkipPhrase(node: AccessibilityNodeInfo): Boolean =
        matchesAny(node, Constants.AD_EXPLICIT_SKIP_PHRASES)

    /**
     * True for bare/ambiguous skip words ("Skip", "تخطي", "Ignorer"). These
     * appear throughout ordinary app UI — onboarding, tutorials, setup
     * wizards — so a match here is only a CANDIDATE and must clear the
     * ad-context confidence check before it is ever clicked.
     */
    fun matchesGenericSkipPhrase(node: AccessibilityNodeInfo): Boolean =
        matchesAny(node, Constants.GENERIC_SKIP_PHRASES)

    private fun matchesAny(node: AccessibilityNodeInfo, phrases: List<String>): Boolean {
        val candidates = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
        if (candidates.isEmpty()) return false
        return candidates.any { raw ->
            val normalized = raw.trim()
            phrases.any { phrase ->
                normalized.equals(phrase, ignoreCase = true) ||
                    normalized.equals("$phrase >", ignoreCase = true) ||
                    normalized.equals("▶ $phrase", ignoreCase = true)
            }
        }
    }

    private fun findClickableSelfOrAncestor(
        node: AccessibilityNodeInfo,
        maxDepth: Int = 5
    ): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < maxDepth) {
            if (current.isClickable && current.isEnabled) {
                return current
            }
            current = current.parent
            depth++
        }
        return null
    }
}
