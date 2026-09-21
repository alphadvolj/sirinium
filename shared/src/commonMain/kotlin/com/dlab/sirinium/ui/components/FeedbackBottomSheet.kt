package com.dlab.sirinium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.platform.LocalPlatformActions
import com.dlab.sirinium.ui.theme.expressiveBounceClick

enum class SharedFeedbackType(val label: String) {
    BUG("Ошибка"),
    SUGGESTION("Предложение")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    onDismiss: () -> Unit
) {
    val platformActions = LocalPlatformActions.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var feedbackType by remember { mutableStateOf(SharedFeedbackType.BUG) }
    var contactEmail by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }

    fun generateReportText(): String {
        return buildString {
            appendLine("=== SIRINIUM FEEDBACK REPORT ===")
            appendLine("Тип: ${feedbackType.label}")
            appendLine("Email: ${contactEmail.ifBlank { "Не указан" }}")
            appendLine("Платформа: ${platformActions.getPlatformName()}")
            appendLine("Версия приложения: 3.0.1")
            if (attachedImages.isNotEmpty()) {
                appendLine("Прикреплено файлов: ${attachedImages.size}")
            }
            appendLine()
            appendLine("=== ОПИСАНИЕ ===")
            appendLine(description.trim())
        }
    }

    fun sendReport() {
        val trimmed = description.trim()
        if (trimmed.length < 5) {
            descriptionError = "Пожалуйста, опишите подробнее (минимум 5 символов)"
            return
        }
        descriptionError = null

        val subject = "[Sirinium ${platformActions.getPlatformName()}] ${feedbackType.label}: ${trimmed.take(40)}"
        val reportBody = generateReportText()

        if (attachedImages.isNotEmpty()) {
            platformActions.shareFeedback(subject, reportBody, attachedImages)
        } else {
            val encodedSubject = subject.replace(" ", "%20")
            val encodedBody = reportBody.replace("\n", "%0A").replace(" ", "%20")
            val mailUrl = "mailto:dmitry@avh-vless.work?subject=$encodedSubject&body=$encodedBody"
            platformActions.openUrl(mailUrl)
        }
        platformActions.showToast("Открываем окно отправки...")
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Потряси и сообщи",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Обратная связь и сообщение об ошибках",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Segmented Type Selector: "Ошибка" / "Предложение"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SharedFeedbackTypeChip(
                        title = "Ошибка",
                        icon = Icons.Rounded.BugReport,
                        isSelected = feedbackType == SharedFeedbackType.BUG,
                        onClick = { feedbackType = SharedFeedbackType.BUG },
                        modifier = Modifier.weight(1f)
                    )
                    SharedFeedbackTypeChip(
                        title = "Предложение",
                        icon = Icons.Rounded.Lightbulb,
                        isSelected = feedbackType == SharedFeedbackType.SUGGESTION,
                        onClick = { feedbackType = SharedFeedbackType.SUGGESTION },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Contact info (email)
            item {
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Контактный email (необязательно)") },
                    placeholder = { Text("example@mail.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Description text field
            item {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            if (descriptionError != null && it.trim().length >= 5) {
                                descriptionError = null
                            }
                        },
                        label = {
                            Text(
                                if (feedbackType == SharedFeedbackType.BUG) "Что пошло не так? *"
                                else "Ваша идея или пожелание *"
                            )
                        },
                        placeholder = {
                            Text(
                                if (feedbackType == SharedFeedbackType.BUG) "Опишите проблему, с которой столкнулись..."
                                else "Какую функцию вы хотели бы видеть в Sirinium?"
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        isError = descriptionError != null,
                        supportingText = {
                            if (descriptionError != null) {
                                Text(
                                    text = descriptionError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Photo attachments section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Скриншоты или фото (${attachedImages.size}/3)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (attachedImages.size < 3) {
                            TextButton(
                                onClick = {
                                    val remaining = 3 - attachedImages.size
                                    platformActions.pickImages(remaining) { newUris ->
                                        attachedImages = (attachedImages + newUris).distinct().take(3)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить", fontSize = 13.sp)
                            }
                        }
                    }

                    if (attachedImages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachedImages.size) { index ->
                                val uri = attachedImages[index]
                                val filename = uri.substringAfterLast("/").substringAfterLast("\\").takeLast(20)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = filename,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = {
                                                attachedImages = attachedImages.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Удалить",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Send Action Button
            item {
                Button(
                    onClick = { sendReport() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .expressiveBounceClick(scaleDown = 0.96f) { sendReport() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (attachedImages.isNotEmpty()) "Поделиться отчетом с фото" else "Отправить по почте",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Secondary Quick Actions (Copy Report / GitHub Issues)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = generateReportText()
                            platformActions.copyToClipboard(text)
                            platformActions.showToast("Отчет скопирован в буфер обмена")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Скопировать", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            platformActions.openUrl("https://github.com/alphadvolj/sirinium/issues")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub Issue", fontSize = 13.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SharedFeedbackTypeChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
