package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.localization.t
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.SubScreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.example.ai.GeminiFarmAgent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.data.network.WeatherService

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    farms: List<Farm>,
    selectedFarmId: String,
    onSelectFarm: (String) -> Unit,
    parcels: List<LandParcel>,
    crops: List<CropRecord>,
    marketPrices: List<MarketPrice>,
    tasks: List<FarmTask> = emptyList(),
    onToggleTask: (FarmTask) -> Unit = {},
    onNavigateTab: (MainTab) -> Unit,
    onNavigateSubScreen: (SubScreen) -> Unit,
    onAskDetails: () -> Unit
) {
    val activeFarm = farms.find { it.id == selectedFarmId } ?: farms.firstOrNull()
    var homeWeather by remember(activeFarm?.id) { mutableStateOf<FarmWeather?>(null) }

    LaunchedEffect(activeFarm?.id, activeFarm?.latitude, activeFarm?.longitude) {
        if (activeFarm != null) {
            try {
                homeWeather = WeatherService.fetchLiveWeatherForFarm(activeFarm)
            } catch (e: Exception) {
                // Handled gracefully inside WeatherService fallback
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
// Today's Action Plan Card
        item {
            val completedCount = tasks.count { it.isCompleted }
            val totalCount = tasks.size
            val progressVal = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateSubScreen(SubScreen.TASKS_LIST) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Today's Action Plan",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (totalCount > 0) "$completedCount/$totalCount tasks completed" else "0/1 tasks completed",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressVal.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Color(0xFF005C22))
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onNavigateSubScreen(SubScreen.TASKS_LIST) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D1A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OPEN FULL PLAN", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }
        }

        // Grid of 4 Cards
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Weather
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.WEATHER_REPORT) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF80DEEA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFF006064))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Weather", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = homeWeather?.let { "${it.conditionText}, ${it.currentTempC}°C" } ?: "Clear, 28°C",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // Crop Scan
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.CROP_DOCTOR) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF8D6E63)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Crop Scan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "Check health",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Livestock
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.LIVESTOCK_LIST) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Livestock", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "2 alerts",
                                fontSize = 12.sp,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // Payouts
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.GOVT_SCHEMES) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF424242))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Payouts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "View relief",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                // Row 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Mastitis AI
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.MASTITIS_SCANNER) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFCDD2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFFC62828))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Mastitis AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "Scan udders",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // Poultry Audio
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.POULTRY_SCANNER) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFF9C4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFF57F17))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Poultry Audio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "Listen sounds",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                // Row 4
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // CowCatcher AI
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.CCTV_MONITOR) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE1BEE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF6A1B9A))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("CowCatcher", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "Live CCTV",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // Virtual Fencing
                    Card(
                        modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateSubScreen(SubScreen.VIRTUAL_FENCING) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFCC80)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFFE65100))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Virtual Fence", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "Geo-AI",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

    }

    }

@Composable
private fun HelplineRowItem(
    title: String,
    number: String,
    subtitle: String,
    onCall: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCall() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(
                color = PrimaryGreen,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

}

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropDamageReportDialog(
    farms: List<Farm>,
    onDismiss: () -> Unit,
    onCallOfficial: (String) -> Unit
) {
    var selectedFarmName by remember { mutableStateOf(farms.firstOrNull()?.name ?: "Main Farm") }
    var causeOfLoss by remember { mutableStateOf("Hailstorm & Heavy Rain") }
    var estimatedLoss by remember { mutableStateOf("50% - 70% Damage") }
    var isSubmitted by remember { mutableStateOf(false) }

    val causes = listOf("Hailstorm & Heavy Rain", "Pest / Insect Attack", "Unseasonal Drought", "Flood & Waterlogging", "Accidental Fire")
    val lossPercentages = listOf("20% - 40% Partial", "50% - 70% Heavy Damage", "80% - 100% Complete Destroyed")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null, tint = AlertRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report Crop Damage to Govt", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AlertRed)
            }
        },
        text = {
            if (!isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Under PM Crop Insurance rules, crop damage due to weather/pesticides must be reported within 72 hours.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Affected Farm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(farms) { farm ->
                            FilterChip(
                                selected = selectedFarmName == farm.name,
                                onClick = { selectedFarmName = farm.name },
                                label = { Text(farm.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text("Cause of Crop Damage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(causes) { cause ->
                            FilterChip(
                                selected = causeOfLoss == cause,
                                onClick = { causeOfLoss = cause },
                                label = { Text(cause, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AlertRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text("Estimated Loss Severity", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(lossPercentages) { loss ->
                            FilterChip(
                                selected = estimatedLoss == loss,
                                onClick = { estimatedLoss = loss },
                                label = { Text(loss, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AlertRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(SecondaryContainerGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
                    }
                    Text("Report Registered Successfully!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    Text(
                        text = "Report Reference #PMFBY-2026-8912\n\nCall the official hotline below to connect immediately with the Krishi Adhikari officer for field inspection.",
                        fontSize = 12.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            if (!isSubmitted) {
                Button(
                    onClick = { isSubmitted = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Submit Damage Claim Report", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = { onCallOfficial("18002005142") },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Govt Loss Hotline Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

enum class PolicyStatus {
    ACTIVE,
    EXPIRED
}

data class FarmInsurancePolicy(
    val planName: String,
    val policyNumber: String,
    val sumInsuredPerAcre: Double,
    val farmerPremiumPerAcre: Double,
    val govtSubsidyPercent: Int,
    val validFrom: String,
    val validTill: String,
    val status: PolicyStatus
)

@Composable
fun CropInsuranceAssessmentDialog(
    farms: List<Farm>,
    onDismiss: () -> Unit,
    onNavigateToFarms: () -> Unit
) {
    if (farms.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFD97706))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crop Insurance & Disaster Relief", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "No Farm Added Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "No farm data found. To check PMFBY crop insurance eligibility, enroll in subsidized schemes, or claim disaster compensation, you must add your farm first.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Button(
                        onClick = onNavigateToFarms,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Farm First", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
        return
    }

    // When farms are present
    var selectedFarmId by remember { mutableStateOf(farms.first().id) }
    val selectedFarm = farms.find { it.id == selectedFarmId } ?: farms.first()

    // Dynamic Insurance Policy Records per Farm
    val policiesState = remember {
        mutableStateMapOf<String, FarmInsurancePolicy?>().apply {
            // Default seed: first farm has an active policy, others can get new insurance
            if (farms.isNotEmpty()) {
                put(
                    farms[0].id,
                    FarmInsurancePolicy(
                        planName = "PMFBY Kharif/Rabi Comprehensive Crop Cover",
                        policyNumber = "PMFBY-${farms[0].name.take(4).uppercase()}-2026-8821",
                        sumInsuredPerAcre = 45000.0,
                        farmerPremiumPerAcre = 280.0,
                        govtSubsidyPercent = 90,
                        validFrom = "01 Apr 2026",
                        validTill = "31 Mar 2027",
                        status = PolicyStatus.ACTIVE
                    )
                )
            }
        }
    }

    var successMessage by remember { mutableStateOf<String?>(null) }
    val currentPolicy = policiesState[selectedFarm.id]
    var fieldInsuranceNumberInput by remember { mutableStateOf("") }
    var isDetailsUnlocked by remember { mutableStateOf(false) }
    var isApplyingNew by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crop Insurance & Govt Schemes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // STEP 1: If details not unlocked and not applying new, FIRST ask for Field Insurance Number
                if (!isDetailsUnlocked && !isApplyingNew) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondaryContainerGreen.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enter Field Insurance Number",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PrimaryGreen
                                )
                            }

                            Text(
                                text = "Please provide your Field Insurance Policy Number or Application ID to verify policy coverage, sum insured, subsidy, and disaster claims.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = fieldInsuranceNumberInput,
                                onValueChange = {
                                    fieldInsuranceNumberInput = it
                                    successMessage = null
                                },
                                label = { Text("Field Insurance Number *") },
                                placeholder = { Text("e.g. PMFBY-LUDH-2026-9104") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryGreen)
                                },
                                trailingIcon = {
                                    if (fieldInsuranceNumberInput.isNotEmpty()) {
                                        IconButton(onClick = { fieldInsuranceNumberInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )

                            Button(
                                onClick = {
                                    if (fieldInsuranceNumberInput.isBlank()) {
                                        fieldInsuranceNumberInput = currentPolicy?.policyNumber ?: "PMFBY-${selectedFarm.name.take(4).uppercase()}-2026-9104"
                                    }
                                    isDetailsUnlocked = true
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify & Show Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Text("Quick Options / Registered Fields:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        fieldInsuranceNumberInput = currentPolicy?.policyNumber ?: "PMFBY-${selectedFarm.name.take(4).uppercase()}-2026-8821"
                                        isDetailsUnlocked = true
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Use Farm Policy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isApplyingNew = true
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Apply New Policy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // STEP 2: When unlocked or applying new -> Display full farm & insurance details
                if (isDetailsUnlocked || isApplyingNew) {
                    // Verified Policy Banner with Switch Button
                    Surface(
                        color = SecondaryContainerGreen,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (fieldInsuranceNumberInput.isNotBlank()) "Policy #: $fieldInsuranceNumberInput" else "Registered Field Cover",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            TextButton(
                                onClick = {
                                    isDetailsUnlocked = false
                                    isApplyingNew = false
                                    fieldInsuranceNumberInput = ""
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("Change Number", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }

                    // Farm Selection Cards with Farm Address displayed below
                    Text("Select Farm:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(farms) { farm ->
                            val isSelected = farm.id == selectedFarmId
                            val farmPolicy = policiesState[farm.id]
                            val farmAddress = listOfNotNull(
                                farm.village.takeIf { it.isNotBlank() } ?: farm.location.takeIf { it.isNotBlank() },
                                farm.district.takeIf { it.isNotBlank() },
                                farm.state.takeIf { it.isNotBlank() }
                            ).joinToString(", ").ifBlank { "Location: Lat ${String.format("%.2f", farm.latitude)}, Lon ${String.format("%.2f", farm.longitude)}" }

                            Surface(
                                modifier = Modifier
                                    .widthIn(min = 160.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedFarmId = farm.id
                                        successMessage = null
                                    },
                                color = if (isSelected) SecondaryContainerGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = farm.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        if (farmPolicy != null) {
                                            Surface(
                                                color = if (farmPolicy.status == PolicyStatus.ACTIVE) PrimaryGreen else Color(0xFFD97706),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (farmPolicy.status == PolicyStatus.ACTIVE) "ACTIVE" else "EXPIRED",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = farmAddress,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )

                                    Text(
                                        text = "${farm.primaryCropOrAnimal} • ${farm.totalArea} ${farm.unit}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PrimaryGreen
                                    )
                                }
                            }
                        }
                    }

                    // Success notification banner
                    successMessage?.let { msg ->
                        Surface(
                            color = SecondaryContainerGreen,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(msg, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }

                    // Selected Farm Details Card
                    val fullAddress = listOfNotNull(
                        selectedFarm.village.takeIf { it.isNotBlank() } ?: selectedFarm.location.takeIf { it.isNotBlank() },
                        selectedFarm.district.takeIf { it.isNotBlank() },
                        selectedFarm.state.takeIf { it.isNotBlank() }
                    ).joinToString(", ").ifBlank { "Coordinates: ${selectedFarm.latitude}, ${selectedFarm.longitude}" }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Farm Address:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                            Text(
                                text = "${selectedFarm.name} — $fullAddress",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Crop: ${selectedFarm.primaryCropOrAnimal} | Registered Area: ${selectedFarm.totalArea} ${selectedFarm.unit}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Case 1: NO Insurance on Selected Farm -> Show "Get Insurance" with all available plans
                    if (currentPolicy == null || isApplyingNew) {
                        Text("Available Crop Insurance Plans for ${selectedFarm.name}:", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Text(
                            text = "Select a government-subsidized plan below to get 100% disaster cover for your standing ${selectedFarm.primaryCropOrAnimal} crop.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Plan 1: PMFBY
                        Surface(
                            color = SecondaryContainerGreen.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Pradhan Mantri Fasal Bima Yojana (PMFBY)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                    Surface(color = PrimaryGreen, shape = RoundedCornerShape(4.dp)) {
                                        Text("90% Subsidized", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Text("• Farmer Premium: ₹280 / Acre (Total: ₹${(280 * selectedFarm.totalArea).toInt()})", fontSize = 11.sp)
                                Text("• Total Sum Insured: ₹45,000 / Acre (Total: ₹${String.format("%,d", (45000 * selectedFarm.totalArea).toInt())})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("• Covers: Flood, drought, unseasonal rain, hailstorm, pest epidemics & post-harvest loss.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Button(
                                    onClick = {
                                        val newPolNumber = "PMFBY-${selectedFarm.name.take(4).uppercase()}-2026-9104"
                                        policiesState[selectedFarm.id] = FarmInsurancePolicy(
                                            planName = "PMFBY Kharif/Rabi Comprehensive Crop Cover",
                                            policyNumber = newPolNumber,
                                            sumInsuredPerAcre = 45000.0,
                                            farmerPremiumPerAcre = 280.0,
                                            govtSubsidyPercent = 90,
                                            validFrom = "01 Apr 2026",
                                            validTill = "31 Mar 2027",
                                            status = PolicyStatus.ACTIVE
                                        )
                                        fieldInsuranceNumberInput = newPolNumber
                                        isApplyingNew = false
                                        isDetailsUnlocked = true
                                        successMessage = "✓ Successfully enrolled ${selectedFarm.name} under PMFBY!"
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply & Get PMFBY Insurance", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // Plan 2: RWBCIS
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Weather-Based Crop Insurance (RWBCIS)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp)) {
                                        Text("Index Based", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Text("• Farmer Premium: ₹340 / Acre | Sum Insured: ₹40,000 / Acre", fontSize = 11.sp)
                                Text("• Automatic DBT payout triggered if rainfall deficit exceeds 40% or temperatures spike.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                OutlinedButton(
                                    onClick = {
                                        val newPolNumber = "RWBCIS-${selectedFarm.name.take(4).uppercase()}-2026-4720"
                                        policiesState[selectedFarm.id] = FarmInsurancePolicy(
                                            planName = "Restructured Weather-Based Crop Insurance (RWBCIS)",
                                            policyNumber = newPolNumber,
                                            sumInsuredPerAcre = 40000.0,
                                            farmerPremiumPerAcre = 340.0,
                                            govtSubsidyPercent = 85,
                                            validFrom = "01 Apr 2026",
                                            validTill = "31 Mar 2027",
                                            status = PolicyStatus.ACTIVE
                                        )
                                        fieldInsuranceNumberInput = newPolNumber
                                        isApplyingNew = false
                                        isDetailsUnlocked = true
                                        successMessage = "✓ Successfully enrolled ${selectedFarm.name} under RWBCIS!"
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Apply & Get RWBCIS Insurance", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                }
                            }
                        }
                    }

                    // Case 2: Insurance EXISTS on Selected Farm -> Show Active or Expired
                    if (currentPolicy != null && !isApplyingNew) {
                        if (currentPolicy.status == PolicyStatus.ACTIVE) {
                            // ACTIVE Policy
                            Surface(
                                color = SecondaryContainerGreen,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ACTIVE INSURANCE COVER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryGreen)
                                        }
                                        Surface(color = PrimaryGreen, shape = RoundedCornerShape(4.dp)) {
                                            Text("ACTIVE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }

                                    Text("Policy #: ${currentPolicy.policyNumber}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text("Plan: ${currentPolicy.planName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Sum Insured: ₹${String.format("%,d", (currentPolicy.sumInsuredPerAcre * selectedFarm.totalArea).toInt())} (₹${currentPolicy.sumInsuredPerAcre.toInt()}/Acre)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                    Text("Validity: ${currentPolicy.validFrom} to ${currentPolicy.validTill}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Coverage: 100% DBT bank payout for floods, hailstorms, drought, pests & post-harvest loss.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                successMessage = "Auto-claim intimation generated! Surveyor helpline: 1800-180-1551"
                                            },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                        ) {
                                            Text("Report Loss / Claim", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                policiesState[selectedFarm.id] = currentPolicy.copy(
                                                    status = PolicyStatus.EXPIRED,
                                                    validTill = "15 Feb 2026"
                                                )
                                                successMessage = "Status toggled to EXPIRED for demonstration."
                                            },
                                            modifier = Modifier.height(36.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Simulate Expired", fontSize = 10.5.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            // EXPIRED Policy -> Show Warning and "Renew Insurance"
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("POLICY EXPIRED", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AlertRed)
                                        }
                                        Surface(color = AlertRed, shape = RoundedCornerShape(4.dp)) {
                                            Text("EXPIRED", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }

                                    Text("Policy #: ${currentPolicy.policyNumber}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text("Plan: ${currentPolicy.planName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Expired On: ${currentPolicy.validTill}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                                    Text(
                                        text = "Your crop insurance for ${selectedFarm.name} has expired. Your current standing crop of ${selectedFarm.primaryCropOrAnimal} is unprotected against unseasonal rains, drought, or pest outbreaks.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF7F1D1D),
                                        lineHeight = 15.sp
                                    )

                                    Button(
                                        onClick = {
                                            val renewedNumber = "PMFBY-${selectedFarm.name.take(4).uppercase()}-2026-RENEW-99"
                                            policiesState[selectedFarm.id] = currentPolicy.copy(
                                                status = PolicyStatus.ACTIVE,
                                                validFrom = "01 Apr 2026",
                                                validTill = "31 Mar 2027",
                                                policyNumber = renewedNumber
                                            )
                                            fieldInsuranceNumberInput = renewedNumber
                                            successMessage = "✓ Insurance successfully renewed for ${selectedFarm.name} till 31 Mar 2027!"
                                        },
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                    ) {
                                        Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Renew Insurance Now", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

data class FarmerBankDetails(
    val accountHolderName: String = "Ramesh Kumar",
    val bankName: String = "State Bank of India",
    val accountNumber: String = "382910482918",
    val ifscCode: String = "SBIN0001892",
    val isAadhaarDbtLinked: Boolean = true,
    val kccLimit: Double = 300000.0,
    val kccAvailed: Double = 85000.0
)

@Composable
fun AiCropVisionScannerDialog(
    onDismiss: () -> Unit,
    onNavigateToCropDoctor: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var visionResult by remember { mutableStateOf<CropGrowthVisionAnalysis?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoSourceLabel by remember { mutableStateOf<String?>(null) }

    // Real Camera launcher that captures live photo from device camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            photoSourceLabel = "Live Camera Photo"
            isScanning = true
            scope.launch {
                val res = GeminiFarmAgent.analyzeCropGrowthVision(bitmap, "Standing Crop Field Photo")
                visionResult = res
                isScanning = false
            }
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    photoSourceLabel = "Field Gallery Photo"
                    isScanning = true
                    scope.launch {
                        val res = GeminiFarmAgent.analyzeCropGrowthVision(bitmap, "Crop Leaf Diagnosis Photo")
                        visionResult = res
                        isScanning = false
                    }
                }
            } catch (e: Exception) {
                // Fallback scan
                isScanning = true
                scope.launch {
                    val res = GeminiFarmAgent.analyzeCropGrowthVision(null, "Crop Leaf Diagnosis Photo")
                    visionResult = res
                    isScanning = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Crop & Disease Scanner", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Capture a live camera photo or upload an image. Gemini AI analyzes leaf foliage, crop growth stage %, days to harvest, and disease infection markers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Field Photo Scan Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Image", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }

                // Show captured image preview if available
                capturedBitmap?.let { bmp ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Captured Crop Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = photoSourceLabel ?: "Captured Field Image",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }

                if (isScanning) {
                    Surface(
                        color = SecondaryContainerGreen,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AI Analyzing leaf foliage, lesions & growth markers...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryGreen
                            )
                        }
                    }
                }

                // Detailed AI Vision Results Display
                visionResult?.let { result ->
                    if (result.provenance == ResultProvenance.ERROR) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                            modifier = Modifier.fillMaxWidth().testTag("crop_growth_vision_error")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = AlertRed,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ERROR",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Vision Scan Failed", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AlertRed)
                                }
                                Text(
                                    text = result.keyActionRecommendation,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Explanation: ${result.provenance.plainLanguageExplanation}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        isScanning = true
                                        scope.launch {
                                            val res = GeminiFarmAgent.analyzeCropGrowthVision(capturedBitmap, "Standing Crop Field Photo")
                                            visionResult = res
                                            isScanning = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry Vision Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = SecondaryContainerGreen.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (result.provenance == ResultProvenance.LIVE) PrimaryGreen.copy(alpha = 0.5f) else Color(0xFFF57C00).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Provenance Badge Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isLive = result.provenance == ResultProvenance.LIVE
                                    Surface(
                                        color = if (isLive) PrimaryGreen.copy(alpha = 0.12f) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isLive) PrimaryGreen else Color(0xFFE65100))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isLive) "LIVE AI VISION" else "OFFLINE ESTIMATE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isLive) PrimaryGreen else Color(0xFFE65100)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isLive) "Gemini Vision Active" else "Offline Heuristic (Not Live)",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = if (result.provenance == ResultProvenance.LIVE)
                                        "Analyzed live from image using Gemini Vision AI."
                                    else
                                        "Estimated using offline agronomic heuristics. Live AI is unavailable.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🌾 Crop: ${result.cropType} (${result.variety})",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = if (result.healthScore >= 80) PrimaryGreen else AlertRed,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Health: ${result.healthScore}%",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "📈 Growth: ${result.growthStage}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = "${result.growthPercentage}% grown",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                LinearProgressIndicator(
                                    progress = { result.growthPercentage / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = PrimaryGreen,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⏳ Est. Harvest: ${result.estimatedHarvestDate}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${result.estimatedDaysRemaining} days left",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }

                            Surface(
                                color = if (result.healthScore >= 80) MaterialTheme.colorScheme.surface else Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "🩺 Disease Check: ${result.diseaseStatus}",
                                        fontSize = 11.sp,
                                        color = if (result.healthScore >= 80) MaterialTheme.colorScheme.onSurface else Color(0xFF991B1B),
                                        lineHeight = 15.sp
                                    )
                                    if (result.keyActionRecommendation.isNotBlank()) {
                                        Text(
                                            text = "💡 Advice: ${result.keyActionRecommendation}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryGreen,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onNavigateToCropDoctor,
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Text("Open Crop Doctor / Treatment Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

@Composable
fun CattleTrackingModalDialog(
    cattleList: List<Livestock>,
    onDismiss: () -> Unit,
    onPairNewCattle: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isScanningBle by remember { mutableStateOf(false) }
    var selectedAnimalDetail by remember { mutableStateOf<Livestock?>(cattleList.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cattle Track & Fencing Alert", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (cattleList.isEmpty()) {
                    Surface(
                        color = SecondaryContainerGreen.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "No Cattle / Livestock Added",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No animals are registered for Bluetooth collar tracking or geofencing yet. Add your cattle to monitor live GPS/BLE proximity and wandering alerts.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = onPairNewCattle,
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Cattle / Pair Animal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Live Bluetooth collar tracking and pasture geofencing. Instant alert when any livestock steps outside 150m farm perimeter.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Cattle Items List
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cattleList.forEach { animal ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedAnimalDetail = animal },
                                color = if (!animal.hasBleTracker) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                } else if (!animal.isInsideFence) {
                                    Color(0xFFFEF2F2)
                                } else {
                                    SecondaryContainerGreen.copy(alpha = 0.5f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (!animal.hasBleTracker) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    else if (!animal.isInsideFence) Color(0xFFEF4444)
                                    else PrimaryGreen.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (!animal.hasBleTracker) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                    else if (!animal.isInsideFence) Color(0xFFFEE2E2)
                                                    else SecondaryContainerGreen
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (!animal.hasBleTracker) Icons.Default.SensorsOff
                                                else if (!animal.isInsideFence) Icons.Default.Warning
                                                else Icons.Default.Pets,
                                                contentDescription = null,
                                                tint = if (!animal.hasBleTracker) MaterialTheme.colorScheme.onSurfaceVariant
                                                else if (!animal.isInsideFence) Color(0xFFDC2626)
                                                else PrimaryGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = animal.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (animal.hasBleTracker) {
                                                Text(
                                                    text = "Device: ${animal.bleDeviceName} • ${animal.lastPingTime}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Text(
                                                    text = "No Bluetooth Device Attached",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF9A3412)
                                                )
                                            }
                                        }
                                    }

                                    // Right Status Indicator
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (animal.hasBleTracker) {
                                            if (animal.isInsideFence) {
                                                Surface(
                                                    color = PrimaryGreen,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "INSIDE (${animal.distanceMetersFromCenter}m)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "🔋 ${animal.batteryPercent}% • Safe",
                                                    fontSize = 10.5.sp,
                                                    color = PrimaryGreen,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            } else {
                                                Surface(
                                                    color = Color(0xFFDC2626),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "🚨 BEYOND (${animal.distanceMetersFromCenter}m)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Wandering Alert!",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "NO DATA",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Animal Detail Inside Modal
                    selectedAnimalDetail?.let { animal ->
                        Surface(
                            color = if (animal.isInsideFence) SecondaryContainerGreen else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Selected: ${animal.name} (${animal.breed})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (animal.isInsideFence) PrimaryGreen else Color(0xFFDC2626)
                                )
                                Text(
                                    text = if (animal.hasBleTracker) "Tag: ${animal.tagId} • Distance: ${animal.distanceMetersFromCenter}m from center (Fence: ${animal.fenceRadiusMeters}m)"
                                    else "No BLE collar attached. Pair a new Bluetooth tracker to enable geofencing.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isScanningBle = true
                                scope.launch {
                                    delay(1200)
                                    isScanningBle = false
                                }
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isScanningBle) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scanning...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ping All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onPairNewCattle,
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pair New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

@Composable
fun BankDetailsInputDialog(
    initialHolderName: String,
    onDismiss: () -> Unit,
    onSave: (FarmerBankDetails) -> Unit
) {
    var bankName by remember { mutableStateOf("State Bank of India") }
    var holderName by remember { mutableStateOf(initialHolderName) }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("SBIN0001892") }
    var isDbtLinked by remember { mutableStateOf(true) }

    val popularBanks = listOf("State Bank of India", "Punjab National Bank", "HDFC Bank", "Bank of Baroda", "Canara Bank", "ICICI Bank")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter Bank Account Details", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Please enter your bank account details for Aadhaar DBT scheme benefit payouts, PM-KISAN, and Kisan Credit Card (KCC).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Select Bank Name:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(popularBanks) { b ->
                        FilterChip(
                            selected = bankName == b,
                            onClick = { bankName = b },
                            label = { Text(b, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Account Holder Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Bank Account Number") },
                    placeholder = { Text("e.g. 384910284919") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = { ifscCode = it },
                    label = { Text("Bank IFSC Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryContainerGreen.copy(alpha = 0.5f))
                        .clickable { isDbtLinked = !isDbtLinked }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDbtLinked,
                        onCheckedChange = { isDbtLinked = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aadhaar Linked for Direct Benefit Transfer (DBT)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAcc = accountNumber.ifBlank { "382910482918" }
                    onSave(
                        FarmerBankDetails(
                            accountHolderName = holderName.ifBlank { "Ramesh Kumar" },
                            bankName = bankName,
                            accountNumber = finalAcc,
                            ifscCode = ifscCode.ifBlank { "SBIN0001892" },
                            isAadhaarDbtLinked = isDbtLinked
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Save & Open Accounts", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun BankAccountsAndCreditsDialog(
    bankDetails: FarmerBankDetails,
    onDismiss: () -> Unit,
    onEditBankDetails: () -> Unit
) {
    var selectedSchemeDetail by remember { mutableStateOf<SchemeCreditRecord?>(null) }

    val schemeCredits = remember(bankDetails) {
        listOf(
            SchemeCreditRecord(
                schemeName = "PM-KISAN Samman Nidhi (17th Installment)",
                amount = 2000.0,
                creditDate = "15 July 2026",
                bankName = bankDetails.bankName,
                accountNumber = "XXXX-${bankDetails.accountNumber.takeLast(4)}",
                utrNumber = "SBI-DBT-98124501",
                category = "Direct Income Transfer",
                description = "17th Installment direct income support credited under PM-KISAN for 2.5 Acres landholding.",
                status = "Credited via Aadhaar DBT"
            ),
            SchemeCreditRecord(
                schemeName = "PM-KUSUM Solar Pump Subsidy (80% Govt Share)",
                amount = 32000.0,
                creditDate = "28 May 2026",
                bankName = bankDetails.bankName,
                accountNumber = "XXXX-${bankDetails.accountNumber.takeLast(4)}",
                utrNumber = "NABARD-DBT-772109",
                category = "Equipment Subsidy",
                description = "80% Central & State Govt Subsidy credit for installation of 5HP Submersible Solar Pump Set.",
                status = "Credited via NABARD"
            ),
            SchemeCreditRecord(
                schemeName = "PMFBY Hailstorm Crop Damage Compensation",
                amount = 8500.0,
                creditDate = "10 March 2026",
                bankName = bankDetails.bankName,
                accountNumber = "XXXX-${bankDetails.accountNumber.takeLast(4)}",
                utrNumber = "PMFBY-CLM-449102",
                category = "Insurance Claim",
                description = "Crop Loss Compensation for unseasonal hailstorm & rain damage to Wheat crop.",
                status = "Credited via PMFBY"
            ),
            SchemeCreditRecord(
                schemeName = "Fertilizer Direct Subsidy (Nano Urea & DAP)",
                amount = 3500.0,
                creditDate = "05 January 2026",
                bankName = bankDetails.bankName,
                accountNumber = "XXXX-${bankDetails.accountNumber.takeLast(4)}",
                utrNumber = "IFFCO-DBT-102934",
                category = "Input Subsidy",
                description = "Direct Benefit Voucher subsidy for purchase of 10 bags IFFCO Nano Urea & DAP fertilizers.",
                status = "Credited via IFFCO DBT"
            )
        )
    }

    val totalBenefitsReceived = schemeCredits.sumOf { it.amount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bank Accounts & Scheme Credit", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bank Account Info Card
                Surface(
                    color = SecondaryContainerGreen,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bankDetails.bankName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PrimaryGreen)
                            Surface(
                                color = PrimaryGreen,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Aadhaar DBT Active", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                        Text("Holder: ${bankDetails.accountHolderName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Account: XXXX-${bankDetails.accountNumber.takeLast(4)} • IFSC: ${bankDetails.ifscCode}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Total Benefits Received Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Total Govt Benefits & Subsidies Received", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format("%,.0f", totalBenefitsReceived)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        Text("KCC Farm Credit Limit: ₹${String.format("%,.0f", bankDetails.kccLimit)} @ 4% Interest Subvention", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent DBT Scheme Credits", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onEditBankDetails) {
                        Text("Edit Bank Info", fontSize = 11.5.sp, color = PrimaryGreen)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    schemeCredits.forEach { record ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedSchemeDetail = record },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(record.schemeName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${record.creditDate} • ${record.status}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("+₹${String.format("%,.0f", record.amount)}", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )

    // Sub-receipt modal if tapped
    selectedSchemeDetail?.let { record ->
        AlertDialog(
            onDismissRequest = { selectedSchemeDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scheme DBT Receipt", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("₹${String.format("%,.0f", record.amount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    Text(record.schemeName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(record.description, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    Text("UTR: ${record.utrNumber}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                    Text("Date: ${record.creditDate}", fontSize = 11.5.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedSchemeDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }
}

@Composable
fun FarmerEmergencyHelplinesDialog(
    onDismiss: () -> Unit,
    onReportDamageClick: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = AlertRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Farmer Emergency & Helplines", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "24x7 Direct Toll-Free Helplines and Emergency Loss Support for farmers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Crop Destroyed / Loss Report Action Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReportDamageClick() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🚨 Report Crop Destroyed / Damage",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRedText
                                )
                                Text(
                                    text = "Connect with Govt Official & Claim within 72 hrs",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF410002)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AlertRedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Emergency Helplines Quick Call Items
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelplineRowItem(
                        title = "Kisan Call Centre (Govt Support)",
                        number = "1800-180-1551",
                        subtitle = "Free Agriculture Expert Advice in Local Language",
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001801551"))
                            context.startActivity(intent)
                        }
                    )

                    HelplineRowItem(
                        title = "PM Crop Insurance Loss Helpline",
                        number = "1800-200-5142",
                        subtitle = "Claim Compensation for Weather & Hail Damage",
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18002005142"))
                            context.startActivity(intent)
                        }
                    )

                    HelplineRowItem(
                        title = "District Agriculture Officer (Krishi Adhikari)",
                        number = "1800-180-2111",
                        subtitle = "Local District Govt Official for Field Verification",
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001802111"))
                            context.startActivity(intent)
                        }
                    )

                    HelplineRowItem(
                        title = "Pashu Chikitsa & Veterinary Helpline",
                        number = "1962",
                        subtitle = "24x7 Emergency Cattle & Livestock Disease Doctor",
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1962"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}
