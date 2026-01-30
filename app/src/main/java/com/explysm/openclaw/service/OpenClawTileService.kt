package com.explysm.openclaw.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.explysm.openclaw.MainActivity
import com.explysm.openclaw.utils.ApiClient

@RequiresApi(Build.VERSION_CODES.N)
class OpenClawTileService : TileService() {
    
    companion object {
        private const val DEFAULT_API_URL = "http://127.0.0.1:5039"
        
        fun requestTileUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestListeningState(context, ComponentName(context, OpenClawTileService::class.java))
            }
        }
    }
    
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }
    
    override fun onClick() {
        super.onClick()
        
        val tile = qsTile
        when (tile.state) {
            Tile.STATE_INACTIVE -> {
                // Start OpenClaw
                ApiClient.post("$DEFAULT_API_URL/api/start", "") { result ->
                    result.onSuccess {
                        updateTileState(Tile.STATE_ACTIVE, "OpenClaw Running")
                    }.onFailure {
                        updateTileState(Tile.STATE_UNAVAILABLE, "Failed to start")
                    }
                }
            }
            Tile.STATE_ACTIVE -> {
                // Stop OpenClaw
                ApiClient.post("$DEFAULT_API_URL/api/stop", "") { result ->
                    result.onSuccess {
                        updateTileState(Tile.STATE_INACTIVE, "OpenClaw Stopped")
                    }.onFailure {
                        updateTileState(Tile.STATE_UNAVAILABLE, "Failed to stop")
                    }
                }
            }
            else -> {
                // Open app
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivityAndCollapse(intent)
            }
        }
    }
    
    private fun updateTile() {
        ApiClient.get("$DEFAULT_API_URL/api/status") { result ->
            result.onSuccess { response ->
                val isRunning = response.contains("running", ignoreCase = true)
                if (isRunning) {
                    updateTileState(Tile.STATE_ACTIVE, "OpenClaw Running")
                } else {
                    updateTileState(Tile.STATE_INACTIVE, "OpenClaw Stopped")
                }
            }.onFailure {
                updateTileState(Tile.STATE_UNAVAILABLE, "OpenClaw Unreachable")
            }
        }
    }
    
    private fun updateTileState(state: Int, label: String) {
        val tile = qsTile ?: return
        tile.state = state
        tile.label = label
        tile.contentDescription = label
        tile.updateTile()
    }
}
