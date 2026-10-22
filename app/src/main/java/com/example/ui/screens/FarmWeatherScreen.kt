package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Farm
import com.example.data.model.FarmWeather
import com.example.data.model.ResultProvenance
import com.example.data.network.WeatherService
import com.example.ui.components.KisanTopAppBar
import com.example.ui.theme.AlertRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryContainerGreen
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun FarmWeatherScreen(
    farms: List<Farm>,
    selectedFarmId: String,
    onSelectFarm: (String) -> Unit,
    onSendAlertNotification: (farmName: String, title: String, message: String) -> Unit,
    onBack: () -> Unit
) {
    if (farms.isEmpty()) {
        Scaffold(
            topBar = {
                KisanTopAppBar(
                    title = "Meteorological Intelligence",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_weather_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "No Farm Selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Please add a farm with GPS coordinates on the map to fetch real-time live satellite weather, evapotranspiration indices, and agricultural advisories.",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
        return
    }

    // Active farm
    val activeFarm = farms.find { it.id == selectedFarmId } ?: farms.first()

    // Live weather state fetched via Open-Meteo GPS service
    var liveWeather by remember(activeFarm.id) { mutableStateOf<FarmWeather?>(null) }
    var isLoadingWeather by remember(activeFarm.id) { mutableStateOf(true) }
    var alertSentMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshWeather() {
        coroutineScope.launch {
            isLoadingWeather = true
            alertSentMessage = null
            try {
                val result = WeatherService.fetchLiveWeatherForFarm(activeFarm)
                liveWeather = result
            } catch (e: Exception) {
                // Handled gracefully inside WeatherService fallback
            } finally {
                isLoadingWeather = false
            }
        }
    }

    LaunchedEffect(activeFarm.id, activeFarm.latitude, activeFarm.longitude) {
        refreshWeather()
    }

    val rotationTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotationAngle by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            KisanTopAppBar(
                title = "Farm Weather & Climate Insights",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Farm Selector Horizontal Carousel
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Farm (${farms.size} Registered):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Live Refresh Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { if (!isLoadingWeather) refreshWeather() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Live Telemetry",
                                tint = PrimaryGreen,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(if (isLoadingWeather) rotationAngle else 0f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLoadingWeather) "Syncing..." else "Refresh Radar",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(farms) { farm ->
                            val isSelected = farm.id == activeFarm.id
                            Surface(
                                modifier = Modifier
                                    .clickable { onSelectFarm(farm.id) }
                                    .testTag("weather_farm_${farm.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = farm.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isLoadingWeather || liveWeather == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(color = PrimaryGreen)
                                Text("Connecting to Meteorological Radar & Satellites for ${activeFarm.name}...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else if (liveWeather!!.provenance == ResultProvenance.ERROR) {
                val farmWeather = liveWeather!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("weather_error_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertRed)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = AlertRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "RADAR SERVICE ERROR",
                                    color = AlertRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = farmWeather.conditionText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertRed,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = farmWeather.agriculturalAdvisory,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "Explanation: ${farmWeather.provenance.plainLanguageExplanation}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { refreshWeather() },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("weather_retry_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Radar Sync", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                val farmWeather = liveWeather!!

                // 1. Live Weather Hero Card with Full Meteorological Telemetry
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("weather_hero_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            val isLive = farmWeather.provenance == ResultProvenance.LIVE
                            val badgeBg = if (isLive) PrimaryGreen.copy(alpha = 0.12f) else Color(0xFFFFF3E0)
                            val badgeColor = if (isLive) PrimaryGreen else Color(0xFFE65100)

                            // Top Bar: Location + Realtime Badge + Updated Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${farmWeather.farmName} • ${farmWeather.locationName}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen,
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    color = badgeBg,
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
                                                .background(badgeColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isLive) "LIVE SATELLITE" else "Estimated, not live",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }
                            }

                            if (!isLive) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Estimated, not live",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = if (!farmWeather.lastLiveSuccessfulTime.isNullOrBlank())
                                                "Last live fetch: ${farmWeather.lastLiveSuccessfulTime}"
                                            else
                                                "No prior live fetch",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Temperature & Condition Hero Banner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${farmWeather.currentTempC}°C",
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Feels ${farmWeather.feelsLikeTempC}°C",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }

                                    Text(
                                        text = farmWeather.conditionText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "GPS: ${String.format(Locale.getDefault(), "%.4f", activeFarm.latitude)}, ${String.format(Locale.getDefault(), "%.4f", activeFarm.longitude)} • Synced ${farmWeather.lastUpdatedTime}",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val weatherIcon = when (farmWeather.conditionIcon) {
                                    "rain" -> Icons.Default.WaterDrop
                                    "storm" -> Icons.Default.Thunderstorm
                                    "cloud" -> Icons.Default.Cloud
                                    else -> Icons.Default.WbSunny
                                }
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = weatherIcon,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // 8-Point High Precision Meteorological Telemetry Grid
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    WeatherMetricItem("Rain Chance", "${farmWeather.rainProbabilityPercent}%", Icons.Default.WaterDrop, Modifier.weight(1f))
                                    WeatherMetricItem("Rainfall", "${farmWeather.precipitationMm} mm", Icons.Default.Grain, Modifier.weight(1f))
                                    WeatherMetricItem("Wind", "${farmWeather.windSpeedKm} km/h ${farmWeather.windDirection}", Icons.Default.Air, Modifier.weight(1f))
                                    WeatherMetricItem("Wind Gusts", "${farmWeather.windGustsKm} km/h", Icons.Default.Speed, Modifier.weight(1f))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    WeatherMetricItem("Humidity", "${farmWeather.humidityPercent}%", Icons.Default.Opacity, Modifier.weight(1f))
                                    WeatherMetricItem("Dew Point", "${farmWeather.dewPointC}°C", Icons.Default.Thermostat, Modifier.weight(1f))
                                    WeatherMetricItem("Soil Moisture", "${farmWeather.soilMoisturePercent}%", Icons.Default.Landscape, Modifier.weight(1f))
                                    WeatherMetricItem("Soil Temp", "${farmWeather.soilTemperatureC}°C", Icons.Default.Grass, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // General Farm Advisory Banner & Notification Trigger Button
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondaryContainerGreen)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Agronomy Directives for ${activeFarm.primaryCropOrAnimal}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = farmWeather.agriculturalAdvisory,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.5.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onSendAlertNotification(
                                        farmWeather.farmName,
                                        "⚠️ Climate Advisory: ${farmWeather.farmName}",
                                        farmWeather.agriculturalAdvisory
                                    )
                                    alertSentMessage = "Meteorological advisory logged to Farm Notifications!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("trigger_weather_alert_button")
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Advisory to Notifications", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            if (alertSentMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = alertSentMessage!!,
                                    color = PrimaryGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Hourly Rain & Temperature Forecast
                item {
                    Column {
                        Text(
                            text = "Next Hours Meteorological Breakdown",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(farmWeather.hourlyForecast) { hour ->
                                Card(
                                    modifier = Modifier.width(96.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(text = hour.timeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Icon(
                                            imageVector = when(hour.icon) {
                                                "rain" -> Icons.Default.WaterDrop
                                                "storm" -> Icons.Default.Thunderstorm
                                                "cloud" -> Icons.Default.Cloud
                                                else -> Icons.Default.WbSunny
                                            },
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(text = "${hour.tempC}°", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(text = "${hour.rainChance}%", fontSize = 10.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                        }
                                        Text(text = "${hour.windSpeedKm} km/h", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // 7-Day Farm Weather & Water Balance Forecast Table
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "7-Day Agronomic Forecast",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Rain mm / ET0 mm",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            farmWeather.weeklyForecast.forEachIndexed { idx, day ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = day.dayLabel,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (idx == 0) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(85.dp)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = day.condition,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (day.rainPercent > 0) {
                                            Surface(
                                                color = PrimaryGreen.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${day.rainPercent}%",
                                                    fontSize = 10.sp,
                                                    color = PrimaryGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${day.tempMax}° / ${day.tempMin}°",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (idx < farmWeather.weeklyForecast.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
