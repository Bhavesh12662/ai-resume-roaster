package com.example.util

import android.annotation.SuppressLint
import android.content.Context
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object JsPdfGenerator {

    /**
     * Generates a beautifully formatted PDF report based on AnalysisResult JSON.
     * Returns the Base64 string of the PDF document.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun generatePdf(
        context: Context,
        jsonStr: String,
        tone: String,
        focusMode: String,
        applyPrivacy: Boolean,
        fileName: String
    ): ByteArray = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            var isResumed = false
            var webView: WebView? = null

            fun safeResume(result: Result<ByteArray>) {
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
                val activeWebView = WebView(context.applicationContext)
                webView = activeWebView

                val settings = activeWebView.settings
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.domStorageEnabled = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                val jsBridge = object {
                    @JavascriptInterface
                    fun onPdfGenerationSuccess(base64Pdf: String) {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                val bytes = Base64.decode(base64Pdf, Base64.DEFAULT)
                                safeResume(Result.success(bytes))
                            } catch (e: Exception) {
                                safeResume(Result.failure(e))
                            } finally {
                                webView?.let { cleanupWebView(it) }
                            }
                        }
                    }

                    @JavascriptInterface
                    fun onPdfGenerationFailure(error: String) {
                        Handler(Looper.getMainLooper()).post {
                            safeResume(Result.failure(Exception("jsPDF Error: $error")))
                            webView?.let { cleanupWebView(it) }
                        }
                    }
                }

                activeWebView.addJavascriptInterface(jsBridge, "AndroidPdfInterface")

                activeWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Escape quotes and backslashes in jsonStr to make it safe for JS evaluation
                        val escapedJson = jsonStr
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                        val escapedFileName = fileName.replace("'", "\\'")
                        val script = "generateReportPdf('$escapedJson', '$tone', '$focusMode', $applyPrivacy, '$escapedFileName')"
                        activeWebView.evaluateJavascript(script, null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        Handler(Looper.getMainLooper()).post {
                            safeResume(Result.failure(Exception("WebView load error: $description")))
                            webView?.let { cleanupWebView(it) }
                        }
                    }
                }

                activeWebView.loadUrl("file:///android_asset/pdf_generator.html")

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
            webView.removeJavascriptInterface("AndroidPdfInterface")
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
