package com.bernaprioste.pocketlock

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: one tap blacks out the screen. */
class LockTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        val ready = LockService.instance != null
        tile.state = Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (ready) R.string.tile_subtitle_ready else R.string.tile_subtitle_setup)
        }
        tile.updateTile()
    }

    override fun onClick() {
        val service = LockService.instance
        if (service != null) {
            service.collapseShadeThenLock()
        } else {
            openSetupScreen()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openSetupScreen() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
