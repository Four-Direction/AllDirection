package com.fourDirection.allDirection.page.tinyApps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ArrowDropDown
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.ui.theme.GlowBlue

data class EmergencyContact(
    val title: String,
    val number: String,
    val icon: ImageVector,
    val color: Color
)

data class CountryEmergency(
    val name: String,
    val flag: String,
    val police: String,
    val ambulance: String,
    val fire: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyInfoPage(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    
    val countries = remember {
        listOf(
            CountryEmergency("United States", "🇺🇸", "911", "911", "911"),
            CountryEmergency("United Kingdom", "🇬🇧", "999", "999", "999"),
            CountryEmergency("European Union", "🇪🇺", "112", "112", "112"),
            CountryEmergency("Japan", "🇯🇵", "110", "119", "119"),
            CountryEmergency("South Korea", "🇰🇷", "112", "119", "119"),
            CountryEmergency("China", "🇨🇳", "110", "120", "119"),
            CountryEmergency("Australia", "🇦🇺", "000", "000", "000"),
            CountryEmergency("Canada", "🇨🇦", "911", "911", "911"),
            CountryEmergency("Thailand", "🇹🇭", "191", "1669", "199"),
            CountryEmergency("Singapore", "🇸🇬", "999", "995", "995"),
            CountryEmergency("France", "🇫🇷", "17", "15", "18"),
            CountryEmergency("Germany", "🇩🇪", "110", "112", "112"),
            CountryEmergency("Italy", "🇮🇹", "113", "118", "115"),
            CountryEmergency("Spain", "🇪🇸", "091", "061", "080"),
            CountryEmergency("India", "🇮🇳", "100", "102", "101"),
            CountryEmergency("Brazil", "🇧🇷", "190", "192", "193"),
            CountryEmergency("Mexico", "🇲🇽", "911", "911", "911"),
            CountryEmergency("Turkey", "🇹🇷", "155", "112", "110"),
            CountryEmergency("Vietnam", "🇻🇳", "113", "115", "114"),
            CountryEmergency("Malaysia", "🇲🇾", "999", "999", "999"),
            CountryEmergency("Indonesia", "🇮🇩", "110", "118", "113"),
            CountryEmergency("Philippines", "🇵🇭", "911", "911", "911"),
            CountryEmergency("United Arab Emirates", "🇦🇪", "999", "998", "997"),
            CountryEmergency("Hong Kong", "🇭🇰", "999", "999", "999"),
            CountryEmergency("Taiwan", "🇹🇼", "110", "119", "119"),
            CountryEmergency("Switzerland", "🇨🇭", "117", "144", "118"),
            CountryEmergency("Sweden", "🇸🇪", "112", "112", "112"),
            CountryEmergency("Norway", "🇳🇴", "112", "113", "110"),
            CountryEmergency("Netherlands", "🇳🇱", "112", "112", "112")
        ).sortedBy { it.name }
    }

    var selectedCountry by remember { mutableStateOf(countries.find { it.name == "United States" } ?: countries[0]) }
    var expanded by remember { mutableStateOf(false) }

    val contacts = remember(selectedCountry) {
        listOf(
            EmergencyContact("Police", selectedCountry.police, Icons.Default.LocalPolice, Color(0xFF4285F4)),
            EmergencyContact("Ambulance", selectedCountry.ambulance, Icons.Default.LocalHospital, Color(0xFFEA4335)),
            EmergencyContact("Fire Department", selectedCountry.fire, Icons.Default.LocalFireDepartment, Color(0xFFFBBC05))
        )
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Emergency Info",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Country Selector
            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedCountry.flag, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Region", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Text(selectedCountry.name, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(Color(0xFF1A1A1A))
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 400.dp)
                ) {
                    countries.forEach { country ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(country.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(country.name, color = Color.White)
                                }
                            },
                            onClick = {
                                selectedCountry = country
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            contacts.forEach { contact ->
                EmergencyCard(contact)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Safe Travel Tips", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            TipItem("Share your itinerary with a connection or family member.")
            TipItem("Keep digital and physical copies of your passport.")
            TipItem("Learn basic emergency phrases in the local language.")
            TipItem("Keep your embassy contact details saved.")
        }
    }
}

@Composable
fun EmergencyCard(contact: EmergencyContact) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = contact.color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(contact.icon, contentDescription = null, tint = contact.color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(contact.number, color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${contact.number}")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.Green)
            }
        }
    }
}

@Composable
fun TipItem(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("•", color = GlowBlue, modifier = Modifier.padding(end = 8.dp))
        Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}
