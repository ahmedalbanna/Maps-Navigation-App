package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.viewmodel.MapType
import com.example.viewmodel.MapViewModel
import kotlin.math.roundToInt

@Composable
fun MapDashboard(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val selectedPlace by viewModel.selectedPlace.collectAsState()
    val favoritePlaces by viewModel.favoritePlaces.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val isTrackingLocation by viewModel.isTrackingLocation.collectAsState()

    val routeNodes by viewModel.routeNodes.collectAsState()
    val isSimulatingNavigation by viewModel.isSimulatingNavigation.collectAsState()
    val simulatedSpeed by viewModel.simulatedSpeed.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceRemainingKm by viewModel.distanceRemainingKm.collectAsState()
    val navigationInstruction by viewModel.navigationInstruction.collectAsState()

    val showAddPlaceDialog by viewModel.showAddPlaceDialog.collectAsState()

    var showMapTypeMenu by remember { mutableStateOf(false) }
    var showFavoritesDrawer by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Box(modifier = modifier.fillMaxSize()) {
        // 1. The Core Interactive Map Viewport (Fill Screen Background)
        InteractiveMap(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. SEARCH FLOATING CONTAINER (Top Overlay)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .fillMaxWidth()
                ) {
                    IconButton(onClick = { showFavoritesDrawer = !showFavoritesDrawer }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "المفضلة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                "ابحث عن مدينة، مطعم، فندق أو معلم...",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = Color.Gray,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(32.dp)
                            .background(Color(0xFF004A77), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "أ",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Quick Filter Chips from Sleek Interface
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChipSleek(
                        label = "🍔 مطاعم",
                        selected = searchQuery == "مطعم",
                        onClick = { viewModel.updateSearchQuery(if (searchQuery == "مطعم") "" else "مطعم") }
                    )
                }
                item {
                    FilterChipSleek(
                        label = "🏨 فنادق",
                        selected = searchQuery == "فندق",
                        onClick = { viewModel.updateSearchQuery(if (searchQuery == "فندق") "" else "فندق") }
                    )
                }
                item {
                    FilterChipSleek(
                        label = "🌳 حدائق",
                        selected = searchQuery == "حديقة",
                        onClick = { viewModel.updateSearchQuery(if (searchQuery == "حديقة") "" else "حديقة") }
                    )
                }
            }

            // Suggestions dropdown card
            AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 240.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(suggestions) { result ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectSearchResult(result)
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                val (icon, iconColor, title, subtitle) = when (result) {
                                    is MapViewModel.SearchResult.CityResult -> {
                                        val city = result.city
                                        Quad(Icons.Default.LocationCity, Color(0xFFE65100), city.name, "مدينة في الوطن العربي")
                                    }
                                    is MapViewModel.SearchResult.PoiResult -> {
                                        val poi = result.poi
                                        val c = when (poi.category) {
                                            "مطعم" -> Icons.Default.Restaurant
                                            "فندق" -> Icons.Default.Hotel
                                            "حديقة" -> Icons.Default.Eco
                                            else -> Icons.Default.Place
                                        }
                                        val col = when (poi.category) {
                                            "مطعم" -> Color(0xFFE53935)
                                            "فندق" -> Color(0xFF3949AB)
                                            "حديقة" -> Color(0xFF43A047)
                                            else -> Color(0xFF8E24AA)
                                        }
                                        Quad(c, col, poi.name, "${poi.category} • ${result.cityName}")
                                    }
                                    is MapViewModel.SearchResult.FavResult -> {
                                        val fav = result.fav
                                        Quad(Icons.Default.Star, Color(0xFFFFD700), fav.name, "${fav.placeType} • مفضلتي")
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = iconColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // 3. RIGHT HAND SIDE FLOATING CONTROLS (Map Tools)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedPlace != null || routeNodes.isNotEmpty()) 220.dp else 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Map type layer cards context menu
            AnimatedVisibility(
                visible = showMapTypeMenu,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.width(130.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            "نوع الخريطة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        MapTypeOption("عادي", mapType == MapType.NORMAL) {
                            viewModel.setMapType(MapType.NORMAL)
                            showMapTypeMenu = false
                        }
                        MapTypeOption("قمر صناعي", mapType == MapType.SATELLITE) {
                            viewModel.setMapType(MapType.SATELLITE)
                            showMapTypeMenu = false
                        }
                        MapTypeOption("تضاريس", mapType == MapType.TERRAIN) {
                            viewModel.setMapType(MapType.TERRAIN)
                            showMapTypeMenu = false
                        }
                    }
                }
            }

            // Layer button
            FloatingActionButton(
                onClick = { showMapTypeMenu = !showMapTypeMenu },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = "طبقات الخريطة")
            }

            // Tracking button
            FloatingActionButton(
                onClick = { viewModel.toggleTracking() },
                containerColor = if (isTrackingLocation) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isTrackingLocation) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = if (isTrackingLocation) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    contentDescription = "موقعي"
                )
            }

            // Zoom in button
            FloatingActionButton(
                onClick = { viewModel.zoom.value = (viewModel.zoom.value + 1.0f).coerceAtMost(18.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.DarkGray,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "تكبير")
            }

            // Zoom out button
            FloatingActionButton(
                onClick = { viewModel.zoom.value = (viewModel.zoom.value - 1.0f).coerceAtLeast(5.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.DarkGray,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "تصغير")
            }
        }

        // 4. BOTTOM DETAIL OR ROUTE HUD SHEETS
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            // A. ACTIVE NAVIGATION SIMULATOR HUD
            if (routeNodes.isNotEmpty()) {
                ElevatedCard(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .align(Alignment.BottomCenter)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "اتجاة",
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = navigationInstruction,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "الوجهة النهائية: ${selectedPlace?.name ?: ""}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            // Cancel routing button
                            IconButton(onClick = { viewModel.cancelNavigation() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HudStat(
                                value = if (isSimulatingNavigation) "${simulatedSpeed.roundToInt()}" else "--",
                                unit = "كم/ساعة",
                                label = "السرعة الحالية"
                            )
                            HudStat(
                                value = if (isSimulatingNavigation || distanceRemainingKm > 0.0) String.format("%.1f", distanceRemainingKm) else "--",
                                unit = "كيلومتر",
                                label = "المسافة المتبقية"
                            )
                            HudStat(
                                value = if (isSimulatingNavigation || etaMinutes > 0) "$etaMinutes" else "--",
                                unit = "دقيقة",
                                label = "الوقت المتبقي"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isSimulatingNavigation) {
                                Button(
                                    onClick = { viewModel.startNavigation() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بدء رحلة المحاكاة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.stopNavigation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إيقاف المحاكاة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // B. DESIGNS OF CHOSEN POI / PLACE SHEET
                selectedPlace?.let { place ->
                    ElevatedCard(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .align(Alignment.BottomCenter)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tagIcon = when (place.type) {
                                    "مطعم" -> Icons.Default.Restaurant
                                    "فندق" -> Icons.Default.Hotel
                                    "حديقة" -> Icons.Default.Eco
                                    "مدينة" -> Icons.Default.LocationCity
                                    else -> Icons.Default.Place
                                }
                                val iconColor = when (place.type) {
                                    "مطعم" -> Color(0xFFE53935)
                                    "فندق" -> Color(0xFF3949AB)
                                    "حديقة" -> Color(0xFF43A047)
                                    "مدينة" -> Color(0xFFE65100)
                                    else -> Color(0xFF1E88E5)
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = iconColor.copy(alpha = 0.12f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(
                                        imageVector = tagIcon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "النوع: ${place.type}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                // Star favorite toggler
                                IconButton(onClick = { viewModel.toggleFavoriteCurrentPlace() }) {
                                    Icon(
                                        imageVector = if (place.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "مفضلة",
                                        tint = if (place.isFavorite) Color(0xFFFFB300) else Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                // Close card
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = place.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الإحداثيات: ${String.format("%.4f", place.latitude)}، ${String.format("%.4f", place.longitude)}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Light
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.planRouteToSelected() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Directions, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("رسم مسار الطريق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. COLLAPSIBLE DRAWER SCREEN (Favorites List Overlay)
        AnimatedVisibility(
            visible = showFavoritesDrawer,
            enter = slideInHorizontally(animationSpec = tween(300)) { -it },
            exit = slideOutHorizontally(animationSpec = tween(300)) { -it },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "الأماكن المفضلة",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showFavoritesDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    if (favoritePlaces.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "لا توجد أماكن محفوظة حتى الآن.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "اضغط مطولاً على الخريطة وسجل مكاناً هنا!",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favoritePlaces) { fav ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectSearchResult(MapViewModel.SearchResult.FavResult(fav))
                                            showFavoritesDrawer = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(fav.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            if (fav.notes.isNotEmpty()) {
                                                Text(fav.notes, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteFavoritePlace(fav) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف المفضلة", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. POPUP DIALOG: ADD CUSTOM PIN PLACE
        if (showAddPlaceDialog) {
            var placeName by remember { mutableStateOf("") }
            var placeNotes by remember { mutableStateOf("") }
            var placeCategory by remember { mutableStateOf("أخرى") }
            val categories = listOf("مطعم", "فندق", "حديقة", "عمل", "منزل", "أخرى")

            Dialog(onDismissRequest = { viewModel.dismissAddPlaceDialog() }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "حفظ موقع مخصص جديد",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = placeName,
                            onValueChange = { placeName = it },
                            label = { Text("اسم الموقع الجغرافي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = placeNotes,
                            onValueChange = { placeNotes = it },
                            label = { Text("ملاحظات / وصف للمكان") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "نوع التصنيف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start),
                            color = Color.Gray
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            categories.take(3).forEach { cat ->
                                catChip(cat, placeCategory == cat) { placeCategory = cat }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            categories.drop(3).forEach { cat ->
                                catChip(cat, placeCategory == cat) { placeCategory = cat }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.dismissAddPlaceDialog() }) {
                                Text("إلغاء", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (placeName.isNotBlank()) {
                                        viewModel.saveTemporaryPlace(placeName, placeNotes, placeCategory)
                                    }
                                },
                                enabled = placeName.isNotBlank()
                            ) {
                                Text("حفظ المكان")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun catChip(cat: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable { onSelect() }
            .clip(RoundedCornerShape(8.dp)),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = cat,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MapTypeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun HudStat(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, fontSize = 10.sp, color = Color.Gray)
        }
        Text(label, fontSize = 9.sp, color = Color.Gray)
    }
}

data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun FilterChipSleek(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFFE3E3E3)
        ),
        shadowElevation = 2.dp,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
