package com.kitawatch.app

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import com.chaquo.python.Python

class MainActivity : TauriActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    startBundledApi()
  }

  /**
   * Starts the bundled Kuhi API (Python via Chaquopy) on 127.0.0.1:8000.
   *
   * Desktop spawns it as a child process (src-tauri/src/api_sidecar.rs); here it
   * runs in-process in a daemon thread. kitawatch_server.start() is idempotent and
   * returns immediately (heavy imports happen inside the thread), and the frontend
   * already retries API calls while the sidecar boots, so a slow or failed start
   * must not crash the activity.
   */
  private fun startBundledApi() {
    try {
      Python.getInstance().getModule("kitawatch_server").callAttr("start")
    } catch (e: Exception) {
      Log.e("KitaWatch", "bundled Kuhi API failed to start", e)
    }
  }
}
