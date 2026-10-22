package com.example.data.network

import android.util.Log
import com.example.data.model.DailyForecast
import com.example.data.model.Farm
import com.example.data.model.FarmWeather
import com.example.data.model.HourlyForecast
import com.example.data.model.ResultProvenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object WeatherService {

    private const val TAG = "WeatherService"

    // Test seam for injectable/overridable base URL
    var baseUrl: String = "https://api.open-meteo.com/v1/forecast"

    // In-memory persistence of the last successful live fetch timestamp
    var lastSuccessfulLiveFetchTime: String? = null

    /**
     * Fetches real-time live satellite meteorological data for a specific farm GPS location
     * using Open-Meteo high-resolution weather models.
     */
    suspend fun fetchLiveWeatherForFarm(farm: Farm): FarmWeather = withContext(Dispatchers.IO) {
        val lat = farm.latitude
        val lon = farm.longitude

        val separator = if (baseUrl.contains("?")) "&" else "?"
        val urlString = "$baseUrl" + separator +
                "latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,surface_pressure,dew_point_2m,uv_index,et0_fao_evapotranspiration,soil_temperature_0cm,soil_moisture_0_to_1cm" +
                "&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,et0_fao_evapotranspiration" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,et0_fao_evapotranspiration_sum,uv_index_max" +
                "&timezone=auto"

        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 9000
                readTimeout = 9000
                setRequestProperty("User-Agent", "KisanAI-SmartAgri/2.0")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val current = json.optJSONObject("current")
                val hourly = json.optJSONObject("hourly")
                val daily = json.optJSONObject("daily")

                val temp = current?.optDouble("temperature_2m", 30.0)?.toInt() ?: 30
                val feelsLike = current?.optDouble("apparent_temperature", temp.toDouble())?.toInt() ?: temp
                val humidity = current?.optInt("relative_humidity_2m", 60) ?: 60
                val windSpeed = current?.optDouble("wind_speed_10m", 12.0)?.toInt() ?: 12
                val windGusts = current?.optDouble("wind_gusts_10m", (windSpeed + 5).toDouble())?.toInt() ?: (windSpeed + 5)
                val windDeg = current?.optDouble("wind_direction_10m", 270.0) ?: 270.0
                val windDirectionStr = degreesToCompass(windDeg)
                val weatherCode = current?.optInt("weather_code", 0) ?: 0
                val rainMm = current?.optDouble("precipitation", 0.0) ?: 0.0
                val dewPoint = current?.optDouble("dew_point_2m", 18.0) ?: 18.0
                val pressure = current?.optDouble("surface_pressure", 1012.0)?.toInt() ?: 1012
                val uv = current?.optDouble("uv_index", 6.0)?.toInt() ?: (if (temp > 34) 8 else 5)
                val et0 = current?.optDouble("et0_fao_evapotranspiration", 4.2) ?: 4.2
                val soilTemp = current?.optDouble("soil_temperature_0cm", 25.5) ?: 25.5
                val soilMoistRaw = current?.optDouble("soil_moisture_0_to_1cm", 0.28) ?: 0.28
                val soilMoistPercent = (soilMoistRaw * 100).roundToInt().coerceIn(20, 95)

                val conditionInfo = interpretWeatherCode(weatherCode)
                val isSevere = weatherCode in listOf(95, 96, 99, 82, 65, 75) || windSpeed > 35 || rainMm > 15.0

                // Parse hourly forecast (Next 8 intervals)
                val hourlyList = mutableListOf<HourlyForecast>()
                if (hourly != null) {
                    val times = hourly.optJSONArray("time")
                    val temps = hourly.optJSONArray("temperature_2m")
                    val rainChances = hourly.optJSONArray("precipitation_probability")
                    val codes = hourly.optJSONArray("weather_code")
                    val winds = hourly.optJSONArray("wind_speed_10m")
                    val et0Hourly = hourly.optJSONArray("et0_fao_evapotranspiration")

                    if (times != null && temps != null && rainChances != null) {
                        val count = minOf(8, times.length())
                        for (i in 0 until count) {
                            val timeStr = times.optString(i)
                            val formattedTime = formatTime(timeStr, i)
                            val tVal = temps.optDouble(i, 30.0).toInt()
                            val rVal = rainChances.optInt(i, 10)
                            val cVal = codes?.optInt(i, 0) ?: 0
                            val wVal = winds?.optDouble(i, 10.0)?.toInt() ?: 10
                            val eVal = et0Hourly?.optDouble(i, 0.4) ?: 0.4
                            val icon = interpretWeatherCode(cVal).second
                            hourlyList.add(HourlyForecast(formattedTime, tVal, rVal, icon, wVal, eVal))
                        }
                    }
                }

                if (hourlyList.isEmpty()) {
                    hourlyList.addAll(
                        listOf(
                            HourlyForecast("11:00 AM", temp, 10, conditionInfo.second, windSpeed, 0.45),
                            HourlyForecast("01:00 PM", temp + 2, 15, conditionInfo.second, windSpeed + 2, 0.65),
                            HourlyForecast("03:00 PM", temp + 1, 20, conditionInfo.second, windSpeed + 3, 0.55),
                            HourlyForecast("05:00 PM", temp - 1, 30, "cloud", windSpeed + 1, 0.35),
                            HourlyForecast("07:00 PM", temp - 3, 20, "cloud", windSpeed, 0.15),
                            HourlyForecast("09:00 PM", temp - 5, 10, "sun", windSpeed - 2, 0.05)
                        )
                    )
                }

                // Parse 7-day agricultural forecast
                val dailyList = mutableListOf<DailyForecast>()
                var totalEt0Week = 0.0
                if (daily != null) {
                    val days = daily.optJSONArray("time")
                    val maxTemps = daily.optJSONArray("temperature_2m_max")
                    val minTemps = daily.optJSONArray("temperature_2m_min")
                    val precipMax = daily.optJSONArray("precipitation_probability_max")
                    val precipSum = daily.optJSONArray("precipitation_sum")
                    val et0Sum = daily.optJSONArray("et0_fao_evapotranspiration_sum")
                    val dCodes = daily.optJSONArray("weather_code")

                    if (days != null && maxTemps != null && minTemps != null) {
                        val dCount = minOf(7, days.length())
                        for (i in 0 until dCount) {
                            val dayLabel = if (i == 0) "Today" else if (i == 1) "Tomorrow" else formatDayLabel(days.optString(i), i)
                            val tMax = maxTemps.optDouble(i, 34.0).toInt()
                            val tMin = minOf(minTemps.optDouble(i, 24.0).toInt(), tMax - 5)
                            val rProb = precipMax?.optInt(i, 20) ?: 20
                            val rSum = precipSum?.optDouble(i, 0.0) ?: 0.0
                            val eSum = et0Sum?.optDouble(i, 4.2) ?: 4.2
                            val dCode = dCodes?.optInt(i, 0) ?: 0
                            val dCond = interpretWeatherCode(dCode).first
                            dailyList.add(DailyForecast(dayLabel, dCond, tMax, tMin, rProb, rSum, eSum))
                            if (i == 0) totalEt0Week = eSum
                        }
                    }
                }

                if (dailyList.isEmpty()) {
                    dailyList.addAll(
                        listOf(
                            DailyForecast("Today", conditionInfo.first, temp + 2, temp - 6, 20, 0.0, et0),
                            DailyForecast("Tomorrow", "Partly Cloudy", temp + 1, temp - 7, 25, 0.0, 4.0),
                            DailyForecast("Day 3", "Sunny & Clear", temp + 3, temp - 5, 10, 0.0, 4.5),
                            DailyForecast("Day 4", "Optimal Field Conditions", temp + 2, temp - 6, 5, 0.0, 4.1),
                            DailyForecast("Day 5", "Scattered Clouds", temp, temp - 8, 15, 0.0, 3.8),
                            DailyForecast("Day 6", "Clear Sky", temp + 1, temp - 7, 10, 0.0, 4.2),
                            DailyForecast("Day 7", "Pleasant", temp, temp - 8, 20, 0.0, 3.9)
                        )
                    )
                }

                val rainProb = dailyList.firstOrNull()?.rainPercent ?: 20
                val advisory = generateAdvisory(farm, temp, humidity, windSpeed, rainProb, weatherCode, et0)

                // Compute actionable agricultural insights
                val sprayingInsight = evaluateSprayingWindow(windSpeed, rainProb, rainMm, temp, humidity)
                val irrigationInsight = evaluateIrrigationNeed(et0, rainMm, soilMoistPercent)
                val diseaseInsight = evaluateDiseasePestRisk(temp, humidity, rainMm)
                val workabilityScore = evaluateFieldWorkability(rainProb, rainMm, soilMoistPercent, windSpeed)
                val (thiScore, livestockStress) = calculateLivestockTHI(temp, humidity)

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                return@withContext FarmWeather(
                    farmId = farm.id,
                    farmName = farm.name,
                    locationName = if (farm.village.isNotBlank()) "${farm.village}, ${farm.district}" else farm.location,
                    state = farm.state,
                    currentTempC = temp,
                    conditionText = conditionInfo.first,
                    conditionIcon = conditionInfo.second,
                    humidityPercent = humidity,
                    windSpeedKm = windSpeed,
                    rainProbabilityPercent = rainProb,
                    soilMoisturePercent = soilMoistPercent,
                    uvIndex = uv,
                    isSevereAlert = isSevere,
                    alertHeader = if (isSevere) "CRITICAL WEATHER ALERT • ${farm.district.uppercase()} ZONE" else null,
                    agriculturalAdvisory = advisory,
                    hourlyForecast = hourlyList,
                    weeklyForecast = dailyList,
                    feelsLikeTempC = feelsLike,
                    precipitationMm = rainMm,
                    dewPointC = dewPoint,
                    windGustsKm = windGusts,
                    windDirection = windDirectionStr,
                    surfacePressureHpa = pressure,
                    soilTemperatureC = soilTemp,
                    et0EvapotranspirationMm = et0,
                    sprayingWindowStatus = sprayingInsight.first,
                    sprayingReason = sprayingInsight.second,
                    irrigationAdvice = irrigationInsight,
                    diseaseRiskLevel = diseaseInsight.first,
                    diseaseRiskReason = diseaseInsight.second,
                    fieldWorkabilityScore = workabilityScore,
                    livestockThiScore = thiScore,
                    livestockStressLevel = livestockStress,
                    lastUpdatedTime = timeFormat,
                    provenance = ResultProvenance.LIVE,
                    lastLiveSuccessfulTime = timeFormat
                ).also {
                    lastSuccessfulLiveFetchTime = timeFormat
                }
            } else {
                Log.e(TAG, "Open-Meteo HTTP error returned code: ${connection.responseCode}")
                return@withContext FarmWeather(
                    farmId = farm.id,
                    farmName = farm.name,
                    locationName = if (farm.village.isNotBlank()) "${farm.village}, ${farm.district}" else farm.location,
                    state = farm.state,
                    currentTempC = 0,
                    conditionText = "Radar Unavailable (HTTP ${connection.responseCode})",
                    conditionIcon = "cloud",
                    humidityPercent = 0,
                    windSpeedKm = 0,
                    rainProbabilityPercent = 0,
                    soilMoisturePercent = 0,
                    uvIndex = 0,
                    isSevereAlert = false,
                    alertHeader = null,
                    agriculturalAdvisory = "Meteorological radar returned HTTP ${connection.responseCode}. Tap 'Refresh Radar' to retry.",
                    hourlyForecast = emptyList(),
                    weeklyForecast = emptyList(),
                    provenance = ResultProvenance.ERROR,
                    errorMessage = "HTTP ${connection.responseCode}"
                )
            }
        } catch (e: Exception) {
            if (e is org.json.JSONException) {
                Log.e(TAG, "Open-Meteo malformed JSON response: ${e.message}", e)
                return@withContext FarmWeather(
                    farmId = farm.id,
                    farmName = farm.name,
                    locationName = if (farm.village.isNotBlank()) "${farm.village}, ${farm.district}" else farm.location,
                    state = farm.state,
                    currentTempC = 0,
                    conditionText = "Malformed Weather Telemetry",
                    conditionIcon = "cloud",
                    humidityPercent = 0,
                    windSpeedKm = 0,
                    rainProbabilityPercent = 0,
                    soilMoisturePercent = 0,
                    uvIndex = 0,
                    isSevereAlert = false,
                    alertHeader = null,
                    agriculturalAdvisory = "Received malformed weather telemetry from server. Tap 'Refresh Radar' to retry.",
                    hourlyForecast = emptyList(),
                    weeklyForecast = emptyList(),
                    provenance = ResultProvenance.ERROR,
                    errorMessage = "Malformed response format"
                )
            } else {
                Log.i(TAG, "Network unavailable for live weather, falling back to offline estimate: ${e.message}")
                return@withContext computeDynamicFallback(farm)
            }
        }
    }

    private fun interpretWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("☀️ Clear & Sunny", "sun")
            1, 2 -> Pair("🌤️ Mostly Sunny / Partly Cloudy", "sun")
            3 -> Pair("☁️ Overcast & Cloudy", "cloud")
            45, 48 -> Pair("🌫️ Foggy & Mist", "cloud")
            51, 53, 55 -> Pair("🌦️ Light Drizzle", "rain")
            61, 63, 65 -> Pair("🌧️ Rain Showers", "rain")
            71, 73, 75 -> Pair("🌨️ Light Snowfall", "cloud")
            80, 81, 82 -> Pair("🌧️ Heavy Rain Showers", "rain")
            95 -> Pair("⛈️ Thunderstorm Alert", "storm")
            96, 99 -> Pair("⛈️ Thunderstorm with Hail", "storm")
            else -> Pair("🌤️ Fair & Stable", "sun")
        }
    }

    private fun degreesToCompass(deg: Double): String {
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = (((deg % 360) / 22.5) + 0.5).toInt() % 16
        return directions[index]
    }

    /**
     * Actionable Spraying Window Advisor
     * Evaluates chemical spray safety based on wind drift, rain washout, humidity, and temperature.
     */
    private fun evaluateSprayingWindow(
        windSpeed: Int,
        rainProb: Int,
        rainMm: Double,
        temp: Int,
        humidity: Int
    ): Pair<String, String> {
        return when {
            windSpeed > 20 -> {
                Pair(
                    "NOT_RECOMMENDED",
                    "High wind speed ($windSpeed km/h) will cause severe spray drift onto non-target crops and waste active chemicals."
                )
            }
            rainProb > 45 || rainMm > 1.0 -> {
                Pair(
                    "NOT_RECOMMENDED",
                    "Rainfall forecasted ($rainProb% chance, ${rainMm}mm). Rain within 4-6 hours will wash off pesticide / foliar spray."
                )
            }
            temp > 35 -> {
                Pair(
                    "CAUTION",
                    "High temperature ($temp°C) causes rapid droplet evaporation and foliar scorch. Spray only early morning (6-9 AM) or dusk."
                )
            }
            humidity < 35 -> {
                Pair(
                    "CAUTION",
                    "Low relative humidity ($humidity%). Droplets evaporate quickly before plant absorption. Add a suitable sticker/surfactant."
                )
            }
            windSpeed in 6..14 && rainProb < 20 && temp in 18..30 && humidity in 45..75 -> {
                Pair(
                    "OPTIMAL",
                    "Ideal spraying window! Calm breeze ($windSpeed km/h), mild temp ($temp°C), and no rain risk ensure maximum foliar uptake."
                )
            }
            else -> {
                Pair(
                    "OPTIMAL",
                    "Favorable spraying conditions. Recommended window: 6:30 AM to 10:00 AM before afternoon temperature peak."
                )
            }
        }
    }

    /**
     * Actionable Smart Irrigation & FAO-56 Reference Crop Evapotranspiration Calculator
     */
    private fun evaluateIrrigationNeed(
        et0Mm: Double,
        rainMm: Double,
        soilMoisturePercent: Int
    ): String {
        val waterDeficitMm = et0Mm - rainMm
        val litersPerAcre = (waterDeficitMm * 4047).roundToInt()

        return when {
            rainMm > 8.0 -> {
                "Recent rainfall (${rainMm}mm) satisfies crop water needs. Pause irrigation for 36-48 hours to conserve groundwater and power."
            }
            soilMoisturePercent > 75 -> {
                "Soil moisture is high ($soilMoisturePercent%). Defer irrigation to avoid waterlogging and root asphyxiation."
            }
            waterDeficitMm > 4.5 -> {
                "Daily crop evapotranspiration loss is high (${String.format(Locale.getDefault(), "%.1f", et0Mm)} mm/day). Apply ~$litersPerAcre L/acre (~50 mins drip cycle) early morning."
            }
            soilMoisturePercent < 45 -> {
                "Root zone moisture is depleted ($soilMoisturePercent%). Immediate irrigation cycle recommended (~$litersPerAcre L/acre)."
            }
            else -> {
                "Normal ET0 water loss (${String.format(Locale.getDefault(), "%.1f", et0Mm)} mm/day). Run scheduled drip irrigation for 35-40 minutes."
            }
        }
    }

    /**
     * Actionable Disease & Fungal Infestation Radar
     */
    private fun evaluateDiseasePestRisk(
        temp: Int,
        humidity: Int,
        rainMm: Double
    ): Pair<String, String> {
        return when {
            humidity > 80 && temp in 18..28 -> {
                Pair(
                    "HIGH",
                    "High Fungal Disease Risk! Warm, humid conditions ($humidity% RH, $temp°C) trigger Yellow Rust, Late Blight, and Downy Mildew. Apply preventive bio-fungicide or Neem oil."
                )
            }
            humidity > 70 || rainMm > 3.0 -> {
                Pair(
                    "MODERATE",
                    "Moderate foliar disease risk due to moisture. Monitor leaf undersides for fungal spots, powdery mildew, and leaf curl."
                )
            }
            temp > 33 && humidity < 45 -> {
                Pair(
                    "MODERATE",
                    "Dry heat favors Sucking Pests (Aphids, Thrips & Whiteflies). Inspect shoot tips and deploy yellow sticky traps."
                )
            }
            else -> {
                Pair(
                    "LOW",
                    "Low disease & pest pressure under current sunny, balanced climate conditions."
                )
            }
        }
    }

    /**
     * Field Workability Score (0-100) for tractor tilling, sowing, harvesting
     */
    private fun evaluateFieldWorkability(
        rainProb: Int,
        rainMm: Double,
        soilMoisturePercent: Int,
        windSpeed: Int
    ): Int {
        var score = 90
        if (rainMm > 5.0) score -= 40
        else if (rainProb > 40) score -= 20

        if (soilMoisturePercent > 80) score -= 25
        else if (soilMoisturePercent < 35) score -= 10

        if (windSpeed > 25) score -= 15
        return score.coerceIn(20, 100)
    }

    /**
     * Temperature-Humidity Index (THI) for Livestock Heat/Cold Stress
     * THI = (1.8 * T + 32) - (0.55 - 0.0055 * RH) * (1.8 * T - 26)
     */
    private fun calculateLivestockTHI(temp: Int, humidity: Int): Pair<Double, String> {
        val t = temp.toDouble()
        val rh = humidity.toDouble()
        val thi = (1.8 * t + 32.0) - (0.55 - 0.0055 * rh) * (1.8 * t - 26.0)
        val roundedThi = (thi * 10).roundToInt() / 10.0

        val level = when {
            thi < 72.0 -> "Normal (Optimal Comfort)"
            thi in 72.0..78.0 -> "Mild Heat Stress"
            thi in 78.1..88.0 -> "Moderate Heat Stress"
            else -> "Severe Heat Stress Emergency"
        }
        return Pair(roundedThi, level)
    }

    private fun generateAdvisory(
        farm: Farm,
        temp: Int,
        humidity: Int,
        windSpeed: Int,
        rainProb: Int,
        weatherCode: Int,
        et0: Double
    ): String {
        val isLivestock = farm.farmType.contains("Dairy", true) ||
                farm.farmType.contains("Cattle", true) ||
                farm.farmType.contains("Poultry", true) ||
                farm.farmType.contains("Goat", true)

        return when {
            weatherCode in listOf(95, 96, 99) || (rainProb > 70 && windSpeed > 30) -> {
                if (isLivestock) {
                    "Severe thunderstorm & high winds ($windSpeed km/h) forecasted. Shelter all livestock in covered sheds, secure feed storage bags, and ensure drinking troughs are clean."
                } else {
                    "Heavy rain ($rainProb% chance) and thunderstorm expected. Postpone all pesticide and fertilizer applications. Clear field drainage outlets to prevent waterlogging."
                }
            }
            rainProb > 50 -> {
                "Moderate rain probability ($rainProb%). Avoid heavy overhead irrigation today. Drip lines can run at 50% capacity. Monitor for foliar fungal risks due to $humidity% humidity."
            }
            temp > 37 -> {
                if (isLivestock) {
                    "High temperature warning ($temp°C). Activate barn misting fans, provide fresh cool water with electrolytes, and shift feeding hours to early morning and late evening."
                } else {
                    "Heat stress alert ($temp°C, ET0: ${String.format(Locale.getDefault(), "%.1f", et0)} mm/day). Irrigate early in the morning before 8:00 AM to reduce evapotranspiration losses and prevent blossom drop."
                }
            }
            windSpeed > 22 -> {
                "High wind speeds ($windSpeed km/h). Avoid chemical spraying due to spray drift risks. Check support stakes for tall standing crops and shade nets."
            }
            else -> {
                if (isLivestock) {
                    "Optimal weather conditions ($temp°C, $humidity% RH). Maintain normal feeding and milking schedules. Ideal time for herd grooming and shed disinfection."
                } else {
                    "Optimal field conditions (ET0: ${String.format(Locale.getDefault(), "%.1f", et0)} mm/day). Great weather for weeding, fertilizer top-dressing, and regular drip irrigation cycles."
                }
            }
        }
    }

    private fun formatTime(isoTime: String, index: Int): String {
        return try {
            if (isoTime.contains("T")) {
                val timePart = isoTime.substringAfter("T")
                val hour = timePart.substringBefore(":").toIntOrNull() ?: (10 + index * 2)
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                String.format(Locale.getDefault(), "%02d:00 %s", displayHour, amPm)
            } else {
                "${10 + index * 2}:00 PM"
            }
        } catch (e: Exception) {
            "${10 + index * 2}:00 PM"
        }
    }

    private fun formatDayLabel(isoDate: String, index: Int): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(isoDate)
            if (date != null) {
                SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date)
            } else {
                "Day ${index + 1}"
            }
        } catch (e: Exception) {
            "Day ${index + 1}"
        }
    }

    private fun computeDynamicFallback(farm: Farm): FarmWeather {
        val latFactor = ((farm.latitude * 10).toInt() % 8)
        val temp = 29 + latFactor
        val humidity = 55 + (latFactor * 3)
        val windSpeed = 12 + latFactor
        val rainProb = 20 + (latFactor * 5)
        val condition = if (rainProb > 45) Pair("🌧️ Passing Showers", "rain") else Pair("🌤️ Partly Cloudy", "sun")
        val et0 = 4.2 + (latFactor * 0.1)

        val spraying = evaluateSprayingWindow(windSpeed, rainProb, 0.0, temp, humidity)
        val irrigation = evaluateIrrigationNeed(et0, 0.0, 60)
        val disease = evaluateDiseasePestRisk(temp, humidity, 0.0)
        val workability = evaluateFieldWorkability(rainProb, 0.0, 60, windSpeed)
        val (thi, stress) = calculateLivestockTHI(temp, humidity)

        return FarmWeather(
            farmId = farm.id,
            farmName = farm.name,
            locationName = if (farm.village.isNotBlank()) "${farm.village}, ${farm.district}" else farm.location,
            state = farm.state,
            currentTempC = temp,
            conditionText = condition.first,
            conditionIcon = condition.second,
            humidityPercent = humidity,
            windSpeedKm = windSpeed,
            rainProbabilityPercent = rainProb,
            soilMoisturePercent = 60,
            uvIndex = 7,
            isSevereAlert = false,
            alertHeader = null,
            agriculturalAdvisory = generateAdvisory(farm, temp, humidity, windSpeed, rainProb, 2, et0),
            hourlyForecast = listOf(
                HourlyForecast("10:00 AM", temp, rainProb, condition.second, windSpeed, 0.4),
                HourlyForecast("12:00 PM", temp + 2, rainProb, condition.second, windSpeed + 2, 0.6),
                HourlyForecast("02:00 PM", temp + 3, rainProb + 5, "cloud", windSpeed + 3, 0.5),
                HourlyForecast("04:00 PM", temp + 1, rainProb + 10, "cloud", windSpeed + 1, 0.3),
                HourlyForecast("06:00 PM", temp - 1, rainProb, "sun", windSpeed, 0.1),
                HourlyForecast("08:00 PM", temp - 3, 10, "sun", windSpeed - 2, 0.0)
            ),
            weeklyForecast = listOf(
                DailyForecast("Today", condition.first, temp + 2, temp - 5, rainProb, 0.0, et0),
                DailyForecast("Tomorrow", "Sunny & Clear", temp + 3, temp - 6, 10, 0.0, 4.4),
                DailyForecast("Day 3", "Partly Cloudy", temp + 1, temp - 7, 20, 0.0, 4.1),
                DailyForecast("Day 4", "Optimal Field Conditions", temp + 2, temp - 6, 5, 0.0, 4.3),
                DailyForecast("Day 5", "Scattered Showers", temp, temp - 7, 40, 3.5, 3.7),
                DailyForecast("Day 6", "Clear Sky", temp + 1, temp - 6, 15, 0.0, 4.2),
                DailyForecast("Day 7", "Pleasant & Calm", temp, temp - 8, 10, 0.0, 4.0)
            ),
            feelsLikeTempC = temp + 1,
            precipitationMm = 0.0,
            dewPointC = 17.5,
            windGustsKm = windSpeed + 4,
            windDirection = "NW",
            surfacePressureHpa = 1012,
            soilTemperatureC = 25.0,
            et0EvapotranspirationMm = et0,
            sprayingWindowStatus = spraying.first,
            sprayingReason = spraying.second,
            irrigationAdvice = irrigation,
            diseaseRiskLevel = disease.first,
            diseaseRiskReason = disease.second,
            fieldWorkabilityScore = workability,
            livestockThiScore = thi,
            livestockStressLevel = stress,
            lastUpdatedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            provenance = ResultProvenance.OFFLINE_ESTIMATE,
            lastLiveSuccessfulTime = lastSuccessfulLiveFetchTime
        )
    }
}
