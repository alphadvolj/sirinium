package com.dlab.sirinium.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FeedbackType(val label: String) {
    BUG("Ошибка"),
    SUGGESTION("Предложение")
}

object FeedbackHelper {

    private const val FEEDBACK_EMAIL = "dmitry@avh-vless.work"

    /**
     * Collects device information, recent logs, copies selected images,
     * and launches the default email client (e.g. Gmail) pre-filled with recipient,
     * subject, message body, and all file attachments.
     */
    suspend fun sendFeedback(
        context: Context,
        type: FeedbackType,
        contactEmail: String,
        description: String,
        attachedImageUris: List<Uri>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val feedbackDir = File(context.cacheDir, "feedback").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // 1. Prepare diagnostics and logcat output
            val diagnostics = collectDiagnostics(context, timestamp)
            val logs = collectLogcat()

            val logFile = File(feedbackDir, "sirinium_logs.txt")
            logFile.writeText("$diagnostics\n\n=== RECENT APP LOGCAT LOGS ===\n$logs")

            // 2. Prepare attachments list
            val attachmentFiles = mutableListOf<File>()
            attachmentFiles.add(logFile)

            attachedImageUris.forEachIndexed { index, uri ->
                try {
                    val extension = getExtensionFromUri(context, uri) ?: "jpg"
                    val targetFile = File(feedbackDir, "screenshot_${index + 1}.$extension")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (targetFile.exists() && targetFile.length() > 0) {
                        attachmentFiles.add(targetFile)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FeedbackHelper", "Failed to copy photo $uri", e)
                }
            }

            // 3. Create FileProvider content URIs
            val authority = "${context.packageName}.fileprovider"
            val fileUris = ArrayList<Uri>()
            attachmentFiles.forEach { file ->
                val contentUri = FileProvider.getUriForFile(context, authority, file)
                fileUris.add(contentUri)
            }

            // 4. Construct email subject and body
            val isCustom = AppIconManager.isCustomIconEnabled(context)
            val appTitle = if (isCustom) "Zernovium" else "Sirinium"
            val subject = when (type) {
                FeedbackType.BUG -> "[$appTitle] Отчет об ошибке"
                FeedbackType.SUGGESTION -> "[$appTitle] Предложение по улучшению"
            }

            val body = buildString {
                appendLine("Тип обращения: ${type.label}")
                appendLine("Контактный email: ${contactEmail.ifBlank { "Не указан" }}")
                appendLine()
                appendLine("Описание:")
                appendLine(description)
                appendLine()
                appendLine("----------------------------------------")
                appendLine("Системная информация:")
                appendLine("- Приложение: $appTitle 3.0.0")
                appendLine("- Устройство: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("- Версия Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("- Время отправки: $timestamp")
                appendLine("----------------------------------------")
                appendLine("(Логи приложения и прикрепленные файлы добавлены во вложения)")
            }

            // 5. Build Intent for sending email with attachments
            withContext(Dispatchers.Main) {
                dispatchEmailIntent(context, subject, body, fileUris)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("FeedbackHelper", "Error dispatching feedback email", e)
            false
        }
    }

    /**
     * Sends an email report about a missing group that the user specified manually,
     * including device diagnostics and app logs.
     */
    suspend fun sendMissingGroupReport(
        context: Context,
        groupName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val feedbackDir = File(context.cacheDir, "feedback").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val diagnostics = collectDiagnostics(context, timestamp)
            val logs = collectLogcat()

            val logFile = File(feedbackDir, "sirinium_logs.txt")
            logFile.writeText("$diagnostics\n\n=== RECENT APP LOGCAT LOGS ===\n$logs")

            val fileUris = ArrayList<Uri>()
            val authority = "${context.packageName}.fileprovider"
            fileUris.add(FileProvider.getUriForFile(context, authority, logFile))

            val subject = "[Sirinium] Запрос на добавление группы: $groupName"
            val body = buildString {
                appendLine("Здравствуйте!")
                appendLine()
                appendLine("В списке групп расписания Sirinium не найдена группа: $groupName")
                appendLine("Пользователь выбрал её вручную и отправил запрос на добавление.")
                appendLine("Пожалуйста, проверьте наличие и добавьте расписание для этой группы.")
                appendLine()
                appendLine("----------------------------------------")
                appendLine("Системная информация:")
                appendLine("- Приложение: Sirinium 3.0.0")
                appendLine("- Запрошенная группа: $groupName")
                appendLine("- Устройство: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("- Версия Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("- Время отправки: $timestamp")
                appendLine("----------------------------------------")
                appendLine("(Логи приложения прикреплены во вложении)")
            }

            withContext(Dispatchers.Main) {
                dispatchEmailIntent(context, subject, body, fileUris)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("FeedbackHelper", "Error sending missing group report", e)
            false
        }
    }

    private fun dispatchEmailIntent(
        context: Context,
        subject: String,
        body: String,
        fileUris: ArrayList<Uri>
    ) {
        val sendIntent = if (fileUris.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
            }
        } else if (fileUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, fileUris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND)
        }

        sendIntent.apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (fileUris.isNotEmpty()) {
                val clipData = ClipData.newRawUri("Feedback Attachments", fileUris[0])
                for (i in 1 until fileUris.size) {
                    clipData.addItem(ClipData.Item(fileUris[i]))
                }
                this.clipData = clipData
            }
        }

        val packageManager = context.packageManager

        // 1. Query installed applications that handle ACTION_SENDTO with mailto: (STRICTLY email apps)
        val mailtoQueryIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$FEEDBACK_EMAIL")
        }
        val emailResolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                mailtoQueryIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(mailtoQueryIntent, 0)
        }

        val emailPackages = emailResolveInfos
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filter { it != context.packageName }

        // 2. Build targeted send intents ONLY for discovered email clients
        val targetedEmailIntents = mutableListOf<Intent>()
        for (pkg in emailPackages) {
            val targetedIntent = Intent(sendIntent).apply {
                setPackage(pkg)
                clipData = sendIntent.clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val canHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    targetedIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                ).isNotEmpty()
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(targetedIntent, 0).isNotEmpty()
            }
            if (canHandle) {
                targetedEmailIntents.add(targetedIntent)
            }
        }

        // 3. If email apps with attachment support were found, present chooser with ONLY email apps
        if (targetedEmailIntents.isNotEmpty()) {
            val primaryEmailIntent = targetedEmailIntents.removeAt(0)
            val chooser = Intent.createChooser(primaryEmailIntent, "Отправить через почту...").apply {
                if (targetedEmailIntents.isNotEmpty()) {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedEmailIntents.toTypedArray<Parcelable>())
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(chooser)
                return
            } catch (e: Exception) {
                android.util.Log.e("FeedbackHelper", "Failed to launch email chooser", e)
            }
        }

        // 4. Fallback 1: Direct mailto chooser (also strictly email apps only)
        val directMailto = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$FEEDBACK_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val mailtoResolvers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                directMailto,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(directMailto, 0)
        }

        if (mailtoResolvers.isNotEmpty()) {
            val mailtoChooser = Intent.createChooser(directMailto, "Отправить через почту...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(mailtoChooser)
                return
            } catch (e: Exception) {
                android.util.Log.e("FeedbackHelper", "Failed to launch direct mailto chooser", e)
            }
        }

        // 5. Fallback 2: General email chooser directly with sendIntent
        try {
            val generalChooser = Intent.createChooser(sendIntent, "Отправить через почту...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(generalChooser)
            return
        } catch (e: Exception) {
            android.util.Log.e("FeedbackHelper", "Failed to launch general send intent chooser", e)
        }

        // 6. Ultimate Fallback: No email client installed on the device
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(
                ClipData.newPlainText(
                    "Отчет Sirinium",
                    "Кому: $FEEDBACK_EMAIL\nТема: $subject\n\n$body"
                )
            )
            Toast.makeText(
                context,
                "Почтовые клиенты не найдены. Текст отчета скопирован в буфер обмена для отправки на $FEEDBACK_EMAIL",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.util.Log.e("FeedbackHelper", "No email app found to send feedback", e)
        }
    }

    private fun collectDiagnostics(context: Context, timestamp: String): String {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val versionName = packageInfo?.versionName ?: "3.0.1"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 1L
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 1L
        }

        val runtime = Runtime.getRuntime()
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)
        val totalMemMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemMb = runtime.freeMemory() / (1024 * 1024)

        return buildString {
            appendLine("=== ДИАГНОСТИКА SIRINIUM ===")
            appendLine("Дата/Время: $timestamp")
            appendLine("Приложение: Sirinium $versionName (Код: $versionCode)")
            appendLine("Устройство: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
            appendLine("Плата: ${Build.BOARD}, Железо: ${Build.HARDWARE}")
            appendLine("Версия Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Сборка: ${Build.DISPLAY}")
            appendLine("Память JVM: Использовано ${totalMemMb - freeMemMb} MB / Всего ${totalMemMb} MB / Макс ${maxMemMb} MB")
            appendLine("Язык системы: ${Locale.getDefault().toLanguageTag()}")
        }
    }

    private fun collectLogcat(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "-t", "400"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val log = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                log.append(line).append("\n")
            }
            reader.close()
            process.destroy()
            if (log.isEmpty()) "Логи пусты или недоступны" else log.toString()
        } catch (e: Exception) {
            "Не удалось получить логи: ${e.message}"
        }
    }

    private fun getExtensionFromUri(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri) ?: return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }

    /**
     * Loads a sampled down thumbnail Bitmap from a Uri safely without OOM.
     */
    fun loadThumbnail(context: Context, uri: Uri, targetSize: Int = 180): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            var inSampleSize = 1
            while (options.outWidth / (inSampleSize * 2) >= targetSize && options.outHeight / (inSampleSize * 2) >= targetSize) {
                inSampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }
}
