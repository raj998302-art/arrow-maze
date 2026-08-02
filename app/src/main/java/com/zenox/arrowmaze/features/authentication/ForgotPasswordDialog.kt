package com.zenox.arrowmaze.features.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Branded Material 3 [AlertDialog] for the "Forgot password?" flow.
 *
 * The user enters their email; on submit, [onSubmit] is invoked with the
 * trimmed value. The dialog is dismissable via [onDismiss] (cancel button
 * or outside-tap). Basic email-shape validation gates the Submit button —
 * invalid input shows the [emailError] message beneath the field.
 *
 * @param initialEmail Pre-filled email (typically the value already typed
 *                     into the Auth screen's email field).
 * @param onSubmit     Invoked with the trimmed email when the user taps
 *                     "Send reset link" and the email passes shape validation.
 * @param onDismiss    Invoked when the dialog is cancelled.
 * @param isSending    When true, disables the Submit button.
 */
@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    isSending: Boolean = false,
) {
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var emailError by remember { mutableStateOf<String?>(null) }
    val cs = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reset your password",
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            ) {
                Text(
                    text = "We'll send a reset link to your email address.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(SpacingTokens.xs))
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_email)) },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isSending,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSending,
                onClick = {
                    val trimmed = email.trim()
                    if (!isValidEmail(trimmed)) {
                        emailError = "Enter a valid email address."
                        return@TextButton
                    }
                    onSubmit(trimmed)
                },
            ) {
                Text("Send reset link", color = cs.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = cs.onSurfaceVariant)
            }
        },
        containerColor = cs.surface,
        tonalElevation = 6.dp,
    )
}

/** Lightweight RFC-822-ish email shape check; sufficient for client-side gating. */
internal fun isValidEmail(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > 254) return false
    val regex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    return regex.matches(trimmed)
}
