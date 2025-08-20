package com.example.contadorhoras.ui.calendar

import android.content.res.Configuration
import android.os.LocaleList
import android.view.ContextThemeWrapper
import android.widget.CalendarView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.contadorhoras.R
import com.example.contadorhoras.TimeViewModel
import com.example.contadorhoras.data.TimeEntry
import com.example.contadorhoras.ui.theme.Charcoal
import com.example.contadorhoras.util.APP_ZONE
import com.example.contadorhoras.util.formatDateLongEs
import com.example.contadorhoras.util.formatHHmm
import com.example.contadorhoras.util.formatHmsUnlimited
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: TimeViewModel, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(LocalDate.now(APP_ZONE)) }

    // Lista de sesiones del día seleccionado
    val entries by vm.dayEntries(selected).collectAsState(initial = emptyList())

    var editing by remember { mutableStateOf<TimeEntry?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    // Contexto configurado en español (Chile) solo para el CalendarView
    val ctx = LocalContext.current
    val esCtx = remember(ctx) {
        val conf = Configuration(ctx.resources.configuration)
        conf.setLocales(LocaleList(Locale("es", "CL")))
        ctx.createConfigurationContext(conf)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    BackPill(onClick = onBack)
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AndroidView(
                factory = {
                    val esCL = Locale.Builder().setLanguage("es").setRegion("CL").build()
                    val conf = Configuration(it.resources.configuration)
                    conf.setLocales(LocaleList(esCL))
                    val esCtx2 = it.createConfigurationContext(conf)

                    val themedCtx = ContextThemeWrapper(esCtx2, R.style.AppCalendar)
                    CalendarView(themedCtx).apply {
                        firstDayOfWeek = java.util.Calendar.MONDAY
                        setOnDateChangeListener { _, y, m, d ->
                            selected = LocalDate.of(y, m + 1, d)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            DayCardList(
                selected = selected,
                entries = entries,
                onEdit = { editing = it },
                onCreate = { showCreate = true }
            )
        }
    }

    // Diálogo editar
    editing?.let { e ->
        EditEntryDialog(
            entry = e,
            onDismiss = { editing = null },
            onSave = { startText, endText ->
                fun parseHmOrNull(s: String): Long? = runCatching {
                    if (s.isBlank()) return@runCatching null
                    val (h, m) = s.split(":").map { it.toInt() }
                    val date = LocalDate.ofEpochDay(e.dateEpochDay)
                    date.atTime(h, m).atZone(APP_ZONE).toInstant().toEpochMilli()
                }.getOrNull()

                val newStart = parseHmOrNull(startText) ?: e.startMillis
                val newEnd   = parseHmOrNull(endText)
                vm.updateTimes(e, newStart, newEnd)
                editing = null
            }
        )
    }

    // Diálogo crear
    if (showCreate) {
        CreateEntryDialog(
            date = selected,
            onDismiss = { showCreate = false },
            onSave = { startText, endText ->
                fun parseHm(s: String): Long {
                    val (h, m) = s.split(":").map { it.toInt() }
                    return selected.atTime(h, m).atZone(APP_ZONE).toInstant().toEpochMilli()
                }
                val startMs = parseHm(startText)
                val endMs = endText.takeIf { it.isNotBlank() }?.let { parseHm(it) }
                vm.createEntryForDay(selected, startMs, endMs)
                showCreate = false
            }
        )
    }
}

@Composable
private fun DayCardList(
    selected: LocalDate,
    entries: List<TimeEntry>,
    onEdit: (TimeEntry) -> Unit,
    onCreate: () -> Unit
) {
    // Solo rangos que INICIAN y TERMINAN el mismo día seleccionado (zona Chile)
    val sameDay = remember(selected, entries) {
        entries.filter { e ->
            val end = e.endMillis ?: return@filter false
            val startDay = Instant.ofEpochMilli(e.startMillis).atZone(APP_ZONE).toLocalDate()
            val endDay   = Instant.ofEpochMilli(end).atZone(APP_ZONE).toLocalDate()
            startDay == selected && endDay == selected
        }.sortedBy { it.startMillis }
    }

    if (entries.isEmpty()) {
        Text("Sin registro para ${formatDateLongEs(selected)}", style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onCreate) { Text("Crear registro", style = MaterialTheme.typography.labelLarge) }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
    ) {
        val dayTotal = sameDay.sumOf { ((it.endMillis ?: 0L) - it.startMillis).coerceAtLeast(0) }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Detalle del día", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                HeaderCell("Inicio")
                HeaderCell("Fin")
                HeaderCell("Total")
                HeaderCell("") // para botón Editar
            }

            if (sameDay.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "No hay rangos cerrados completamente en este día.\n" +
                            "Los turnos abiertos o que cruzan medianoche no se listan aquí.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(sameDay) { e ->
                        val totalMs = ((e.endMillis ?: System.currentTimeMillis()) - e.startMillis)
                            .coerceAtLeast(0)

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DataCell(formatHHmm(e.startMillis))
                            DataCell(formatHHmm(e.endMillis))
                            DataCell(formatHmsUnlimited(totalMs))

                            Spacer(Modifier.weight(1f)) // empuja el botón a la derecha
                            PillButton(text = "Editar") { onEdit(e) }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(top = 4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Total del día", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
                    Text(formatHmsUnlimited(dayTotal), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/* ====== Celdas de tabla ====== */

@Composable
private fun RowScope.HeaderCell(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
}

@Composable
private fun RowScope.DataCell(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
}

/* ====== Diálogos ====== */

@Composable
private fun EditEntryDialog(
    entry: TimeEntry,
    onDismiss: () -> Unit,
    onSave: (startHHmm: String, endHHmm: String) -> Unit
) {
    var startText by remember(entry) { mutableStateOf(formatHHmm(entry.startMillis)) }
    var endText by remember(entry) { mutableStateOf(formatHHmm(entry.endMillis)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar registro", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("Inicio (HH:mm)", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("Fin (HH:mm, vacío si sigues dentro)", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(startText, endText) }) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun CreateEntryDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (startHHmm: String, endHHmm: String) -> Unit
) {
    val nowText = formatHHmm(Instant.now().toEpochMilli())
    var startText by remember(date) { mutableStateOf(nowText) }
    var endText by remember(date) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo registro • ${formatDateLongEs(date)}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("Inicio (HH:mm)", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("Fin (HH:mm, opcional)", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(startText, endText) }) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/* ====== Pills reutilizables ====== */

@Composable
private fun BackPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Charcoal,
            contentColor = Color.White
        )
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
    }
}

@Composable
private fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Charcoal,
            contentColor   = Color.White
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

