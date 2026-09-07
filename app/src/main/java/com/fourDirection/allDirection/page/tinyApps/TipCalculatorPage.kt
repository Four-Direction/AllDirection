package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.ui.theme.GlowBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipCalculatorPage(onDismiss: () -> Unit) {
    var billAmount by remember { mutableStateOf("") }
    var tipPercentage by remember { mutableFloatStateOf(15f) }
    var splitCount by remember { mutableIntStateOf(1) }

    val bill = billAmount.toDoubleOrNull() ?: 0.0
    val tipAmount = bill * (tipPercentage / 100)
    val totalAmount = bill + tipAmount
    val amountPerPerson = totalAmount / splitCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Tip Calculator",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Result Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GlowBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total per person", color = Color.Black.copy(alpha = 0.7f))
                    Text(
                        text = "$${String.format("%.2f", amountPerPerson)}",
                        color = Color.Black,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Bill", color = Color.Black.copy(alpha = 0.7f))
                            Text("$${String.format("%.2f", totalAmount)}", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tip", color = Color.Black.copy(alpha = 0.7f))
                            Text("$${String.format("%.2f", tipAmount)}", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Inputs
            Text("Bill Amount", color = Color.White.copy(alpha = 0.6f))
            TextField(
                value = billAmount,
                onValueChange = { if (it.length <= 10) billAmount = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("$", color = Color.White) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = GlowBlue
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Tip Percentage: ${tipPercentage.toInt()}%", color = Color.White.copy(alpha = 0.6f))
            Slider(
                value = tipPercentage,
                onValueChange = { tipPercentage = it },
                valueRange = 0f..30f,
                steps = 5,
                colors = SliderDefaults.colors(thumbColor = GlowBlue, activeTrackColor = GlowBlue)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Split: $splitCount people", color = Color.White.copy(alpha = 0.6f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { if (splitCount > 1) splitCount-- }) {
                    Text("-", color = GlowBlue, fontSize = 32.sp)
                }
                Text(splitCount.toString(), color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 24.dp))
                IconButton(onClick = { splitCount++ }) {
                    Text("+", color = GlowBlue, fontSize = 32.sp)
                }
            }
        }
    }
}
