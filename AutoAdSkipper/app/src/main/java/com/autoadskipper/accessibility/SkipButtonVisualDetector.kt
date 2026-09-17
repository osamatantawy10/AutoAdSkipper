package com.autoadskipper.accessibility

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Layer 4 of the detection engine: visual (image-based) skip-button
 * recognition, intended to catch buttons whose text and resource IDs don't
 * match anything in Layers 1–2 (e.g. a redesigned ad network overlay).
 *
 * NOTE: this is an architectural placeholder for V3, not a working model.
 * [NoOpVisualDetector] always returns null. Wiring in a real on-device model
 * (e.g. a small TFLite classifier trained on skip-button crops) is future
 * work — this interface is the intended integration point so the rest of the
 * engine (AdDetector, the accessibility service) doesn't need to change when
 * that lands.
 */
interface SkipButtonVisualDetector {
    /**
     * Given a screenshot-style bitmap of the current window, return the
     * bounding box of a detected skip button, or null if none is found /
     * the detector isn't implemented yet.
     */
    fun detect(screenshot: Bitmap): Rect?

    val isImplemented: Boolean
}

class NoOpVisualDetector : SkipButtonVisualDetector {
    override fun detect(screenshot: Bitmap): Rect? = null
    override val isImplemented: Boolean = false
}
