package com.antigravity.writingassistant

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var bubbleView: View? = null
    private var removeView: View? = null

    private fun showRemoveView() {
        if (removeView != null) return
        try {
            removeView = LayoutInflater.from(context).inflate(R.layout.remove_bubble_view, null)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 100 // Margin from bottom
            windowManager.addView(removeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideRemoveView() {
        removeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            removeView = null
        }
    }
    
    fun showBubble() {
        if (bubbleView != null) return

        try {
            bubbleView = LayoutInflater.from(context).inflate(R.layout.bubble_view, null)
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 0
            params.y = 200

            bubbleView?.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private val touchSlop = 10
                private var isDragging = false

                override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                    val layoutParams = v.layoutParams as WindowManager.LayoutParams
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            showRemoveView()
                            return true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - initialTouchX
                            val dy = event.rawY - initialTouchY
                            if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                                isDragging = true
                            }
                            layoutParams.x = initialX + dx.toInt()
                            layoutParams.y = initialY + dy.toInt()
                            windowManager.updateViewLayout(v, layoutParams)
                            
                            // Check overlap
                            if (removeView != null) {
                                val bubbleLoc = IntArray(2)
                                v.getLocationOnScreen(bubbleLoc)
                                val bubbleCenterX = bubbleLoc[0] + v.width / 2
                                val bubbleCenterY = bubbleLoc[1] + v.height / 2
                                val removeLoc = IntArray(2)
                                removeView!!.getLocationOnScreen(removeLoc)
                                val removeCenterX = removeLoc[0] + removeView!!.width / 2
                                val removeCenterY = removeLoc[1] + removeView!!.height / 2
                                val distance = Math.sqrt(Math.pow((bubbleCenterX - removeCenterX).toDouble(), 2.0) + Math.pow((bubbleCenterY - removeCenterY).toDouble(), 2.0))
                                if (distance < 150) {
                                    removeView?.scaleX = 1.2f; removeView?.scaleY = 1.2f
                                } else {
                                    removeView?.scaleX = 1.0f; removeView?.scaleY = 1.0f
                                }
                            }
                            return true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            var dismissed = false
                            if (isDragging && removeView != null) {
                                val bubbleLoc = IntArray(2)
                                v.getLocationOnScreen(bubbleLoc)
                                val bubbleCenterX = bubbleLoc[0] + v.width / 2
                                val bubbleCenterY = bubbleLoc[1] + v.height / 2
                                val removeLoc = IntArray(2)
                                removeView!!.getLocationOnScreen(removeLoc)
                                val removeCenterX = removeLoc[0] + removeView!!.width / 2
                                val removeCenterY = removeLoc[1] + removeView!!.height / 2
                                val distance = Math.sqrt(Math.pow((bubbleCenterX - removeCenterX).toDouble(), 2.0) + Math.pow((bubbleCenterY - removeCenterY).toDouble(), 2.0))
                                if (distance < 150) {
                                    hideBubble()
                                    dismissed = true
                                }
                            }
                            hideRemoveView()
                            if (!dismissed && !isDragging) {
                                onBubbleClick()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            windowManager.addView(bubbleView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var panelView: View? = null

    private fun onBubbleClick() {
        android.util.Log.d("OverlayManager", "Bubble Clicked")
        hideBubble()
        showActionPanel()
    }

    interface OverlayListener {
        fun onReplace(text: String)
    }
    
    var listener: OverlayListener? = null

    // MEMBERS - Gemini only
    private val geminiClient = GeminiClient()
    
    private var currentText: String = ""
    private var currentPackageName: String = ""
    private var originalTextBeforeRewrite: String = ""
    private var lastRewrittenText: String? = null
    private var contextBefore: String? = null
    private var contextAfter: String? = null
    
    private val prefs by lazy { context.getSharedPreferences("WritingAssistPrefs", Context.MODE_PRIVATE) }

    fun updateText(text: String, packageName: String, before: String? = null, after: String? = null) {
        currentText = text
        currentPackageName = packageName
        originalTextBeforeRewrite = text
        lastRewrittenText = null
        contextBefore = before
        contextAfter = after
    }

    private fun showActionPanel() {
        if (panelView != null) return

        try {
            val wrappedContext = android.view.ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_DayNight)
            panelView = LayoutInflater.from(wrappedContext).inflate(R.layout.action_panel_view, null)
            
            val previewTextView = panelView?.findViewById<android.widget.EditText>(R.id.preview_text)
            previewTextView?.setText(currentText)

            panelView?.findViewById<View>(R.id.btn_close)?.setOnClickListener {
                hideActionPanel()
                showBubble() 
            }
            
            val inputInstruction = panelView?.findViewById<android.widget.EditText>(R.id.input_instruction)
            val lastInstruction = prefs.getString("last_instruction_$currentPackageName", "")
            inputInstruction?.setText(lastInstruction)
            
            val chipProfessional = panelView?.findViewById<View>(R.id.chip_professional)
            val chipCasual = panelView?.findViewById<View>(R.id.chip_casual)
            val chipConcise = panelView?.findViewById<View>(R.id.chip_concise)
            val chipRefine = panelView?.findViewById<View>(R.id.chip_refine)
            val tvCharCount = panelView?.findViewById<android.widget.TextView>(R.id.tv_char_count)
            tvCharCount?.text = "${currentText.length} / 3000 characters"

            val chipWarm = panelView?.findViewById<View>(R.id.chip_warm)
            val chipLove = panelView?.findViewById<View>(R.id.chip_love)
            val chipEmojify = panelView?.findViewById<View>(R.id.chip_emojify)
            val chipGrammar = panelView?.findViewById<View>(R.id.chip_grammar)
            val chipHinglish = panelView?.findViewById<View>(R.id.chip_hinglish)


            val allChips = listOfNotNull(chipRefine, chipGrammar, chipProfessional, chipCasual, chipConcise, chipWarm, chipLove, chipEmojify, chipHinglish)
            
            var selectedChipInstruction: String? = "Rewrite the text to be clearer, more fluent, and easier to read while preserving the original meaning, intent, and length. Improve grammar, sentence flow, and word choice. Do not add new ideas, remove information, or change the tone. Keep it natural and neutral."

            val chipListener = View.OnClickListener { v ->
                val btn = v as android.widget.Button
                var text = btn.text.toString()
                
                allChips.forEach { it.isSelected = false }
                v.isSelected = true
                
                if (text.equals("Warm", ignoreCase = true)) {
                    selectedChipInstruction = "Rewrite the text in a warm, supportive, and human tone. Sound approachable, respectful, and emotionally aware without being overly sentimental. Keep the message clear and sincere. Do not exaggerate emotions or add unnecessary affection. Preserve the original meaning."
                } else if (text.equals("Love", ignoreCase = true)) {
                    selectedChipInstruction = "Rewrite the text with gentle affection and care, expressing warmth, appreciation, and emotional closeness. Keep it sincere and balanced—avoid romantic excess, poetic language, or dramatic expressions. The tone should feel caring and respectful, not intense or flirtatious. Preserve the original intent."
                } else if (text.equals("Emojify", ignoreCase = true)) {
                    selectedChipInstruction = "Rewrite the text to be clear and engaging, then add a small number of relevant emojis to enhance expression. Emojis should feel natural and supportive, not excessive or distracting. Do not change the original meaning or make the message childish. Limit emojis to 1–3 per paragraph."
                } else if (text.equals("Hinglish", ignoreCase = true)) {
                    selectedChipInstruction = "Rewrite the text in casual Hinglish (a natural mix of Hindi and English) as spoken by urban Indians. Use common Hindi words written in Roman script (like 'karna', 'matlab', 'bilkul', 'mast', 'yaar'). Keep the tone conversational, friendly, and informal. Preserve the original meaning but make it sound like a natural text message or chat."
                } else if (text.equals("Refine", ignoreCase = true)) {
                    selectedChipInstruction = "Rewrite the text to be clearer, more fluent, and easier to read while preserving the original meaning, intent, and length. Improve grammar, sentence flow, and word choice. Do not add new ideas, remove information, or change the tone. Keep it natural and neutral."
                } else if (text.equals("Grammar", ignoreCase = true)) {
                    selectedChipInstruction = "Check the text for grammar, spelling, punctuation, and basic sentence structure errors. Correct only what is necessary to make the text grammatically correct and readable. Do not rewrite for style, tone, or clarity unless required for correctness. Do not change wording, intent, or sentence order beyond the minimum needed. Preserve the original tone and meaning exactly."
                } else if (text.equals("Professional", ignoreCase = true)) {
                    selectedChipInstruction = "Rewrite the text in a professional, polished, and confident tone. Use clear, concise language suitable for workplace or formal communication. Avoid slang, emojis, exaggeration, or emotional phrasing. Preserve the original meaning and intent without adding or removing information."
                } else if (text.equals("Casual", ignoreCase = true)) {
                    selectedChipInstruction = "Rewrite the text in a relaxed, friendly, and conversational tone. Keep it simple and natural, as if speaking to a colleague or friend. Avoid sounding overly formal or robotic. Do not use emojis. Preserve the original meaning and intent."
                } else if (text.equals("Concise", ignoreCase = true)) {
                     selectedChipInstruction = "Rewrite the text to be shorter and more concise without losing key information."

                } else {
                    selectedChipInstruction = "Make it $text"
                }
                
                inputInstruction?.setText("")
                inputInstruction?.clearFocus()
            }
            allChips.forEach { it.setOnClickListener(chipListener) }
            
            if (inputInstruction?.text.isNullOrEmpty()) {
                chipRefine?.performClick()
            }
            
            val rewriteBtn = panelView?.findViewById<View>(R.id.btn_rewrite)
            val loadingIndicator = panelView?.findViewById<View>(R.id.loading_indicator)
            val resultActions = panelView?.findViewById<View>(R.id.result_actions)
            val replaceBtn = panelView?.findViewById<View>(R.id.btn_replace)
            val compareBtn = panelView?.findViewById<android.widget.Button>(R.id.btn_compare)
            
            rewriteBtn?.setOnClickListener {
                if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
                    android.widget.Toast.makeText(context, "API Key Missing. Check local.properties.", android.widget.Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                rewriteBtn.visibility = View.GONE
                loadingIndicator?.visibility = View.VISIBLE
                resultActions?.visibility = View.GONE
                
                var instruction = inputInstruction?.text.toString()
                
                if (instruction.isBlank()) {
                    instruction = selectedChipInstruction ?: "Refine this text to be better"
                }

                // Always use Gemini API
                val useContext = true 
                prefs.edit().putString("last_instruction_$currentPackageName", instruction).apply()
                
                val config = GeminiClient.PromptConfig(
                    originalText = currentText,
                    instruction = if (instruction.isNotBlank()) instruction else null,
                    contextBefore = if (useContext) contextBefore else null,
                    contextAfter = if (useContext) contextAfter else null
                )
                
                geminiClient.generateContent(config) { result ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (result != null) {
                            previewTextView?.setText(result)
                            previewTextView?.requestFocus()
                            previewTextView?.selectAll()
                            lastRewrittenText = result
                            loadingIndicator?.visibility = View.GONE
                            resultActions?.visibility = View.VISIBLE
                            
                            replaceBtn?.setOnClickListener {
                                listener?.onReplace(result)
                                hideActionPanel()
                            }
                            
                            compareBtn?.setOnClickListener {
                                if (previewTextView?.text.toString() == lastRewrittenText) {
                                    previewTextView?.setText(originalTextBeforeRewrite)
                                    compareBtn.text = "Show Rewrite"
                                } else {
                                    previewTextView?.setText(lastRewrittenText)
                                    compareBtn.text = "Show Original"
                                }
                            }
                        } else {
                             loadingIndicator?.visibility = View.GONE
                             rewriteBtn.visibility = View.VISIBLE
                             android.widget.Toast.makeText(context, "Failed to rewrite", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 0
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

            // Auto-close on keyboard dismiss
            panelView?.viewTreeObserver?.addOnGlobalLayoutListener {
                val r = android.graphics.Rect()
                panelView?.getWindowVisibleDisplayFrame(r)
                val screenHeight = panelView?.rootView?.height ?: 0
                val keypadHeight = screenHeight - r.bottom
                
                // If keypad height is small, keyboard is closed. 
                // We check if it WAS open previously to avoid closing on initial show.
                // Simplified: If user was typing (focused) and keyboard closes -> close panel?
                // Or simply: If keypadHeight < 100dp, close? No, that prevents showing it initially.
                
                // Better approach: If we detect height change significantly
                // But simplified for now: Rely on outside touch for general dismissal
            }
            
            // Close on touch outside
            panelView?.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    hideActionPanel()
                    true
                } else {
                    false
                }
            }

            // Keyboard State Listener to close popup when keyboard closes
            var isKeyboardShowing = false
            panelView?.viewTreeObserver?.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (panelView == null) return
                    
                    val r = android.graphics.Rect()
                    panelView!!.getWindowVisibleDisplayFrame(r)
                    val screenHeight = panelView!!.rootView.height
                    val keypadHeight = screenHeight - r.bottom

                    if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is enough to suspect keyboard
                        isKeyboardShowing = true
                    } else {
                        if (isKeyboardShowing) {
                            // Keyboard was showing, now it's gone -> Close Popup
                            hideActionPanel()
                        }
                        isKeyboardShowing = false
                    }
                }
            })

            windowManager.addView(panelView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideActionPanel() {
        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            panelView = null
        }
    }

    fun hideBubble() {
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bubbleView = null
        }
    }

    fun hideAll() {
        hideBubble()
        hideActionPanel()
    }
}
