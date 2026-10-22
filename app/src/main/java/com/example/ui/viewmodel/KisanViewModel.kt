package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiFarmAgent
import com.example.data.local.AppDatabase
import com.example.data.localization.AppLanguage
import com.example.data.model.*
import com.example.data.repository.KisanRepository
import com.example.data.util.FarmHealthCalculator
import com.example.data.util.LocationCoordinatesHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppNavState {
    MAIN_APP
}

enum class MainTab {
    HOME,
    FARMS,
    AI_AGENT,
    MARKET,
    MORE
}

enum class SubScreen {
    NONE,
    SOIL_REPORT,
    IRRIGATION_DETAIL,
    SET_SCHEDULE,
    LIVESTOCK_LIST,
    ANIMAL_DETAIL,
    ADD_ANIMAL,
    FINANCE_DASHBOARD,
    CROP_DOCTOR,
    TASKS_LIST,
    NOTIFICATIONS,
    SETTINGS,
    USER_PROFILE,
    WEATHER_REPORT,
    GOVT_SCHEMES,
    CROP_GROWTH_TRACKER,
    CCTV_MONITOR,
    POULTRY_SCANNER,
    MASTITIS_SCANNER,
    VIRTUAL_FENCING
}

class KisanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KisanRepository

    private val _appState = MutableStateFlow(AppNavState.MAIN_APP)
    val appState: StateFlow<AppNavState> = _appState.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _currentSubScreen = MutableStateFlow(SubScreen.NONE)
    val currentSubScreen: StateFlow<SubScreen> = _currentSubScreen.asStateFlow()

    private val _selectedFarmId = MutableStateFlow("farm_1")
    val selectedFarmId: StateFlow<String> = _selectedFarmId.asStateFlow()

    private val _selectedAnimalId = MutableStateFlow("c_01")
    val selectedAnimalId: StateFlow<String> = _selectedAnimalId.asStateFlow()

    // Theme & Localization
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()



    // Crop Doctor State
    val doctorSelectedCrop = MutableStateFlow("Wheat")
    val isDoctorAnalyzing = MutableStateFlow(false)
    val doctorDiagnosis = MutableStateFlow<String?>(null)
    val doctorPathologyResult = MutableStateFlow<CropPathologyResult?>(null)

    // Chat sending state
    val isAgentThinking = MutableStateFlow(false)

    // Room Database Observation Flows
    val userProfile: StateFlow<UserProfile?>
    val allFarms: StateFlow<List<Farm>>
    val allParcels: StateFlow<List<LandParcel>>
    val allCrops: StateFlow<List<CropRecord>>
    val allIrrigationZones: StateFlow<List<IrrigationZone>>
    val allLivestock: StateFlow<List<Livestock>>
    val allPoultryBatches: StateFlow<List<PoultryBatch>>
    val allMarketPrices: StateFlow<List<MarketPrice>>
    val allTransactions: StateFlow<List<FinancialTransaction>>
    val allTasks: StateFlow<List<FarmTask>>
    val allChatMessages: StateFlow<List<ChatMessage>>
    val allNotifications: StateFlow<List<AppNotification>>
    val unreadNotificationCount: StateFlow<Int>
    val allCropTasks: StateFlow<List<CropTask>>

    // Agri-Commerce & Market State
    val allAgriProducts = MutableStateFlow<List<AgriProduct>>(emptyList())
    val cartItems = MutableStateFlow<Map<String, Int>>(emptyMap()) // Product ID -> Quantity
    val allAgriShops = MutableStateFlow<List<AgriShop>>(emptyList())
    val allCropListings = MutableStateFlow<List<CropSellListing>>(emptyList())
    val allBuyerBids = MutableStateFlow<List<CropBuyerBid>>(emptyList())
    val allPhysicalMandis = MutableStateFlow<List<PhysicalMandi>>(emptyList())
    val allSoilTestingPackages = MutableStateFlow<List<SoilTestingPackage>>(emptyList())
    val allSoilHealthRecords = MutableStateFlow<List<SoilHealthRecord>>(emptyList())


    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = KisanRepository(database.kisanDao())

        viewModelScope.launch {
            repository.checkAndPrepopulateInitialData()
        }

        // Initialize Market Catalog
        allAgriProducts.value = listOf(
            AgriProduct(
                id = "p_1",
                name = "IFFCO Nano Urea Liquid (500 ml)",
                brand = "IFFCO",
                category = "Fertilizers",
                price = 225,
                originalPrice = 250,
                discountPercent = 10,
                rating = 4.8,
                reviewsCount = 3240,
                packSize = "500 ml Bottle",
                packSizesAvailable = listOf("500 ml", "1 L (Pack of 2)", "5 L Canister"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_fertilizer_pack,
                description = "Revolutionary nano-technology liquid fertilizer that replaces 1 traditional 45kg bag of granular urea. 100% bio-available with zero soil leaching.",
                composition = "4.0% Total Nitrogen (w/v) encapsulated in nano-polymers (size: 20-50 nm)",
                recommendedDosage = "2-4 ml per Liter of water during active tillering & vegetative growth stages.",
                isSubsidyEligible = true,
                deliveryDays = 1
            ),
            AgriProduct(
                id = "p_2",
                name = "IFFCO DAP 18:46:00 (50 kg Bag)",
                brand = "IFFCO",
                category = "Fertilizers",
                price = 1350,
                originalPrice = 1500,
                discountPercent = 10,
                rating = 4.9,
                reviewsCount = 8120,
                packSize = "50 kg Bag",
                packSizesAvailable = listOf("25 kg", "50 kg Bag", "100 kg (2 Bags)"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_fertilizer_pack,
                description = "High quality Di-Ammonium Phosphate essential for early root development, rapid tillering, and healthy seed formation.",
                composition = "18% Ammoniacal Nitrogen, 46% Water Soluble Phosphorus (P2O5)",
                recommendedDosage = "50-75 kg per Acre as basal application during sowing / field preparation.",
                isSubsidyEligible = true,
                deliveryDays = 2
            ),
            AgriProduct(
                id = "p_3",
                name = "Mahadhan NPK 19:19:19 Water Soluble (1 kg)",
                brand = "Mahadhan",
                category = "Fertilizers",
                price = 185,
                originalPrice = 230,
                discountPercent = 20,
                rating = 4.7,
                reviewsCount = 1850,
                packSize = "1 kg Pack",
                packSizesAvailable = listOf("1 kg", "5 kg Bag", "25 kg Bag"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_fertilizer_pack,
                description = "100% water-soluble balanced NPK fertilizer specifically engineered for drip fertigation and foliar nutrition sprays.",
                composition = "19% Total Nitrogen, 19% Phosphorus, 19% Potash",
                recommendedDosage = "5g per Liter of water for foliar spray; 3-5 kg per acre through drip irrigation.",
                isSubsidyEligible = false,
                deliveryDays = 1
            ),
            AgriProduct(
                id = "p_4",
                name = "Bio-Gold Organic Vermicompost (25 kg)",
                brand = "Kisan Organic",
                category = "Organic",
                price = 340,
                originalPrice = 450,
                discountPercent = 24,
                rating = 4.9,
                reviewsCount = 940,
                packSize = "25 kg Bag",
                packSizesAvailable = listOf("10 kg", "25 kg Bag", "50 kg Bag"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_soil_testing_kit,
                description = "Premium aged earthworm casting fertilizer enriched with Trichoderma and mycorrhiza to restore soil organic carbon and micro-flora.",
                composition = "Organic Carbon 16%, Total N 1.8%, P 1.4%, K 1.2%, Moisture 20%",
                recommendedDosage = "200-300 kg per Acre during land preparation or tree basin application.",
                isSubsidyEligible = false,
                deliveryDays = 2
            ),
            AgriProduct(
                id = "p_5",
                name = "Neem Guard 10,000 PPM Bio-Pesticide (1 L)",
                brand = "Kisan Bio",
                category = "Pesticides",
                price = 420,
                originalPrice = 550,
                discountPercent = 24,
                rating = 4.8,
                reviewsCount = 1220,
                packSize = "1 L Bottle",
                packSizesAvailable = listOf("500 ml", "1 L Bottle", "5 L Can"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_fertilizer_pack,
                description = "Pure cold-pressed Azadirachtin organic bio-repellent. Highly effective against aphids, caterpillars, whiteflies, and bollworms without killing honeybees.",
                composition = "Azadirachtin 10,000 PPM (1.0% w/w)",
                recommendedDosage = "2.5-3 ml per Liter of water as preventive / curative spray in evening.",
                isSubsidyEligible = false,
                deliveryDays = 1
            ),
            AgriProduct(
                id = "p_6",
                name = "Bayer Nativo Broad-Spectrum Fungicide (100 g)",
                brand = "Bayer CropScience",
                category = "Pesticides",
                price = 780,
                originalPrice = 920,
                discountPercent = 15,
                rating = 4.9,
                reviewsCount = 3400,
                packSize = "100 g Pack",
                packSizesAvailable = listOf("100 g", "250 g", "500 g"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_fertilizer_pack,
                description = "Systemic fungicide providing superior control of Tan Spot, Yellow Rust, Sheath Blight, and Powdery Mildew with stress-shield technology.",
                composition = "Tebuconazole 50% + Trifloxystrobin 25% WG",
                recommendedDosage = "0.5 g per Liter of water (120 g per Acre in 200L water).",
                isSubsidyEligible = false,
                deliveryDays = 1
            ),
            AgriProduct(
                id = "p_7",
                name = "Certified Hybrid Wheat Seeds HD-3086 (40 kg)",
                brand = "Punjab State Seeds Corp",
                category = "Seeds",
                price = 1480,
                originalPrice = 1750,
                discountPercent = 15,
                rating = 4.9,
                reviewsCount = 4100,
                packSize = "40 kg Bag",
                packSizesAvailable = listOf("40 kg Bag", "80 kg (2 Bags)"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_wheat_field,
                description = "High yielding certified wheat seed variety with strong resistance to lodging and yellow rust. Average yield: 24-26 Quintals/Acre.",
                composition = "Germination: 95% min, Purity: 99%, Moisture: 12% max",
                recommendedDosage = "40 kg per Acre using seed drill with recommended spacing.",
                isSubsidyEligible = true,
                deliveryDays = 2
            ),
            AgriProduct(
                id = "p_8",
                name = "Godrej MaxMilk Cattle Feed Pellets (50 kg)",
                brand = "Godrej Agrovet",
                category = "Cattle Feed",
                price = 1550,
                originalPrice = 1800,
                discountPercent = 14,
                rating = 4.8,
                reviewsCount = 2100,
                packSize = "50 kg Bag",
                packSizesAvailable = listOf("50 kg Bag", "100 kg"),
                inStock = true,
                imageDrawableRes = com.example.R.drawable.img_fertilizer_pack,
                description = "Scientifically formulated cattle bypass protein and mineral compound feed that enhances daily milk fat percentage and lactational persistence.",
                composition = "Crude Protein 22% min, Fat 4.5% min, Calcium 1.2%, Phosphorus 0.6%",
                recommendedDosage = "1 kg feed per 2.5 Liters of milk produced + 1.5 kg for body maintenance.",
                isSubsidyEligible = false,
                deliveryDays = 1
            )
        )

        // Offline Shops Catalog
        allAgriShops.value = listOf(
            AgriShop(
                id = "shop_1",
                name = "IFFCO Kisan Seva Kendra - Samrala",
                shopType = "IFFCO Kisan Kendra",
                address = "Main Market Chowk, Near Old Bus Stand, Samrala",
                distanceKm = 1.2,
                rating = 4.9,
                phone = "+91 98140 22319",
                isOpenNow = true,
                openingHours = "08:00 AM - 07:30 PM",
                availableStock = listOf("Nano Urea (500ml) - 240 units", "DAP 50kg - 180 bags", "NPK 19-19-19 - 85 bags", "Govt Subsidized Urea"),
                directionsSummary = "Opposite Cooperative Bank, Main GT Link Road"
            ),
            AgriShop(
                id = "shop_2",
                name = "Kisan Beej & Khad Bhandar",
                shopType = "Authorized Agri Dealer",
                address = "Shop #14, Grain Market Complex, Samrala",
                distanceKm = 2.4,
                rating = 4.7,
                phone = "+91 98721 88902",
                isOpenNow = true,
                openingHours = "08:30 AM - 08:00 PM",
                availableStock = listOf("Bayer Nativo Fungicide", "Syngenta Amistar", "Zinc EDTA 12%", "Drip Irrigation Parts"),
                directionsSummary = "Gate No. 2, Old Grain Market"
            ),
            AgriShop(
                id = "shop_3",
                name = "Krishi Vigyan Kendra (KVK) Govt Store",
                shopType = "Govt Research & Input Center",
                address = "PAU Zonal Station, Samrala Road, Ludhiana District",
                distanceKm = 5.8,
                rating = 4.9,
                phone = "+91 161 240 1960",
                isOpenNow = true,
                openingHours = "09:00 AM - 05:00 PM",
                availableStock = listOf("PAU Certified Wheat Seeds", "Bio-Fertilizer Culture (Azotobacter)", "Soil Sample Collection Bags"),
                directionsSummary = "Next to Agricultural University Extension Wing"
            ),
            AgriShop(
                id = "shop_4",
                name = "Punjab Agri Power Tools & Feed Store",
                shopType = "Machinery & Livestock Depot",
                address = "Plot 8, Ludhiana Highway, Near Toll Plaza",
                distanceKm = 6.5,
                rating = 4.6,
                phone = "+91 94172 66530",
                isOpenNow = true,
                openingHours = "08:00 AM - 07:00 PM",
                availableStock = listOf("Battery Sprayers (16L)", "Godrej Cattle Feed Pellets", "Silage Bags", "Solar Pest Traps"),
                directionsSummary = "100m before Khanna Road Crossing"
            )
        )

        // Physical APMC Mandis
        allPhysicalMandis.value = listOf(
            PhysicalMandi(
                id = "mandi_1",
                name = "Khanna Grain Market (Asia's Largest APMC Mandi)",
                district = "Ludhiana",
                state = "Punjab",
                distanceKm = 11.5,
                todayArrivalsQuintals = 42800,
                openHours = "06:00 AM - 04:00 PM",
                modalPriceToday = 2425,
                activeTradersCount = 74,
                mspAssistanceCounter = true,
                helplinePhone = "+91 1628 226100"
            ),
            PhysicalMandi(
                id = "mandi_2",
                name = "Samrala Grain Yard (APMC Sub-Market)",
                district = "Ludhiana",
                state = "Punjab",
                distanceKm = 3.2,
                todayArrivalsQuintals = 14200,
                openHours = "07:00 AM - 02:30 PM",
                modalPriceToday = 2410,
                activeTradersCount = 38,
                mspAssistanceCounter = true,
                helplinePhone = "+91 1628 262040"
            ),
            PhysicalMandi(
                id = "mandi_3",
                name = "Ludhiana Dana Mandi (Main APMC Complex)",
                district = "Ludhiana",
                state = "Punjab",
                distanceKm = 22.0,
                todayArrivalsQuintals = 58000,
                openHours = "05:30 AM - 05:00 PM",
                modalPriceToday = 2440,
                activeTradersCount = 92,
                mspAssistanceCounter = true,
                helplinePhone = "+91 161 240 3320"
            ),
            PhysicalMandi(
                id = "mandi_4",
                name = "Sahnewal MSP Procurement Centre",
                district = "Ludhiana",
                state = "Punjab",
                distanceKm = 16.4,
                todayArrivalsQuintals = 9500,
                openHours = "07:30 AM - 03:00 PM",
                modalPriceToday = 2395,
                activeTradersCount = 24,
                mspAssistanceCounter = true,
                helplinePhone = "+91 161 284 4110"
            )
        )

        // Online Crop Sell Listings
        allCropListings.value = listOf(
            CropSellListing(
                id = "list_1",
                cropName = "Wheat (Kanak)",
                variety = "HD-3086 Sharbati",
                quantityQuintals = 80.0,
                askingPricePerQuintal = 2450,
                moisturePercent = 11.2,
                grade = "Grade A Premium",
                pickupLocation = "Samrala Farm, Field 1",
                listedDate = "Today",
                status = "Active",
                activeBidsCount = 4,
                highestBidPrice = 2445
            ),
            CropSellListing(
                id = "list_2",
                cropName = "Mustard (Sarson)",
                variety = "Pusa Bold 42% Oil",
                quantityQuintals = 25.0,
                askingPricePerQuintal = 5400,
                moisturePercent = 8.0,
                grade = "High Oil Content",
                pickupLocation = "East Plot, Farm 1",
                listedDate = "Yesterday",
                status = "Active",
                activeBidsCount = 2,
                highestBidPrice = 5380
            )
        )

        // Live Buyer Bids on Listings
        allBuyerBids.value = listOf(
            CropBuyerBid(
                id = "bid_1",
                buyerName = "Punjab Agro Millers & Exporters Ltd",
                buyerType = "Verified Roller Flour Mill",
                bidPricePerQuintal = 2445,
                totalAmount = 195600,
                pickupPromise = "Free Farm Pickup within 24 hours (Truck arranged)",
                paymentTerms = "Instant Direct Bank Transfer (DBT) on electronic scale weighing",
                rating = 4.9
            ),
            CropBuyerBid(
                id = "bid_2",
                buyerName = "Adani Agri Logistics Procurement",
                buyerType = "National Bulk Aggregator",
                bidPricePerQuintal = 2435,
                totalAmount = 194800,
                pickupPromise = "Pickup scheduled for tomorrow morning 9:00 AM",
                paymentTerms = "UPI / RTGS bank transfer within 2 hours of loading",
                rating = 4.8
            ),
            CropBuyerBid(
                id = "bid_3",
                buyerName = "Ludhiana Food Products FPO",
                buyerType = "Farmer Producer Organisation",
                bidPricePerQuintal = 2440,
                totalAmount = 195200,
                pickupPromise = "Farmgate collection + Moisture testing report included",
                paymentTerms = "Direct Bank Transfer on spot",
                rating = 4.7
            )
        )

        // Soil & Testing Packages
        allSoilTestingPackages.value = listOf(
            SoilTestingPackage(
                id = "test_1",
                title = "Essential Soil Health & N-P-K Test",
                category = "Soil",
                price = 299,
                originalPrice = 500,
                isGovtSubsidized = true,
                turnaroundDays = 2,
                parametersTested = listOf("Nitrogen (N)", "Phosphorus (P)", "Potassium (K)", "pH Level (Acidity/Alkalinity)", "Electrical Conductivity (EC)", "Organic Carbon (OC)"),
                description = "Certified technician visits your field, collects scientific core samples at multiple grid points, and provides digital Soil Health Card with custom crop fertilizer schedule.",
                sampleCollectionType = "Doorstep Sample Collection by Certified Agronomist",
                imageDrawableRes = com.example.R.drawable.img_soil_testing_kit
            ),
            SoilTestingPackage(
                id = "test_2",
                title = "Comprehensive 14-Parameter Micro-Nutrient Test",
                category = "Soil",
                price = 599,
                originalPrice = 1200,
                isGovtSubsidized = false,
                turnaroundDays = 3,
                parametersTested = listOf("NPK & pH", "Zinc (Zn)", "Iron (Fe)", "Copper (Cu)", "Manganese (Mn)", "Boron (B)", "Sulphur (S)", "Soil Texture (Clay/Sand ratio)"),
                description = "Complete micronutrient profile scan to prevent hidden nutrient deficiencies, yellowing, and stunted crop growth. Includes AI dosage advice.",
                sampleCollectionType = "Precision GPS-tagged Core Soil Sample Collection",
                imageDrawableRes = com.example.R.drawable.img_soil_testing_kit
            ),
            SoilTestingPackage(
                id = "test_3",
                title = "Borewell & Irrigation Water Quality Test",
                category = "Water",
                price = 399,
                originalPrice = 750,
                isGovtSubsidized = false,
                turnaroundDays = 2,
                parametersTested = listOf("Total Dissolved Solids (TDS)", "Water pH", "Sodium Adsorption Ratio (SAR)", "Hardness / Carbonates", "Chloride Toxicity Level"),
                description = "Tests tube-well and canal water suitability to prevent drip dripper clogging and soil salinity buildup.",
                sampleCollectionType = "Sterilized Water Vial Sample Collection",
                imageDrawableRes = com.example.R.drawable.img_soil_testing_kit
            ),
            SoilTestingPackage(
                id = "test_4",
                title = "Leaf Tissue & Plant Pathology Lab Scan",
                category = "Leaf Tissue",
                price = 449,
                originalPrice = 850,
                isGovtSubsidized = false,
                turnaroundDays = 2,
                parametersTested = listOf("Active Fungal / Bacterial Spore Scan", "Chlorophyll Index", "Leaf Nitrogen Absorption", "Chemical Burn vs Disease Confirmation"),
                description = "Laboratory diagnosis for unknown leaf spot, yellowing, or blight problems with exact spray prescription.",
                sampleCollectionType = "Cold-Pack Leaf Sample Dispatch",
                imageDrawableRes = com.example.R.drawable.img_leaf_disease
            )
        )

        userProfile = repository.userProfile.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
        allFarms = repository.allFarms.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allParcels = repository.allLandParcels.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allCrops = repository.allCrops.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allIrrigationZones = repository.allIrrigationZones.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allLivestock = repository.allLivestock.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allPoultryBatches = repository.allPoultryBatches.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allMarketPrices = repository.allMarketPrices.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allTransactions = repository.allTransactions.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allTasks = repository.allTasks.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allChatMessages = repository.allChatMessages.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allNotifications = repository.allNotifications.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allCropTasks = repository.allCropTasks.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        unreadNotificationCount = repository.allNotifications.map { list ->
            list.count { !it.isRead }
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), 0
        )

        // Initialize Baseline Soil Health Historical Logs
        allSoilHealthRecords.value = listOf(
            SoilHealthRecord(
                id = "soil_1",
                farmId = "farm_1",
                dateLabel = "14 Aug",
                timestamp = System.currentTimeMillis() - 7 * 86400000L,
                phLevel = 6.6,
                moisturePercent = 46.0,
                nitrogenKgHa = 136,
                phosphorusKgHa = 36,
                potassiumKgHa = 195,
                organicCarbonPercent = 0.69,
                soilTemperatureCelsius = 24.2,
                electricalConductivityEc = 1.10,
                source = "Lab Soil Health Card",
                sensorDeviceName = "PAU Zonal Soil Lab",
                sensorBatteryPercent = 100,
                notes = "Pre-sowing baseline lab sample verification"
            ),
            SoilHealthRecord(
                id = "soil_2",
                farmId = "farm_1",
                dateLabel = "16 Aug",
                timestamp = System.currentTimeMillis() - 5 * 86400000L,
                phLevel = 6.6,
                moisturePercent = 42.0,
                nitrogenKgHa = 138,
                phosphorusKgHa = 36,
                potassiumKgHa = 196,
                organicCarbonPercent = 0.70,
                soilTemperatureCelsius = 24.8,
                electricalConductivityEc = 1.12,
                source = "KrishiProbe-BLE Sensor",
                sensorDeviceName = "KrishiProbe-BLE-S4",
                sensorBatteryPercent = 98,
                notes = "Moisture dropping to 42%. Irrigation scheduled."
            ),
            SoilHealthRecord(
                id = "soil_3",
                farmId = "farm_1",
                dateLabel = "18 Aug",
                timestamp = System.currentTimeMillis() - 3 * 86400000L,
                phLevel = 6.7,
                moisturePercent = 68.0,
                nitrogenKgHa = 142,
                phosphorusKgHa = 39,
                potassiumKgHa = 204,
                organicCarbonPercent = 0.71,
                soilTemperatureCelsius = 23.0,
                electricalConductivityEc = 1.16,
                source = "KrishiProbe-BLE Sensor",
                sensorDeviceName = "KrishiProbe-BLE-S4",
                sensorBatteryPercent = 96,
                notes = "Post-drip irrigation hydration surge"
            ),
            SoilHealthRecord(
                id = "soil_4",
                farmId = "farm_1",
                dateLabel = "20 Aug",
                timestamp = System.currentTimeMillis() - 1 * 86400000L,
                phLevel = 6.8,
                moisturePercent = 62.0,
                nitrogenKgHa = 145,
                phosphorusKgHa = 41,
                potassiumKgHa = 208,
                organicCarbonPercent = 0.72,
                soilTemperatureCelsius = 23.5,
                electricalConductivityEc = 1.15,
                source = "Manual Entry",
                sensorDeviceName = "Digital Field Probe",
                sensorBatteryPercent = 95,
                notes = "Soil moisture stabilizing in optimal root zone"
            ),
            SoilHealthRecord(
                id = "soil_5",
                farmId = "farm_1",
                dateLabel = "Today",
                timestamp = System.currentTimeMillis(),
                phLevel = 6.8,
                moisturePercent = 58.0,
                nitrogenKgHa = 146,
                phosphorusKgHa = 42,
                potassiumKgHa = 210,
                organicCarbonPercent = 0.72,
                soilTemperatureCelsius = 23.6,
                electricalConductivityEc = 1.18,
                source = "KrishiProbe-BLE Sensor",
                sensorDeviceName = "KrishiProbe-BLE-S4",
                sensorBatteryPercent = 94,
                notes = "Current optimal status: pH 6.8, 58% Moisture"
            )
        )
    }

    // Soil Health Telemetry Actions
    fun logSoilHealthRecord(
        farmId: String,
        phLevel: Double,
        moisturePercent: Double,
        source: String,
        notes: String = "",
        dateLabel: String = "Today",
        temp: Double = 23.6,
        ec: Double = 1.18,
        battery: Int = 94,
        deviceName: String = "KrishiProbe-BLE-S4"
    ) {
        val newRecord = SoilHealthRecord(
            id = "soil_${System.currentTimeMillis()}",
            farmId = farmId,
            dateLabel = dateLabel,
            timestamp = System.currentTimeMillis(),
            phLevel = phLevel,
            moisturePercent = moisturePercent,
            nitrogenKgHa = (135 + (phLevel * 2).toInt()).coerceIn(120, 180),
            phosphorusKgHa = (30 + (moisturePercent * 0.2).toInt()).coerceIn(25, 60),
            potassiumKgHa = 210,
            organicCarbonPercent = 0.72,
            soilTemperatureCelsius = temp,
            electricalConductivityEc = ec,
            source = source,
            sensorDeviceName = deviceName,
            sensorBatteryPercent = battery,
            notes = notes.ifBlank { "Recorded via $source" }
        )
        val current = allSoilHealthRecords.value.toMutableList()
        current.add(newRecord)
        allSoilHealthRecords.value = current

        // Also sync moisture & pH to parcel
        viewModelScope.launch {
            val parcel = allParcels.value.find { it.farmId == farmId }
            if (parcel != null) {
                repository.updateParcel(
                    parcel.copy(
                        ph = phLevel,
                        moisture = moisturePercent.toInt()
                    )
                )
            }
        }
    }

    fun deleteSoilHealthRecord(id: String) {
        allSoilHealthRecords.value = allSoilHealthRecords.value.filter { it.id != id }
    }


    // Notification Actions
    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun triggerSevereWeatherAlert(farmName: String, title: String, message: String) {
        viewModelScope.launch {
            val alert = AppNotification(
                id = "notif_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                farmName = farmName,
                title = title,
                message = message,
                severity = "CRITICAL",
                timeFormatted = "Just now",
                isRead = false,
                dateMillis = System.currentTimeMillis()
            )
            repository.addNotification(alert)
        }
    }

    // Navigation and UI actions
    fun setAppState(state: AppNavState) {
        _appState.value = state
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
        _currentSubScreen.value = SubScreen.NONE
    }

    fun selectFarm(farmId: String) {
        _selectedFarmId.value = farmId
    }

    fun selectAnimal(animalId: String) {
        _selectedAnimalId.value = animalId
    }

    fun navigateToSubScreen(subScreen: SubScreen) {
        _currentSubScreen.value = subScreen
    }

    fun navigateBack() {
        if (_currentSubScreen.value != SubScreen.NONE) {
            _currentSubScreen.value = SubScreen.NONE
        } else {
            _currentTab.value = MainTab.HOME
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.saveUserProfile(current.copy(isDarkTheme = _isDarkTheme.value))
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.saveUserProfile(current.copy(selectedLanguage = language.code))
            }
        }
    }



    fun updateUserProfile(updated: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(updated)
        }
    }

    fun addNewActiveFarm(
        type: String,
        name: String = "",
        area: Double = 5.0,
        unit: String = "Acres",
        primaryCropOrAnimal: String = "",
        ownership: String = "Ancestral Owned",
        houseNo: String = "",
        landmark: String = "",
        village: String = "",
        district: String = "",
        state: String = "",
        pinCode: String = "",
        latitude: Double = 30.8987,
        longitude: Double = 75.8573,
        soilType: String = "Loamy Alluvial",
        soilTestingStatus: String = "Recent Test (Within 6 Months)",
        fertilizerPractice: String = "Balanced Organic + NPK",
        drainageCondition: String = "Excellent (No Waterlogging)",
        waterSource: String = "Borewell & Solar Pump",
        pestPressure: String = "None (Clean & Healthy)",
        diseaseSigns: String = "No Disease Signs (Vibrant Green)",
        seedQuality: String = "Certified Hybrid / Govt Verified",
        boundaryFencing: String = "Solar / Wire Fenced"
    ) {
        viewModelScope.launch {
            val count = allFarms.value.size + 1
            val farmName = name.trim().ifBlank {
                when {
                    type.contains("Poultry", true) -> "Poultry Unit #$count"
                    type.contains("Cattle", true) || type.contains("Dairy", true) -> "Dairy Barn #$count"
                    type.contains("Goat", true) || type.contains("Sheep", true) -> "Goat Pen #$count"
                    type.contains("Fish", true) -> "Fish Pond #$count"
                    type.contains("Bee", true) -> "Honey Apiary #$count"
                    type.contains("Fruit", true) || type.contains("Horticulture", true) -> "Orchard #$count"
                    type.contains("Vegetable", true) -> "Vegetable Greenhouse #$count"
                    type.contains("Mushroom", true) -> "Mushroom Shed #$count"
                    type.contains("Nursery", true) -> "Plant Nursery #$count"
                    type.contains("Organic", true) -> "Organic Plot #$count"
                    else -> "Farm Unit #$count"
                }
            }
            val farmVillage = village.ifBlank { userProfile.value?.village ?: "Samrala" }
            val farmDistrict = district.ifBlank { userProfile.value?.district ?: "Ludhiana" }
            val farmState = state.ifBlank { userProfile.value?.state ?: "Punjab" }
            val farmPin = pinCode.ifBlank { userProfile.value?.pinCode ?: "141114" }
            val farmPrimary = primaryCropOrAnimal.ifBlank {
                if (type.contains("Poultry", true)) "Broiler Birds" else if (type.contains("Dairy", true) || type.contains("Cattle", true)) "Milch Cows" else "Wheat"
            }

            // Resolve exact coordinates from Village/District/State/Pin
            val resolvedCoords = LocationCoordinatesHelper.resolveCoordinates(
                village = farmVillage,
                district = farmDistrict,
                state = farmState,
                pinCode = farmPin,
                fallbackLat = latitude,
                fallbackLng = longitude
            )

            // Comprehensive Health Calculation from user answers
            val healthAssessment = FarmHealthCalculator.calculateHealth(
                farmType = type,
                primaryCropOrAnimal = farmPrimary,
                soilType = soilType,
                soilTestingStatus = soilTestingStatus,
                fertilizerPractice = fertilizerPractice,
                drainageCondition = drainageCondition,
                waterSource = waterSource,
                pestPressure = pestPressure,
                diseaseSigns = diseaseSigns,
                seedQuality = seedQuality,
                boundaryFencing = boundaryFencing
            )

            val newFarm = Farm(
                id = "farm_${System.currentTimeMillis()}",
                name = farmName,
                farmType = type,
                location = listOf(farmVillage, farmDistrict, farmState).filter { it.isNotBlank() }.joinToString(", "),
                houseNo = houseNo.ifBlank { userProfile.value?.houseNo ?: "" },
                landmark = landmark.ifBlank { userProfile.value?.landmark ?: "" },
                village = farmVillage,
                district = farmDistrict,
                state = farmState,
                pinCode = farmPin,
                latitude = resolvedCoords.latitude,
                longitude = resolvedCoords.longitude,
                totalArea = area,
                unit = unit,
                ownership = ownership,
                waterAvailability = waterSource,
                soilType = soilType,
                terrain = "Plain / Flat",
                primaryCropOrAnimal = farmPrimary,
                isPrimary = allFarms.value.isEmpty(),
                healthScore = healthAssessment.overallScore,
                healthStatus = healthAssessment.status,
                soilTestingStatus = soilTestingStatus,
                fertilizerPractice = fertilizerPractice,
                drainageCondition = drainageCondition,
                waterSource = waterSource,
                pestPressure = pestPressure,
                diseaseSigns = diseaseSigns,
                seedQuality = seedQuality,
                boundaryFencing = boundaryFencing,
                soilHealthScore = healthAssessment.soilScore,
                waterHealthScore = healthAssessment.waterScore,
                pestHealthScore = healthAssessment.pestScore,
                healthSummary = healthAssessment.summary
            )
            repository.addFarm(newFarm)
            _selectedFarmId.value = newFarm.id

            // Generate realistic LandParcel matching soil scores
            val newParcel = LandParcel(
                id = "parcel_${System.currentTimeMillis()}",
                farmId = newFarm.id,
                name = "$farmName Main Plot",
                area = area,
                currentCrop = farmPrimary,
                soilType = soilType,
                ph = if (soilType.contains("Black", true)) 7.2 else 6.8,
                nitrogen = (healthAssessment.soilScore * 0.6).toInt().coerceIn(30, 85),
                phosphorus = (healthAssessment.soilScore * 0.7).toInt().coerceIn(35, 80),
                potassium = (healthAssessment.soilScore * 0.5).toInt().coerceIn(25, 75),
                organicMatter = if (fertilizerPractice.contains("Organic", true)) 2.8 else 2.2,
                moisture = if (healthAssessment.waterScore > 70) 65 else 42,
                overallSoilScore = healthAssessment.soilScore,
                verifiedSampleDate = if (soilTestingStatus.contains("Recent", true)) "Verified (Last 30 Days)" else "Not Certified"
            )
            repository.updateParcel(newParcel)

            // Generate Irrigation Zone matching farm's water setup
            val zone = IrrigationZone(
                id = "zone_${System.currentTimeMillis()}",
                farmId = newFarm.id,
                zoneName = "$farmPrimary Sector 1",
                cropName = farmPrimary,
                isRunning = false,
                remainingMinutes = 0,
                soilMoisture = if (healthAssessment.waterScore > 70) 65 else 40,
                isSmartScheduleEnabled = true,
                scheduledTime = "06:00 AM",
                durationMinutes = 45,
                activeDaysSummary = "Daily",
                estimatedUsageLiters = 1200
            )
            repository.addIrrigationZone(zone)

            // Automatically add crop or animal record corresponding to this new farm
            if (type.contains("Dairy", true) || type.contains("Cattle", true) || type.contains("Poultry", true) || type.contains("Goat", true) || type.contains("Sheep", true) || type.contains("Fish", true) || type.contains("Bee", true)) {
                val animal = Livestock(
                    id = "animal_${System.currentTimeMillis()}",
                    farmId = newFarm.id,
                    name = "$farmName Stock #$count",
                    tagId = "TAG-${100 + count}",
                    type = type,
                    breed = farmPrimary,
                    ageYears = 3,
                    barnSection = "Section 1",
                    healthScore = healthAssessment.overallScore,
                    lastVaccineDaysAgo = 10,
                    nextVaccineDays = 50,
                    dailyMilkYield = if (type.contains("Dairy", true) || type.contains("Cattle", true)) 14.5 else 0.0,
                    status = if (healthAssessment.overallScore >= 70) "Healthy" else "Needs Checkup",
                    insight = "Active livestock profile registered with ${healthAssessment.overallScore}% health score."
                )
                repository.addAnimal(animal)
            } else {
                val cropRecord = CropRecord(
                    id = "crop_${System.currentTimeMillis()}",
                    farmId = newFarm.id,
                    parcelId = newParcel.id,
                    cropName = farmPrimary,
                    variety = seedQuality,
                    growthStage = "Vegetative / Early Stage",
                    healthScore = healthAssessment.overallScore,
                    sowingDate = "Recent",
                    expectedHarvest = "In 90-120 Days",
                    expectedYieldPerAcre = 22.0
                )
                repository.addCrop(cropRecord)
            }

            // Insert Dynamic Tailored Action Plan Tasks for this Farm!
            healthAssessment.generatedActionTasks.forEachIndexed { index, taskTemplate ->
                val task = FarmTask(
                    id = "task_${System.currentTimeMillis()}_$index",
                    farmId = newFarm.id,
                    title = taskTemplate.title,
                    description = taskTemplate.description,
                    priority = taskTemplate.priority,
                    priorityLabel = taskTemplate.priorityLabel,
                    targetArea = farmName,
                    category = taskTemplate.category,
                    whyReason = taskTemplate.whyReason,
                    isCompleted = false
                )
                repository.addTask(task)
            }

            // Create initial baseline soil health records for new farm
            val currentRecords = allSoilHealthRecords.value.toMutableList()
            val basePh = if (soilType.contains("Black", true)) 7.2 else 6.8
            val baseMoist = if (healthAssessment.waterScore > 70) 62.0 else 48.0
            currentRecords.addAll(
                listOf(
                    SoilHealthRecord(
                        id = "soil_init_${System.currentTimeMillis()}_1",
                        farmId = newFarm.id,
                        dateLabel = "5 Days Ago",
                        timestamp = System.currentTimeMillis() - 5 * 86400000L,
                        phLevel = basePh - 0.1,
                        moisturePercent = baseMoist - 6.0,
                        nitrogenKgHa = 140,
                        phosphorusKgHa = 38,
                        potassiumKgHa = 200,
                        organicCarbonPercent = 0.70,
                        source = "Soil Health Card",
                        sensorDeviceName = "Field Baseline Assay",
                        notes = "Initial baseline reading on registration"
                    ),
                    SoilHealthRecord(
                        id = "soil_init_${System.currentTimeMillis()}_2",
                        farmId = newFarm.id,
                        dateLabel = "2 Days Ago",
                        timestamp = System.currentTimeMillis() - 2 * 86400000L,
                        phLevel = basePh,
                        moisturePercent = baseMoist + 4.0,
                        nitrogenKgHa = 144,
                        phosphorusKgHa = 40,
                        potassiumKgHa = 205,
                        organicCarbonPercent = 0.71,
                        source = "Manual Entry",
                        sensorDeviceName = "Field Probe",
                        notes = "Post field prep observation"
                    ),
                    SoilHealthRecord(
                        id = "soil_init_${System.currentTimeMillis()}_3",
                        farmId = newFarm.id,
                        dateLabel = "Today",
                        timestamp = System.currentTimeMillis(),
                        phLevel = basePh,
                        moisturePercent = baseMoist,
                        nitrogenKgHa = 146,
                        phosphorusKgHa = 42,
                        potassiumKgHa = 210,
                        organicCarbonPercent = 0.72,
                        source = "KrishiProbe-BLE Sensor",
                        sensorDeviceName = "KrishiProbe-BLE-S4",
                        notes = "Active baseline status: pH $basePh, ${baseMoist.toInt()}% Moisture"
                    )
                )
            )
            allSoilHealthRecords.value = currentRecords
        }
    }


    fun toggleIrrigationZone(zone: IrrigationZone) {
        viewModelScope.launch {
            val updated = zone.copy(
                isRunning = !zone.isRunning,
                remainingMinutes = if (!zone.isRunning) zone.durationMinutes else 0
            )
            repository.updateIrrigationZone(updated)
        }
    }

    fun updateIrrigationSchedule(
        zoneId: String,
        startTime: String,
        durationMinutes: Int,
        activeDays: String,
        smartScheduling: Boolean
    ) {
        viewModelScope.launch {
            val zone = allIrrigationZones.value.find { it.id == zoneId }
            if (zone != null) {
                val updated = zone.copy(
                    scheduledTime = startTime,
                    durationMinutes = durationMinutes,
                    activeDaysSummary = activeDays,
                    isSmartScheduleEnabled = smartScheduling
                )
                repository.updateIrrigationZone(updated)
                _currentSubScreen.value = SubScreen.IRRIGATION_DETAIL
            }
        }
    }

    fun logMilkYield(animalId: String, yieldLiters: Double) {
        viewModelScope.launch {
            val animal = allLivestock.value.find { it.id == animalId }
            if (animal != null) {
                val updated = animal.copy(
                    dailyMilkYield = yieldLiters,
                    insight = "Logged yield of $yieldLiters L. Performance aligns with seasonal target."
                )
                repository.updateAnimal(updated)
            }
        }
    }

    fun addNewAnimal(
        name: String,
        type: String,
        breed: String,
        ageYears: Int,
        tagId: String,
        hasBleTracker: Boolean = true,
        bleDeviceName: String = "KisanTrack-BLE"
    ) {
        viewModelScope.launch {
            val newAnimal = Livestock(
                id = "c_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                name = name,
                tagId = tagId,
                type = type,
                breed = breed,
                ageYears = ageYears,
                barnSection = "Barn B",
                healthScore = 90,
                lastVaccineDaysAgo = 0,
                nextVaccineDays = 60,
                dailyMilkYield = 12.0,
                status = "Healthy",
                insight = "New animal profile initialized. Vaccine schedule active.",
                hasBleTracker = hasBleTracker,
                bleDeviceName = if (hasBleTracker) bleDeviceName else "None",
                isInsideFence = true,
                distanceMetersFromCenter = 30,
                fenceRadiusMeters = 150,
                lastPingTime = "Just now",
                batteryPercent = 95
            )
            repository.addAnimal(newAnimal)
            _currentSubScreen.value = SubScreen.LIVESTOCK_LIST
        }
    }

    fun addFarmTask(
        farmId: String,
        title: String,
        description: String,
        category: String,
        priority: String,
        targetArea: String,
        whyReason: String
    ) {
        viewModelScope.launch {
            val newTask = FarmTask(
                id = "custom_task_${System.currentTimeMillis()}",
                farmId = farmId,
                title = title,
                description = description,
                priority = priority,
                priorityLabel = if (priority.equals("High", ignoreCase = true)) "DO FIRST" else "SCHEDULED",
                targetArea = targetArea,
                category = category,
                whyReason = whyReason,
                isCompleted = false,
                estimatedTimeMinutes = 30
            )
            repository.addTask(newTask)
        }
    }

    fun toggleTaskComplete(task: FarmTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: FarmTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun advanceFarmDay(farmId: String) {
        viewModelScope.launch {
            val farm = allFarms.value.find { it.id == farmId } ?: return@launch
            val nextDay = farm.activeDay + 1
            val updatedFarm = farm.copy(
                activeDay = nextDay,
                isWeekUnlocked = true,
                lastTaskCycleTimestamp = System.currentTimeMillis()
            )
            repository.addFarm(updatedFarm)

            val currentFarmTasks = allTasks.value.filter { it.farmId == farmId }
            currentFarmTasks.forEach {
                repository.deleteTask(it)
            }

            val crop = farm.primaryCropOrAnimal
            val newTasksList = when (nextDay % 3) {
                2 -> listOf(
                    FarmTask(
                        id = "task_${System.currentTimeMillis()}_1",
                        farmId = farm.id,
                        title = "Morning $crop Leaf Health & Pest Scouting",
                        description = "Examine lower and middle foliage in ${farm.name} before 09:00 AM for early sucking pests or fungal specks.",
                        priority = "High",
                        priorityLabel = "DO BEFORE 9 AM",
                        targetArea = farm.name,
                        category = "Pest",
                        whyReason = "Early morning scouting detects aphid and mite nymph buildup before heat triggers migration.",
                        isCompleted = false
                    ),
                    FarmTask(
                        id = "task_${System.currentTimeMillis()}_2",
                        farmId = farm.id,
                        title = "Drip / Lateral Line Flush & Sand Trap Clean",
                        description = "Open sub-main flush end-caps for 3 minutes to discharge trapped silt and maintain uniform dripper emission.",
                        priority = "Medium",
                        priorityLabel = "ROUTINE",
                        targetArea = farm.name,
                        category = "Irrigation",
                        whyReason = "Prevents emitter clogging and guarantees uniform root-zone hydration.",
                        isCompleted = false
                    ),
                    FarmTask(
                        id = "task_${System.currentTimeMillis()}_3",
                        farmId = farm.id,
                        title = "Bio-Stimulant Micronutrient Foliar Spray",
                        description = "Apply Seaweed Extract or Zinc-EDTA (2 ml / L) via fine knapsack sprayer during mild sunshine hours.",
                        priority = "Medium",
                        priorityLabel = "NUTRITION",
                        targetArea = farm.name,
                        category = "Fertilizer",
                        whyReason = "Enhances photosynthetic enzyme activity and reinforces cell wall resistance.",
                        isCompleted = false
                    )
                )
                0 -> listOf(
                    FarmTask(
                        id = "task_${System.currentTimeMillis()}_1",
                        farmId = farm.id,
                        title = "Root Zone Soil Moisture Probe Check",
                        description = "Probe 15cm depth near root zone to verify soil aeration and avoid over-saturation.",
                        priority = "High",
                        priorityLabel = "WATER CHECK",
                        targetArea = farm.name,
                        category = "Irrigation",
                        whyReason = "Ensures root respiration is not choked by excess moisture.",
                        isCompleted = false
                    ),
                    FarmTask(
                        id = "task_${System.currentTimeMillis()}_2",
                        farmId = farm.id,
                        title = "Border Weed Clearing & Sticky Trap Replenish",
                        description = "Inspect yellow sticky traps on field perimeters and remove weed harbors along bunds.",
                        priority = "Low",
                        priorityLabel = "MAINTENANCE",
                        targetArea = farm.name,
                        category = "Pest",
                        whyReason = "Blocks alternate host weed species that harbor virus vectors.",
                        isCompleted = false
                    )
                )
                else -> emptyList()
            }

            newTasksList.forEach { task ->
                repository.addTask(task)
            }
        }
    }

    fun sendChatMessage(userText: String, attachedImageRes: String? = null) {
        if (userText.isBlank() && attachedImageRes == null) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                isFromUser = true,
                text = userText,
                timeFormatted = "Just now",
                attachedImageUri = attachedImageRes
            )
            repository.addChatMessage(userMsg)

            // App Navigation & Operations Control based on natural query
            val queryLower = userText.lowercase().trim()
            if (queryLower.contains("finance") || queryLower.contains("expense") || queryLower.contains("profit") || queryLower.contains("revenue") || queryLower.contains("ledger")) {
                _currentSubScreen.value = SubScreen.FINANCE_DASHBOARD
            } else if (queryLower.contains("market") || queryLower.contains("mandi") || queryLower.contains("vet") || queryLower.contains("clinic")) {
                _currentTab.value = MainTab.MARKET
                _currentSubScreen.value = SubScreen.NONE
            } else if (queryLower.contains("farm") || queryLower.contains("field") || queryLower.contains("plot")) {
                _currentTab.value = MainTab.FARMS
                _currentSubScreen.value = SubScreen.NONE
            } else if (queryLower.contains("crop doctor") || queryLower.contains("scan crop") || queryLower.contains("leaf disease")) {
                _currentSubScreen.value = SubScreen.CROP_DOCTOR
            } else if (queryLower.contains("settings") || queryLower.contains("language") || queryLower.contains("theme")) {
                _currentSubScreen.value = SubScreen.SETTINGS
            } else if (queryLower.contains("home")) {
                _currentTab.value = MainTab.HOME
                _currentSubScreen.value = SubScreen.NONE
            }

            isAgentThinking.value = true

            val activeFarm = allFarms.value.find { it.id == _selectedFarmId.value }
            val advisorResult = GeminiFarmAgent.askFarmAdvisor(
                prompt = userText,
                userProfile = userProfile.value,
                activeFarm = activeFarm,
                parcels = allParcels.value,
                livestock = allLivestock.value,
                tasks = allTasks.value
            )

            isAgentThinking.value = false

            val agentMsg = if (userText.lowercase().contains("spot") || userText.lowercase().contains("disease") || attachedImageRes != null) {
                ChatMessage(
                    isFromUser = false,
                    text = advisorResult.text,
                    timeFormatted = "Just now",
                    isDiagnosis = true,
                    diagnosisTitle = "Pathology Diagnostic Analysis",
                    diagnosisConfidence = if (advisorResult.provenance == ResultProvenance.LIVE) 92 else 0,
                    actionDoToday = "Apply targeted organic fungicide or neem extract spray on affected foliage.",
                    actionAvoid = "Avoid excess nitrogen fertilizer application during humid periods.",
                    provenance = advisorResult.provenance.name,
                    errorMessage = advisorResult.errorMessage
                )
            } else {
                ChatMessage(
                    isFromUser = false,
                    text = advisorResult.text,
                    timeFormatted = "Just now",
                    provenance = advisorResult.provenance.name,
                    errorMessage = advisorResult.errorMessage
                )
            }

            repository.addChatMessage(agentMsg)
        }
    }

    fun runCropDoctorScan(bitmap: android.graphics.Bitmap? = null, cropName: String = doctorSelectedCrop.value) {
        isDoctorAnalyzing.value = true
        viewModelScope.launch {
            val result = GeminiFarmAgent.analyzeCropLeafWithGemini(bitmap, cropName)
            isDoctorAnalyzing.value = false
            doctorPathologyResult.value = result
            doctorDiagnosis.value = "${result.diseaseName} (${result.confidence})"
        }
    }

    fun clearCropDoctorDiagnosis() {
        doctorPathologyResult.value = null
        doctorDiagnosis.value = null
    }

    fun addFinancialTransaction(
        title: String,
        category: String,
        amount: Double,
        isIncome: Boolean,
        dateDisplay: String = "Today"
    ) {
        viewModelScope.launch {
            val tx = FinancialTransaction(
                id = "tx_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                title = title,
                category = category,
                amount = amount,
                isIncome = isIncome,
                dateDisplay = dateDisplay
            )
            repository.addTransaction(tx)
        }
    }

    fun deleteFinancialTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addNewCrop(
        cropName: String,
        variety: String,
        growthStage: String,
        isNewCrop: Boolean = false,
        daysElapsed: Int = if (isNewCrop) 0 else 45
    ) {
        viewModelScope.launch {
            val crop = CropRecord(
                id = "crop_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                parcelId = "parcel_1",
                cropName = cropName,
                variety = variety.ifBlank { "High Yield Variety" },
                growthStage = if (isNewCrop) "Sowing & Germination" else growthStage.ifBlank { "Vegetative" },
                healthScore = if (isNewCrop) 98 else 90,
                sowingDate = if (isNewCrop) "Today (Day 0)" else "45 days ago",
                expectedHarvest = if (isNewCrop) "In 120 Days" else "In 75 Days",
                expectedYieldPerAcre = 20.0,
                isNewCrop = isNewCrop,
                daysElapsed = daysElapsed
            )
            repository.addCrop(crop)
        }
    }

    // ==========================================
    // Agri-Commerce & Marketplace Action Methods
    // ==========================================

    fun addToCart(productId: String, qty: Int = 1) {
        val current = cartItems.value.toMutableMap()
        val existingQty = current[productId] ?: 0
        current[productId] = (existingQty + qty).coerceAtLeast(1)
        cartItems.value = current
    }

    fun updateCartQty(productId: String, qty: Int) {
        val current = cartItems.value.toMutableMap()
        if (qty <= 0) {
            current.remove(productId)
        } else {
            current[productId] = qty
        }
        cartItems.value = current
    }

    fun clearCart() {
        cartItems.value = emptyMap()
    }

    /**
     * Place order online with delivery address and payment choice.
     * Triggers financial record, cart clearance, and alert notification in tasks/notifications.
     */
    fun placeAgriOrder(
        product: AgriProduct,
        quantity: Int,
        selectedPack: String,
        deliveryAddress: String,
        paymentMode: String
    ): String {
        val orderTrackingId = "KIS-ORD-${(10000..99999).random()}"
        val totalCost = product.price * quantity

        viewModelScope.launch {
            // 1. Add notification task
            val orderTask = FarmTask(
                id = "task_ord_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                title = "Order Confirmed: ${product.name} (x$quantity)",
                description = "Tracking ID: #$orderTrackingId. Total ₹$totalCost ($paymentMode). Expected delivery in ${product.deliveryDays} day(s) to $deliveryAddress.",
                priority = "High",
                priorityLabel = "ORDER CONFIRMED",
                targetArea = "Agri Store Delivery",
                category = "Fertilizer",
                whyReason = "Delivery partner dispatched from nearest hub. Keep payment ready if COD.",
                isCompleted = false,
                estimatedTimeMinutes = 15
            )
            repository.addTask(orderTask)

            // 2. Add expense transaction
            val transaction = FinancialTransaction(
                id = "tx_ord_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                title = "Purchased ${product.name} (x$quantity)",
                category = "Fertilizer",
                amount = totalCost.toDouble(),
                isIncome = false,
                dateDisplay = "Today"
            )
            repository.addTransaction(transaction)

            // 3. Clear cart
            clearCart()
        }

        return orderTrackingId
    }

    /**
     * List harvest to sell online directly to millers and bulk buyers.
     */
    fun listCropForSale(
        cropName: String,
        variety: String,
        quantityQuintals: Double,
        askingPricePerQuintal: Int,
        moisturePercent: Double,
        pickupLocation: String
    ) {
        val newListing = CropSellListing(
            id = "list_${System.currentTimeMillis()}",
            cropName = cropName,
            variety = variety,
            quantityQuintals = quantityQuintals,
            askingPricePerQuintal = askingPricePerQuintal,
            moisturePercent = moisturePercent,
            grade = "Grade A Verified",
            pickupLocation = pickupLocation,
            listedDate = "Just now",
            status = "Active",
            activeBidsCount = 1,
            highestBidPrice = (askingPricePerQuintal * 0.98).toInt()
        )

        val updated = allCropListings.value.toMutableList()
        updated.add(0, newListing)
        allCropListings.value = updated

        viewModelScope.launch {
            val alertTask = FarmTask(
                id = "task_sell_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                title = "Harvest Listed: $quantityQuintals Qtl $cropName",
                description = "Asking Price: ₹$askingPricePerQuintal/Qtl. Active buyer bids are arriving on Digital Mandi.",
                priority = "Medium",
                priorityLabel = "MARKET LISTING",
                targetArea = pickupLocation,
                category = "MandiSale",
                whyReason = "Verified millers and exporters are reviewing your listing for digital procurement.",
                isCompleted = false,
                estimatedTimeMinutes = 10
            )
            repository.addTask(alertTask)
        }
    }

    /**
     * Accept a buyer's bid and schedule a farm pickup truck.
     */
    fun acceptBuyerBid(listing: CropSellListing, bid: CropBuyerBid) {
        // Update listing status
        val updated = allCropListings.value.map {
            if (it.id == listing.id) it.copy(status = "Sold to ${bid.buyerName}") else it
        }
        allCropListings.value = updated

        viewModelScope.launch {
            // Notification
            val task = FarmTask(
                id = "task_bid_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                title = "Pickup Scheduled: ${listing.cropName} (${listing.quantityQuintals} Qtl)",
                description = "Buyer: ${bid.buyerName}. Total Payment: ₹${bid.totalAmount}. Truck arriving tomorrow for loading. Payment via DBT.",
                priority = "High",
                priorityLabel = "TRUCK SCHEDULED",
                targetArea = listing.pickupLocation,
                category = "MandiSale",
                whyReason = "Keep electronic weighing and gate pass ready for truck driver.",
                isCompleted = false,
                estimatedTimeMinutes = 45
            )
            repository.addTask(task)

            // Record expected income
            val incomeTx = FinancialTransaction(
                id = "tx_sale_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                title = "Mandi Sale: ${listing.quantityQuintals} Qtl ${listing.cropName} (${bid.buyerName})",
                category = "MandiSale",
                amount = bid.totalAmount.toDouble(),
                isIncome = true,
                dateDisplay = "Expected Tomorrow"
            )
            repository.addTransaction(incomeTx)
        }
    }

    /**
     * Book at-home soil / water / tissue testing visit.
     */
    fun bookSoilTest(
        testPackage: SoilTestingPackage,
        parcelName: String,
        date: String,
        timeSlot: String,
        address: String,
        farmerNotes: String = ""
    ): String {
        val bookingId = "SOIL-BK-${(1000..9999).random()}"

        viewModelScope.launch {
            val bookingTask = FarmTask(
                id = "task_soil_${System.currentTimeMillis()}",
                farmId = _selectedFarmId.value,
                title = "Soil Test Booked: ${testPackage.title}",
                description = "Booking ID: #$bookingId. Field: $parcelName. Date: $date ($timeSlot). Certified agronomist will collect samples at $address.",
                priority = "High",
                priorityLabel = "FIELD VISIT BOOKED",
                targetArea = parcelName,
                category = "Fertilizer",
                whyReason = "Technician will perform core sampling. Digital Soil Health Card ready in ${testPackage.turnaroundDays} days.",
                isCompleted = false,
                estimatedTimeMinutes = 30
            )
            repository.addTask(bookingTask)

            if (testPackage.price > 0) {
                val testTx = FinancialTransaction(
                    id = "tx_soil_${System.currentTimeMillis()}",
                    farmId = _selectedFarmId.value,
                    title = "Soil Testing Booking: ${testPackage.title}",
                    category = "Fertilizer",
                    amount = testPackage.price.toDouble(),
                    isIncome = false,
                    dateDisplay = "Today"
                )
                repository.addTransaction(testTx)
            }
        }

        return bookingId
    }

    fun addTask(task: FarmTask) {
        viewModelScope.launch {
            repository.addTask(task)
        }
    }

    // ==========================================
    // CropTask Seasonal Schedules & Irrigation
    // ==========================================

    fun toggleCropTask(task: CropTask) {
        viewModelScope.launch {
            repository.setCropTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun deleteCropTask(task: CropTask) {
        viewModelScope.launch {
            repository.deleteCropTask(task)
        }
    }

    fun addCropTask(task: CropTask) {
        viewModelScope.launch {
            repository.addCropTask(task)
        }
    }

    fun calculateFertilizerRecommendation(
        cropName: String,
        fieldArea: Double,
        areaUnit: String = "Acres"
    ): FertilizerDosageRecommendation {
        return com.example.data.util.FertilizerCalculator.calculate(cropName, fieldArea, areaUnit)
    }
}

