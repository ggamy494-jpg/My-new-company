package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ManagerPinDialog(
    isChangingPin: Boolean = false,
    onPinSuccess: () -> Unit,
    onSaveNewPin: ((String) -> Unit)? = null,
    verifyPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pinVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isChangingPin) Icons.Default.Lock else Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isChangingPin) "تغيير الرمز السري للمدير" else "تسجيل دخول حساب المدير",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isChangingPin) {
                    Text(
                        text = "أدخل الرمز السري للوصول إلى لوحة تحكم المدير وإجمالي الأواجل:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 6) enteredPin = it
                            errorMessage = null
                        },
                        label = { Text("الرمز السري للمدير") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                Icon(
                                    imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manager_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 الرمز الافتراضي للمدير هو: 1234 (يمكنك تغييره لاحقاً)",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    // Changing PIN
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            errorMessage = null
                        },
                        label = { Text("الرمز السري الحالي") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 6) newPin = it
                            errorMessage = null
                        },
                        label = { Text("الرمز السري الجديد (4-6 أرقام)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = {
                            if (it.length <= 6) confirmNewPin = it
                            errorMessage = null
                        },
                        label = { Text("تأكيد الرمز السري الجديد") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isChangingPin) {
                        if (verifyPin(enteredPin)) {
                            onPinSuccess()
                            onDismiss()
                        } else {
                            errorMessage = "الرمز السري غير صحيح! (الافتراضي هو 1234)"
                        }
                    } else {
                        if (!verifyPin(enteredPin)) {
                            errorMessage = "الرمز الحالي غير صحيح!"
                        } else if (newPin.length < 4) {
                            errorMessage = "الرمز الجديد يجب أن يتكون من 4 أرقام على الأقل"
                        } else if (newPin != confirmNewPin) {
                            errorMessage = "الرمز الجديد غير متطابق مع التأكيد"
                        } else {
                            onSaveNewPin?.invoke(newPin)
                            onDismiss()
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isChangingPin) "حفظ الرمز الجديد" else "دخول كمدير", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
