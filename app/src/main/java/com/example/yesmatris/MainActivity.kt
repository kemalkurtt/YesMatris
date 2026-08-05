package com.example.yesmatris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.example.yesmatris.ui.theme.YesMatrisTheme
import com.example.yesmatris.utils.GameEngine
import com.example.yesmatris.utils.InAppUpdateHandler
import com.example.yesmatris.utils.ScoreManager
import com.example.yesmatris.utils.SoundManager
import com.example.yesmatris.utils.Tile
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()
    private val inAppUpdateHandler: InAppUpdateHandler by inject()

    private lateinit var appUpdateManager: AppUpdateManager
    private var isAppUpdateMust = true
    private var isFlexibleUpdatePrompted = false

    /** Compose ile gösterilen güncelleme uyarılarının durumu. */
    private var showCompleteUpdateSnackbar by mutableStateOf(false)
    private var showInstallNeededDialog by mutableStateOf(false)

    private val activityResultLauncher =
        registerForActivityResult(StartIntentSenderForResult()) { result: ActivityResult ->
            when {
                result.resultCode != RESULT_OK && isAppUpdateMust -> showInstallNeeded()
                result.resultCode != RESULT_OK && !isAppUpdateMust -> appUpdateManager.unregisterListener(
                    listener
                )
            }
        }

    private val listener = InstallStateUpdatedListener { state ->
        when {
            state.installStatus() == InstallStatus.DOWNLOADED -> {
                popupSnackbarForCompleteUpdate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var showSplash by remember { mutableStateOf(true) }
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2200)
                showSplash = false
            }

            // Esnek (flexible) güncelleme indirildiğinde kurulumu tetikleyen snackbar
            LaunchedEffect(showCompleteUpdateSnackbar) {
                if (!showCompleteUpdateSnackbar) return@LaunchedEffect
                val result = snackbarHostState.showSnackbar(
                    message = getString(R.string.softwareUpdate_snackbar_message),
                    actionLabel = getString(R.string.softwareUpdate_snackbar_button),
                    withDismissAction = false,
                    duration = SnackbarDuration.Indefinite
                )
                showCompleteUpdateSnackbar = false
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateManager.completeUpdate()
                }
            }

            YesMatrisTheme(darkTheme = isDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
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

                if (showInstallNeededDialog) {
                    InstallNeededDialog(
                        onRetry = {
                            showInstallNeededDialog = false
                            inAppUpdateHandler.checkForInAppUpdate(
                                appUpdateManager,
                                AppUpdateType.IMMEDIATE,
                                listener,
                                activityResultLauncher,
                                ::markAppUpdateFlexible
                            )
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
        isFlexibleUpdatePrompted = savedInstanceState?.getBoolean(IS_FLEXIBLE_UPDATE_PROMPTED, false) == true
        appUpdateManager = AppUpdateManagerFactory.create(this)
        viewModel.fetchRemoteConfig()
        lifecycleScope.launch {
            viewModel.minRequiredVersion.collect {
                if (it != 1) {
                    setInAppUpdate(it)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_FLEXIBLE_UPDATE_PROMPTED, isFlexibleUpdatePrompted)
    }

    private fun setInAppUpdate(lastImmediateUpdateVersion: Int) {
        val appBuildVersion = BuildConfig.VERSION_CODE
        if (appBuildVersion < lastImmediateUpdateVersion) {
            inAppUpdateHandler.checkForInAppUpdate(appUpdateManager, AppUpdateType.IMMEDIATE, listener, activityResultLauncher, ::markAppUpdateFlexible)
        } else {
            inAppUpdateHandler.checkForInAppUpdate(appUpdateManager, AppUpdateType.FLEXIBLE, listener, activityResultLauncher, ::markAppUpdateFlexible)
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateHandler.checkIfAnUpdateRunning(appUpdateManager, listener, activityResultLauncher, ::markAppUpdateFlexible)
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(listener)
        super.onDestroy()
    }

    fun markAppUpdateFlexible() {
        isAppUpdateMust = false
    }

    private fun popupSnackbarForCompleteUpdate() {
        if (isFlexibleUpdatePrompted) return
        isFlexibleUpdatePrompted = true
        showCompleteUpdateSnackbar = true
    }

    private fun showInstallNeeded() {
        showInstallNeededDialog = true
    }

    companion object {
        private const val IS_FLEXIBLE_UPDATE_PROMPTED = "isFlexibleUpdatePrompted"
    }
}

/**
 * Zorunlu güncelleme yarıda kesildiğinde gösterilen, kapatılamayan uyarı.
 */
@Composable
private fun InstallNeededDialog(
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* zorunlu güncelleme: dışarı tıklayarak kapatılamaz */ },
        title = { Text(text = stringResource(R.string.softwareUpdate_dialogBox_title)) },
        text = { Text(text = stringResource(R.string.softwareUpdate_dialogBox_message)) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.global_alert_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.global_alert_cancel))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val scoreManager: ScoreManager = koinInject()
    val soundManager: SoundManager = koinInject()
    val coroutineScope = rememberCoroutineScope()

    var showHowToPlay by remember { mutableStateOf(false) }
    val highScore by scoreManager.highScoreFlow.collectAsState(initial = 0)

    val engine = remember { GameEngine() }
    var hasRecordBrokenThisGame by remember { mutableStateOf(false) }
    var boardState by remember { mutableStateOf(Array(4) { Array<Tile?>(4) { null } }) }
    var isAnimating by remember { mutableStateOf(false) }
    var currentScore by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAdDialog by remember { mutableStateOf(false) }
    var isWatchingAd by remember { mutableStateOf(false) }
    var undoStepsCount by remember { mutableIntStateOf(1) }

    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    fun restartGame() {
        engine.resetBoard()
        scoreManager.clearGameState() // YENİ: Eski oyunun kaydını sil!
        hasRecordBrokenThisGame = false
        boardState = engine.board.map { row -> row.map { it?.copy() }.toTypedArray() }.toTypedArray()
        currentScore = engine.score
        isGameOver = false
        isPaused = false
    }

    // YENİ: Oyun açıldığında kaydı yükleyen kısım buraya geldi!
    LaunchedEffect(Unit) {
        val savedBoard = scoreManager.getSavedBoard()
        if (savedBoard != null && savedBoard.isNotBlank()) {
            val savedScore = scoreManager.getSavedScore()
            engine.loadState(savedScore, savedBoard)
            boardState = engine.board.map { row -> row.map { it?.copy() }.toTypedArray() }.toTypedArray()
            currentScore = engine.score
            isGameOver = engine.isGameOver()
        } else {
            restartGame()
        }
    }

    fun handleMove(swipeAction: () -> Boolean) {
        if (isGameOver || isPaused || isAnimating) return

        isAnimating = true

        val oldBoard = engine.board.map { row ->
            row.map { it?.copy() }.toTypedArray()
        }.toTypedArray()

        val oldScore = engine.score

        val moved = swipeAction()

        if (!moved) {
            isAnimating = false
            return
        }

        // Hamle başarılıysa eski durumu hafızaya kaydet
        engine.saveState(oldBoard, oldScore)

        var changed = false
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (oldBoard[r][c]?.value != engine.board[r][c]?.value)
                    changed = true
            }
        }

        val newScore = engine.score

        if (changed) {
            if (highScore > 0 && newScore > highScore && !hasRecordBrokenThisGame) {
                soundManager.playRecordSound()
                hasRecordBrokenThisGame = true
            } else if (newScore > oldScore) {
                soundManager.playMergeSound()
            } else {
                soundManager.playPopSound()
            }

            soundManager.vibrate()
        }

        boardState = engine.board.map { row ->
            row.map { it?.copy() }.toTypedArray()
        }.toTypedArray()

        currentScore = engine.score

        coroutineScope.launch {
            scoreManager.saveHighScore(currentScore)
        }
        coroutineScope.launch {
            kotlinx.coroutines.delay(250)

            engine.addRandomNumber()

            boardState = engine.board.map { row ->
                row.map { it?.copy() }.toTypedArray()
            }.toTypedArray()

            currentScore = engine.score

            if (engine.isGameOver()) {
                isGameOver = true
            }

            // YENİ: Hamle bittikten sonra mevcut tahtayı cihaza kaydet!
            scoreManager.saveGameState(currentScore, engine.getBoardAsString())

            kotlinx.coroutines.delay(300)
            isAnimating = false
        }

        coroutineScope.launch {
            kotlinx.coroutines.delay(550)
            isAnimating = false
        }
    }

    val backgroundColor = if (isDarkMode) Color(0xFF181A20) else Color(0xFFFAF8EF)
    val boardColor = if (isDarkMode) Color(0xFF2B2D37) else Color(0xFFBBADA0)
    val scoreBoxColor = if (isDarkMode) Color(0xFF2B2D37) else Color(0xFFBBADA0)
    val buttonColor = if (isDarkMode) Color(0xFF5B6770) else Color(0xFF8F7A66)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                // Değişkenleri buraya alıyoruz ki hafızada sağlam dursun
                var dragOffsetX = 0f
                var dragOffsetY = 0f

                detectDragGestures(
                    onDragStart = {
                        // Parmağı ekrana koyduğunda mesafeleri sıfırla
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    },
                    onDragEnd = {
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    }
                ) { change, dragAmount ->
                    if (!isGameOver && !isPaused && !showSettings) {
                        change.consume()
                        // Her frame'deki hareketi toplayarak biriktiriyoruz
                        dragOffsetX += dragAmount.x
                        dragOffsetY += dragAmount.y

                        // Toplam hareket 60 pikseli geçtiği an tetikle
                        if (abs(dragOffsetX) > 60 || abs(dragOffsetY) > 60) {
                            if (abs(dragOffsetX) > abs(dragOffsetY)) {
                                if (dragOffsetX > 0) handleMove { engine.swipeRight() }
                                else handleMove { engine.swipeLeft() }
                            } else {
                                if (dragOffsetY > 0) handleMove { engine.swipeDown() }
                                else handleMove { engine.swipeUp() }
                            }
                            // Tetiklendikten sonra sıfırlıyoruz ki tek kaydırmada 2 kez oynamasın
                            dragOffsetX = 0f
                            dragOffsetY = 0f
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
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.yesseffaf),
                contentDescription = "YES Logo",
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(130.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
            )

            // GERİ AL BUTONU (Sadece geçmişte hamle varsa görünür)
            if (engine.canUndo()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE94560), RoundedCornerShape(8.dp))
                            .clickable {
                                undoStepsCount = 1
                                showAdDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⏪ Geri Al (Reklam)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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

            // Ayarlar ve Nasıl Oynanır Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(buttonColor, RoundedCornerShape(10.dp))
                        .clickable { showSettings = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AYARLARSS",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(buttonColor, RoundedCornerShape(10.dp))
                        .clickable { showHowToPlay = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NASILSS OYNANIRSS?",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                TileBox(value = 0, isDarkMode = isDarkMode)
                            }
                        }
                    }
                }

                if (!isPaused) {
                    val activeTiles = mutableListOf<Triple<Tile, Int, Int>>()
                    for (r in 0 until 4) {
                        for (c in 0 until 4) {
                            val tile = boardState[r][c]
                            if (tile != null) {
                                activeTiles.add(Triple(tile, r, c))
                            }
                        }
                    }

                    activeTiles.sortBy { it.first.id }

                    for (item in activeTiles) {
                        val tile = item.first
                        val row = item.second
                        val col = item.third

                        key(tile.id) {
                            val targetX = (col * 83.5f + 4.25f).dp
                            val targetY = (row * 83.5f + 4.25f).dp

                            val animatedX by animateDpAsState(
                                targetValue = targetX,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "x_kayma_${tile.id}"
                            )
                            val animatedY by animateDpAsState(
                                targetValue = targetY,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "y_kayma_${tile.id}"
                            )

                            Box(
                                modifier = Modifier.offset(x = animatedX, y = animatedY)
                            ) {
                                TileBox(value = tile.value, isDarkMode = isDarkMode)
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
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(text = "Tekrar Başla", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (engine.canUndo()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                undoStepsCount = 5
                                showAdDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text(text = "🎥 Reklam İzle & Kurtul", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showHowToPlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
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
                            text = "NASIL OYNANIR?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = popupTextColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "👉 Taşları hareket ettirmek için ekranı sağa, sola, yukarı veya aşağı kaydır.\n\n" +
                                    "💥 Aynı sayıya sahip iki taş çarpıştığında birleşerek iki katı değere ulaşır! (Örn: 2+2=4)\n\n" +
                                    "🎲 Her hamlede tahtaya rastgele '2' veya '4' değerinde yeni bir taş eklenir.\n\n" +
                                    "🚫 Tahta tamamen dolar ve hamle yapacak yer kalmazsa oyun biter.",
                            fontSize = 15.sp,
                            color = popupTextColor,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showHowToPlay = false },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Anladımss, Oynayalımss!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showAdDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                if (isWatchingAd) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Reklam Yükleniyor ve İzleniyor...", color = Color.White)

                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(2000)

                            var isUndone = false
                            for (i in 0 until undoStepsCount) {
                                if (engine.undo()) {
                                    isUndone = true
                                }
                            }

                            if (isUndone) {
                                boardState = engine.board.map { row -> row.map { it?.copy() }.toTypedArray() }.toTypedArray()
                                currentScore = engine.score
                                isGameOver = false
                            }

                            isWatchingAd = false
                            showAdDialog = false
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.width(300.dp).padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF2B2D37) else Color(0xFFFAF8EF))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("PİŞMAN MISIN? 😅", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF776E65))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Kısa bir reklam izleyerek hamlelerini geri alabilirsin!", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { isWatchingAd = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎥 Reklam İzle & Geri Al", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showAdDialog = false }) {
                                Text("Vazgeç", color = Color.Gray)
                            }
                        }
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
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.yesmatrisseffaf),
            contentDescription = "YES Logo",
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(260.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
        )

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