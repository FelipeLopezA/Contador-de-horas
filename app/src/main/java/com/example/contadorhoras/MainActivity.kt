package com.example.contadorhoras

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contadorhoras.ui.calendar.CalendarScreen
import com.example.contadorhoras.ui.fx.AnimatedBlurryBackground
import com.example.contadorhoras.ui.theme.*
import com.example.contadorhoras.util.formatHHmm
import com.example.contadorhoras.util.formatHmsUnlimited
import com.example.contadorhoras.util.nowYearMonth
import com.example.contadorhoras.util.overlapWithMonthMs
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Screen { Home, Calendar }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContadorHorasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: TimeViewModel = viewModel()
                    var screen by remember { mutableStateOf(Screen.Home) }

                    when (screen) {
                        Screen.Home -> HomeScreen(
                            vm = vm,
                            onOpenCalendar = { screen = Screen.Calendar }
                        )
                        Screen.Calendar -> CalendarScreen(vm) { screen = Screen.Home }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: TimeViewModel,
    onOpenCalendar: () -> Unit
) {
    val open = vm.inProgress

    // Estado resumen mensual
    var ym by remember { mutableStateOf(nowYearMonth()) } // YearMonthX (propia)
    val entries by vm.entriesForMonth(ym).collectAsState(initial = emptyList())
    val summary = vm.monthSummary(entries)
    val monthlyLimitMin by vm.monthlyLimitMin.collectAsState(initial = 0)

    // Exportar CSV
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            val csv = vm.csvFor(ym)
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(csv.toByteArray())
            }
        }
    }

    var showSettings by remember { mutableStateOf(false) }

    // Mes en español (ej. “agosto 2025” con mayúscula inicial)
    val mesEs = remember(ym) {
        val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("es", "CL"))
        YearMonth.of(ym.year, ym.month).format(formatter)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es","CL")) else it.toString() }
    }

    // TOTAL con tramo vivo sumado (solo la parte del mes seleccionado)
    val liveMsInMonth = if (open != null) overlapWithMonthMs(open.startMillis, vm.now, ym) else 0L
    val totalWithLiveMs = summary.totalMs + liveMsInMonth
    val limitMs = monthlyLimitMin * 60_000L
    val remainingMs = (limitMs - totalWithLiveMs).coerceAtLeast(0L)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo_splash),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        // Título → Anton
                        Text("Contador de horas", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { inner ->
        Box(Modifier.fillMaxSize()) {

            // --- FONDO ANIMADO DIFUMINADO ---
            AnimatedBlurryBackground(
                colors = listOf(
                    Celadon,
                    CambridgeBlue,
                    Asparagus,
                    Charcoal.copy(alpha = 0.70f)
                ),
                blurRadius = 72.dp,
                globalAlpha = 0.85f
            )

            if (!isLandscape) {
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(inner)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EnterExitBlock(open, vm)

                    // Botón oscuro: Charcoal + texto blanco (label → Open Sans)
                    DarkButton(
                        text = "Abrir calendario",
                        onClick = onOpenCalendar,
                        modifier = Modifier.fillMaxWidth()
                    )

                    MonthSelector(
                        label = mesEs,
                        onPrev = { ym = ym.plusMonths(-1) },
                        onNext = { ym = ym.plusMonths(1) }
                    )

                    TotalsCard(
                        totalMs = totalWithLiveMs,
                        limitMs = limitMs,
                        remainingMs = remainingMs,
                        daysWorked = summary.daysWorked
                    )

                    if (monthlyLimitMin > 0) {
                        DonutProgress(
                            limitMin = monthlyLimitMin,
                            workedMs = totalWithLiveMs,
                            diameter = 260.dp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 30.dp)
                        )
                    }

                    // Exportar CSV (label → Open Sans)
                    DarkOutlinedButton(
                        text = "Exportar CSV",
                        onClick = {
                            val fileName = "horas_${ym.year}_${ym.month.toString().padStart(2, '0')}.csv"
                            launcher.launch(fileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(inner)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Columna izquierda: entrar/salir + calendario
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EnterExitBlock(open, vm)

                        DarkButton(
                            text = "Abrir calendario",
                            onClick = onOpenCalendar,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Columna derecha: selector mes + totales + export + dona
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MonthSelector(
                            label = mesEs,
                            onPrev = { ym = ym.plusMonths(-1) },
                            onNext = { ym = ym.plusMonths(1) }
                        )

                        TotalsCard(
                            totalMs = totalWithLiveMs,
                            limitMs = limitMs,
                            remainingMs = remainingMs,
                            daysWorked = summary.daysWorked
                        )

                        if (monthlyLimitMin > 0) {
                            DonutProgress(
                                limitMin = monthlyLimitMin,
                                workedMs = totalWithLiveMs,
                                diameter = 160.dp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        DarkOutlinedButton(
                            text = "Exportar CSV",
                            onClick = {
                                val fileName = "horas_${ym.year}_${ym.month.toString().padStart(2, '0')}.csv"
                                launcher.launch(fileName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        MonthlyLimitDialog(
            initialMinutes = monthlyLimitMin,
            onDismiss = { showSettings = false },
            onSave = { minutes ->
                scope.launch { vm.updateMonthlyLimitMin(minutes) }
                showSettings = false
            }
        )
    }
}

/* ===== Botones oscuros reutilizables ===== */

@Composable
private fun DarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Charcoal,
            contentColor = Color.White
        )
    ) { Text(text, style = MaterialTheme.typography.labelLarge) } // Open Sans
}

@Composable
private fun DarkOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Charcoal,
            contentColor = Color.White
        )
    ) { Text(text, style = MaterialTheme.typography.labelLarge) } // Open Sans
}

@Composable
private fun MonthPill(label: String, modifier: Modifier = Modifier) {
    Surface(
        color = Charcoal,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleMedium // Anton
        )
    }
}

/* ===== Bloques reutilizables ===== */

@Composable
private fun EnterExitBlock(open: InProgress?, vm: TimeViewModel) {
    if (open == null) {
        Text("No estás dentro.", style = MaterialTheme.typography.bodyLarge) // Open Sans
        Button(
            onClick = { vm.start() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) { Text("ENTRAR", style = MaterialTheme.typography.labelLarge) } // Open Sans
    } else {
        val elapsedMs = (vm.now - open.startMillis).coerceAtLeast(0L)
        Text("Inicio: ${formatHHmm(open.startMillis)}", style = MaterialTheme.typography.bodyLarge) // Open Sans
        Text(formatHmsUnlimited(elapsedMs), style = MaterialTheme.typography.displayMedium) // Anton grande
        Button(
            onClick = { vm.stop() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) { Text("SALIR", style = MaterialTheme.typography.labelLarge) } // Open Sans
    }
}

@Composable
private fun MonthSelector(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DarkButton("< Mes", onPrev)
        MonthPill(label)
        DarkButton("Mes >", onNext)
    }
}

@Composable
private fun TotalsCard(
    totalMs: Long,
    limitMs: Long,
    remainingMs: Long,
    daysWorked: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total (h:mm:ss)", style = MaterialTheme.typography.bodyLarge) // Open Sans
                Text(formatHmsUnlimited(totalMs), style = MaterialTheme.typography.titleLarge) // Anton
            }
            if (limitMs > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Límite mensual", style = MaterialTheme.typography.bodyLarge)
                    Text(formatHmsUnlimited(limitMs), style = MaterialTheme.typography.titleLarge)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Restante", style = MaterialTheme.typography.bodyLarge)
                    Text(formatHmsUnlimited(remainingMs), style = MaterialTheme.typography.titleLarge)
                }
            }
            Text("Días trabajados: $daysWorked", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/* ===== Ajustes (límite) y Dona ===== */

@Composable
private fun MonthlyLimitDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (minutes: Int) -> Unit
) {
    val initH = (initialMinutes / 60).coerceAtLeast(0)
    var hoursText by remember(initialMinutes) { mutableStateOf(initH.toString()) }

    val totalMinutes = runCatching {
        (hoursText.toIntOrNull() ?: 0).coerceAtLeast(0) * 60
    }.getOrDefault(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes de límite mensual", style = MaterialTheme.typography.titleLarge) }, // Anton
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Define un máximo de horas totales para el mes. 0 = sin límite.",
                    style = MaterialTheme.typography.bodyMedium // Open Sans
                )
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Horas (enteras)", style = MaterialTheme.typography.labelMedium) }, // Open Sans
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge, // Open Sans
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Resultado: ${formatHmsUnlimited(totalMinutes * 60_000L)}",
                    style = MaterialTheme.typography.bodyMedium // Open Sans
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(totalMinutes) }) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge) // Open Sans
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge) // Open Sans
            }
        }
    )
}

@Composable
private fun DonutProgress(
    limitMin: Int,
    workedMs: Long,
    modifier: Modifier = Modifier,
    diameter: Dp = 180.dp,
    strokeWidth: Dp = 32.dp
) {
    val limitMs = limitMin * 60_000L
    val progress = if (limitMs > 0) (workedMs.toFloat() / limitMs).coerceIn(0f, 1f) else 0f
    val remainingMs = (limitMs - workedMs).coerceAtLeast(0L)

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2
            val s = size
            val arcSize = Size(s.width - strokePx, s.height - strokePx)

            // Pista
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Progreso
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // Centro: porcentaje → Anton, líneas de apoyo → Open Sans
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge) // Anton
            Text("Trabajadas: " + formatHmsUnlimited(workedMs), style = MaterialTheme.typography.labelMedium) // Open Sans
            Text("Restante: " + formatHmsUnlimited(remainingMs), style = MaterialTheme.typography.labelMedium) // Open Sans
        }
    }
}
