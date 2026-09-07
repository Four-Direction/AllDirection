package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.api.CurrencyApiService
import com.fourDirection.allDirection.ui.theme.GlowBlue
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterPage(
    onDismiss: () -> Unit
) {
    val apiService = remember { CurrencyApiService() }
    var amountText by remember { mutableStateOf("1.0") }
    var sourceCurrency by remember { mutableStateOf("USD") }
    var targetCurrency by remember { mutableStateOf("EUR") }
    var exchangeRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var currencyNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var convertedAmount by remember { mutableStateOf("0.00") }
    
    val scrollState = rememberScrollState()

    // List of popular tourist currencies to keep the UI snappy
    val touristCurrencies = remember {
        listOf(
            "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "HKD", "INR",
            "NZD", "BRL", "ZAR", "TRY", "KRW", "SGD", "MXN", "MYR", "IDR", "PHP",
            "THB", "ILS", "AED", "SAR", "VND", "TWD", "NOK", "SEK", "DKK", "PLN",
            "EGP", "CZK", "HUF", "RON", "BGN"
        )
    }

    // Fetch rates whenever source currency changes
    LaunchedEffect(sourceCurrency) {
        val allRates = apiService.fetchLatestRates(sourceCurrency)
        if (allRates.isNotEmpty()) {
            // Filter to only include tourist-friendly currencies
            val rates = allRates.filterKeys { it in touristCurrencies || it == sourceCurrency }
            exchangeRates = rates
            
            // Derive currency names from the codes using java.util.Currency
            val names = rates.keys.associateWith { code ->
                try {
                    Currency.getInstance(code).getDisplayName(Locale.US)
                } catch (e: Exception) {
                    code
                }
            }.toMutableMap()

            currencyNames = names
        }
    }

    LaunchedEffect(amountText, targetCurrency, exchangeRates) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val rate = exchangeRates[targetCurrency] ?: 1.0
        convertedAmount = String.format(Locale.US, "%.2f", amount * rate)
    }

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
                .verticalScroll(scrollState)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Currency Converter",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Conversion Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Source input
                    CurrencyInputRow(
                        label = "From",
                        amount = amountText,
                        onAmountChange = { amountText = it },
                        selectedCurrency = sourceCurrency,
                        currencyNames = currencyNames,
                        onCurrencyChange = { sourceCurrency = it },
                        isEditable = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                val temp = sourceCurrency
                                sourceCurrency = targetCurrency
                                targetCurrency = temp
                            },
                            modifier = Modifier.background(GlowBlue, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target display
                    CurrencyInputRow(
                        label = "To",
                        amount = convertedAmount,
                        onAmountChange = {},
                        selectedCurrency = targetCurrency,
                        currencyNames = currencyNames,
                        onCurrencyChange = { targetCurrency = it },
                        isEditable = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Exchange info
            if (exchangeRates.containsKey(targetCurrency)) {
                val rate = exchangeRates[targetCurrency]!!
                Text(
                    text = "1 $sourceCurrency = $rate $targetCurrency",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Extra space at bottom
        }
    }
}

@Composable
fun CurrencyInputRow(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedCurrency: String,
    currencyNames: Map<String, String>,
    onCurrencyChange: (String) -> Unit,
    isEditable: Boolean
) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isEditable) {
                TextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = GlowBlue,
                        focusedIndicatorColor = GlowBlue,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f)
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = amount,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                )
            }

            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(start = 8.dp)) {
                Surface(
                    onClick = { expanded = true },
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getFlagEmoji(selectedCurrency),
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = selectedCurrency,
                            color = GlowBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(Color(0xFF1A1A1A))
                        .heightIn(max = 400.dp)
                ) {
                    // Sort currencies by code
                    currencyNames.toSortedMap().forEach { (code, name) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = getFlagEmoji(code), fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = code, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(text = name, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                }
                            },
                            onClick = {
                                onCurrencyChange(code)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

fun getFlagEmoji(currencyCode: String): String {
    // Comprehensive mapping for cases where first 2 letters don't work or are special
    val manualMapping = mapOf(
        "EUR" to "🇪🇺",
        "USD" to "🇺🇸",
        "GBP" to "🇬🇧",
        "JPY" to "🇯🇵",
        "AUD" to "🇦🇺",
        "CAD" to "🇨🇦",
        "CHF" to "🇨🇭",
        "CNY" to "🇨🇳",
        "HKD" to "🇭🇰",
        "INR" to "🇮🇳",
        "NZD" to "🇳🇿",
        "BRL" to "🇧🇷",
        "RUB" to "🇷🇺",
        "ZAR" to "🇿🇦",
        "TRY" to "🇹🇷",
        "KRW" to "🇰🇷",
        "SGD" to "🇸🇬",
        "MXN" to "🇲🇽",
        "MYR" to "🇲🇾",
        "IDR" to "🇮🇩",
        "PHP" to "🇵🇭",
        "THB" to "🇹🇭",
        "ILS" to "🇮🇱",
        "AED" to "🇦🇪",
        "AFN" to "🇦🇫",
        "ALL" to "🇦🇱",
        "AMD" to "🇦🇲",
        "ANG" to "🇳🇱",
        "AOA" to "🇦🇴",
        "ARS" to "🇦🇷",
        "AWG" to "🇦🇼",
        "AZN" to "🇦🇿",
        "BAM" to "🇧🇦",
        "BBD" to "🇧🇧",
        "BDT" to "🇧🇩",
        "BGN" to "🇧🇬",
        "BHD" to "🇧🇭",
        "BIF" to "🇧🇮",
        "BMD" to "🇧🇲",
        "BND" to "🇧🇳",
        "BOB" to "🇧🇴",
        "BSD" to "🇧🇸",
        "BTN" to "🇧🇹",
        "BWP" to "🇧🇼",
        "BYN" to "🇧🇾",
        "BZD" to "🇧🇿",
        "CDF" to "🇨🇩",
        "CLP" to "🇨🇱",
        "COP" to "🇨🇴",
        "CRC" to "🇨🇷",
        "CUP" to "🇨🇺",
        "CVE" to "🇨🇻",
        "CZK" to "🇨🇿",
        "DJF" to "🇩🇯",
        "DKK" to "🇩🇰",
        "DOP" to "🇩🇴",
        "DZD" to "🇩🇿",
        "EGP" to "🇪🇬",
        "ERN" to "🇪🇷",
        "ETB" to "🇪🇹",
        "FJD" to "🇫🇯",
        "FKP" to "🇫🇰",
        "FOK" to "🇫🇴",
        "GEL" to "🇬🇪",
        "GGP" to "🇬🇬",
        "GHS" to "🇬🇭",
        "GIP" to "🇬🇮",
        "GMD" to "🇬🇲",
        "GNF" to "🇬🇳",
        "GTQ" to "🇬🇹",
        "GYD" to "🇬🇾",
        "HNL" to "🇭🇳",
        "HRK" to "🇭🇷",
        "HTG" to "🇭🇹",
        "HUF" to "🇭🇺",
        "IMP" to "🇮🇲",
        "IQD" to "🇮🇶",
        "IRR" to "🇮🇷",
        "ISK" to "🇮🇸",
        "JEP" to "🇯🇪",
        "JMD" to "🇯🇲",
        "JOD" to "🇯🇴",
        "KES" to "🇰🇪",
        "KGS" to "🇰🇬",
        "KHR" to "🇰🇭",
        "KID" to "🇰🇮",
        "KMF" to "🇰🇲",
        "KWD" to "🇰🇼",
        "KYD" to "🇰🇾",
        "KZT" to "🇰🇿",
        "LAK" to "🇱🇦",
        "LBP" to "🇱🇧",
        "LKR" to "🇱🇰",
        "LRD" to "🇱🇷",
        "LSL" to "🇱🇸",
        "LYD" to "🇱🇾",
        "MAD" to "🇲🇦",
        "MDL" to "🇲🇩",
        "MGA" to "🇲🇬",
        "MKD" to "🇲🇰",
        "MMK" to "🇲🇲",
        "MNT" to "🇲🇳",
        "MOP" to "🇲🇴",
        "MRU" to "🇲🇷",
        "MUR" to "🇲🇺",
        "MVR" to "🇲🇻",
        "MWK" to "🇲🇼",
        "NAD" to "🇳🇦",
        "NGN" to "🇳🇬",
        "NIO" to "🇳🇮",
        "NOK" to "🇳🇴",
        "NPR" to "🇳🇵",
        "OMR" to "🇴🇲",
        "PAB" to "🇵🇦",
        "PEN" to "🇵🇪",
        "PGK" to "🇵🇬",
        "PKR" to "🇵🇰",
        "PLN" to "🇵🇱",
        "PYG" to "🇵🇾",
        "QAR" to "🇶🇦",
        "RON" to "🇷🇴",
        "RSD" to "🇷🇸",
        "RWF" to "🇷🇼",
        "SAR" to "🇸🇦",
        "SBD" to "🇸🇧",
        "SCR" to "🇸🇨",
        "SDG" to "🇸🇩",
        "SEK" to "🇸🇪",
        "SHP" to "🇸🇭",
        "SLE" to "🇸🇱",
        "SLL" to "🇸🇱",
        "SOS" to "🇸🇴",
        "SRD" to "🇸🇷",
        "SSP" to "🇸🇸",
        "STN" to "🇸🇹",
        "SYP" to "🇸🇾",
        "SZL" to "🇸🇿",
        "TJS" to "🇹🇯",
        "TMT" to "🇹🇲",
        "TND" to "🇹🇳",
        "TOP" to "🇹🇴",
        "TTD" to "🇹🇹",
        "TVD" to "🇹🇻",
        "TWD" to "🇹🇼",
        "TZS" to "🇹🇿",
        "UAH" to "🇺🇦",
        "UGX" to "🇺🇬",
        "UYU" to "🇺🇾",
        "UZS" to "🇺🇿",
        "VES" to "🇻🇪",
        "VND" to "🇻🇳",
        "VUV" to "🇻🇺",
        "WST" to "🇼🇸",
        "XAF" to "🇨🇲",
        "XCD" to "🇦🇬",
        "XDR" to "🌐",
        "XOF" to "🇸🇳",
        "XPF" to "🇵🇫",
        "YER" to "🇾🇪",
        "ZMW" to "🇿🇲",
        "ZWL" to "🇿🇼"
    )
    
    manualMapping[currencyCode]?.let { return it }
    
    // Fallback: derive from first two letters (works for ~80% of currencies)
    return try {
        if (currencyCode.length >= 2) {
            val countryCode = currencyCode.substring(0, 2).uppercase()
            val firstChar = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        } else {
            "🏳️"
        }
    } catch (e: Exception) {
        "🏳️"
    }
}
