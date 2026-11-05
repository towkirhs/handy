package com.towkirhs.handy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.towkirhs.handy.data.Vocab

/** The speaker's own words and the fixes Handy has learned. */
@Composable
fun WordsScreen(onBack: () -> Unit) {
    val words by Vocab.words.collectAsState()
    val fixes by Vocab.fixes.collectAsState()
    var newWord by remember { mutableStateOf("") }
    var heard by remember { mutableStateOf("") }
    var meant by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("My words", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Handy listens for these and fixes known mistakes. It learns a fix every time you correct a " +
                "dictation with Fix on the home screen, so it gets better the more you use it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SoftCard {
            Text("Words I use", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Names, places, subjects, Banglish words. For example: Towkir, Dhanmondi, PCR, tarikh.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    singleLine = true,
                    label = { Text("Add a word") },
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { Vocab.addWord(newWord); newWord = "" }) { Text("Add") }
            }
            for (w in words.asReversed()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w, modifier = Modifier.weight(1f))
                    TextButton(onClick = { Vocab.removeWord(w) }) { Text("Remove") }
                }
            }
        }

        SoftCard {
            Text("Fixes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "When Handy hears the left side, it writes the right side.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(value = heard, onValueChange = { heard = it }, singleLine = true, label = { Text("Handy hears") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = meant, onValueChange = { meant = it }, singleLine = true, label = { Text("I meant") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { Vocab.addFix(heard, meant); heard = ""; meant = "" }) { Text("Add fix") }
            if (fixes.isEmpty()) {
                Text("No fixes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            for (f in fixes.asReversed()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${f.heard}  →  ${f.meant}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { Vocab.removeFix(f) }) { Text("Remove") }
                }
            }
        }
    }
}
