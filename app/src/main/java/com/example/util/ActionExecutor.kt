package com.example.util

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import com.example.model.JarvisAction

object ActionExecutor {

    fun execute(context: Context, action: JarvisAction, onFlashlightStateChanged: ((Boolean) -> Unit)? = null): String {
        return try {
            when (action) {
                is JarvisAction.FlashlightOn -> {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        cameraManager.getCameraCharacteristics(id)
                            .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } ?: cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) {
                        cameraManager.setTorchMode(cameraId, true)
                        onFlashlightStateChanged?.invoke(true)
                        "Flashlight turned ON, Boss."
                    } else {
                        "Flashlight hardware unavailable, Boss."
                    }
                }

                is JarvisAction.FlashlightOff -> {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        cameraManager.getCameraCharacteristics(id)
                            .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } ?: cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) {
                        cameraManager.setTorchMode(cameraId, false)
                        onFlashlightStateChanged?.invoke(false)
                        "Flashlight turned OFF, Boss."
                    } else {
                        "Flashlight hardware unavailable, Boss."
                    }
                }

                is JarvisAction.YouTube -> {
                    val encodedQuery = Uri.encode(action.query)
                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube.launch")).apply {
                        putExtra("query", action.query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(webIntent)
                    } catch (e: Exception) {
                        context.startActivity(appIntent)
                    }
                    "Opening YouTube for '${action.query}', Boss."
                }

                is JarvisAction.WhatsApp -> {
                    val encodedMsg = Uri.encode(action.message)
                    val url = "https://api.whatsapp.com/send?text=$encodedMsg"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Opening WhatsApp for contact '${action.contact}', Boss."
                }

                is JarvisAction.Camera -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val cameraAppIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.GoogleCamera")
                            ?: context.packageManager.getLaunchIntentForPackage("com.android.camera")
                        if (cameraAppIntent != null) {
                            cameraAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(cameraAppIntent)
                        } else {
                            Toast.makeText(context, "Camera launched, Boss", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Opening Camera, Boss."
                }

                is JarvisAction.Music -> {
                    val encodedQuery = Uri.encode(action.query)
                    val musicIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                        putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                        putExtra(MediaStore.EXTRA_MEDIA_TITLE, action.query)
                        putExtra(SearchManager_QUERY, action.query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val webMusicIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://music.youtube.com/search?q=$encodedQuery")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(webMusicIntent)
                    } catch (e: Exception) {
                        try {
                            context.startActivity(musicIntent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "Playing music for ${action.query}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Playing music for '${action.query}', Boss."
                }

                is JarvisAction.OpenApp -> {
                    val pm = context.packageManager
                    val query = action.appName.lowercase()
                    val installedApps = pm.getInstalledApplications(0)
                    val matchedApp = installedApps.firstOrNull { app ->
                        val label = pm.getApplicationLabel(app).toString().lowercase()
                        label.contains(query) || query.contains(label)
                    }

                    if (matchedApp != null) {
                        val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                            "Opening ${pm.getApplicationLabel(matchedApp)}, Boss."
                        } else {
                            "Found app but could not launch package, Boss."
                        }
                    } else {
                        // Open Play Store or Search
                        val playStoreIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/search?q=${Uri.encode(action.appName)}&c=apps")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playStoreIntent)
                        "Searching Store for ${action.appName}, Boss."
                    }
                }
            }
        } catch (e: Exception) {
            "Execution notice: ${e.localizedMessage ?: "Action triggered"}, Boss."
        }
    }

    private const val SearchManager_QUERY = "query"
}
