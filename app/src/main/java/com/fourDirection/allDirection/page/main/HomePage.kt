package com.fourDirection.allDirection.page.main

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.fourDirection.allDirection.data.TravelRepository
import com.fourDirection.allDirection.data.TrendingCity
import com.fourDirection.allDirection.data.backgroundImages
import com.fourDirection.allDirection.ui.theme.AllDirectionTheme
import com.fourDirection.allDirection.ui.theme.GlowBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    userName: String = "User",
    totalDistance: Double = 0.0,
    period: String = "day",
    hazeState: HazeState,
    onCurrencyClick: () -> Unit = {},
    onTipClick: () -> Unit = {},
    onEmergencyClick: () -> Unit = {},
    onConnectionsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    selectedRoute: String = "home",
    onRouteSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val travelRepository = remember { TravelRepository(context) }
    var trendingCities by remember { mutableStateOf<List<TrendingCity>>(emptyList()) }
    val scrollState = rememberScrollState()

    // Quick Action Definitions & Persistence
    val actionDefinitions = mapOf(
        "currency" to (Icons.Default.CurrencyExchange to "Currency"),
        "tip" to (Icons.Default.Calculate to "Tip Calc"),
        "emergency" to (Icons.Default.HealthAndSafety to "Emergency"),
        "connections" to (Icons.Default.Group to "Connections"),
        "calendar" to (Icons.Default.CalendarToday to "Calendar"),
        "translate" to (Icons.Default.Translate to "Translate"),
        "map" to (Icons.Default.Map to "Offline Map")
    )

    val sharedPrefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    
    val allActions = remember {
        val savedOrder = sharedPrefs.getString("quick_actions_order", null)
        val initialOrder = if (savedOrder != null) {
            val savedIds = savedOrder.split(",")
            val validIds = savedIds.filter { actionDefinitions.containsKey(it) }
            val missingIds = actionDefinitions.keys.filterNot { validIds.contains(it) }
            
            // If connections is a missing ID (new feature), put it at the start
            if (missingIds.contains("connections")) {
                listOf("connections") + validIds + (missingIds - "connections")
            } else {
                validIds + missingIds
            }
        } else {
            listOf("connections", "calendar", "currency", "tip", "emergency", "translate", "map")
        }
        
        mutableStateListOf<Pair<String, Pair<ImageVector, String>>>().apply {
            initialOrder.distinct().forEach { id ->
                actionDefinitions[id]?.let { add(id to it) }
            }
        }
    }

    // Drag and Drop state for Quick Actions
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var touchOffsetInItem by remember { mutableStateOf(Offset.Zero) }
    val slotPositions = remember { mutableStateMapOf<Int, Offset>() }
    val slotSizes = remember { mutableStateMapOf<Int, IntSize>() }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val actionClickHandlers = mapOf(
        "currency" to onCurrencyClick,
        "tip" to onTipClick,
        "emergency" to onEmergencyClick,
        "connections" to onConnectionsClick,
        "calendar" to onCalendarClick,
        "translate" to {},
        "map" to {}
    )

    // Fetch live trending cities from the repository
    LaunchedEffect(Unit) {
        trendingCities = travelRepository.getTrendingCities()
    }

    // Select a random location image once per entry to the page
    val randomLocation = remember { backgroundImages.random() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it }
    ) {
        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background scrolls with content
                Image(
                    painter = painterResource(id = randomLocation.resId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.2f to Color.Black.copy(alpha = 0.3f),
                                0.3f to Color.Black.copy(alpha = 0.9f),
                                0.4f to Color.Black
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Welcome Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Welcome,\n$userName",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 44.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You have traveled over ${totalDistance.toInt()} km\nfor the past $period",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .hazeChild(state = hazeState)
                            .clickable { onSearchClick() },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp, 
                            Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Discover your next destination",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- QUICK ACTION WIDGET ---
                    var isExpanded by remember { mutableStateOf(false) }
                    var isRearranging by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Quick Actions",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { isExpanded = !isExpanded },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedVisibility(
                                        visible = isRearranging,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Text(
                                            text = "Hold to rearrange",
                                            color = GlowBlue.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { isRearranging = !isRearranging },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRearranging) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = "Rearrange",
                                            tint = if (isRearranging) GlowBlue else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Quick Actions Grid (3 columns)
                            val rowCount = if (isExpanded) {
                                (allActions.size + 2) / 3
                            } else {
                                1
                            }

                            for (rowIndex in 0 until rowCount) {
                                if (rowIndex > 0) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    for (colIndex in 0 until 3) {
                                        val i = rowIndex * 3 + colIndex
                                        if (i < allActions.size) {
                                            val (id, data) = allActions[i]
                                            key(id) {
                                                val targetSlot = when {
                                                    draggedIndex == null || targetIndex == null -> i
                                                    i == draggedIndex -> targetIndex!!
                                                    draggedIndex!! < targetIndex!! && i > draggedIndex!! && i <= targetIndex!! -> i - 1
                                                    draggedIndex!! > targetIndex!! && i < draggedIndex!! && i >= targetIndex!! -> i + 1
                                                    else -> i
                                                }

                                                val itemOffset = if (targetSlot != i) {
                                                    val currentPos = slotPositions[i] ?: Offset.Zero
                                                    val targetPos = slotPositions[targetSlot] ?: Offset.Zero
                                                    targetPos - currentPos
                                                } else {
                                                    Offset.Zero
                                                }

                                                val animatedOffset by animateOffsetAsState(
                                                    targetValue = itemOffset,
                                                    label = "shiftOffset"
                                                )

                                                QuickActionItem(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onGloballyPositioned { coords ->
                                                            rootCoordinates?.let { root ->
                                                                slotPositions[i] = root.localPositionOf(coords, Offset.Zero)
                                                                slotSizes[i] = coords.size
                                                            }
                                                        }
                                                        .graphicsLayer {
                                                            translationX = animatedOffset.x
                                                            translationY = animatedOffset.y
                                                            alpha = if (draggedIndex == i) 0.3f else 1f
                                                        }
                                                        .zIndex(if (draggedIndex == i) 0f else 1f)
                                                        .pointerInput(isRearranging, i) {
                                                            if (isRearranging) {
                                                                detectDragGesturesAfterLongPress(
                                                                    onDragStart = { offset ->
                                                                        draggedIndex = i
                                                                        targetIndex = i
                                                                        dragOffset = Offset.Zero
                                                                        touchOffsetInItem = offset
                                                                    },
                                                                    onDrag = { change, amount ->
                                                                        change.consume()
                                                                        dragOffset += amount
                                                                        
                                                                        val currentDragPosition = (slotPositions[i] ?: Offset.Zero) + touchOffsetInItem + dragOffset
                                                                        
                                                                        var bestTarget = targetIndex
                                                                        var minDistance = Float.MAX_VALUE
                                                                        
                                                                        slotPositions.forEach { (index, pos) ->
                                                                            val size = slotSizes[index] ?: return@forEach
                                                                            val center = pos + Offset(size.width / 2f, size.height / 2f)
                                                                            val distance = (center - currentDragPosition).getDistance()
                                                                            if (distance < minDistance) {
                                                                                minDistance = distance
                                                                                bestTarget = index
                                                                            }
                                                                        }
                                                                        
                                                                        if (bestTarget != targetIndex) {
                                                                            targetIndex = bestTarget
                                                                        }
                                                                    },
                                                                    onDragEnd = {
                                                                        if (draggedIndex != null && targetIndex != null && draggedIndex != targetIndex) {
                                                                            val item = allActions.removeAt(draggedIndex!!)
                                                                            allActions.add(targetIndex!!, item)
                                                                            
                                                                            // Save new order
                                                                            val newOrder = allActions.joinToString(",") { it.first }
                                                                            sharedPrefs.edit().putString("quick_actions_order", newOrder).apply()
                                                                        }
                                                                        draggedIndex = null
                                                                        targetIndex = null
                                                                        dragOffset = Offset.Zero
                                                                    },
                                                                    onDragCancel = {
                                                                        draggedIndex = null
                                                                        targetIndex = null
                                                                        dragOffset = Offset.Zero
                                                                    }
                                                                )
                                                            }
                                                        },
                                                    icon = data.first,
                                                    label = data.second,
                                                    onClick = actionClickHandlers[id] ?: {},
                                                    isRearranging = isRearranging
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- TRENDING NOW SECTION ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Trending Now",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { /* TODO: View All */ }) {
                                Text("View all", color = GlowBlue)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(trendingCities) { city ->
                                TrendingCityCard(
                                    cityName = city.name,
                                    countryName = city.country,
                                    imageUrl = city.imageUrl
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(120.dp)) // Padding to scroll past the dock
                }
            }
        }

        // --- DRAG OVERLAY ---
        if (draggedIndex != null) {
            val item = allActions[draggedIndex!!]
            val (_, data) = item
            val initialPos = slotPositions[draggedIndex!!] ?: Offset.Zero
            val size = slotSizes[draggedIndex!!] ?: IntSize.Zero

            val overlayDensity = LocalDensity.current

            Box(
                modifier = Modifier
                    .offset {
                        (initialPos + dragOffset).round()
                    }
                    .size(
                        width = with(overlayDensity) { size.width.toDp() },
                        height = with(overlayDensity) { size.height.toDp() }
                    )
                    .zIndex(1000f)
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                        shadowElevation = 8.dp.toPx()
                    }
            ) {
                QuickActionItem(
                    icon = data.first,
                    label = data.second,
                    isRearranging = true,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isRearranging: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = if (isRearranging) ({}) else onClick,
        modifier = modifier.height(80.dp),
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = if (isRearranging) 0.2f else 0.1f),
        border = if (isRearranging) BorderStroke(1.dp, GlowBlue.copy(alpha = 0.5f)) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isRearranging) GlowBlue else Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TrendingCityCard(
    cityName: String,
    countryName: String,
    imageUrl: String,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(220.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = cityName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Suble Bottom Fade for the card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            startY = 400f // Starts very low for a "little" fade
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = cityName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = countryName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    AllDirectionTheme {
        HomePage(hazeState = HazeState())
        ExplorePage(hazeState = HazeState())
    }
}
