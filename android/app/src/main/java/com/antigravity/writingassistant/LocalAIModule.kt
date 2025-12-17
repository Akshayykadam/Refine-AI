package com.antigravity.writingassistant

import android.content.SharedPreferences
import android.content.ComponentName
import android.provider.Settings
import android.text.TextUtils
import com.antigravity.writingassistant.local.ModelDownloadManager
import com.facebook.react.bridge.*

class LocalAIModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val downloadManager = ModelDownloadManager(reactContext)
    private val prefs: SharedPreferences = reactContext.getSharedPreferences("WritingAssistPrefs", 0)

    override fun getName(): String = "LocalAIModule"

    @ReactMethod
    fun isModelDownloaded(promise: Promise) {
        promise.resolve(downloadManager.isModelDownloaded())
    }

    @ReactMethod
    fun getModelSizeMB(promise: Promise) {
        promise.resolve(downloadManager.getModelSizeMB().toDouble())
    }

    @ReactMethod
    fun isAccessibilityServiceEnabled(promise: Promise) {
        try {
            val expectedComponentName = ComponentName(reactApplicationContext, WritingAssistService::class.java)
            val enabledServicesSetting = Settings.Secure.getString(
                reactApplicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting ?: "")

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledComponent = ComponentName.unflattenFromString(componentNameString)
                if (enabledComponent != null && enabledComponent == expectedComponentName) {
                    promise.resolve(true)
                    return
                }
            }
            promise.resolve(false)
        } catch (e: Exception) {
            promise.resolve(false)
        }
    }

    @ReactMethod
    fun downloadModel() {
        downloadManager.downloadModel(object : ModelDownloadManager.DownloadListener {
            override fun onProgress(bytesDownloaded: Long, totalBytes: Long, percent: Int) {
                val params = Arguments.createMap().apply {
                    putDouble("bytesDownloaded", bytesDownloaded.toDouble())
                    putDouble("totalBytes", totalBytes.toDouble())
                    putInt("percent", percent)
                }
                sendEvent("onDownloadProgress", params)
            }

            override fun onComplete(filePath: String) {
                prefs.edit().putBoolean("model_downloaded", true).apply()
                val params = Arguments.createMap().apply {
                    putString("filePath", filePath)
                }
                sendEvent("onDownloadComplete", params)
            }

            override fun onError(message: String) {
                val params = Arguments.createMap().apply {
                    putString("error", message)
                }
                sendEvent("onDownloadError", params)
            }
        })
    }

    @ReactMethod
    fun cancelDownload() {
        downloadManager.cancelDownload()
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for RN event emitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for RN event emitter
    }
}
