package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        Farm::class,
        LandParcel::class,
        CropRecord::class,
        IrrigationZone::class,
        Livestock::class,
        PoultryBatch::class,
        MarketPrice::class,
        FinancialTransaction::class,
        FarmTask::class,
        ChatMessage::class,
        AppNotification::class,
        SoilHealthRecord::class,
        CropTask::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kisanDao(): KisanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN provenance TEXT NOT NULL DEFAULT 'LIVE'")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN errorMessage TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kisan_ai_database"
                )
                    .addMigrations(MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.kisanDao())
                }
            }
        }
    }
}

suspend fun populateInitialData(dao: KisanDao) {
    // 1. Initial User Profile
    dao.insertUserProfile(
        UserProfile(
            id = "primary_user",
            name = "Ramesh Kumar",
            phone = "+91 98765 43210",
            farmerId = "PMK-9872-4102",
            aadhaarNumber = "5432 8765 0912",
            isPhoneVerified = true,
            age = 38,
            gender = "Male",
            farmingType = "Full-Time",
            selectedLanguage = "en",
            isDarkTheme = false,
            state = "Punjab",
            district = "Ludhiana",
            village = "Samrala",
            pinCode = "141114",
            latitude = 30.8987,
            longitude = 75.8573,
            biggestProblem = "Pest Attacks & Water Scarcity",
            primaryGoal = "Maximize Profit & Crop Health",
            aiPreferredMode = "Text & Voice",
            isOnboardingComplete = true
        )
    )

    // Note: Farms, Land Parcels, Crops, Livestock, Irrigation Zones, and Action Plan Tasks are created dynamically from user questionnaire inputs when adding farms.

    // Market Prices (Mandi)
    dao.insertMarketPrices(
        listOf(
            MarketPrice(
                id = "m_1",
                cropName = "Wheat",
                mandiName = "Local Mandi",
                pricePerQuintal = 2275,
                changePercent = 1.2,
                isBestPrice = true,
                trend7Day = "Rising",
                distanceKm = 6
            ),
            MarketPrice(
                id = "m_2",
                cropName = "Mustard",
                mandiName = "Jaipur Mandi",
                pricePerQuintal = 5420,
                changePercent = -0.8,
                isBestPrice = false,
                trend7Day = "Stable",
                distanceKm = 48
            ),
            MarketPrice(
                id = "m_3",
                cropName = "Cotton",
                mandiName = "Surat Mandi",
                pricePerQuintal = 7150,
                changePercent = 0.0,
                isBestPrice = false,
                trend7Day = "Holding",
                distanceKm = 120
            ),
            MarketPrice(
                id = "m_4",
                cropName = "Paddy / Rice (Basmati)",
                mandiName = "Amritsar Mandi",
                pricePerQuintal = 3850,
                changePercent = 3.4,
                isBestPrice = true,
                trend7Day = "Rising Fast",
                distanceKm = 85
            ),
            MarketPrice(
                id = "m_5",
                cropName = "Sugarcane",
                mandiName = "Jalandhar Mill Mandi",
                pricePerQuintal = 390,
                changePercent = 0.5,
                isBestPrice = false,
                trend7Day = "Stable",
                distanceKm = 34
            )
        )
    )

    // Initial AI Conversation Context
    dao.insertChatMessage(
        ChatMessage(
            isFromUser = false,
            text = "Namaste Ramesh! I am your KisanAI autonomous farm manager. I am continuously monitoring your 3 plots, soil sensors, livestock vaccination cycles, and weather patterns. What can I help you optimize today?",
            timeFormatted = "08:00 AM"
        )
    )
    dao.insertChatMessage(
        ChatMessage(
            isFromUser = true,
            text = "Good morning. I noticed some brown spots on my lower wheat leaves today. Should I be worried?",
            timeFormatted = "08:30 AM"
        )
    )
    dao.insertChatMessage(
        ChatMessage(
            isFromUser = false,
            text = "Namaste! Brown spots can be a sign of early blight or rust. To give you the most accurate advice, could you please take a clear, close-up photo of the affected leaves and upload it here?",
            timeFormatted = "08:31 AM"
        )
    )
    dao.insertChatMessage(
        ChatMessage(
            isFromUser = true,
            text = "Here is a photo of the worst affected area.",
            timeFormatted = "08:32 AM",
            attachedImageUri = "res:drawable/img_leaf_disease"
        )
    )
    dao.insertChatMessage(
        ChatMessage(
            isFromUser = false,
            text = "Thank you for the image. Based on the visual analysis, this appears to be early signs of Tan Spot caused by recent high humidity (65%).",
            timeFormatted = "08:33 AM",
            isDiagnosis = true,
            diagnosisTitle = "Tan Spot (Pyrenophora tritici-repentis)",
            diagnosisConfidence = 88,
            actionDoToday = "Apply a preventative fungicide (like Propiconazole) before tomorrow's expected rain.",
            actionAvoid = "Do not apply overhead irrigation for the next 48 hours to keep leaves dry."
        )
    )

    // Pre-populate initial Severe Weather Alerts & Notifications
    dao.insertNotifications(
        listOf(
            AppNotification(
                id = "notif_1",
                farmId = "farm_1",
                farmName = "Saraswati Farm",
                title = "⛈️ Severe Hailstorm & Thunderstorm Warning",
                message = "Heavy rain (85% probability) and severe hailstorm expected in Samrala area around 3:30 PM today. Secure greenhouse tarpaulins and shelter livestock.",
                severity = "CRITICAL",
                timeFormatted = "10 mins ago",
                isRead = false,
                dateMillis = System.currentTimeMillis() - 600000
            ),
            AppNotification(
                id = "notif_2",
                farmId = "farm_1",
                farmName = "Saraswati Farm",
                title = "💨 High Wind Speed Alert (38 km/h)",
                message = "Strong wind gusts expected. Avoid foliar pesticide spraying today to prevent chemical drift.",
                severity = "WARNING",
                timeFormatted = "1 hour ago",
                isRead = false,
                dateMillis = System.currentTimeMillis() - 3600000
            ),
            AppNotification(
                id = "notif_3",
                farmId = "farm_1",
                farmName = "Saraswati Farm",
                title = "🏛️ New Subsidy Alert: 80% Off Solar Pumps",
                message = "Punjab Agriculture Govt approved 80% subsidy under PM-KUSUM. Check eligibility in Govt Schemes section.",
                severity = "INFO",
                timeFormatted = "Yesterday",
                isRead = false,
                dateMillis = System.currentTimeMillis() - 86400000
            )
        )
    )

    // Pre-populate Seasonal Crop Planting Schedules & Smart Irrigation Reminders
    dao.insertCropTasks(
        listOf(
            CropTask(
                id = "ct_w1",
                farmId = "farm_1",
                cropName = "Wheat",
                season = "Rabi",
                stage = "Land Preparation & Basal",
                taskTitle = "Seedbed Preparation & Basal NPK",
                taskDescription = "Plough twice with cultivator, apply 1 full bag DAP (50kg) and 1/3 bag MOP per acre as basal dose.",
                isPlantingSchedule = true,
                isIrrigationReminder = false,
                recommendedDateDisplay = "Day 0 (Sowing)",
                daysFromSowing = 0,
                irrigationDurationMinutes = 0,
                waterRequirementMm = 0.0,
                fertilizerRecommendation = "DAP 50 kg + MOP 20 kg / acre",
                npkRatio = "120:60:40",
                priority = "High",
                isCompleted = true
            ),
            CropTask(
                id = "ct_w2",
                farmId = "farm_1",
                cropName = "Wheat",
                season = "Rabi",
                stage = "Crown Root Initiation (CRI)",
                taskTitle = "1st Critical Irrigation (CRI Stage)",
                taskDescription = "Crucial first irrigation 21 days after sowing. Top-dress with 1/2 bag Urea (22.5kg) + 5kg Zinc Sulphate.",
                isPlantingSchedule = false,
                isIrrigationReminder = true,
                recommendedDateDisplay = "Day 21 (Today)",
                daysFromSowing = 21,
                irrigationDurationMinutes = 60,
                waterRequirementMm = 55.0,
                fertilizerRecommendation = "Urea 22.5 kg + Zinc 5 kg / acre",
                npkRatio = "120:60:40",
                priority = "High",
                isCompleted = false
            ),
            CropTask(
                id = "ct_w3",
                farmId = "farm_1",
                cropName = "Wheat",
                season = "Rabi",
                stage = "Active Tillering",
                taskTitle = "2nd Irrigation & Tillering Boost",
                taskDescription = "Apply second irrigation 40-45 days after sowing. Inspect field for broadleaf weed emergence.",
                isPlantingSchedule = false,
                isIrrigationReminder = true,
                recommendedDateDisplay = "Day 42",
                daysFromSowing = 42,
                irrigationDurationMinutes = 45,
                waterRequirementMm = 45.0,
                fertilizerRecommendation = "Urea 22.5 kg / acre",
                npkRatio = "120:60:40",
                priority = "Medium",
                isCompleted = false
            ),
            CropTask(
                id = "ct_w4",
                farmId = "farm_1",
                cropName = "Wheat",
                season = "Rabi",
                stage = "Booting & Heading",
                taskTitle = "3rd Irrigation & Foliar Micronutrients",
                taskDescription = "Critical flowering moisture window. Spray 1% Nano Urea or NPK 19:19:19 to boost earhead grain count.",
                isPlantingSchedule = false,
                isIrrigationReminder = true,
                recommendedDateDisplay = "Day 65",
                daysFromSowing = 65,
                irrigationDurationMinutes = 50,
                waterRequirementMm = 50.0,
                fertilizerRecommendation = "Foliar NPK 19:19:19 (1kg/acre)",
                npkRatio = "120:60:40",
                priority = "High",
                isCompleted = false
            ),
            CropTask(
                id = "ct_w5",
                farmId = "farm_1",
                cropName = "Wheat",
                season = "Rabi",
                stage = "Grain Milking & Dough",
                taskTitle = "4th Light Irrigation (Milking Stage)",
                taskDescription = "Avoid heavy flooding if wind speeds exceed 20 km/h to prevent crop lodging. Maintain soil moisture.",
                isPlantingSchedule = false,
                isIrrigationReminder = true,
                recommendedDateDisplay = "Day 88",
                daysFromSowing = 88,
                irrigationDurationMinutes = 35,
                waterRequirementMm = 35.0,
                fertilizerRecommendation = "0:0:50 Potash spray for grain weight",
                npkRatio = "120:60:40",
                priority = "Medium",
                isCompleted = false
            ),
            CropTask(
                id = "ct_p1",
                farmId = "farm_1",
                cropName = "Paddy / Rice (Basmati)",
                season = "Kharif",
                stage = "Nursery & Transplanting",
                taskTitle = "Basal Puddling & 25-Day Seedling Transplant",
                taskDescription = "Transplant 2-3 seedlings per hill with 20x15 cm spacing. Keep 2-3 cm standing water.",
                isPlantingSchedule = true,
                isIrrigationReminder = false,
                recommendedDateDisplay = "Day 0",
                daysFromSowing = 0,
                irrigationDurationMinutes = 0,
                waterRequirementMm = 0.0,
                fertilizerRecommendation = "DAP 35 kg + MOP 20 kg + Zinc 10 kg / acre",
                npkRatio = "150:60:60",
                priority = "High",
                isCompleted = false
            ),
            CropTask(
                id = "ct_m1",
                farmId = "farm_1",
                cropName = "Mustard",
                season = "Rabi",
                stage = "Vegetative & Branching",
                taskTitle = "1st Irrigation at Flowering Emergence",
                taskDescription = "Light irrigation 30 days after sowing. Avoid water stagnation. Apply Sulphur 10kg/acre for oil content boost.",
                isPlantingSchedule = false,
                isIrrigationReminder = true,
                recommendedDateDisplay = "Day 30",
                daysFromSowing = 30,
                irrigationDurationMinutes = 40,
                waterRequirementMm = 30.0,
                fertilizerRecommendation = "Urea 25 kg + Sulphur 90% 10 kg / acre",
                npkRatio = "60:30:30",
                priority = "High",
                isCompleted = false
            )
        )
    )
}
