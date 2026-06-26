package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object JsFileProcessor {

    /**
     * Extracts text from PDF, DOCX, or TXT file using JS-based pdf.js and mammoth.js
     * executed inside a background WebView container.
     *
     * Returns a Pair of (Sanitized Extracted Text, Page Count)
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractText(context: Context, uri: Uri, fileName: String): Pair<String, Int> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            var isResumed = false
            var webView: WebView? = null

            fun safeResume(result: Result<Pair<String, Int>>) {
                if (!isResumed) {
                    isResumed = true
                    if (result.isSuccess) {
                        continuation.resume(result.getOrThrow())
                    } else {
                        continuation.resumeWithException(result.exceptionOrNull() ?: Exception("Unknown error"))
                    }
                }
            }

            try {
                val extension = fileName.lowercase().substringAfterLast('.', "")
                val fileType = when {
                    extension == "pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
                    extension == "docx" || fileName.endsWith(".docx", ignoreCase = true) -> "docx"
                    else -> "txt"
                }

                // 1. Read file bytes and encode to Base64
                val contentResolver = context.contentResolver
                val inputStream: InputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Could not open file stream for $fileName")
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                if (bytes.isEmpty()) {
                    throw Exception("Uploaded file is empty")
                }
                
                val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

                // 2. Create the hidden WebView on Main thread
                val activeWebView = WebView(context.applicationContext)
                webView = activeWebView
                
                // Configure WebView settings securely and enable necessary API support
                val settings = activeWebView.settings
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.domStorageEnabled = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // 3. Define the Javascript Bridge Interface
                val jsBridge = object {
                    @JavascriptInterface
                    fun onExtractionSuccess(text: String, pageCount: Int) {
                        Handler(Looper.getMainLooper()).post {
                            safeResume(Result.success(Pair(text, pageCount)))
                            webView?.let { cleanupWebView(it) }
                        }
                    }

                    @JavascriptInterface
                    fun onExtractionFailure(error: String) {
                        Handler(Looper.getMainLooper()).post {
                            safeResume(Result.failure(Exception(error)))
                            webView?.let { cleanupWebView(it) }
                        }
                    }
                }

                activeWebView.addJavascriptInterface(jsBridge, "AndroidInterface")

                // 4. Set WebView client to listen for loading completion
                activeWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Trigger JS execution
                        activeWebView.evaluateJavascript("processFile('$base64Data', '$fileType')", null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        Handler(Looper.getMainLooper()).post {
                            safeResume(Result.failure(Exception("WebView error: $description (code: $errorCode)")))
                            webView?.let { cleanupWebView(it) }
                        }
                    }
                }

                activeWebView.loadUrl("file:///android_asset/file_processor.html")

                // Ensure cleanup if coroutine is cancelled
                continuation.invokeOnCancellation {
                    webView?.let { cleanupWebView(it) }
                }

            } catch (e: Exception) {
                safeResume(Result.failure(e))
                webView?.let { cleanupWebView(it) }
            }
        }
    }

    private fun cleanupWebView(webView: WebView) {
        try {
            webView.removeJavascriptInterface("AndroidInterface")
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
