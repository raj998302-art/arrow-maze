package com.zenox.arrowmaze.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Modal dialog for editing the user's display name, player name, country
 * and avatar URL. On save, invokes the per-field callbacks (the ViewModel
 * routes them through `updateProfile`).
 *
 * @param initialDisplayName  Pre-filled display name.
 * @param initialPlayerName   Pre-filled player name.
 * @param initialCountry      Pre-filled ISO-2 country code (e.g. "US").
 * @param initialAvatarUrl    Pre-filled avatar URL (may be null).
 * @param onSave              Invoked with the four updated values when the
 *                            user taps "Save" and validation passes.
 * @param onDismiss           Invoked when the dialog is cancelled.
 */
@Composable
fun EditProfileDialog(
    initialDisplayName: String,
    initialPlayerName: String,
    initialCountry: String,
    initialAvatarUrl: String?,
    onSave: (displayName: String, playerName: String, country: String, avatarUrl: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var playerName by remember { mutableStateOf(initialPlayerName) }
    var country by remember { mutableStateOf(initialCountry) }
    var avatarUrl by remember { mutableStateOf(initialAvatarUrl ?: "") }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    var playerNameError by remember { mutableStateOf<String?>(null) }

    val cs = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit profile",
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; displayNameError = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    singleLine = true,
                    isError = displayNameError != null,
                    supportingText = displayNameError?.let { { Text(it) } },
                )
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it; playerNameError = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Player name") },
                    singleLine = true,
                    isError = playerNameError != null,
                    supportingText = playerNameError?.let { { Text(it) } },
                )
                CountryDropdown(
                    selectedCode = country,
                    onSelect = { country = it },
                )
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Avatar URL (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedDisplay = displayName.trim()
                    val trimmedPlayer = playerName.trim()
                    if (trimmedDisplay.length < 2) {
                        displayNameError = "Display name must be at least 2 characters."
                        return@TextButton
                    }
                    if (trimmedPlayer.isEmpty()) {
                        playerNameError = "Player name cannot be empty."
                        return@TextButton
                    }
                    onSave(
                        trimmedDisplay,
                        trimmedPlayer,
                        country,
                        avatarUrl.trim().takeIf(String::isNotBlank),
                    )
                },
            ) {
                Text("Save", color = cs.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        },
        containerColor = cs.surface,
        tonalElevation = 6.dp,
    )
}

/**
 * Dropdown picker for the user's country. Renders an [AssistChip] showing
 * the flag emoji + ISO-2 code; tapping it opens a [DropdownMenu] with the
 * full [COUNTRIES] list.
 */
@Composable
private fun CountryDropdown(
    selectedCode: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = COUNTRIES.firstOrNull { it.code == selectedCode } ?: COUNTRIES.first()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Country",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(SpacingTokens.xs))
        Row {
            AssistChip(
                onClick = { expanded = true },
                label = { Text("${selected.flag}  ${selected.code}") },
                trailingIcon = {
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.size(width = 240.dp, height = 360.dp),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState()),
                ) {
                    COUNTRIES.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.flag}  ${country.name}") },
                            onClick = {
                                onSelect(country.code)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Country list used by the [CountryDropdown]. */
internal data class Country(val code: String, val name: String, val flag: String)

internal val COUNTRIES: List<Country> = listOf(
    Country("US", "United States", "🇺🇸"),
    Country("CA", "Canada", "🇨🇦"),
    Country("GB", "United Kingdom", "🇬🇧"),
    Country("AU", "Australia", "🇦🇺"),
    Country("DE", "Germany", "🇩🇪"),
    Country("FR", "France", "🇫🇷"),
    Country("ES", "Spain", "🇪🇸"),
    Country("IT", "Italy", "🇮🇹"),
    Country("NL", "Netherlands", "🇳🇱"),
    Country("SE", "Sweden", "🇸🇪"),
    Country("NO", "Norway", "🇳🇴"),
    Country("FI", "Finland", "🇫🇮"),
    Country("DK", "Denmark", "🇩🇰"),
    Country("PL", "Poland", "🇵🇱"),
    Country("RU", "Russia", "🇷🇺"),
    Country("UA", "Ukraine", "🇺🇦"),
    Country("TR", "Turkey", "🇹🇷"),
    Country("IN", "India", "🇮🇳"),
    Country("CN", "China", "🇨🇳"),
    Country("JP", "Japan", "🇯🇵"),
    Country("KR", "South Korea", "🇰🇷"),
    Country("SG", "Singapore", "🇸🇬"),
    Country("MY", "Malaysia", "🇲🇾"),
    Country("ID", "Indonesia", "🇮🇩"),
    Country("PH", "Philippines", "🇵🇭"),
    Country("TH", "Thailand", "🇹🇭"),
    Country("VN", "Vietnam", "🇻🇳"),
    Country("BR", "Brazil", "🇧🇷"),
    Country("AR", "Argentina", "🇦🇷"),
    Country("MX", "Mexico", "🇲🇽"),
    Country("CL", "Chile", "🇨🇱"),
    Country("CO", "Colombia", "🇨🇴"),
    Country("PE", "Peru", "🇵🇪"),
    Country("ZA", "South Africa", "🇿🇦"),
    Country("EG", "Egypt", "🇪🇬"),
    Country("NG", "Nigeria", "🇳🇬"),
    Country("SA", "Saudi Arabia", "🇸🇦"),
    Country("AE", "United Arab Emirates", "🇦🇪"),
    Country("IL", "Israel", "🇮🇱"),
    Country("NZ", "New Zealand", "🇳🇿"),
)

/** Looks up the flag emoji for an ISO-2 country code; returns 🏳 on miss. */
internal fun flagForCountry(code: String): String =
    COUNTRIES.firstOrNull { it.code.equals(code, ignoreCase = true) }?.flag ?: "🏳️"
