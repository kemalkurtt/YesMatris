package com.example.yesmatris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yesmatris.ui.theme.YesMatrisTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var showSplash by remember { mutableStateOf(true) }

            // Süreyi 2.2 saniyeye çıkardık, hem emülatör rahat açsın hem de imzan net görünsün!
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2200)
                showSplash = false
            }

            YesMatrisTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showSplash) {
                        SplashScreen(isDarkMode = isDarkMode)
                    } else {
                        GameScreen(
                            modifier = Modifier.padding(innerPadding),
                            isDarkMode = isDarkMode,
                            onThemeChanged = { isDarkMode = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scoreManager = remember { ScoreManager(context) }
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val highScore by scoreManager.highScoreFlow.collectAsState(initial = 0)

    val engine = remember { GameEngine() }
    var boardState by remember { mutableStateOf(Array(4) { IntArray(4) { 0 } }) }
    var currentScore by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    fun restartGame() {
        engine.resetBoard()
        boardState = engine.board.map { it.clone() }.toTypedArray()
        currentScore = engine.score
        isGameOver = false
        isPaused = false
    }

    LaunchedEffect(Unit) {
        restartGame()
    }

    fun handleMove(swipeAction: () -> Unit) {
        if (isGameOver || isPaused) return

        val oldBoard = engine.board.map { it.clone() }.toTypedArray()
        swipeAction()

        var changed = false
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (oldBoard[r][c] != engine.board[r][c]) changed = true
            }
        }

        if (changed) {
            soundManager.playPopSound()
            soundManager.vibrate()
        }

        boardState = engine.board.map { it.clone() }.toTypedArray()
        currentScore = engine.score

        coroutineScope.launch {
            scoreManager.saveHighScore(currentScore)
        }

        if (engine.isGameOver()) {
            isGameOver = true
        }
    }

    // --- TEMAYA GÖRE DİNAMİK RENK PALETİ ---
    val backgroundColor = if (isDarkMode) Color(0xFF181A20) else Color(0xFFFAF8EF)
    val boardColor = if (isDarkMode) Color(0xFF2B2D37) else Color(0xFFBBADA0)
    val scoreBoxColor = if (isDarkMode) Color(0xFF2B2D37) else Color(0xFFBBADA0)
    val buttonColor = if (isDarkMode) Color(0xFF5B6770) else Color(0xFF8F7A66)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = {}) { change, dragAmount ->
                    if (!isGameOver && !isPaused && !showSettings) {
                        change.consume()
                        val (x, y) = dragAmount
                        if (abs(x) > abs(y)) {
                            if (x > 50) handleMove { engine.swipeRight() }
                            else if (x < -50) handleMove { engine.swipeLeft() }
                        } else {
                            if (y > 50) handleMove { engine.swipeDown() }
                            else if (y < -50) handleMove { engine.swipeUp() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- DEVASA YES LOGOSU (OYUN EKRANI) ---
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.yesseffaf),
                contentDescription = "YES Logo",
                modifier = Modifier
                    .fillMaxWidth(0.95f) // Genişliği ekrana sonuna kadar yay!
                    .height(130.dp),     // Aşağıdaki kutuları itmeyecek ideal yükseklik
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth // Boşluk dinleme, enine büyüt!
            )

            // 1. Satır: İki Kare Kutu (Skor ve Rekor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreBoxSquare(
                    title = "SKORSS",
                    value = currentScore.toString(),
                    boxColor = scoreBoxColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ScoreBoxSquare(
                    title = "REKORSS",
                    value = highScore.toString(),
                    boxColor = scoreBoxColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Satır: Altındaki Uzun Ayarlar & Mola Butonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(buttonColor, RoundedCornerShape(10.dp))
                    .clickable { showSettings = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AYARLAR & MOLASS",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Oyun Tahtası
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .background(boardColor, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 4) {
                                val value = if (isPaused) 0 else boardState[row][col]
                                TileBox(value = value, isDarkMode = isDarkMode)
                            }
                        }
                    }
                }

                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PAUSE",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF776E65)
                        )
                    }
                }
            }
        }

        // --- AYARLAR PANELİ (POPUP) ---
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF2B2D37) else Color(0xFFFAF8EF)
                    )
                ) {
                    val popupTextColor = if (isDarkMode) Color.White else Color(0xFF776E65)

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AYARLARSS",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = popupTextColor
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Sesss", fontSize = 16.sp, color = popupTextColor)
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = {
                                    soundEnabled = it
                                    soundManager.isSoundEnabled = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Titreşimss", fontSize = 16.sp, color = popupTextColor)
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = {
                                    vibrationEnabled = it
                                    soundManager.isVibrationEnabled = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Gece Modusss", fontSize = 16.sp, color = popupTextColor)
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = onThemeChanged
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showSettings = false },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Kapat", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- OYUN BİTTİ PANELİ ---
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(text = "OYUN BİTTİ", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Skorun: $currentScore", fontSize = 20.sp, color = Color(0xFFEEE4DA))
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { restartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Tekrar Başla", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBoxSquare(
    title: String,
    value: String,
    boxColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(70.dp)
            .background(boxColor, RoundedCornerShape(10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFFEEE4DA),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TileBox(value: Int, isDarkMode: Boolean) {
    // Gece modu açıksa boş kutular koyu gri, kapalıysa 2048'in klasik krem-gri tonu olsun
    val emptyColor = if (isDarkMode) Color(0xFF3C3F41) else Color(0xFFCDC1B4)

    val backgroundColor = when (value) {
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        else -> emptyColor
    }

    val textColor = if (value <= 4 && value != 0) Color(0xFF776E65) else Color.White

    val scale by animateFloatAsState(
        targetValue = if (value > 0) 1f else 0.8f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "TileScaleAnimation"
    )

    Box(
        modifier = Modifier
            .size(75.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                fontSize = if (value < 100) 28.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
            )
        }
    }
}

@Composable
fun SplashScreen(isDarkMode: Boolean) {
    val backgroundColor = if (isDarkMode) Color(0xFF181A20) else Color(0xFFFAF8EF)
    val textColor = if (isDarkMode) Color(0xFF776E65) else Color(0xFF9E948A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // --- DEVASA AÇILIŞ LOGOSU ---
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.yesmatrisseffaf),
            contentDescription = "YES Logo",
            modifier = Modifier
                .fillMaxWidth(1f)   // Ekran genişliğinin %100'ünü kullansın, boşluk kalmasın
                .height(260.dp),    // Yüksekliği 180'den 260'a fırlattık!
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth // Enine tam yay!
        )

        // --- İMZAN ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "powered by KEMAL KURT",
                fontSize = 15.sp,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}