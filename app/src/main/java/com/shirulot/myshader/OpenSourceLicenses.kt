package com.shirulot.myshader

import android.app.AlertDialog
import android.content.Context
import android.webkit.WebView

/** 离线显示随包声明；SDK 原始 NOTICE 较大，单独打开，不截断其内容。 */
object OpenSourceLicenses {
    fun show(context: Context) {
        val labels = context.resources.getStringArray(R.array.face_license_entries)
        val files = arrayOf("MediaPipe.txt", "Apache-2.0.txt", "NOTICE")
        AlertDialog.Builder(context)
            .setTitle(R.string.open_source_licenses)
            .setItems(labels) { _, index ->
                val document = WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.blockNetworkLoads = true
                    // 只展示固定的本地声明，内容按纯文本转义，不执行脚本。
                    val content = context.assets.open("licenses/${files[index]}")
                        .bufferedReader().use { it.readText() }
                    loadDataWithBaseURL(null,
                        "<meta name='viewport' content='width=device-width, initial-scale=1'><pre style='white-space:pre-wrap;overflow-wrap:anywhere'>" +
                            android.text.TextUtils.htmlEncode(content) + "</pre>",
                        "text/html", "UTF-8", null)
                }
                AlertDialog.Builder(context)
                    .setTitle(labels[index])
                    .setView(document)
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener { document.destroy() }
                    .show()
            }
            .show()
    }
}
