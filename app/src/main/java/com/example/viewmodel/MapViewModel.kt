package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.*

enum class MapType {
    NORMAL, SATELLITE, TERRAIN
}

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val favoriteRepository = FavoritePlaceRepository(db.favoritePlaceDao())

    // 1. Map Viewport state (starting in Riyadh)
    val centerLat = MutableStateFlow(24.7136)
    val centerLon = MutableStateFlow(46.6753)
    val zoom = MutableStateFlow(12.0f) // zoom level from e.g. 5.0 to 18.0

    // 2. Map style layer
    private val _mapType = MutableStateFlow(MapType.NORMAL)
    val mapType = _mapType.asStateFlow()

    // 3. User location (GPS Simulation)
    val userLat = MutableStateFlow(24.7136)
    val userLon = MutableStateFlow(46.6753)
    private val _isTrackingLocation = MutableStateFlow(true)
    val isTrackingLocation = _isTrackingLocation.asStateFlow()

    // 4. Room Database Favorites State
    val favoritePlaces: StateFlow<List<FavoritePlace>> = favoriteRepository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 5. Search Engine state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SearchResult>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _selectedPlace = MutableStateFlow<SelectedPlaceDetails?>(null)
    val selectedPlace = _selectedPlace.asStateFlow()

    // 6. Navigation and Routing state
    private val _routeNodes = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routeNodes = _routeNodes.asStateFlow()

    private val _isSimulatingNavigation = MutableStateFlow(false)
    val isSimulatingNavigation = _isSimulatingNavigation.asStateFlow()

    private val _currentNodeIndex = MutableStateFlow(0)
    val currentNodeIndex = _currentNodeIndex.asStateFlow()

    private val _simulatedSpeed = MutableStateFlow(0f)
    val simulatedSpeed = _simulatedSpeed.asStateFlow()

    private val _etaMinutes = MutableStateFlow(0)
    val etaMinutes = _etaMinutes.asStateFlow()

    private val _distanceRemainingKm = MutableStateFlow(0.0)
    val distanceRemainingKm = _distanceRemainingKm.asStateFlow()

    private val _navigationInstruction = MutableStateFlow("مستعد للبدء")
    val navigationInstruction = _navigationInstruction.asStateFlow()

    // 7. Temporary marker (Long press pin drop)
    private val _temporaryMarker = MutableStateFlow<Pair<Double, Double>?>(null)
    val temporaryMarker = _temporaryMarker.asStateFlow()

    private val _showAddPlaceDialog = MutableStateFlow(false)
    val showAddPlaceDialog = _showAddPlaceDialog.asStateFlow()

    // Navigation coroutine reference
    private var simulationJob: Job? = null

    // Search results data class helper
    sealed class SearchResult {
        data class CityResult(val city: PresetCity) : SearchResult()
        data class PoiResult(val poi: MapPoi, val cityName: String) : SearchResult()
        data class FavResult(val fav: FavoritePlace) : SearchResult()
    }

    // Selected place helper
    data class SelectedPlaceDetails(
        val name: String,
        val description: String,
        val type: String, // "مدينة" / "مطعم" / "فندق" / "حديقة" / "معلم" / "موقع مخصص"
        val latitude: Double,
        val longitude: Double,
        val isFavorite: Boolean = false,
        val favoriteId: Int? = null
    )

    init {
        // Initially set user location to Riyadh center
        userLat.value = 24.7136
        userLon.value = 46.6753
    }

    // Toggle Map Type
    fun setMapType(type: MapType) {
        _mapType.value = type
    }

    // Toggle real-time tracking
    fun toggleTracking() {
        val next = !_isTrackingLocation.value
        _isTrackingLocation.value = next
        if (next) {
            // Smooth reset camera to user location
            animateCameraTo(userLat.value, userLon.value, 13.5f)
        }
    }

    fun animateCameraTo(lat: Double, lon: Double, targetZoom: Float? = null) {
        viewModelScope.launch {
            // Simple ease animations for moving camera focus
            val steps = 15
            val startLat = centerLat.value
            val startLon = centerLon.value
            val startZoom = zoom.value
            val endZoom = targetZoom ?: zoom.value

            for (i in 1..steps) {
                val t = i.toFloat() / steps
                centerLat.value = startLat + (lat - startLat) * t
                centerLon.value = startLon + (lon - startLon) * t
                zoom.value = startZoom + (endZoom - startZoom) * t
                delay(15)
            }
        }
    }

    // Update query and search presets & favorites
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }

        val results = mutableListOf<SearchResult>()
        val lowercaseQuery = query.trim()

        // 1. Search in local Room favorites
        val currentFavorites = favoritePlaces.value
        currentFavorites.forEach { fav ->
            if (fav.name.contains(lowercaseQuery, ignoreCase = true) ||
                fav.notes.contains(lowercaseQuery, ignoreCase = true)) {
                results.add(SearchResult.FavResult(fav))
            }
        }

        // 2. Search in Preset Cities
        MapDataPresets.cities.forEach { city ->
            if (city.name.contains(lowercaseQuery, ignoreCase = true)) {
                results.add(SearchResult.CityResult(city))
            }
            // 3. Search in POIs of cities
            city.pois.forEach { poi ->
                if (poi.name.contains(lowercaseQuery, ignoreCase = true) ||
                    poi.description.contains(lowercaseQuery, ignoreCase = true)) {
                    results.add(SearchResult.PoiResult(poi, city.name))
                }
            }
        }

        _suggestions.value = results
    }

    // Select search result
    fun selectSearchResult(result: SearchResult) {
        _temporaryMarker.value = null // clear temp markers
        cancelNavigation()

        when (result) {
            is SearchResult.CityResult -> {
                val city = result.city
                val isFav = favoritePlaces.value.any { it.name == city.name && it.latitude == city.latitude }
                val favId = favoritePlaces.value.find { it.name == city.name && it.latitude == city.latitude }?.id
                _selectedPlace.value = SelectedPlaceDetails(
                    name = city.name,
                    description = city.description,
                    type = "مدينة",
                    latitude = city.latitude,
                    longitude = city.longitude,
                    isFavorite = isFav,
                    favoriteId = favId
                )
                animateCameraTo(city.latitude, city.longitude, 11f)
            }
            is SearchResult.PoiResult -> {
                val poi = result.poi
                val isFav = favoritePlaces.value.any { it.name == poi.name && it.latitude == poi.latitude }
                val favId = favoritePlaces.value.find { it.name == poi.name && it.latitude == poi.latitude }?.id
                _selectedPlace.value = SelectedPlaceDetails(
                    name = poi.name,
                    description = poi.description,
                    type = poi.category,
                    latitude = poi.latitude,
                    longitude = poi.longitude,
                    isFavorite = isFav,
                    favoriteId = favId
                )
                animateCameraTo(poi.latitude, poi.longitude, 14.5f)
            }
            is SearchResult.FavResult -> {
                val fav = result.fav
                _selectedPlace.value = SelectedPlaceDetails(
                    name = fav.name,
                    description = if (fav.notes.isNotEmpty()) fav.notes else "موقع مفضل محفوظ",
                    type = fav.placeType,
                    latitude = fav.latitude,
                    longitude = fav.longitude,
                    isFavorite = true,
                    favoriteId = fav.id
                )
                animateCameraTo(fav.latitude, fav.longitude, 14.5f)
            }
        }
        _searchQuery.value = ""
        _suggestions.value = emptyList()
    }

    // Select POI directly on map tap
    fun selectPoiDirectly(poi: MapPoi, cityName: String) {
        _temporaryMarker.value = null
        cancelNavigation()

        val isFav = favoritePlaces.value.any { it.name == poi.name && it.latitude == poi.latitude }
        val favId = favoritePlaces.value.find { it.name == poi.name && it.latitude == poi.latitude }?.id
        _selectedPlace.value = SelectedPlaceDetails(
            name = poi.name,
            description = poi.description,
            type = poi.category,
            latitude = poi.latitude,
            longitude = poi.longitude,
            isFavorite = isFav,
            favoriteId = favId
        )
        animateCameraTo(poi.latitude, poi.longitude, 14.5f)
    }

    // Select city directly
    fun selectCityDirectly(city: PresetCity) {
        _temporaryMarker.value = null
        cancelNavigation()

        val isFav = favoritePlaces.value.any { it.name == city.name && it.latitude == city.latitude }
        val favId = favoritePlaces.value.find { it.name == city.name && it.latitude == city.latitude }?.id
        _selectedPlace.value = SelectedPlaceDetails(
            name = city.name,
            description = city.description,
            type = "مدينة",
            latitude = city.latitude,
            longitude = city.longitude,
            isFavorite = isFav,
            favoriteId = favId
        )
        animateCameraTo(city.latitude, city.longitude, 11f)
    }

    // Clear selection
    fun clearSelection() {
        _selectedPlace.value = null
        cancelNavigation()
    }

    // Long press Map Pin creation
    fun onMapLongPress(lat: Double, lon: Double) {
        _temporaryMarker.value = Pair(lat, lon)
        _showAddPlaceDialog.value = true
    }

    fun dismissAddPlaceDialog() {
        _showAddPlaceDialog.value = false
        _temporaryMarker.value = null
    }

    fun saveTemporaryPlace(name: String, notes: String, category: String) {
        val coords = _temporaryMarker.value ?: return
        viewModelScope.launch {
            val place = FavoritePlace(
                name = name,
                placeType = category,
                latitude = coords.first,
                longitude = coords.second,
                notes = notes
            )
            favoriteRepository.insert(place)

            // Auto-select the newly added place
            _selectedPlace.value = SelectedPlaceDetails(
                name = place.name,
                description = place.notes,
                type = place.placeType,
                latitude = place.latitude,
                longitude = place.longitude,
                isFavorite = true
            )
            _showAddPlaceDialog.value = false
            _temporaryMarker.value = null
        }
    }

    // Save current selection to room DB
    fun toggleFavoriteCurrentPlace() {
        val current = _selectedPlace.value ?: return
        viewModelScope.launch {
            if (current.isFavorite && current.favoriteId != null) {
                // Remove favorite
                favoriteRepository.deleteById(current.favoriteId)
                _selectedPlace.value = current.copy(isFavorite = false, favoriteId = null)
            } else {
                // Add to favorite
                val fav = FavoritePlace(
                    name = current.name,
                    placeType = current.type,
                    latitude = current.latitude,
                    longitude = current.longitude,
                    notes = current.description
                )
                favoriteRepository.insert(fav)
                // We re-query the ID later, but for now we set isFavorite true
                _selectedPlace.value = current.copy(isFavorite = true)
            }
        }
    }

    fun deleteFavoritePlace(fav: FavoritePlace) {
        viewModelScope.launch {
            favoriteRepository.delete(fav)
            if (_selectedPlace.value?.latitude == fav.latitude && _selectedPlace.value?.longitude == fav.longitude) {
                _selectedPlace.value = _selectedPlace.value?.copy(isFavorite = false, favoriteId = null)
            }
        }
    }

    // ROUTING AND SIMULATED NAVIGATION
    fun planRouteToSelected() {
        val target = _selectedPlace.value ?: return
        val startLat = userLat.value
        val startLon = userLon.value

        // Try to find if there is a matching predefined highway!
        var nodes = findPresetHighway(startLat, startLon, target.latitude, target.longitude)

        if (nodes.isEmpty()) {
            // Generate a natural-looking winding road path with curving segments (simulated path)
            nodes = generateSimulatedHighwayNodes(startLat, startLon, target.latitude, target.longitude)
        }

        _routeNodes.value = nodes
        _currentNodeIndex.value = 0
        _isSimulatingNavigation.value = false

        // Draw path and show fit layout
        val centerL = (startLat + target.latitude) / 2
        val centerLo = (startLon + target.longitude) / 2
        // estimate a good zoom level to encompass both points
        val d = calculateDistance(startLat, startLon, target.latitude, target.longitude)
        val viewZoom = when {
            d < 5.0 -> 13.5f
            d < 20.0 -> 11f
            d < 100.0 -> 8.5f
            d < 500.0 -> 6.5f
            else -> 5f
        }
        animateCameraTo(centerL, centerLo, viewZoom)

        // Set info initial HUD
        _distanceRemainingKm.value = d
        _simulatedSpeed.value = 0f
        _etaMinutes.value = ((d / 90.0) * 60).roundToInt() // 90 km/h baseline
        _navigationInstruction.value = "جاهز لتشغيل الملاحة"
    }

    fun startNavigation() {
        if (_routeNodes.value.isEmpty()) return
        cancelNavigation() // clear existing jobs

        _isSimulatingNavigation.value = true
        _isTrackingLocation.value = true // automatically lock camera to the vehicle

        simulationJob = viewModelScope.launch {
            val nodes = _routeNodes.value
            _currentNodeIndex.value = 0

            // Teleport simulated GPS position to source node immediately
            userLat.value = nodes[0].first
            userLon.value = nodes[0].second

            _navigationInstruction.value = "بدأت محاكاة الملاحة! انطلق على المسار المقترح."
            animateCameraTo(userLat.value, userLon.value, 14.5f)

            for (i in 0 until (nodes.size - 1)) {
                if (!_isSimulatingNavigation.value) break

                val fromNode = nodes[i]
                val toNode = nodes[i + 1]
                _currentNodeIndex.value = i

                // Interpolate steps along this highway leg
                val distanceLeg = calculateDistance(fromNode.first, fromNode.second, toNode.first, toNode.second)
                val totalSteps = max(5, (distanceLeg * 2).roundToInt())

                val prevInstruction = _navigationInstruction.value
                val isLastLeg = (i == nodes.size - 2)

                // Instruction updates
                _navigationInstruction.value = when {
                    i == 0 -> "اسلك طريق الملاحة السريع نحو الوجهة."
                    isLastLeg -> "اقتربت من الوجهة؛ استعد للانعطاف بعد قليل."
                    else -> "استمر على الطريق السريع، الحركة المرورية ممتازة."
                }

                for (step in 1..totalSteps) {
                    if (!_isSimulatingNavigation.value) break

                    val ratio = step.toDouble() / totalSteps
                    val stepLat = fromNode.first + (toNode.first - fromNode.first) * ratio
                    val stepLon = fromNode.second + (toNode.second - fromNode.second) * ratio

                    // Speed simulation
                    val targetSpeed = if (isLastLeg) 60f else 115f
                    val variance = (Math.random() * 10 - 5).toFloat()
                    _simulatedSpeed.value = max(30f, targetSpeed + variance)

                    userLat.value = stepLat
                    userLon.value = stepLon

                    // auto center map if tracking is true
                    if (_isTrackingLocation.value) {
                        centerLat.value = stepLat
                        centerLon.value = stepLon
                    }

                    // compute remaining whole journey distance
                    var remainingD = calculateDistance(stepLat, stepLon, toNode.first, toNode.second)
                    for (j in (i + 1) until (nodes.size - 1)) {
                        remainingD += calculateDistance(nodes[j].first, nodes[j].second, nodes[j + 1].first, nodes[j + 1].second)
                    }

                    _distanceRemainingKm.value = remainingD
                    _etaMinutes.value = max(1, ((remainingD / _simulatedSpeed.value.toDouble()) * 60).roundToInt())

                    delay(800) // tick interval
                }
            }

            // Route completed!
            if (_isSimulatingNavigation.value) {
                val lastNode = nodes.last()
                userLat.value = lastNode.first
                userLon.value = lastNode.second
                animateCameraTo(userLat.value, userLon.value, 14.5f)

                _isSimulatingNavigation.value = false
                _simulatedSpeed.value = 0f
                _distanceRemainingKm.value = 0.0
                _etaMinutes.value = 0
                _navigationInstruction.value = "لقد وصلت إلى وجهتك المستهدفة بأمان!"
            }
        }
    }

    fun stopNavigation() {
        _isSimulatingNavigation.value = false
        _simulatedSpeed.value = 0f
        simulationJob?.cancel()
        _navigationInstruction.value = "تم إيقاف الملاحة"
    }

    fun cancelNavigation() {
        stopNavigation()
        _routeNodes.value = emptyList()
    }

    // Helper: find highway that connects near these places
    private fun findPresetHighway(
        sLat: Double, sLon: Double, tLat: Double, tLon: Double
    ): List<Pair<Double, Double>> {
        MapDataPresets.highways.forEach { highway ->
            val startNode = highway.nodes.first()
            val endNode = highway.nodes.last()

            // Is starting near startNode, and target near endNode?
            val dStart = calculateDistance(sLat, sLon, startNode.first, startNode.second)
            val dEnd = calculateDistance(tLat, tLon, endNode.first, endNode.second)

            if (dStart < 1.0 && dEnd < 1.0) {
                return highway.nodes
            }

            // Check reversed direction
            val dStartRev = calculateDistance(sLat, sLon, endNode.first, endNode.second)
            val dEndRev = calculateDistance(tLat, tLon, startNode.first, startNode.second)
            if (dStartRev < 1.0 && dEndRev < 1.0) {
                return highway.nodes.reversed()
            }
        }
        return emptyList()
    }

    // Helper: generate a beautiful segmented curvy highway route for organic look
    private fun generateSimulatedHighwayNodes(
        sLat: Double, sLon: Double, tLat: Double, tLon: Double
    ): List<Pair<Double, Double>> {
        val nodes = mutableListOf<Pair<Double, Double>>()
        nodes.add(Pair(sLat, sLon))

        val segmentsCount = 6
        val dLat = tLat - sLat
        val dLon = tLon - sLon

        for (i in 1 until segmentsCount) {
            val progress = i.toDouble() / segmentsCount
            // Direct linear point
            val lineLat = sLat + dLat * progress
            val lineLon = sLon + dLon * progress

            // Add organic wiggle perpendicular to the direct road direction to make highways feel real
            val normalX = -dLon / sqrt(dLat * dLat + dLon * dLon)
            val normalY = dLat / sqrt(dLat * dLat + dLon * dLon)

            // wiggle size depends on route scale and sine wave
            val wiggleMag = 0.08 * sin(progress * Math.PI)
            val wiggleLat = lineLat + normalY * wiggleMag
            val wiggleLon = lineLon + normalX * wiggleMag

            nodes.add(Pair(wiggleLat, wiggleLon))
        }

        nodes.add(Pair(tLat, tLon))
        return nodes
    }

    // Haversine formula for actual geo distance in kilometers
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
