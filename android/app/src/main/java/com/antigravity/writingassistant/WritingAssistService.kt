package com.antigravity.writingassistant

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WritingAssistService : AccessibilityService() {

    private var overlayManager: OverlayManager? = null

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                overlayManager?.hideAll()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("WritingAssistService", "Service Connected")
        overlayManager = OverlayManager(this)
        overlayManager?.listener = object : OverlayManager.OverlayListener {
            override fun onReplace(text: String) {
                performReplace(text)
            }
        }
        
        try {
            val filter = android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performReplace(newText: String) {
        val root = rootInActiveWindow ?: return
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        
        if (focusedNode.isEditable && !focusedNode.isPassword) {
            val currentText = focusedNode.text
            if (currentText != null) {
                val start = focusedNode.textSelectionStart
                val end = focusedNode.textSelectionEnd
                
                if (start >= 0 && end >= 0 && start != end) {
                    val sb = StringBuilder(currentText)
                    val min = if (start < end) start else end
                    val max = if (start > end) start else end
                    
                    sb.replace(min, max, newText)
                    
                    val arguments = android.os.Bundle()
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sb.toString())
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                } else {
                    val arguments = android.os.Bundle()
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }
            }
            focusedNode.recycle()
        }
        root.recycle()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            overlayManager?.hideAll()
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val source = event.source
            if (source != null) {
                val selectionStart = source.textSelectionStart
                val selectionEnd = source.textSelectionEnd
                
                // Logic Change: Explicitly hide if selection is cleared or invalid
                if (selectionStart != selectionEnd && selectionStart != -1 && selectionEnd != -1) {
                    if (!source.isPassword) {
                        val text = source.text
                        if (text != null) {
                            val selectedText = text.subSequence(selectionStart, selectionEnd).toString()
                            
                            // Context Extraction logic
                            var beforeText: String? = null
                            var afterText: String? = null
                            
                            try {
                                val min = if (selectionStart < selectionEnd) selectionStart else selectionEnd
                                val max = if (selectionStart > selectionEnd) selectionStart else selectionEnd
                                
                                val contextStart = if (min - 250 > 0) min - 250 else 0
                                val contextEnd = if (max + 250 < text.length) max + 250 else text.length
                                
                                if (min > 0) {
                                    beforeText = text.subSequence(contextStart, min).toString()
                                }
                                if (max < text.length) {
                                    afterText = text.subSequence(max, contextEnd).toString()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            Log.d("WritingAssistService", "Text: $selectedText | Pre: ${beforeText?.take(20)} | Post: ${afterText?.take(20)}")
                            
                            val packageName = source.packageName?.toString() ?: "unknown"
                            overlayManager?.updateText(selectedText, packageName, beforeText, afterText)
                            overlayManager?.showBubble()
                        }
                    }
                } else {
                    // Selection cleared or single cursor tap -> Hide Bubble
                    overlayManager?.hideAll()
                }
                
                source.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.d("WritingAssistService", "Service Interrupted")
        overlayManager?.hideAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        overlayManager?.hideAll()
    }
}
