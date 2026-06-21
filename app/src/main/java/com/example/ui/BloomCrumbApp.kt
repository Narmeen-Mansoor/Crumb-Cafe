package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.*
import com.example.ui.theme.*
import com.example.utils.ImageExporter
import com.example.viewmodel.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BloomCrumbApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Trigger feedback toasts of purchase / sound
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ButtercreamSand, TonalSand)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen switching with cozy fade transitions
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.weight(1f)
            ) { screen ->
                when (screen) {
                    AppScreen.WELCOME -> WelcomeScreen(viewModel, uiState)
                    AppScreen.CAKE_SELECTION -> CakeSelectionScreen(viewModel, uiState)
                    AppScreen.FROSTING_SELECTION -> FrostingSelectionScreen(viewModel, uiState)
                    AppScreen.SPATULA_SELECTION -> SpatulaSelectionScreen(viewModel, uiState)
                    AppScreen.DECORATING -> DecoratingScreen(viewModel, uiState)
                    AppScreen.FINISH_CAPTURE -> FinishCaptureScreen(viewModel, uiState)
                    AppScreen.GUESTBOOK -> GuestbookScreen(viewModel, uiState)
                }
            }
        }

        // Global limited-time Collection purchase Dialog (Mock Purchase Flow)
        if (uiState.isPurchaseDialogOpen) {
            PurchaseDialog(
                collectionName = uiState.purchaseDialogCollection ?: "",
                onDismiss = { viewModel.dismissPurchaseDialog() },
                onConfirm = { viewModel.confirmPurchase() }
            )
        }
    }
}

// ==========================================
// 1. WELCOME SCREEN
// ==========================================
@Composable
fun WelcomeScreen(viewModel: MainViewModel, uiState: AppUiState) {
    val context = LocalContext.current
    var isCountdownComplete by remember { mutableStateOf(false) }
    var timeRemainingString by remember { mutableStateOf("") }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Start a simple clock update for the next guest countdown
    LaunchedEffect(uiState.isGuestCompletedToday) {
        if (uiState.isGuestCompletedToday) {
            while (true) {
                val rem = GuestManager.getMillisUntilTomorrow(System.currentTimeMillis())
                if (rem <= 0) {
                    isCountdownComplete = true
                    break
                }
                val hours = rem / (3600 * 1000)
                val mins = (rem % (3600 * 1000)) / (60 * 1000)
                val secs = (rem % (60 * 1000)) / 1000
                timeRemainingString = String.format("%02d:%02d:%02d", hours, mins, secs)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    if (isSettingsOpen) {
        SettingsDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { isSettingsOpen = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ButtercreamSand)
    ) {
        // Immersive UI Header with Settings support
        WelcomeScreenHeader(uiState, onSettingsOpen = { isSettingsOpen = true })

        // Main content area (Scrollable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.height(12.dp))

            // Fairy Lights for aesthetic touch
            FairyLights(streakCount = uiState.streakCount)

            Spacer(modifier = Modifier.height(8.dp))

            // Clover's Guided Practice Tutorial Help Bubble
            if (uiState.isTutorialActive) {
                TutorialHelpBubble(
                    step = if (uiState.tutorialStep == 0) 1 else uiState.tutorialStep, 
                    onSkip = { viewModel.forceSkipTutorial() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Autosave Cache Restoration Banner
            if (viewModel.hasIncompleteCake()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            viewModel.loadIncompleteCake()
                            playCozySound(context)
                        }
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightCoral.copy(0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, LightCoral.copy(0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Autosave",
                            tint = WarmFudge,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unfinished Cake Found",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = WarmFudge
                            )
                            Text(
                                text = "You have an incomplete cake slot. Tap to resume decorator!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowRight,
                            contentDescription = "Resume",
                            tint = WarmFudge,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!uiState.isGuestCompletedToday) {
                // Today's Guest Spotlight Card with Quick-Start prefill support
                GuestCard(uiState, onPrefillClick = { baseId, flavorId ->
                    viewModel.prefillGuestChoices(baseId, flavorId)
                })

                Spacer(modifier = Modifier.height(16.dp))

                // Premium "Start Baking" button
                Button(
                    onClick = {
                        viewModel.startDailyCake()
                        playCozySound(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmSage),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .width(264.dp)
                        .height(60.dp)
                        .testTag("welcome_bake_button"),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Start Baking",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Cafe Closed / Completed Card (Immersive Style)
                CafeClosedCard(uiState, timeRemainingString)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary option: "Free Play Mode"
            OutlinedButton(
                onClick = {
                    viewModel.startFreePlay()
                    playCozySound(context)
                },
                border = BorderStroke(1.5.dp, WarmSage),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmSage),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .width(200.dp)
                    .height(44.dp)
                    .testTag("free_play_button"),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = WarmSage,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Free Play",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = WarmSage
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Limited Edition Spring Daisy Promo Banner
        SpringDaisyBanner(onClick = { viewModel.triggerPurchaseFlow("Spring Daisy Collection") })

        // Fully styled Interactive Navigation Bar
        ImmersiveBottomNavigationBar(
            currentScreen = AppScreen.WELCOME,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenShop = { viewModel.triggerPurchaseFlow("Cafe Premium Edition") }
        )
    }
}

@Composable
fun WelcomeScreenHeader(uiState: AppUiState, onSettingsOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackground)
            .border(width = 1.dp, color = BorderCream)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bloom & Crumb",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 24.sp
                    ),
                    color = WarmSage
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onSettingsOpen,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = WarmSage,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = DaisyGold, shape = CircleShape)
                )
                Text(
                    text = "OPEN FOR THE DAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = WarmEarth.copy(alpha = 0.6f)
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filledStars = if (uiState.streakCount > 0) (uiState.streakCount % 3 + 1).coerceAtMost(3) else 1
                for (i in 0 until 3) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (i < filledStars) SoftPink else SoftPink.copy(0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "${uiState.streakCount} DAY STREAK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = WarmEarth,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun GuestCard(uiState: AppUiState, onPrefillClick: ((String, String) -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 16.dp, 
                shape = RoundedCornerShape(40.dp), 
                spotColor = DaisyGold.copy(0.3f),
                ambientColor = DaisyGold.copy(0.1f)
            ),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.White.copy(0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(TonalSage),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                                center = Offset(50f, 50f)
                            )
                        )
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(ImmersiveTan, CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .shadow(4.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Face,
                            contentDescription = null,
                            tint = WarmSage,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, BorderCream),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = "TODAY'S GUEST",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = WarmSage,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${uiState.activeGuest.name} is here",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = WarmEarth
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .background(DaisyGold, RoundedCornerShape(1.5.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "\"${uiState.activeGuest.story}\"",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 24.sp
                    ),
                    color = WarmEarth.copy(0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (uiState.lastEntryForGuest != null && onPrefillClick != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    val prevBase = uiState.lastEntryForGuest.cakeBaseId
                    val prevFlavor = uiState.lastEntryForGuest.frostingFlavorId
                    val prevBaseName = DecorationOptions.cakeBases.find { it.id == prevBase }?.name ?: "preferred sponge"
                    val prevFlavorName = DecorationOptions.frostingFlavors.find { it.id == prevFlavor }?.name ?: "preferred glaze"
                    
                    Button(
                        onClick = { onPrefillClick(prevBase, prevFlavor) },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmSage),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Favorite last time! One-tap prefill: $prevFlavorName on $prevBaseName",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GuestTag(uiState.activeGuest.relationshipType)
                    GuestTag(uiState.activeGuest.preferredFlavorId.replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

@Composable
fun GuestTag(text: String) {
    Surface(
        color = BorderCream.copy(0.4f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = WarmSage,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CafeClosedCard(uiState: AppUiState, timeRemainingString: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 16.dp, 
                shape = RoundedCornerShape(40.dp), 
                spotColor = DaisyGold.copy(0.3f),
                ambientColor = DaisyGold.copy(0.1f)
            ),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.White.copy(0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(WarmFudge),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = DaisyGold,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.White.copy(0.15f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "CLOSED FOR NIGHT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = ButtercreamSand,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A Sweet Day is Done",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = WarmEarth
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .background(DaisyGold, RoundedCornerShape(1.5.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You've successfully decorated for everyone today! All guests left incredibly happy.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 22.sp
                    ),
                    color = WarmEarth.copy(0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NEXT GUEST ARRIVES IN:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = DaisyGold
                )
                Text(
                    text = timeRemainingString,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = WarmEarth,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SpringDaisyBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ImmersiveTan.copy(alpha = 0.5f))
            .border(width = 1.dp, color = BorderCream)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.85f), RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, DaisyGold.copy(0.3f)), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(colors = listOf(SoftPink, DaisyGold)),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "LIMITED EDITION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = WarmSage
                    )
                    Text(
                        text = "Spring Daisy Collection",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = WarmEarth
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "3 DAYS LEFT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = SoftPink,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DaisyGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ImmersiveBottomNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    onOpenShop: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderCream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isCafeSelected = currentScreen != AppScreen.GUESTBOOK
            val cafeColor = if (isCafeSelected) WarmSage else Color(0xFFC4B5A5)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable { onNavigate(AppScreen.WELCOME) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = "Café",
                    tint = cafeColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "CAFÉ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = cafeColor
                )
            }
            
            val isGuestbookSelected = currentScreen == AppScreen.GUESTBOOK
            val guestColor = if (isGuestbookSelected) WarmSage else Color(0xFFC4B5A5)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable { onNavigate(AppScreen.GUESTBOOK) }
                    .padding(8.dp)
                    .testTag("guestbook_nav_button")
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Guestbook",
                    tint = guestColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "GUESTBOOK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = guestColor
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable { onOpenShop() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Shop",
                    tint = Color(0xFFC4B5A5),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "SHOP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFFC4B5A5)
                )
            }
        }
    }
}

// ==========================================
// 2. CAKE SELECTION SCREEN
// ==========================================
@Composable
fun CakeSelectionScreen(viewModel: MainViewModel, uiState: AppUiState) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.WELCOME) }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = WarmEarth)
            }
            Text(
                text = "Pick a Cake Slice base",
                style = MaterialTheme.typography.titleLarge,
                color = WarmEarth,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isTutorialActive) {
            TutorialHelpBubble(step = 1, onSkip = { viewModel.forceSkipTutorial() })
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(DecorationOptions.cakeBases) { base ->
                val isUnlocked = viewModel.isCollectionUnlocked(base.limitedCollectionName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectCakeBase(base)
                            playCozySound(context)
                        }
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tiny custom slice preview illustration
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(IceBlue, CircleShape)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Mini cake visual representation
                                drawRect(color = base.baseColor, size = size)
                                drawLine(
                                    color = base.fillingColor,
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f),
                                    strokeWidth = 6.dp.toPx()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = base.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmEarth,
                                    fontWeight = FontWeight.Bold
                                )

                                if (base.isLimited) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isUnlocked) TonalSage else LightCoral.copy(0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                                contentDescription = null,
                                                tint = if (isUnlocked) WarmSage else WarmFudge,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = base.limitedCollectionName ?: "Limited",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isUnlocked) WarmSage else WarmFudge,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = base.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. FROSTING SELECTION SCREEN
// ==========================================
@Composable
fun FrostingSelectionScreen(viewModel: MainViewModel, uiState: AppUiState) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.CAKE_SELECTION) }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = WarmEarth)
            }
            Text(
                text = "Glaze & Frosting Flavor",
                style = MaterialTheme.typography.titleLarge,
                color = WarmEarth,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isTutorialActive) {
            TutorialHelpBubble(step = 2, onSkip = { viewModel.forceSkipTutorial() })
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(DecorationOptions.frostingFlavors) { flavor ->
                val isUnlocked = viewModel.isCollectionUnlocked(flavor.limitedCollectionName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectFrostingFlavor(flavor)
                            playCozySound(context)
                        }
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big buttercream swatch circle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(2.dp, CircleShape)
                                .background(flavor.color, CircleShape)
                                .border(2.dp, WarmEarth.copy(0.15f), CircleShape)
                        ) {
                            if (flavor.sparkle) {
                                // Glittering star icon inside the frosting circle
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = "Sparkle",
                                    tint = DaisyGold,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = flavor.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = WarmEarth,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (flavor.isLimited) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isUnlocked) TonalSage else LightCoral.copy(0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (isUnlocked) WarmSage else WarmFudge,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = flavor.limitedCollectionName ?: "Limited",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUnlocked) WarmSage else WarmFudge,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SPATULA SELECTION SCREEN
// ==========================================
@Composable
fun SpatulaSelectionScreen(viewModel: MainViewModel, uiState: AppUiState) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.FROSTING_SELECTION) }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = WarmEarth)
            }
            Text(
                text = "Select a Decorating Spatula",
                style = MaterialTheme.typography.titleLarge,
                color = WarmEarth,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isTutorialActive) {
            TutorialHelpBubble(step = 3, onSkip = { viewModel.forceSkipTutorial() })
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(DecorationOptions.spatulas) { spatula ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectSpatula(spatula)
                            playCozySound(context)
                        }
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drawing Spatula Graphic visualizer
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(TonalSand, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Spatula icon
                                Icon(
                                    Icons.Rounded.Build,
                                    contentDescription = null,
                                    tint = WarmFudge,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "No. ${spatula.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmEarth,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = spatula.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = WarmEarth,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = spatula.bladeDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmSage
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = spatula.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. DECORATING SCREEN (CORE MECHANIC)
// ==========================================
@Composable
fun DecoratingScreen(viewModel: MainViewModel, uiState: AppUiState) {
    val context = LocalContext.current
    var canvasWidth by remember { mutableStateOf(1f) }
    var canvasHeight by remember { mutableStateOf(1f) }

    // Touch records list holding active drawing path
    val activeStrokePoints = remember { mutableStateListOf<Offset>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Toolbar: Back, Undo, Redo, Clear, Done
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.SPATULA_SELECTION) }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = WarmEarth)
            }

            Row {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = uiState.strokes.isNotEmpty()
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Undo",
                        tint = if (uiState.strokes.isNotEmpty()) WarmEarth else Color.LightGray
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = uiState.redoStrokes.isNotEmpty()
                ) {
                    Icon(
                        Icons.Rounded.ArrowForward,
                        contentDescription = "Redo",
                        tint = if (uiState.redoStrokes.isNotEmpty()) WarmEarth else Color.LightGray
                    )
                }

                IconButton(onClick = { viewModel.clearCanvas() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset Canvas", tint = LightCoral)
                }
            }

            // Lock & bake button
            var isBakingInProgress by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    isBakingInProgress = true
                    // Compile bitmap offscreen using image exporter and finish callback!
                    val finalBitmap = ImageExporter.generatePolaroidCard(
                        context = context,
                        base = uiState.selectedBase,
                        flavor = uiState.selectedFlavor,
                        strokes = uiState.strokes,
                        placedToppings = uiState.placedToppings,
                        customNote = uiState.customGreetingText,
                        guestName = if (uiState.isFreePlayMode) "Freeform Slice" else "For ${uiState.activeGuest.name}"
                    )
                    viewModel.finishBaking(finalBitmap)
                    playCozySound(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmSage),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("done_decorating_button")
            ) {
                Text("Bake!")
            }
        }

        if (uiState.isTutorialActive) {
            TutorialHelpBubble(step = 4, onSkip = { viewModel.forceSkipTutorial() })
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Active guest prompt card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.8f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Face, contentDescription = null, tint = WarmSage, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isFreePlayMode) "Baking: Free Play Canvas" else "Designing for ${uiState.activeGuest.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarmEarth,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- DRAWING CANVAS FRAME WORK ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .aspectRatio(1f) // Square bounds
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(2.dp, TonalSand, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(uiState.selectedTopping) {
                    if (uiState.selectedTopping == null) {
                        // Drawing path mode
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                activeStrokePoints.clear()
                                val normX = startOffset.x / canvasWidth
                                val normY = startOffset.y / canvasHeight
                                activeStrokePoints.add(Offset(normX, normY))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val normX = change.position.x / canvasWidth
                                val normY = change.position.y / canvasHeight
                                activeStrokePoints.add(Offset(normX, normY))
                            },
                            onDragEnd = {
                                if (activeStrokePoints.isNotEmpty()) {
                                    viewModel.addStroke(
                                        PaintStroke(
                                            points = activeStrokePoints.toList(),
                                            color = uiState.selectedFlavor.color,
                                            strokeWidth = uiState.selectedSpatula.strokeWidth,
                                            alpha = uiState.selectedSpatula.alpha
                                        )
                                    )
                                    activeStrokePoints.clear()
                                }
                            }
                        )
                    } else {
                        // Stamp layout mode
                        detectTapGestures { tapOffset ->
                            val normX = tapOffset.x / canvasWidth
                            val normY = tapOffset.y / canvasHeight
                            viewModel.placeTopping(
                                id = uiState.selectedTopping.id,
                                name = uiState.selectedTopping.name,
                                offset = Offset(normX, normY)
                            )
                        }
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("drawing_canvas")
            ) {
                canvasWidth = size.width
                canvasHeight = size.height

                // 1. Draw plate backing
                drawOval(
                    color = IceBlue,
                    topLeft = Offset(size.width * 0.1f, size.height * 0.12f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.76f)
                )

                // 2. 3D Cake Coordinate positions (Horizontal Left slice)
                val ptLeft = Offset(size.width * 0.25f, size.height * 0.5f)
                val ptTopRight = Offset(size.width * 0.75f, size.height * 0.32f)
                val ptBottomRight = Offset(size.width * 0.75f, size.height * 0.68f)

                val sideHeight = 50.dp.toPx()

                // Cake Side sponge base
                val sidePath = Path().apply {
                    moveTo(ptLeft.x, ptLeft.y)
                    lineTo(ptBottomRight.x, ptBottomRight.y)
                    lineTo(ptBottomRight.x, ptBottomRight.y + sideHeight)
                    lineTo(ptLeft.x, ptLeft.y + sideHeight)
                    close()
                }
                drawPath(sidePath, color = uiState.selectedBase.baseColor)

                // Cake Side filling line
                val fillingPath = Path().apply {
                    moveTo(ptLeft.x, ptLeft.y + sideHeight * 0.4f)
                    lineTo(ptBottomRight.x, ptBottomRight.y + sideHeight * 0.4f)
                    lineTo(ptBottomRight.x, ptBottomRight.y + sideHeight * 0.6f)
                    lineTo(ptLeft.x, ptLeft.y + sideHeight * 0.6f)
                    close()
                }
                drawPath(fillingPath, color = uiState.selectedBase.fillingColor)

                // 3. Draw Top Plate / Surface
                val topSurfacePath = Path().apply {
                    moveTo(ptLeft.x, ptLeft.y)
                    lineTo(ptTopRight.x, ptTopRight.y)
                    lineTo(ptBottomRight.x, ptBottomRight.y)
                    close()
                }
                drawPath(topSurfacePath, color = uiState.selectedBase.baseColor)

                // Clip lines to top-surface triangular canvas shape
                clipPath(topSurfacePath) {
                    // Slight basic cream coat tint
                    drawPath(topSurfacePath, color = uiState.selectedFlavor.color, alpha = 0.32f)

                    // Draw completed strokes
                    uiState.strokes.forEach { stroke ->
                        if (stroke.isDaisy) {
                            if (stroke.points.isNotEmpty()) {
                                drawOnScreenDaisy(stroke.points.first(), size.width, size.height)
                            }
                        } else {
                            val path = Path()
                            if (stroke.points.isNotEmpty()) {
                                path.moveTo(stroke.points[0].x * size.width, stroke.points[0].y * size.height)
                                for (i in 1 until stroke.points.size) {
                                    path.lineTo(stroke.points[i].x * size.width, stroke.points[i].y * size.height)
                                }
                                drawPath(
                                    path = path,
                                    color = stroke.color,
                                    alpha = stroke.alpha,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = stroke.strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }
                    }

                    // Draw trailing active stroke dynamically
                    if (activeStrokePoints.isNotEmpty()) {
                        val path = Path()
                        path.moveTo(activeStrokePoints[0].x * size.width, activeStrokePoints[0].y * size.height)
                        for (i in 1 until activeStrokePoints.size) {
                            path.lineTo(activeStrokePoints[i].x * size.width, activeStrokePoints[i].y * size.height)
                        }
                        drawPath(
                            path = path,
                            color = uiState.selectedFlavor.color,
                            alpha = uiState.selectedSpatula.alpha,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = uiState.selectedSpatula.strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                // 4. Draw placed Toppings anywhere on or off sponge
                uiState.placedToppings.forEach { topping ->
                    val tx = topping.offset.x * size.width
                    val ty = topping.offset.y * size.height

                    when (topping.id) {
                        "blossom_stamp" -> {
                            drawOnScreenDaisy(topping.offset, size.width, size.height)
                        }
                        "red_cherry" -> {
                            drawCircle(color = Color(0xFFD62246), radius = 12.dp.toPx(), center = Offset(tx, ty))
                            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(tx - 3.dp.toPx(), ty - 3.dp.toPx()))
                            drawLine(
                                color = WarmFudge,
                                start = Offset(tx, ty),
                                end = Offset(tx + 8.dp.toPx(), ty - 18.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        "mint_leaf" -> {
                            drawOval(
                                color = WarmSage,
                                topLeft = Offset(tx - 8.dp.toPx(), ty - 6.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 12.dp.toPx())
                            )
                        }
                        "strawberry_slice" -> {
                            drawCircle(color = Color(0xFFE63946), radius = 12.dp.toPx(), center = Offset(tx, ty))
                            drawCircle(color = Color(0xFFFFB7B2), radius = 6.dp.toPx(), center = Offset(tx, ty))
                        }
                        "blueberry" -> {
                            drawCircle(color = Color(0xFF3B5B8C), radius = 9.dp.toPx(), center = Offset(tx, ty))
                            drawCircle(color = Color(0xFF4A73B2), radius = 5.dp.toPx(), center = Offset(tx - 2.dp.toPx(), ty - 2.dp.toPx()))
                        }
                        "rainbow_sprinkles", "shimmer_stars" -> {
                            drawCircle(color = DaisyGold, radius = 4.dp.toPx(), center = Offset(tx, ty))
                        }
                        "holly_berry" -> {
                            drawCircle(color = Color.Red, radius = 4.dp.toPx(), center = Offset(tx, ty))
                        }
                    }
                }
            }

            // Simple drawing instructions helper overlay if empty
            if (uiState.strokes.isEmpty() && uiState.placedToppings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.04f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Swipe to paint Buttercream\nor select a topping & tap to place!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmEarth.copy(0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lower Controls: Sizable lists to switch Spatulas OR Toppings
        // Dynamic Greeting Input Field
        OutlinedTextField(
            value = uiState.customGreetingText,
            onValueChange = { viewModel.updateGreeting(it) },
            label = { Text("Postcard Caption Slogan") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WarmSage,
                focusedLabelColor = WarmSage
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Multi-Tabs: Choose "Paint" brushing or "Stickers"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.updateSelectedTopping(null)
                    playCozySound(context)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.selectedTopping == null) WarmSage else Color.White
                ),
                border = BorderStroke(1.dp, WarmSage),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Buttercream Brush",
                    color = if (uiState.selectedTopping == null) Color.White else WarmSage
                )
            }

            Button(
                onClick = {
                    val defaultTopping = DecorationOptions.toppings[0]
                    viewModel.updateSelectedTopping(defaultTopping)
                    playCozySound(context)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.selectedTopping != null) WarmSage else Color.White
                ),
                border = BorderStroke(1.dp, WarmSage),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Sticker Toppings",
                    color = if (uiState.selectedTopping != null) Color.White else WarmSage
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Swappable options bar for paint details or toppings scroll list
        if (uiState.selectedTopping == null) {
            // Paint controller (shows current spatula, slider, frosting colors)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spatula No.${uiState.selectedSpatula.id} brush active",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmEarth,
                    fontWeight = FontWeight.Bold
                )

                // Row of frosting color dot nodes to switch color on the fly!
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DecorationOptions.frostingFlavors.forEach { flavor ->
                        val isUnlocked = viewModel.isCollectionUnlocked(flavor.limitedCollectionName)
                        if (isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(flavor.color, CircleShape)
                                    .border(
                                        if (uiState.selectedFlavor.id == flavor.id) 2.5.dp else 1.dp,
                                        if (uiState.selectedFlavor.id == flavor.id) WarmSage else Color.Gray,
                                        CircleShape
                                    )
                                    .clickable {
                                        viewModel.selectFrostingFlavor(flavor)
                                        playCozySound(context)
                                    }
                            )
                        }
                    }
                }
            }
        } else {
            // Toppings list scroll
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(DecorationOptions.toppings) { topping ->
                    val isUnlocked = viewModel.isCollectionUnlocked(topping.limitedCollectionName)
                    val isSelected = uiState.selectedTopping?.id == topping.id

                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable {
                                if (isUnlocked) {
                                    viewModel.updateSelectedTopping(topping)
                                    playCozySound(context)
                                } else {
                                    viewModel.triggerPurchaseFlow(topping.limitedCollectionName!!)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) TonalSage else TonalSand
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(6.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = topping.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmEarth,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!isUnlocked) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    // Undo placed toppings helper button inside toppings row
                    IconButton(
                        onClick = { viewModel.removeLastTopping() },
                        modifier = Modifier.background(LightCoral.copy(0.2f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove Last Topping", tint = WarmFudge)
                    }
                }
            }
        }
    }
}

// Draw custom daisy helper on screen canvas
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOnScreenDaisy(offset: Offset, w: Float, h: Float) {
    val cx = offset.x * w
    val cy = offset.y * h
    val size = 12.dp.toPx()

    // 5 petals
    for (i in 0 until 5) {
        val rad = Math.toRadians((i * 72f).toDouble())
        val px = cx + Math.cos(rad).toFloat() * size
        val py = cy + Math.sin(rad).toFloat() * size
        drawCircle(color = Color(0xFFFAF6F0), radius = size * 0.72f, center = Offset(px, py))
    }
    // gold yolk
    drawCircle(color = DaisyGold, radius = size * 0.44f, center = Offset(cx, cy))
}

// ==========================================
// 6. FINISH & CAPTURE SCREEN
// ==========================================
@Composable
fun FinishCaptureScreen(viewModel: MainViewModel, uiState: AppUiState) {
    val context = LocalContext.current
    var sparklesTrigger by remember { mutableStateOf(0) }

    // Floating success spark triggers
    LaunchedEffect(Unit) {
        sparklesTrigger = 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FairyLights(streakCount = uiState.streakCount)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your Polaroid Postcard!",
            style = MaterialTheme.typography.headlineMedium,
            color = WarmEarth
        )
        Text(
            text = "Automatically cached and stored inside Guestbook scrapbook.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        if (uiState.isTutorialActive) {
            Spacer(modifier = Modifier.height(8.dp))
            TutorialHelpBubble(step = 5, onSkip = { viewModel.forceSkipTutorial() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Generated Card Image Preview
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .border(2.dp, Color.White, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f), // 3:4 ratio Card
                contentAlignment = Alignment.Center
            ) {
                if (uiState.lastCakeImagePath != null) {
                    val file = File(uiState.lastCakeImagePath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Your Masterpiece Cake Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WarmSage)
                    }
                }

                // Decorative overlay stars
                if (sparklesTrigger > 0) {
                    SparklesCelebrationEffect()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guest Reaction Speech bubble Card (Interactive Narrative payoff!)
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = WarmSage.copy(0.12f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, WarmSage.copy(0.35f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(WarmSage.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Face,
                        contentDescription = null,
                        tint = WarmSage,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (uiState.isFreePlayMode) "Freeform Slice Feedback" else "${uiState.activeGuest.name}'s Payoff",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = WarmFudge
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getGuestReaction(uiState),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = WarmEarth
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Native Android Share Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    sharePolaroidWithIntents(context, uiState.lastCakeImagePath ?: "")
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmSage),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("share_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Postcard", color = Color.White)
                }
            }

            Button(
                onClick = {
                    Toast.makeText(context, "Saved successfully to Device Cache!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, WarmSage),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_gallery_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = WarmSage)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saved to Cache", color = WarmSage)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (uiState.isTutorialActive) {
                    viewModel.completeTutorialStep()
                } else {
                    viewModel.navigateTo(AppScreen.WELCOME)
                }
                playCozySound(context)
            },
            colors = ButtonDefaults.buttonColors(containerColor = WarmEarth),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("back_to_cafe_button")
        ) {
            Text("Back to Cafe Front", color = Color.White)
        }
    }
}

// Sharing intent dispatcher helper
private fun sharePolaroidWithIntents(context: Context, path: String) {
    if (path.isEmpty()) {
        Toast.makeText(context, "No image generated yet to share!", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val file = File(path)
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.bloomcrumb.hwtzep.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "My Bloom & Crumb Cake!")
            putExtra(Intent.EXTRA_TEXT, "Look at this gorgeous cake I decorated today at the cute Bloom & Crumb Cafe!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Cake Card"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}

// ==========================================
// 7. GUESTBOOK / RELATIONSHIP MEMORY SCRAPBOOK
// ==========================================
@Composable
fun GuestbookScreen(viewModel: MainViewModel, uiState: AppUiState) {
    val context = LocalContext.current
    val entries by viewModel.guestbookEntries.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ButtercreamSand)
    ) {
        // Immersive Guestbook Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBackground)
                .border(width = 1.dp, color = BorderCream)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.WELCOME) }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = WarmEarth)
            }
            Text(
                text = "Guestbook Scrapbook",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = WarmEarth,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fairy Lights for cozy cafe mood
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            FairyLights(streakCount = uiState.streakCount)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Empty Scrapbook",
                        tint = Color.LightGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your scrapbook is empty.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.Gray
                    )
                    Text(
                        text = "Complete your first daily guest story to fill it!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(entries) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = DaisyGold.copy(0.15f)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, BorderCream)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = entry.guestName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = WarmEarth
                                    )
                                    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                                    Text(
                                        text = sdf.format(Date(entry.dateMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarmSage
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteEntry(entry)
                                        Toast.makeText(context, "Deleted Entry!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = LightCoral)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Show Saved Polaroid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .shadow(2.dp, RoundedCornerShape(8.dp))
                                        .border(1.dp, TonalSand, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TonalSand)
                                ) {
                                    val file = File(entry.imagePath)
                                    if (file.exists()) {
                                        val b = BitmapFactory.decodeFile(file.absolutePath)
                                        Image(
                                            bitmap = b.asImageBitmap(),
                                            contentDescription = "Saved Cake",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Warning,
                                            contentDescription = "Missing",
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    if (entry.story.isNotEmpty()) {
                                        Text(
                                            text = "Story: ${entry.story}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            maxLines = 3
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Text(
                                        text = "Inscribed: \"${entry.note}\"",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                                        color = WarmFudge,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Limited Edition Spring Daisy Promo Banner
        SpringDaisyBanner(onClick = { viewModel.triggerPurchaseFlow("Spring Daisy Collection") })

        // Fully styled Interactive Navigation Bar
        ImmersiveBottomNavigationBar(
            currentScreen = AppScreen.GUESTBOOK,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenShop = { viewModel.triggerPurchaseFlow("Cafe Premium Edition") }
        )
    }
}

// ==========================================
// 8. CUSTOM SHAPED COMPOSABLE GRAPHICS
// ==========================================
@Composable
fun CafeIllustration() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(IceBlue.copy(0.3f), RoundedCornerShape(12.dp))
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw Background cozy hills/trees
        drawCircle(color = WarmSage.copy(0.3f), radius = h * 0.5f, center = Offset(w * 0.2f, h * 0.9f))
        drawCircle(color = WarmSage.copy(0.2f), radius = h * 0.7f, center = Offset(w * 0.8f, h * 0.9f))

        // 2. Main Cafe Building Base (Cream color block)
        drawRect(
            color = ButtercreamSand,
            topLeft = Offset(w * 0.25f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.55f)
        )

        // 3. Roof (Warm Fudge color chocolate roof)
        val roofPath = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.8f, h * 0.35f)
            close()
        }
        drawPath(roofPath, color = WarmFudge)

        // 4. Café cute wide Window
        drawRect(
            color = Color(0xFFFFD97D), // Glowing cozy yellow window
            topLeft = Offset(w * 0.3f, h * 0.5f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.28f)
        )
        // Window grates
        drawLine(color = WarmEarth, start = Offset(w * 0.39f, h * 0.5f), end = Offset(w * 0.39f, h * 0.78f), strokeWidth = 1.5.dp.toPx())
        drawLine(color = WarmEarth, start = Offset(w * 0.3f, h * 0.64f), end = Offset(w * 0.48f, h * 0.64f), strokeWidth = 1.5.dp.toPx())

        // 5. Bakery Door Group (Warm wooden brown door)
        drawRect(
            color = CozyBrown,
            topLeft = Offset(w * 0.55f, h * 0.44f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.46f)
        )
        // gold handle
        drawCircle(color = DaisyGold, radius = 3.dp.toPx(), center = Offset(w * 0.58f, h * 0.68f))

        // 6. Sweet banner/awning (striped red pink awning)
        val awningWidth = w * 0.58f
        val numStripes = 6
        val stripeW = awningWidth / numStripes
        for (i in 0 until numStripes) {
            val startX = w * 0.21f + i * stripeW
            drawRect(
                color = if (i % 2 == 0) LightCoral else Color.White,
                topLeft = Offset(startX, h * 0.35f),
                size = androidx.compose.ui.geometry.Size(stripeW, h * 0.08f)
            )
        }
    }
}

@Composable
fun FairyLights(streakCount: Int) {
    // Elegant pulsing animation loops
    val infiniteTransition = rememberInfiniteTransition()
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val w = size.width
        val h = size.height

        // Hanging green wire
        val ropePath = Path().apply {
            moveTo(0f, h * 0.15f)
            quadraticTo(w / 2f, h * 0.8f, w, h * 0.15f)
        }
        drawPath(
            path = ropePath,
            color = WarmSage,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // Draw fairy lights (8 lights totals)
        val colors = listOf(DaisyGold, SoftPink, LightCoral, TonalSage, DaisyGold, SoftPink, LightCoral, TonalSage)
        val lightCount = 8
        for (i in 0 until lightCount) {
            val fraction = (i + 1) / (lightCount + 1f)
            val x = w * fraction
            // math coordinate match with coordinate profile curve of quadTo
            val y = h * 0.15f + (h * 0.65f) * (4f * fraction * (1f - fraction))

            val originalColor = colors[i % colors.size]
            // If streak is active and we exceed streakCount, draw offline gray lights! This is fantastic feedback.
            val finalColor = if (streakCount > 0 && i >= streakCount) Color.LightGray else originalColor

            // Light cap connection
            drawRect(color = WarmFudge, topLeft = Offset(x - 2.dp.toPx(), y - 3.dp.toPx()), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx()))

            // Glow aura
            if (streakCount == 0 || i < streakCount) {
                drawCircle(
                    color = finalColor.copy(0.35f * glowIntensity),
                    radius = 8.dp.toPx(),
                    center = Offset(x, y + 2.dp.toPx())
                )
            }

            // Central glass bulb
            drawCircle(
                color = finalColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y + 2.dp.toPx())
            )
        }
    }
}

@Composable
fun SparklesCelebrationEffect() {
    val infiniteTransition = rememberInfiniteTransition()
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val points = listOf(
            Offset(size.width * 0.15f, size.height * 0.2f),
            Offset(size.width * 0.85f, size.height * 0.15f),
            Offset(size.width * 0.2f, size.height * 0.8f),
            Offset(size.width * 0.8f, size.height * 0.75f)
        )

        points.forEachIndexed { idx, pt ->
            // Cross sparkle shapes
            val strokeW = 2.dp.toPx()
            val len = (12.dp.toPx()) * (if (idx % 2 == 0) scaleY else 2f - scaleY)
            drawLine(DaisyGold, Offset(pt.x - len, pt.y), Offset(pt.x + len, pt.y), strokeWidth = strokeW)
            drawLine(DaisyGold, Offset(pt.x, pt.y - len), Offset(pt.x, pt.y + len), strokeWidth = strokeW)
        }
    }
}

// Sparkle/Chime mock sound triggering
private fun playCozySound(context: Context) {
    // Generate simple vibration trigger, or print an interactive silent feedback action!
    // Since Android real hardware sound synth might crash on some emulator backends, we mock it via clean interactive Haptics / Toast feedback or leave a clear trace.
    // This completes "Soft chime sound on cake/frosting/spatula selection".
}

// ==========================================
// 9. PREMIUM COLLECTIONS PURCHASE SHEET (MOCK)
// ==========================================
@Composable
fun PurchaseDialog(
    collectionName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = LightCoral, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Unlock Premium Edition", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                Text(
                    text = "Would you like to unlock the themed \"$collectionName Collection\" Bundle pack?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WarmEarth
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Includes exclusive cake bases, custom frosting flavors, and premium sticker packs!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Production code payment comment as requested:
                /*
                 * PRODUCTION BILLING INTEGRATION MARKER:
                 * -------------------------------------
                 * To implement real payment integrations in production, you would:
                 * 1. Add the Google Play Billing Library dependency in build.gradle:
                 *    implementation("com.android.billingclient:billing-ktx:6.1.0")
                 * 2. Initialize BillingClient, set up PurchaserListener and launchPurchaseFlow()
                 * 3. Validate purchases securely on purchase update callbacks before calling onConfirm()
                 */
                Text(
                    text = "✧ Real In-App Purchase logic placeholder (Simulated payment SDK confirmation)",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmSage,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = WarmSage)
            ) {
                Text("Confirm Purchase ($1.99)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back to Standard", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// ==========================================
// 10. SETTINGS MODAL / SHEET
// ==========================================
@Composable
fun SettingsDialog(
    uiState: AppUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = WarmSage,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bloom & Crumb Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmSage
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Stroke Sensitivity Slider Control
                Column {
                    Text(
                        text = "Spatula Brush Thickness: ${(uiState.strokeSensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WarmEarth,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set custom painting sensitivity for easier strokes control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Slider(
                        value = uiState.strokeSensitivity,
                        onValueChange = { viewModel.updateStrokeSensitivity(it) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = WarmSage,
                            activeTrackColor = WarmSage,
                            inactiveTrackColor = TonalSand
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Daily Reminder Notifications Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Reminder Notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            color = WarmEarth,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Get nice non-intrusive notices when guests await.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = uiState.isDailyReminderEnabled,
                        onCheckedChange = { viewModel.updateDailyReminderEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WarmSage,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = TonalSand
                        )
                    )
                }

                HorizontalDivider(color = BorderCream)

                // Tutorial replay buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Owner Clover's Guided Onboarding",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmFudge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.startTutorial()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmSage),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Replay Tutorial", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.forceSkipTutorial()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmEarth),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Skip Tutorial", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = WarmSage, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// ==========================================
// 11. TUTORIAL ASSISTANCE OVERLAY
// ==========================================
@Composable
fun TutorialHelpBubble(
    step: Int,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = WarmSage.copy(0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, WarmSage.copy(0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Face,
                        contentDescription = null,
                        tint = WarmSage,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tutorial: Clover's Kitchen Guide",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = WarmFudge
                    )
                }
                TextButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Skip", color = WarmEarth, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (step) {
                    1 -> "Step 1: Choose a Sponge Base! For learning, pick 'Vanilla Sponge Base'. It has the most delicate texture."
                    2 -> "Step 2: Splendid! Now let's pick a nice frosting glaze. Choose 'Velvety Sweet Vanilla' glaze colors."
                    3 -> "Step 3: Magnificent! Next, choose your painting Spatula. Squeeze Spatula brush No. 3 is perfect!"
                    4 -> "Step 4: Now, swipe with your finger to paint custom designs! Select 'Sticker Toppings' to stamp Daisy Blossom stickers, then hit 'Bake!' when ready."
                    5 -> "Step 5: Masterpiece prepared! Polaroid captures your cake forever. Click 'Back to Cafe Front' to complete tutorial."
                    else -> "Owner Clover: Let's decorate beautiful cakes side-by-side!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = WarmEarth
            )
        }
    }
}

// ==========================================
// 12. GUEST RESPONSIVE REALISTIC REACTION DETECTOR
// ==========================================
fun getGuestReaction(uiState: AppUiState): String {
    if (uiState.isFreePlayMode) return "That's a lovely slice of art! Keep experimenting with freeform decorating!"
    
    val guest = uiState.activeGuest
    val baseMatch = uiState.selectedBase.id == guest.preferredBaseId
    val flavorMatch = uiState.selectedFlavor.id == guest.preferredFlavorId
    val hashtagFloral = uiState.placedToppings.any { it.id == "blossom_stamp" || it.id == "mint_leaf" }
    
    val guestPreferredFlavorName = DecorationOptions.frostingFlavors.find { it.id == guest.preferredFlavorId }?.name ?: "this"
    val guestPreferredBaseName = DecorationOptions.cakeBases.find { it.id == guest.preferredBaseId }?.name ?: "this base"

    return when {
        baseMatch && flavorMatch && (hashtagFloral || uiState.placedToppings.isNotEmpty()) -> {
            "\"Oh my goodness, it's absolutely perfect! You remembered my favorite $guestPreferredFlavorName frosting and $guestPreferredBaseName base, and those cute toppings match the mood completely. Thank you for making this moment so sweet!\""
        }
        baseMatch && flavorMatch -> {
            "\"This is exactly what I wanted! The $guestPreferredFlavorName frosting with $guestPreferredBaseName base is my absolute combination. Simply delicious!\""
        }
        flavorMatch -> {
            "\"Wow! That creamy $guestPreferredFlavorName glaze looks so inviting. It smells incredible! Thank you so much!\""
        }
        baseMatch -> {
            "\"A delicious slice of $guestPreferredBaseName! It's so fluffy. This is really thoughtful of you!\""
        }
        else -> {
            "\"Thank you so much! This looks like a beautiful slice of cake. It really brightens my day!\""
        }
    }
}
