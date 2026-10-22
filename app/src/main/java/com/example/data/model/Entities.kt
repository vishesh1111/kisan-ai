package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "primary_user",
    val name: String = "Ramesh Kumar",
    val phone: String = "+91 98765 43210",
    val farmerId: String = "PMK-9872-4102",
    val aadhaarNumber: String = "5432 8765 0912",
    val isPhoneVerified: Boolean = true,
    val age: Int = 38,
    val gender: String = "Male",
    val farmingType: String = "Full-Time",
    val selectedLanguage: String = "en",
    val isDarkTheme: Boolean = false,
    val houseNo: String = "Plot 14-B",
    val landmark: String = "Near Panchayat Bhavan",
    val state: String = "Punjab",
    val district: String = "Ludhiana",
    val village: String = "Samrala",
    val pinCode: String = "141114",
    val latitude: Double = 30.8987,
    val longitude: Double = 75.8573,
    val biggestProblem: String = "Pest Attacks & Water Scarcity",
    val primaryGoal: String = "Maximize Profit & Crop Health",
    val aiPreferredMode: String = "Text & Voice",
    val isOnboardingComplete: Boolean = true
)

@Entity(tableName = "farms")
data class Farm(
    @PrimaryKey val id: String,
    val name: String,
    val farmType: String = "Agriculture", // Agriculture, Poultry, Cattle/Dairy, Goat/Sheep, Fish/Aquaculture, Beekeeping, Horticulture, Vegetable, Mushroom, Nursery, Organic, Mixed, Other
    val location: String,
    val houseNo: String = "",
    val landmark: String = "",
    val village: String = "Samrala",
    val district: String = "Ludhiana",
    val state: String = "Punjab",
    val pinCode: String = "141114",
    val latitude: Double = 30.8987,
    val longitude: Double = 75.8573,
    val totalArea: Double = 5.0,
    val unit: String = "Acres", // Acres, Bigha, Hectares, Guntha, Cent, Sq Ft
    val ownership: String = "Ancestral Owned", // Ancestral Owned, Leased, Sharecropping, Govt Allotted
    val waterAvailability: String = "Borewell & Solar Pump",
    val electricityBackup: Boolean = true,
    val roadAccessible: Boolean = true,
    val soilType: String = "Loamy Alluvial",
    val terrain: String = "Plain / Flat",
    val primaryCropOrAnimal: String = "Wheat",
    val isPrimary: Boolean = false,

    // Dynamic Farm Health & Questionnaire Condition Assessment
    val healthScore: Int = 85,
    val healthStatus: String = "Good Health", // Optimal Condition, Good Health, Needs Attention, Critical Health Risk
    val soilTestingStatus: String = "Recent Test (Within 6 Months)",
    val fertilizerPractice: String = "Balanced Organic + NPK",
    val drainageCondition: String = "Excellent (No Waterlogging)",
    val waterSource: String = "Borewell & Solar Pump",
    val pestPressure: String = "None (Clean & Healthy)",
    val diseaseSigns: String = "No Disease Signs (Vibrant Green)",
    val seedQuality: String = "Certified Hybrid / Govt Verified",
    val boundaryFencing: String = "Solar / Wire Fenced",
    val soilHealthScore: Int = 85,
    val waterHealthScore: Int = 85,
    val pestHealthScore: Int = 90,
    val healthSummary: String = "Optimal conditions registered during setup.",
    val activeDay: Int = 1,
    val isWeekUnlocked: Boolean = false,
    val lastTaskCycleTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "land_parcels")
data class LandParcel(
    @PrimaryKey val id: String,
    val farmId: String,
    val name: String,
    val area: Double,
    val currentCrop: String,
    val soilType: String = "Loamy Alluvial",
    val ph: Double = 6.8,
    val nitrogen: Int = 45, // %
    val phosphorus: Int = 70, // %
    val potassium: Int = 30, // %
    val organicMatter: Double = 2.5, // %
    val moisture: Int = 65, // %
    val overallSoilScore: Int = 82,
    val verifiedSampleDate: String = "Oct 15, 2023"
)

@Entity(tableName = "crops")
data class CropRecord(
    @PrimaryKey val id: String,
    val farmId: String,
    val parcelId: String,
    val cropName: String,
    val variety: String,
    val growthStage: String,
    val healthScore: Int,
    val sowingDate: String,
    val expectedHarvest: String,
    val expectedYieldPerAcre: Double,
    val notes: String = "",
    val isNewCrop: Boolean = false,
    val daysElapsed: Int = 48
)

@Entity(tableName = "irrigation_zones")
data class IrrigationZone(
    @PrimaryKey val id: String,
    val farmId: String,
    val zoneName: String,
    val cropName: String,
    val isRunning: Boolean,
    val remainingMinutes: Int,
    val soilMoisture: Int,
    val isSmartScheduleEnabled: Boolean = true,
    val scheduledTime: String = "05:30 AM",
    val durationMinutes: Int = 45,
    val activeDaysSummary: String = "M, W, F",
    val estimatedUsageLiters: Int = 1250
)

@Entity(tableName = "livestock")
data class Livestock(
    @PrimaryKey val id: String,
    val farmId: String,
    val name: String,
    val tagId: String,
    val type: String, // Cow, Buffalo, Goat, Sheep
    val breed: String,
    val ageYears: Int,
    val barnSection: String = "Barn A, Section 2",
    val healthScore: Int = 85,
    val lastVaccineDaysAgo: Int = 12,
    val nextVaccineDays: Int = 48,
    val dailyMilkYield: Double = 14.5,
    val status: String = "Healthy",
    val insight: String = "Milk production is 5% higher than last week. Current feed mix is optimal.",
    // Smart Bluetooth / GPS Cattle Tag Tracker
    val hasBleTracker: Boolean = true,
    val bleDeviceName: String = "KisanTrack-BLE-04",
    val isInsideFence: Boolean = true,
    val distanceMetersFromCenter: Int = 45,
    val fenceRadiusMeters: Int = 150,
    val lastPingTime: String = "Just now",
    val batteryPercent: Int = 88
)

@Entity(tableName = "poultry_batches")
data class PoultryBatch(
    @PrimaryKey val id: String,
    val farmId: String,
    val farmName: String,
    val birdType: String, // Broiler, Layer
    val totalBirds: Int,
    val dailyEggs: Int,
    val feedRemainingDays: Int,
    val status: String = "Healthy"
)

@Entity(tableName = "market_prices")
data class MarketPrice(
    @PrimaryKey val id: String,
    val cropName: String,
    val mandiName: String,
    val pricePerQuintal: Int,
    val changePercent: Double,
    val isBestPrice: Boolean = false,
    val trend7Day: String = "Rising",
    val distanceKm: Int = 12
)

@Entity(tableName = "financial_transactions")
data class FinancialTransaction(
    @PrimaryKey val id: String,
    val farmId: String,
    val title: String,
    val category: String, // Fertilizer, Labor, Seeds, Irrigation, MandiSale
    val amount: Double,
    val isIncome: Boolean,
    val dateDisplay: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val date: String get() = dateDisplay
}

@Entity(tableName = "farm_tasks")
data class FarmTask(
    @PrimaryKey val id: String,
    val farmId: String,
    val title: String,
    val description: String,
    val priority: String, // High, Medium, Low
    val priorityLabel: String, // "PRIORITY 1", "DO NOW", "MONITOR"
    val targetArea: String, // e.g. "Field 2", "Poultry Farm"
    val category: String, // Irrigation, Pest, Fertilizer, Livestock
    val whyReason: String,
    val isCompleted: Boolean = false,
    val estimatedTimeMinutes: Int = 30
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isFromUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "Just now",
    val attachedImageUri: String? = null,
    val actionDoToday: String? = null,
    val actionAvoid: String? = null,
    val isDiagnosis: Boolean = false,
    val diagnosisTitle: String? = null,
    val diagnosisConfidence: Int? = null,
    val provenance: String = "LIVE",
    val errorMessage: String? = null
)

/**
 * Result wrapper for AI Advisor queries carrying honest provenance metadata.
 */
data class FarmAdvisorResult(
    val text: String,
    val provenance: ResultProvenance = ResultProvenance.LIVE,
    val errorMessage: String? = null
)

/**
 * In-memory draft representation for multi-farm onboarding list.
 */
data class FarmDraftItem(
    val id: String,
    var name: String,
    var type: String, // Agriculture, Poultry, Cattle/Dairy, etc.
    var area: String = "5.0",
    var unit: String = "Acres",
    var ownership: String = "Ancestral Owned",
    var primaryCropOrLivestock: String = "Wheat",
    var houseNo: String = "",
    var landmark: String = "",
    var village: String = "Samrala",
    var district: String = "Ludhiana",
    var state: String = "Punjab",
    var pinCode: String = "141114",
    var latitude: Double = 30.8987,
    var longitude: Double = 75.8573,
    var soilOrHousingType: String = "Loamy Alluvial",
    var soilTestingStatus: String = "Recent Test (Within 6 Months)",
    var fertilizerPractice: String = "Balanced Organic + NPK",
    var drainageCondition: String = "Excellent (No Waterlogging)",
    var waterAvailability: String = "Borewell & Solar Pump",
    var pestPressure: String = "None (Clean & Healthy)",
    var diseaseSigns: String = "No Disease Signs (Vibrant Green)",
    var seedQuality: String = "Certified Hybrid / Govt Verified",
    var boundaryFencing: String = "Solar / Wire Fenced",
    var countOrYield: String = "15",
    var details: String = "Personalized setup configured by farmer"
)

/**
 * E-commerce Agri Product Model for Buy Online.
 */
data class AgriProduct(
    val id: String,
    val name: String,
    val brand: String,
    val category: String, // "Fertilizers", "Seeds", "Pesticides", "Organic", "Equipment", "Cattle Feed"
    val price: Int,
    val originalPrice: Int,
    val discountPercent: Int,
    val rating: Double,
    val reviewsCount: Int,
    val packSize: String,
    val packSizesAvailable: List<String> = listOf("500 g", "1 kg", "5 kg", "25 kg", "50 kg"),
    val inStock: Boolean = true,
    val imageDrawableRes: Int,
    val description: String,
    val composition: String,
    val recommendedDosage: String,
    val isSubsidyEligible: Boolean = false,
    val deliveryDays: Int = 1
)

/**
 * Nearby Physical Agri Stores & Krishi Kendras for Buy Offline.
 */
data class AgriShop(
    val id: String,
    val name: String,
    val shopType: String, // "IFFCO Kisan Kendra", "Govt Seed Depot", "Agri Input Dealer", "Krishi Vigyan Kendra"
    val address: String,
    val distanceKm: Double,
    val rating: Double,
    val phone: String,
    val isOpenNow: Boolean = true,
    val openingHours: String = "8:00 AM - 7:30 PM",
    val availableStock: List<String> = listOf("Urea (45kg)", "DAP (50kg)", "NPK 19-19-19", "Certified Hybrid Seeds"),
    val directionsSummary: String = "Near Main GT Road, Opposite Bus Stand"
)

/**
 * Crop Listing for Sell Online.
 */
data class CropSellListing(
    val id: String,
    val cropName: String,
    val variety: String,
    val quantityQuintals: Double,
    val askingPricePerQuintal: Int,
    val moisturePercent: Double,
    val grade: String = "Grade A Premium",
    val pickupLocation: String,
    val listedDate: String = "Today",
    val status: String = "Active", // "Active", "Negotiating", "Sold"
    val activeBidsCount: Int = 3,
    val highestBidPrice: Int = 2450
)

/**
 * Digital Buyer Bid from verified millers/exporters.
 */
data class CropBuyerBid(
    val id: String,
    val buyerName: String,
    val buyerType: String, // "Flour Mill", "Agro Exporter", "Govt Procurement Agency", "FPO Buyer"
    val bidPricePerQuintal: Int,
    val totalAmount: Int,
    val pickupPromise: String = "Free Farm Pickup within 24 hours",
    val paymentTerms: String = "Instant Direct Bank Transfer (DBT) upon weighing",
    val rating: Double = 4.8
)

/**
 * Physical APMC Mandi for Sell Offline.
 */
data class PhysicalMandi(
    val id: String,
    val name: String,
    val district: String,
    val state: String,
    val distanceKm: Double,
    val todayArrivalsQuintals: Int,
    val openHours: String = "06:00 AM - 02:00 PM",
    val modalPriceToday: Int,
    val activeTradersCount: Int = 45,
    val mspAssistanceCounter: Boolean = true,
    val helplinePhone: String = "+91 161 240 1960"
)

/**
 * Soil & Water Testing Packages for At-Home Booking.
 */
data class SoilTestingPackage(
    val id: String,
    val title: String,
    val category: String, // "Soil", "Water", "Leaf Tissue", "Organic"
    val price: Int,
    val originalPrice: Int,
    val isGovtSubsidized: Boolean,
    val turnaroundDays: Int,
    val parametersTested: List<String>,
    val description: String,
    val sampleCollectionType: String = "Certified Technician Doorstep Sample Collection",
    val imageDrawableRes: Int
)

/**
 * Booking confirmation item.
 */
data class SoilTestBooking(
    val id: String,
    val packageTitle: String,
    val parcelName: String,
    val appointmentDate: String,
    val timeSlot: String,
    val address: String,
    val status: String = "Technician Assigned",
    val price: Int
)

/**
 * Weather & Severe Alert Notification Entity
 */
@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey val id: String,
    val farmId: String = "farm_1",
    val farmName: String = "Main Farm",
    val title: String,
    val message: String,
    val severity: String = "WARNING", // CRITICAL, WARNING, INFO
    val timeFormatted: String = "Just now",
    val isRead: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis()
)

/**
 * Government Schemes Model
 */
data class GovtScheme(
    val id: String,
    val title: String,
    val shortDescription: String,
    val category: String, // "Subsidies", "Direct Transfer", "Insurance", "Credit & Loans", "Solar & Tech"
    val subsidyAmount: String, // "Up to 80%", "₹6,000 / Year", "4% Interest Rate"
    val eligibility: String,
    val requiredDocuments: List<String>,
    val applicationProcess: String,
    val officialPortalUrl: String,
    val isFeaturedHome: Boolean = true
)

/**
 * Government Agri News Item
 */
data class GovtNewsItem(
    val id: String,
    val title: String,
    val dateDisplay: String,
    val category: String,
    val summary: String,
    val officialSource: String = "Ministry of Agriculture & Farmers Welfare"
)

/**
 * Farm Weather & Advisory Model
 */
data class FarmWeather(
    val farmId: String,
    val farmName: String,
    val locationName: String,
    val state: String,
    val currentTempC: Int,
    val conditionText: String, // "Heavy Thunderstorm Alert", "High Wind & Rain", "Sunny & Clear", "Frost Risk"
    val conditionIcon: String, // "storm", "rain", "sun", "cloud", "frost"
    val humidityPercent: Int,
    val windSpeedKm : Int,
    val rainProbabilityPercent: Int,
    val soilMoisturePercent: Int,
    val uvIndex: Int,
    val isSevereAlert: Boolean,
    val alertHeader: String?,
    val agriculturalAdvisory: String,
    val hourlyForecast: List<HourlyForecast>,
    val weeklyForecast: List<DailyForecast>,
    // Real-Time Meteorological Telemetry & Actionable Insight Indicators
    val feelsLikeTempC: Int = currentTempC,
    val precipitationMm: Double = 0.0,
    val dewPointC: Double = 18.0,
    val windGustsKm: Int = windSpeedKm + 4,
    val windDirection: String = "NW",
    val surfacePressureHpa: Int = 1012,
    val soilTemperatureC: Double = 25.0,
    val et0EvapotranspirationMm: Double = 4.2, // FAO-56 Reference Crop Evapotranspiration mm/day
    val sprayingWindowStatus: String = "OPTIMAL", // OPTIMAL, CAUTION, NOT_RECOMMENDED
    val sprayingReason: String = "Calm winds, no rain expected in next 8 hrs.",
    val irrigationAdvice: String = "Drip run recommended for 40-50 mins based on ET0 water deficit.",
    val diseaseRiskLevel: String = "LOW", // LOW, MODERATE, HIGH
    val diseaseRiskReason: String = "Foliar fungal risk is low under current humidity and temperature.",
    val fieldWorkabilityScore: Int = 85, // 0 - 100%
    val livestockThiScore: Double = 72.0, // Temperature-Humidity Index
    val livestockStressLevel: String = "Normal", // Normal, Mild Stress, Moderate Stress, Severe
    val lastUpdatedTime: String = "Just now",
    val provenance: ResultProvenance = ResultProvenance.LIVE,
    val lastLiveSuccessfulTime: String? = null,
    val errorMessage: String? = null
)

data class HourlyForecast(
    val timeLabel: String,
    val tempC: Int,
    val rainChance: Int,
    val icon: String,
    val windSpeedKm: Int = 10,
    val et0Rate: Double = 0.4
)

data class DailyForecast(
    val dayLabel: String,
    val condition: String,
    val tempMax: Int,
    val tempMin: Int,
    val rainPercent: Int,
    val rainMm: Double = 0.0,
    val et0Sum: Double = 4.2
)

data class CropPathologyResult(
    val diseaseName: String,
    val category: String, // Fungal Pathogen, Nutrient Deficiency, Bacterial Blight, Viral Infection, Pest Infestation
    val confidence: String, // e.g. "96% Match"
    val severity: String, // Mild, Moderate, Severe
    val symptomsSummary: String,
    val treatmentSteps: List<String>,
    val recommendedChemical: String,
    val organicRemedy: String,
    val soilAndNutrientAdvice: String,
    val isGeminiVisionAnalyzed: Boolean = true,
    val provenance: ResultProvenance = ResultProvenance.LIVE,
    val errorMessage: String? = null
)

data class SchemeCreditRecord(
    val schemeName: String,
    val amount: Double,
    val creditDate: String,
    val bankName: String,
    val accountNumber: String,
    val utrNumber: String,
    val category: String,
    val description: String,
    val status: String
)

data class CropGrowthVisionAnalysis(
    val cropType: String,
    val variety: String,
    val growthStage: String,
    val growthPercentage: Int,
    val daysGrown: Int,
    val totalDaysToMaturity: Int,
    val estimatedDaysRemaining: Int,
    val estimatedHarvestDate: String,
    val diseaseStatus: String,
    val healthScore: Int,
    val keyActionRecommendation: String,
    val confidence: String = "96% AI Accuracy",
    val provenance: ResultProvenance = ResultProvenance.LIVE,
    val errorMessage: String? = null
)

@Entity(tableName = "soil_health_records")
data class SoilHealthRecord(
    @PrimaryKey val id: String,
    val farmId: String,
    val dateLabel: String, // e.g. "14 Aug", "16 Aug", "18 Aug", "20 Aug", "Today"
    val timestamp: Long = System.currentTimeMillis(),
    val phLevel: Double = 6.8,
    val moisturePercent: Double = 58.0,
    val nitrogenKgHa: Int = 142,
    val phosphorusKgHa: Int = 38,
    val potassiumKgHa: Int = 205,
    val organicCarbonPercent: Double = 0.72,
    val soilTemperatureCelsius: Double = 23.5,
    val electricalConductivityEc: Double = 1.15,
    val source: String = "Manual Entry", // "Manual Entry", "KrishiProbe BLE Sensor", "IoT Telemetry Probe", "Soil Health Card"
    val sensorDeviceName: String = "KrishiProbe-BLE-S4",
    val sensorBatteryPercent: Int = 94,
    val notes: String = "Optimal root-zone moisture after light drip cycle"
)

/**
 * Seasonal planting schedule milestones and smart irrigation reminders for crops.
 */
@Entity(tableName = "crop_tasks")
data class CropTask(
    @PrimaryKey val id: String,
    val farmId: String = "farm_1",
    val cropName: String = "Wheat",
    val season: String = "Rabi", // Kharif, Rabi, Zaid, Perennial, All Season
    val stage: String = "Land Preparation & Basal", // Land Preparation, Sowing, Tillering, Flowering, Ripening, Harvest
    val taskTitle: String,
    val taskDescription: String,
    val isIrrigationReminder: Boolean = false,
    val isPlantingSchedule: Boolean = false,
    val recommendedDateDisplay: String = "In 3 Days",
    val daysFromSowing: Int = 0,
    val irrigationDurationMinutes: Int = 45,
    val waterRequirementMm: Double = 35.0,
    val fertilizerRecommendation: String = "",
    val npkRatio: String = "120:60:40",
    val priority: String = "High", // High, Medium, Low
    val isCompleted: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val dueTimestamp: Long = System.currentTimeMillis() + 86400000L * 3
)

/**
 * Fertilizer Dosage Recommendation Output Model
 */
data class FertilizerDosageRecommendation(
    val cropName: String,
    val fieldArea: Double,
    val areaUnit: String,
    val recommendedNpkRatio: String, // e.g. "120 : 60 : 40 kg/ha" (or per acre)
    val pureNitrogenKg: Double,
    val purePhosphorusKg: Double,
    val purePotassiumKg: Double,
    val ureaBags45Kg: Double,
    val dapBags50Kg: Double,
    val mopBags50Kg: Double,
    val zincSulphateKg: Double,
    val nanoUreaBottles: Int,
    val basalDoseSummary: String,
    val topDressing1Summary: String,
    val topDressing2Summary: String,
    val organicCompostRecommendedTonnes: Double,
    val estimatedCostInr: Int,
    val applicationAdvice: String
)



