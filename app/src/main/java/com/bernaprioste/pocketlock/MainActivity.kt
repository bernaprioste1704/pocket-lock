package com.bernaprioste.pocketlock

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Setup screen: enable the service, add the tile, try it out. */
class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var enableButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        enableButton = findViewById(R.id.enable_button)

        enableButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.add_tile_button).setOnClickListener { requestTile() }
        findViewById<Button>(R.id.try_button).setOnClickListener {
            val service = LockService.instance
            if (service == null) {
                Toast.makeText(this, R.string.enable_first, Toast.LENGTH_LONG).show()
            } else {
                service.lock()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val ready = LockService.instance != null
        status.setText(if (ready) R.string.status_ready else R.string.status_not_ready)
        enableButton.setText(if (ready) R.string.button_open_accessibility else R.string.button_enable)
    }

    private fun requestTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.add_tile_manually, Toast.LENGTH_LONG).show()
            return
        }
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, LockTileService::class.java),
            getString(R.string.tile_label),
            Icon.createWithResource(this, R.drawable.ic_tile),
            mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
