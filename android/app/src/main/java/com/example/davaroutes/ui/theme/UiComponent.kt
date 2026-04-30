package com.example.davaroutes.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import com.example.davaroutes.ui.theme.*
import androidx.compose.ui.unit.dp

@Composable
fun ModernInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        colors = modernTextFieldColors()
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Orange,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
fun modernTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Orange,
    unfocusedBorderColor = Color(0xFF35566B),
    focusedLabelColor = Orange,
    unfocusedLabelColor = MutedText,
    cursorColor = Orange,
    focusedTextColor = SoftWhite,
    unfocusedTextColor = SoftWhite,
    focusedContainerColor = Color(0xFF0B2133),
    unfocusedContainerColor = Color(0xFF0B2133)
)