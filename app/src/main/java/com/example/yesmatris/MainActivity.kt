package com.example.yesmatris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            // Gece modu durumunu en tepede tutuyoruz ki bütün uygulama haberdar olsun
            var isDarkMode by remember { mutableStateOf(true) }

            YesMatrisTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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

    // Ayarlar state'leri
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181A20))
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Üst Bar: Skorlar, Pause ve Ayarlar Butonu
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScoreBox(title = "SKORSS", value = currentScore.toString())
                    ScoreBox(title = "REKORSS", value = highScore.toString())
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Pause Butonu
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF8F7A66), RoundedCornerShape(8.dp))
                            .clickable { isPaused = !isPaused }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isPaused) "DEVAMSS" else "MOLASS", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Ayarlar Butonu
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF5B6770), RoundedCornerShape(8.dp))
                            .clickable { showSettings = true }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "AYARLARSS", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Oyun Tahtası
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .background(Color(0xFF2B2D37), RoundedCornerShape(12.dp))
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
                                TileBox(value = value)
                            }
                        }
                    }
                }

                if (isPaused) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF181A20).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "PAUSE", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Ayarlar Paneli (Popup / Modal)
        if (showSettings) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(300.dp).padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D37))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "AYARLARSS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Ses Aç/Kapat Satırı
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Sesss", fontSize = 16.sp, color = Color.White)
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = {
                                    soundEnabled = it
                                    soundManager.isSoundEnabled = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Titreşim Aç/Kapat Satırı
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Titreşimss", fontSize = 16.sp, color = Color.White)
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = {
                                    vibrationEnabled = it
                                    soundManager.isVibrationEnabled = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gece Modu Aç/Kapat Satırı
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Gece Modusss", fontSize = 16.sp, color = Color.White)
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = onThemeChanged
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Kapat Butonu
                        Button(
                            onClick = { showSettings = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Kapat", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Oyun Bitti Paneli
        if (isGameOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66)),
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
fun ScoreBox(title: String, value: String) {
    Box(
        modifier = Modifier.background(Color(0xFFBBADA0), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 9.sp, color = Color(0xFFEEE4DA), fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TileBox(value: Int) {
    val backgroundColor = when (value) {
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        else -> Color(0xFF3C3F41)
    }

    val textColor = if (value <= 4 && value != 0) Color(0xFF776E65) else Color.White

    Box(
        modifier = Modifier.size(75.dp).background(backgroundColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(text = value.toString(), fontSize = if (value < 100) 28.sp else 24.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}