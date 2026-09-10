/*
 * Copyright (C) 2026 LooKeR & Contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.looker.kenko.R

/**
 * Ввод числа с клавиатуры.
 *
 * Линейка хороша, когда до нужного веса пара щелчков, и мучительна, когда до него полсотни.
 * Здесь число набирается целиком: поле открывается заполненным и выделенным, первая же цифра
 * стирает старое значение.
 */
@Composable
fun NumberInputDialog(
    title: String,
    value: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    modifier: Modifier = Modifier,
    allowDecimals: Boolean = true,
) {
    var field by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(0, value.length)))
    }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    val parsed = field.text.replace(',', '.').toFloatOrNull()
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focus),
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                isError = parsed == null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (allowDecimals) {
                        KeyboardType.Decimal
                    } else {
                        KeyboardType.Number
                    },
                ),
                textStyle = MaterialTheme.typography.headlineSmall,
            )
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = { parsed?.let(onConfirm) },
            ) {
                Text(text = stringResource(R.string.label_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.label_cancel))
            }
        },
    )
}
