package com.kitawatch.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chaquo.python.Python

class MainActivity : TauriActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    applySystemBarInsets()
    startBundledApi()
  }

  /**
   * Edge-to-edge is mandatory for targetSdk 35+ and neither Tauri's generated
   * WryActivity nor this template consumed the resulting insets, so the webview
   * filled the whole window and content sat under the status/navigation bars.
   * Pad the content view instead: the webview then gets the visible area only.
   */
  private fun applySystemBarInsets() {
    val content = findViewById<View>(android.R.id.content) ?: return
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
      val bars =
        insets.getInsets(
          WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      insets
    }
  }

  /**
   * Starts the bundled Kuhi API and proxy (Python via Chaquopy) on 127.0.0.1
   * ports 8000/8001.
   *
   * Desktop spawns them as child processes (src-tauri/src/api_sidecar.rs); here
   * they run in-process in daemon threads. kitawatch_server.start() is idempotent
   * and returns immediately (heavy imports happen inside the threads), and the
   * frontend already retries API calls while the sidecars boot, so a slow or
   * failed start must not crash the activity.
   */
  private fun startBundledApi() {
    try {
      Python.getInstance().getModule("kitawatch_server").callAttr("start")
    } catch (e: Exception) {
      Log.e("KitaWatch", "bundled Python services failed to start", e)
    }
  }
}
