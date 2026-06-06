package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.MapType
import com.example.viewmodel.MapViewModel
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun InteractiveMap(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val centerLat by viewModel.centerLat.collectAsState()
    val centerLon by viewModel.centerLon.collectAsState()
    val zoom by viewModel.zoom.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val routeNodes by viewModel.routeNodes.collectAsState()
    val userLat by viewModel.userLat.collectAsState()
    val userLon by viewModel.userLon.collectAsState()
    val favorites by viewModel.favoritePlaces.collectAsState()
    val temporaryMarker by viewModel.temporaryMarker.collectAsState()

    // Animation for flowing route path
    val infiniteTransition = rememberInfiniteTransition(label = "route_drawing")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Pulsing radar animation for user blue dot
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .background(
                when (mapType) {
                    MapType.NORMAL -> Color(0xFFE5E9EC)   // Sleek silver-gray land
                    MapType.SATELLITE -> Color(0xFF0C101B) // Deep space black
                    MapType.TERRAIN -> Color(0xFFCBD2D8)   // High-contrast slate topography land
                }
            )
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Projection mapping helper
        val scale = 60f * (1.5f.pow(zoom - 5f))
        fun toScreen(lat: Double, lon: Double): Offset {
            val x = (width / 2f) + (lon - centerLon).toFloat() * scale
            val y = (height / 2f) - (lat - centerLat).toFloat() * scale
            return Offset(x, y)
        }

        // Draw Map Geometry & Networks First
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(centerLat, centerLon, zoom) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.centerLon.value -= (dragAmount.x / scale).toDouble()
                        viewModel.centerLat.value += (dragAmount.y / scale).toDouble()
                    }
                }
                .pointerInput(centerLat, centerLon, zoom) {
                    detectTapGestures(
                        onLongPress = { tapOffset ->
                            val lon = centerLon + (tapOffset.x - (width / 2f)) / scale
                            val lat = centerLat - (tapOffset.y - (height / 2f)) / scale
                            viewModel.onMapLongPress(lat, lon)
                        },
                        onTap = { tapOffset ->
                            // Look up clickable cities, POIs, or favorites
                            var clickedSomething = false

                            // 1. Check favorites first
                            for (fav in favorites) {
                                val scr = toScreen(fav.latitude, fav.longitude)
                                val dist = (tapOffset.x - scr.x).pow(2) + (tapOffset.y - scr.y).pow(2)
                                if (dist < 1000f) {
                                    viewModel.selectSearchResult(MapViewModel.SearchResult.FavResult(fav))
                                    clickedSomething = true
                                    break
                                }
                            }

                            // 2. Check POIs inside cities (rendered if zoom > 10.5)
                            if (!clickedSomething && zoom > 10.5f) {
                                for (city in MapDataPresets.cities) {
                                    for (poi in city.pois) {
                                        val scr = toScreen(poi.latitude, poi.longitude)
                                        val dist = (tapOffset.x - scr.x).pow(2) + (tapOffset.y - scr.y).pow(2)
                                        if (dist < 800f) {
                                            viewModel.selectPoiDirectly(poi, city.name)
                                            clickedSomething = true
                                            break
                                        }
                                    }
                                    if (clickedSomething) break
                                }
                            }

                            // 3. Check general cities
                            if (!clickedSomething) {
                                for (city in MapDataPresets.cities) {
                                    val scr = toScreen(city.latitude, city.longitude)
                                    val dist = (tapOffset.x - scr.x).pow(2) + (tapOffset.y - scr.y).pow(2)
                                    if (dist < 1200f) {
                                        viewModel.selectCityDirectly(city)
                                        clickedSomething = true
                                        break
                                    }
                                }
                            }

                            if (!clickedSomething) {
                                viewModel.clearSelection()
                            }
                        }
                    )
                }
        ) {
            // Colors based on map layers
            val seaColor = when (mapType) {
                MapType.NORMAL -> Color(0xFFA1C4FD) // Beautiful pastel ice-blue
                MapType.SATELLITE -> Color(0xFF0F1E36)
                MapType.TERRAIN -> Color(0xFF90E0EF)
            }
            val greenBgColor = when (mapType) {
                MapType.NORMAL -> Color(0xFFC1E8C1) // Clean fresh parks green
                MapType.SATELLITE -> Color(0xFF143026)
                MapType.TERRAIN -> Color(0xFFC1E8C1)
            }
            val roadColor = when (mapType) {
                MapType.NORMAL -> Color(0xFFFFFFFF)
                MapType.SATELLITE -> Color(0xFFF39C12) // Golden highway
                MapType.TERRAIN -> Color(0xFF7F8C8D)
            }
            val gridColor = when (mapType) {
                MapType.NORMAL -> Color(0xFFE3E3E3) // Sleek crisp light-gray coordinates grid
                MapType.SATELLITE -> Color(0xFF232D3F)
                MapType.TERRAIN -> Color(0xFFCBD2D8)
            }

            // A. DRAW GRIDS (Geographic coordinates grids)
            val dLonInc = 5.0
            val dLatInc = 5.0
            val minLatGrid = 10.0
            val maxLatGrid = 40.0
            val minLonGrid = 25.0
            val maxLonGrid = 65.0

            // Latlines (horizontal)
            var latGrid = minLatGrid
            while (latGrid <= maxLatGrid) {
                val startP = toScreen(latGrid, minLonGrid)
                val endP = toScreen(latGrid, maxLonGrid)
                drawLine(
                    color = gridColor,
                    start = startP,
                    end = endP,
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                )
                latGrid += dLatInc
            }

            // Lonlines (vertical)
            var lonGrid = minLonGrid
            while (lonGrid <= maxLonGrid) {
                val startP = toScreen(minLatGrid, lonGrid)
                val endP = toScreen(maxLatGrid, lonGrid)
                drawLine(
                    color = gridColor,
                    start = startP,
                    end = endP,
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                )
                lonGrid += dLonInc
            }

            // B. DRAW GEOGRAPHICAL WATER BODIES (Red Sea, Persian Gulf, Nile)
            // 1. Red Sea Polygon
            val redSeaGeo = listOf(
                Pair(15.0, 42.0), Pair(18.0, 40.0), Pair(21.0, 38.5), Pair(24.0, 36.5),
                Pair(27.0, 34.5), Pair(29.8, 32.5), Pair(29.9, 32.7), Pair(28.0, 34.0),
                Pair(27.0, 35.0), Pair(24.5, 38.0), Pair(22.0, 40.0), Pair(19.0, 42.0),
                Pair(15.0, 43.5)
            )
            val redSeaPath = Path().apply {
                val firstXy = toScreen(redSeaGeo[0].first, redSeaGeo[0].second)
                moveTo(firstXy.x, firstXy.y)
                for (j in 1 until redSeaGeo.size) {
                    val xy = toScreen(redSeaGeo[j].first, redSeaGeo[j].second)
                    lineTo(xy.x, xy.y)
                }
                close()
            }
            drawPath(path = redSeaPath, color = seaColor)

            // 2. Persian Gulf Polygon
            val gulfGeo = listOf(
                Pair(24.0, 51.5), Pair(25.5, 52.5), Pair(26.5, 50.5), Pair(29.0, 48.0),
                Pair(30.3, 48.0), Pair(29.0, 50.5), Pair(27.5, 52.8), Pair(26.0, 56.4),
                Pair(25.0, 56.4)
            )
            val gulfPath = Path().apply {
                val firstXy = toScreen(gulfGeo[0].first, gulfGeo[0].second)
                moveTo(firstXy.x, firstXy.y)
                for (j in 1 until gulfGeo.size) {
                    val xy = toScreen(gulfGeo[j].first, gulfGeo[j].second)
                    lineTo(xy.x, xy.y)
                }
                close()
            }
            drawPath(path = gulfPath, color = seaColor)

            // 3. Nile River (Thick flowing line)
            val nileGeo = listOf(
                Pair(15.0, 32.5), Pair(17.5, 32.0), Pair(20.0, 32.2), Pair(22.0, 31.5),
                Pair(24.0, 32.8), Pair(26.0, 32.7), Pair(28.0, 31.0), Pair(30.0, 31.2),
                Pair(31.2, 30.1)
            )
            val nilePath = Path().apply {
                val firstXy = toScreen(nileGeo[0].first, nileGeo[0].second)
                moveTo(firstXy.x, firstXy.y)
                for (j in 1 until nileGeo.size) {
                    val xy = toScreen(nileGeo[j].first, nileGeo[j].second)
                    lineTo(xy.x, xy.y)
                }
            }
            drawPath(
                path = nilePath,
                color = seaColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // C. DRAW TERRAIN HYPSOMETRIC ELEVATION CURVES
            if (mapType == MapType.TERRAIN) {
                // Drawing elevation contour shapes around Makkah Mountains (Sarawat Mountains)
                val mtnCenters = listOf(
                    Pair(21.28, 40.42), // Taif
                    Pair(18.21, 42.50), // Abha
                    Pair(22.50, 40.0)
                )
                mtnCenters.forEach { mtn ->
                    val c = toScreen(mtn.first, mtn.second)
                    drawCircle(
                        color = Color(0x288B5A2B),
                        center = c,
                        radius = 40.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0x358B5A2B),
                        center = c,
                        radius = 25.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0x408B5A2B),
                        center = c,
                        radius = 12.dp.toPx()
                    )
                }
            }

            // D. DRAW PRESET ROADS AND HIGHWAYS
            MapDataPresets.highways.forEach { hw ->
                val roadPath = Path().apply {
                    val first = toScreen(hw.nodes[0].first, hw.nodes[0].second)
                    moveTo(first.x, first.y)
                    for (i in 1 until hw.nodes.size) {
                        val node = toScreen(hw.nodes[i].first, hw.nodes[i].second)
                        lineTo(node.x, node.y)
                    }
                }
                // Under normal mode draw boundary and road core
                if (mapType == MapType.NORMAL) {
                    drawPath(
                        path = roadPath,
                        color = Color(0xFFD6DBDF),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                drawPath(
                    path = roadPath,
                    color = roadColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // E. DRAW ACTIVE ROUTING PATH (POLYLINE WITH DYNAMIC FLOW)
            if (routeNodes.isNotEmpty()) {
                val polylinePath = Path().apply {
                    val first = toScreen(routeNodes[0].first, routeNodes[0].second)
                    moveTo(first.x, first.y)
                    for (i in 1 until routeNodes.size) {
                        val node = toScreen(routeNodes[i].first, routeNodes[i].second)
                        lineTo(node.x, node.y)
                    }
                }

                // 1. Draw solid thick glow under-layer
                drawPath(
                    path = polylinePath,
                    color = Color(0xFF0B57D0).copy(alpha = 0.25f),
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 2. Draw active flowing blue path
                drawPath(
                    path = polylinePath,
                    color = Color(0xFF0B57D0),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(35f, 25f), flowPhase)
                    )
                )
            }

            // F. DRAW SIMULATED REAL-TIME POSITION PULSING RADAR RIPPLE
            val userPos = toScreen(userLat, userLon)
            drawCircle(
                color = Color(0xFF0B57D0).copy(alpha = pulseAlpha),
                center = userPos,
                radius = 16.dp.toPx() * pulseScale
            )
            drawCircle(
                color = Color.White,
                center = userPos,
                radius = 8.dp.toPx()
            )
            drawCircle(
                color = Color(0xFF0B57D0),
                center = userPos,
                radius = 6.dp.toPx()
            )
        }

        // Overlay Markers (Rendered as robust Compose widgets absolute-positioned)
        // This is extremely efficient, looks premium, handles proper Arabic rendering and click events!
        MapMarkersOverlay(
            toScreen = ::toScreen,
            zoom = zoom,
            favorites = favorites,
            temporaryMarker = temporaryMarker,
            viewModel = viewModel
        )
    }
}

@Composable
fun MapMarkersOverlay(
    toScreen: (Double, Double) -> Offset,
    zoom: Float,
    favorites: List<FavoritePlace>,
    temporaryMarker: Pair<Double, Double>?,
    viewModel: MapViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // A. CITIES LABELS (Visible at all zooms)
        MapDataPresets.cities.forEach { city ->
            val screenPos = toScreen(city.latitude, city.longitude)
            // Cull offscreen markers to maximize graphics performance
            if (screenPos.x in -100f..1500f && screenPos.y in -100f..2500f) {
                Box(
                    modifier = Modifier
                        .offset(x = (screenPos.x / 2.625f).dp - 50.dp, y = (screenPos.y / 2.625f).dp - 30.dp) // coordinate normalization
                        .width(100.dp)
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF6C00),
                            modifier = Modifier.size(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {}
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = city.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // B. POINTS OF INTEREST (Visible only when deep zoomed > 10.5)
        if (zoom > 10.5f) {
            MapDataPresets.cities.forEach { city ->
                city.pois.forEach { poi ->
                    val screenPos = toScreen(poi.latitude, poi.longitude)
                    if (screenPos.x in -100f..1500f && screenPos.y in -100f..2500f) {
                        Box(
                            modifier = Modifier
                                .offset(x = (screenPos.x / 2.625f).dp - 40.dp, y = (screenPos.y / 2.625f).dp - 30.dp)
                                .width(80.dp)
                                .wrapContentHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .padding(1.dp)
                            ) {
                                val icon = when (poi.category) {
                                    "مطعم" -> Icons.Default.Restaurant
                                    "فندق" -> Icons.Default.Hotel
                                    "حديقة" -> Icons.Default.Eco
                                    else -> Icons.Default.Place
                                }
                                val iconColor = when (poi.category) {
                                    "مطعم" -> Color(0xFFE53935)
                                    "فندق" -> Color(0xFF3949AB)
                                    "حديقة" -> Color(0xFF43A047)
                                    else -> Color(0xFF8E24AA)
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = iconColor,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = poi.name,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = poi.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // C. FAVORITE PLACES (Visible with glittering golden stars)
        favorites.forEach { fav ->
            val screenPos = toScreen(fav.latitude, fav.longitude)
            if (screenPos.x in -100f..1500f && screenPos.y in -100f..2500f) {
                Box(
                    modifier = Modifier
                        .offset(x = (screenPos.x / 2.625f).dp - 30.dp, y = (screenPos.y / 2.625f).dp - 40.dp)
                        .width(60.dp)
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFD700),
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "مفضل",
                                tint = Color(0xFFE65100),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                        Text(
                            text = fav.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color(0xFFFFFDE7).copy(alpha = 0.95f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // D. TEMPORARY PIN (User dropped pin on long press)
        temporaryMarker?.let { temp ->
            val screenPos = toScreen(temp.first, temp.second)
            Box(
                modifier = Modifier
                    .offset(x = (screenPos.x / 2.625f).dp - 20.dp, y = (screenPos.y / 2.625f).dp - 45.dp)
                    .width(40.dp)
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "دبوس مؤقت",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(2.dp, CircleShape)
                )
            }
        }
    }
}
