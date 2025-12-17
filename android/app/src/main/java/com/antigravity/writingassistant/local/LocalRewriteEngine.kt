package com.antigravity.writingassistant.local

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local AI engine using MediaPipe LLM Inference with Gemma 2B model.
 * Handles model loading, inference, and fallback to simulation mode.
 */
class LocalRewriteEngine(private val context: Context) {

    companion object {
        private const val TAG = "LocalRewriteEngine"
        private const val MODEL_FILE = "gemma-2b-it-cpu-int4.bin"
        private const val MODEL_LOAD_TIMEOUT_SECONDS = 60L
        private const val INFERENCE_TIMEOUT_SECONDS = 30L
        private const val MIN_MEMORY_MB = 1000L // Minimum free memory to load model
    }

    private var llmInference: LlmInference? = null
    private var isModelLoaded = false
    private var isModelLoading = false
    private var loadError: String? = null
    
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val modelPath: String
        get() = File(context.filesDir, MODEL_FILE).absolutePath

    /**
     * Listener for model loading state changes
     */
    interface LoadingStateListener {
        fun onLoadingStarted()
        fun onLoadingComplete(success: Boolean, error: String?)
    }
    
    private var loadingListener: LoadingStateListener? = null
    
    fun setLoadingStateListener(listener: LoadingStateListener?) {
        loadingListener = listener
    }

    init {
        val modelFile = File(modelPath)
        if (modelFile.exists()) {
            android.util.Log.i(TAG, "Model file found (${modelFile.length() / (1024 * 1024)} MB). Will load on first use.")
        } else {
            android.util.Log.w(TAG, "Model file not found. Running in SIMULATION MODE.")
        }
    }

    /**
     * Check if device has enough free memory to load the model
     */
    private fun hasEnoughMemory(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val availableMB = memInfo.availMem / (1024 * 1024)
            android.util.Log.d(TAG, "Available memory: $availableMB MB, required: $MIN_MEMORY_MB MB")
            availableMB >= MIN_MEMORY_MB
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Could not check memory: ${e.message}")
            true // Assume we have enough if check fails
        }
    }

    /**
     * Load model asynchronously with timeout and retry
     */
    private fun loadModelAsync(onComplete: (Boolean) -> Unit) {
        if (isModelLoaded) {
            onComplete(true)
            return
        }
        
        if (isModelLoading) {
            // Wait for existing load to complete
            executor.execute {
                var attempts = 0
                while (isModelLoading && attempts < 600) { // Max 60 seconds wait
                    Thread.sleep(100)
                    attempts++
                }
                mainHandler.post { onComplete(isModelLoaded) }
            }
            return
        }
        
        // Check memory before loading
        if (!hasEnoughMemory()) {
            loadError = "Not enough memory. Please close some apps and try again."
            android.util.Log.e(TAG, loadError!!)
            onComplete(false)
            return
        }
        
        isModelLoading = true
        loadError = null
        
        mainHandler.post { loadingListener?.onLoadingStarted() }
        
        val loadTask = executor.submit<Boolean> {
            loadModelInternal()
        }
        
        // Timeout handling
        executor.execute {
            try {
                val success = loadTask.get(MODEL_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                isModelLoading = false
                mainHandler.post {
                    loadingListener?.onLoadingComplete(success, loadError)
                    onComplete(success)
                }
            } catch (e: Exception) {
                loadTask.cancel(true)
                isModelLoading = false
                loadError = when (e) {
                    is java.util.concurrent.TimeoutException -> "Model loading timed out. Please try again."
                    else -> "Failed to load model: ${e.message}"
                }
                android.util.Log.e(TAG, loadError!!, e)
                mainHandler.post {
                    loadingListener?.onLoadingComplete(false, loadError)
                    onComplete(false)
                }
            }
        }
    }
    
    private fun loadModelInternal(): Boolean {
        return try {
            android.util.Log.i(TAG, "Loading MediaPipe model from $modelPath...")
            
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(256)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            isModelLoaded = true
            
            android.util.Log.i(TAG, "MediaPipe LLM Loaded Successfully!")
            true
            
        } catch (e: Exception) {
            loadError = "Model error: ${e.message}"
            android.util.Log.e(TAG, "Failed to load model", e)
            isModelLoaded = false
            false
        }
    }

    /**
     * Rewrite text using local AI or simulation fallback
     */
    fun rewrite(input: String, instruction: String, callback: (String) -> Unit) {
        // 1. Validate input
        val validation = ConstraintValidator.validateInput(input)
        if (validation is ConstraintValidator.ValidationResult.Invalid) {
            callback("[Error] ${validation.reason}")
            return
        }

        // 2. Build prompt
        val prompt = LocalPromptBuilder.buildPrompt(input, instruction)
        val taskType = LocalPromptBuilder.getTaskType(instruction)

        // 3. Check if model exists
        if (!isModelAvailable()) {
            runSimulation(input, taskType, callback)
            return
        }

        // 4. Load model if needed, then run inference
        loadModelAsync { success ->
            if (success && llmInference != null) {
                runInference(prompt, input, taskType, callback)
            } else {
                if (loadError != null) {
                    callback("[Error] $loadError")
                } else {
                    runSimulation(input, taskType, callback)
                }
            }
        }
    }

    private fun runSimulation(input: String, taskType: String, callback: (String) -> Unit) {
        mainHandler.postDelayed({
            val result = fakeRewrite(taskType, input)
            callback("[Demo] $result")
        }, 800)
    }
    
    private fun fakeRewrite(taskType: String, text: String): String {
        return when (taskType) {
            "Professional", "Formal" -> "Dear recipient, $text"
            "Casual" -> "Hey! $text"
            "Warm" -> "$text (warmly)"
            "Love" -> "My dearest, $text"
            "Concise" -> text.take(50) + if (text.length > 50) "..." else ""
            "Grammar" -> text.replaceFirstChar { it.uppercase() } + "."
            "Emojify" -> "$text [with emojis]"
            else -> text
        }
    }
    
    private fun runInference(prompt: String, originalInput: String, taskType: String, callback: (String) -> Unit) {
        val cancelled = AtomicBoolean(false)
        
        val inferenceTask = executor.submit {
            try {
                if (cancelled.get()) return@submit
                
                val response = llmInference?.generateResponse(prompt)
                
                if (cancelled.get()) return@submit
                
                val cleanedResponse = cleanModelOutput(response ?: "")
                
                mainHandler.post {
                    if (!cancelled.get()) {
                        if (cleanedResponse.isNotBlank()) {
                            callback(cleanedResponse)
                        } else {
                            callback("[Error] Empty response from model.")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Inference failed: ${e.message}", e)
                if (!cancelled.get()) {
                    mainHandler.post {
                        callback("[Error] Inference failed: ${e.localizedMessage}")
                    }
                }
            }
        }
        
        // Timeout for inference
        mainHandler.postDelayed({
            if (!inferenceTask.isDone) {
                cancelled.set(true)
                inferenceTask.cancel(true)
                callback("[Error] Response timed out. Please try again.")
            }
        }, INFERENCE_TIMEOUT_SECONDS * 1000)
    }
    
    private fun cleanModelOutput(output: String): String {
        return output
            .replace("<end_of_turn>", "")
            .replace("<start_of_turn>", "")
            .replace("model", "")
            // Remove common conversational prefixes (case insensitive, allowing for variable length preamble)
            .replace(Regex("(?i)^(Sure|Here is|Here's|Okay|Certainly|Here are).{0,100}(:|\\n|\\.)\\s*"), "")
            .trim()
            // Remove surrounding quotes if model outputs them
            .removeSurrounding("\"")
    }

    /**
     * Check if model file exists
     */
    fun isModelAvailable(): Boolean {
        return File(modelPath).exists()
    }
    
    /**
     * Check if model is currently loaded in memory
     */
    fun isLoaded(): Boolean = isModelLoaded
    
    /**
     * Check if model is currently loading
     */
    fun isLoading(): Boolean = isModelLoading
    
    /**
     * Get last error message if any
     */
    fun getLastError(): String? = loadError

    /**
     * Reload model after download completes
     */
    fun reloadModel() {
        close()
        loadError = null
    }

    /**
     * Release resources
     */
    fun close() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error closing model: ${e.message}")
        }
        llmInference = null
        isModelLoaded = false
        isModelLoading = false
    }
}
