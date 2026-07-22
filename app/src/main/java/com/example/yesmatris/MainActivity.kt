package com.example.yesmatris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
            YesMatrisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scoreManager = remember { ScoreManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val highScore by scoreManager.highScoreFlow.collectAsState(initial = 0)

    val engine = remember { GameEngine() }
    var boardState by remember { mutableStateOf(Array(4) { IntArray(4) { 0 } }) }
    var currentScore by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        engine.resetBoard()
        boardState = engine.board.map { it.clone() }.toTypedArray()
        currentScore = engine.score
    }

    fun updateGameState() {
        boardState = engine.board.map { it.clone() }.toTypedArray()
        currentScore = engine.score

        coroutineScope.launch {
            scoreManager.saveHighScore(currentScore)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181A20))
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {}
                ) { change, dragAmount ->
                    change.consume()
                    val (x, y) = dragAmount
                    if (abs(x) > abs(y)) {
                        if (x > 50) { engine.swipeRight(); updateGameState() }
                        else if (x < -50) { engine.swipeLeft(); updateGameState() }
                    } else {
                        if (y > 50) { engine.swipeDown(); updateGameState() }
                        else if (y < -50) { engine.swipeUp(); updateGameState() }
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ScoreBox(title = "SKOR", value = currentScore.toString())
            ScoreBox(title = "EN YÜKSEK", value = highScore.toString())
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                            TileBox(value = boardState[row][col])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBox(title: String, value: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFBBADA0), RoundedCornerShape(6.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 12.sp, color = Color(0xFFEEE4DA), fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
        else -> Color(0xFF3C3F41)
    }

    val textColor = if (value <= 4 && value != 0) Color(0xFF776E65) else Color.White

    Box(
        modifier = Modifier
            .size(75.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}