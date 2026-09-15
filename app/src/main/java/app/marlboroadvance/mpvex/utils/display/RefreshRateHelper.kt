package app.marlboroadvance.mpvex.utils.display

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Window
import android.view.WindowManager

object RefreshRateHelper {
  private const val TAG = "RefreshRateHelper"
  private const val TARGET_144HZ = 144f

  /**
   * Applies 144Hz (or highest supported high refresh rate up to 144Hz+) to the window.
   * If enabled is false, resets back to system default.
   */
  fun applyRefreshRate(activity: Activity, enableHighRefreshRate: Boolean) {
    applyRefreshRate(activity.window, activity, enableHighRefreshRate)
  }

  fun applyRefreshRate(window: Window, context: Context, enableHighRefreshRate: Boolean) {
    try {
      if (!enableHighRefreshRate) {
        // Reset to default
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          window.attributes.preferredDisplayModeId = 0
          window.attributes = window.attributes
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          val layoutParams = window.attributes
          layoutParams.preferredDisplayModeId = 0
          window.attributes = layoutParams
        }
        return
      }

      // If Android M (API 23) or higher, look for display modes
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          context.display ?: (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
          @Suppress("DEPRECATION")
          (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }

        if (display == null) {
          Log.w(TAG, "Unable to get Display instance")
          return
        }

        val supportedModes = display.supportedModes
        if (supportedModes.isNullOrEmpty()) {
          Log.w(TAG, "No supported modes found on display")
          return
        }

        // Find the mode closest to 144Hz or highest available >= 120Hz/144Hz
        // 1. Try to find a mode with refreshRate ~144Hz (within 2Hz tolerance, e.g., 143.9 - 144.1Hz)
        val mode144 = supportedModes.firstOrNull { mode ->
          kotlin.math.abs(mode.refreshRate - TARGET_144HZ) <= 2.0f
        }

        val chosenMode = mode144 ?: supportedModes
          .filter { it.refreshRate >= 90f }
          .maxByOrNull { it.refreshRate }

        if (chosenMode != null) {
          Log.d(TAG, "Setting preferred display mode: id=${chosenMode.modeId}, refreshRate=${chosenMode.refreshRate}")
          val layoutParams = window.attributes
          layoutParams.preferredDisplayModeId = chosenMode.modeId
          window.attributes = layoutParams
        } else {
          // Fallback: pick the mode with highest refresh rate
          val highestMode = supportedModes.maxByOrNull { it.refreshRate }
          if (highestMode != null && highestMode.refreshRate > 60f) {
            val layoutParams = window.attributes
            layoutParams.preferredDisplayModeId = highestMode.modeId
            window.attributes = layoutParams
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to apply refresh rate", e)
    }
  }
}
