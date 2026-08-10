package com.veera.expense

import android.os.Bundle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.widget.Toast
import android.graphics.pdf.PdfDocument
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow

enum class EntryType { INCOME, EXPENSE, TRANSFER }

data class MoneyEntry(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: EntryType,
    val category: String,
    val paymentMethod: String = "Cash",
    val time: Long = System.currentTimeMillis(),
    val photoUri: String? = null
)

private val Midnight = Color(0xFF0B1020)
private val Ink = Color(0xFF161A2B)
private val Violet = Color(0xFF6C5CE7)
private val VioletDeep = Color(0xFF4D3BCB)
private val Gold = Color(0xFFFFC857)
private val Mint = Color(0xFF35D0A0)
private val Coral = Color(0xFFFF6B7A)
private val CanvasBg = Color(0xFFF6F7FB)

@Composable
fun VeeraTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Color(0xFF9A8CFF),
            onPrimary = Color.White,
            secondary = Color(0xFF57E0B5),
            tertiary = Gold,
            background = Midnight,
            surface = Ink,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF242A40),
            onSurfaceVariant = Color(0xFFB9BED0),
            error = Coral
        ) else lightColorScheme(
            primary = Violet,
            onPrimary = Color.White,
            secondary = Mint,
            tertiary = Gold,
            background = CanvasBg,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Color(0xFFEFF0F6),
            onSurfaceVariant = Color(0xFF697087),
            error = Coral
        ),
        typography = Typography(),
        content = content
    )
}

class MainActivity : FragmentActivity() {
    private var lockPromptShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VeeraApp() }
    }

    override fun onResume() {
        super.onResume()
        if (!lockPromptShown && getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("app_lock", false)) {
            lockPromptShown = true
            showBiometricLock()
        }
    }

    fun enableAndShowBiometricLock() {
        lockPromptShown = false
        showBiometricLock()
    }

    fun showBiometricLock() {
        val manager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Fingerprint / device lock is not available on this phone", Toast.LENGTH_LONG).show()
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    finish()
                }
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Veera")
            .setSubtitle("Protect your financial information")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VeeraApp() {
    var tab by remember { mutableIntStateOf(0) }
    var darkTheme by remember { mutableStateOf(false) }
    var showExplore by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val entries = remember {
        mutableStateListOf<MoneyEntry>().apply {
            addAll(loadEntries(context))
        }
    }
    var showAdd by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf(EntryType.EXPENSE) }
    var selectedEntry by remember { mutableStateOf<MoneyEntry?>(null) }
    var editingEntry by remember { mutableStateOf<MoneyEntry?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<MoneyEntry?>(null) }

    VeeraTheme(dark = darkTheme) {
    Scaffold(
        containerColor = if (darkTheme) Midnight else CanvasBg,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(CanvasBg).statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VeeraLogo(modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("VEERA", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = Ink)
                    Text("Your money, elevated.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp) {
                    IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "Notifications", tint = Ink) }
                }
            }
        },
        bottomBar = {
            Surface(shadowElevation = 18.dp, color = Color.White) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.Analytics, null) }, label = { Text("Insights") })
                    NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("More") })
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addType = EntryType.EXPENSE; showAdd = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add transaction", fontWeight = FontWeight.Bold) },
                containerColor = Ink,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(padding),
            label = "main"
        ) { selected ->
            when (selected) {
                0 -> HomeScreen(entries, { addType = EntryType.INCOME; showAdd = true }, { addType = EntryType.EXPENSE; showAdd = true }, { addType = EntryType.TRANSFER; showAdd = true }, { selectedEntry = it })
                1 -> ReportsScreen(entries) { selectedEntry = it }
                else -> MoreScreen(onExplore = { showExplore = true })
            }
        }
    }

    if (showAdd) {
        AddEntryDialog(addType, { showAdd = false }) { title, amount, category, paymentMethod, timestamp, photoUri ->
            val storedPhoto = photoUri?.let { persistPhoto(context, it) }
            val entry = MoneyEntry(System.currentTimeMillis(), title, amount, addType, category, paymentMethod, timestamp, storedPhoto)
            entries.add(0, entry)
            saveEntries(context, entries)
            showAdd = false
        }
    }

    if (selectedEntry != null) {
        EntryDetailDialog(
            entry = selectedEntry!!,
            onDismiss = { selectedEntry = null },
            onEdit = { editingEntry = it; selectedEntry = null },
            onDelete = { showDeleteConfirm = selectedEntry; selectedEntry = null }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete transaction?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This transaction will be removed from Veera. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = showDeleteConfirm
                    if (target != null) {
                        entries.removeAll { it.id == target.id }
                        saveEntries(context, entries)
                    }
                    showDeleteConfirm = null
                }) { Text("Delete", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") } }
        )
    }

    if (editingEntry != null) {
        EditEntryDialog(
            entry = editingEntry!!,
            onDismiss = { editingEntry = null },
            onSave = { updated ->
                val index = entries.indexOfFirst { it.id == updated.id }
                if (index >= 0) entries[index] = updated
                saveEntries(context, entries)
                editingEntry = null
            }
        )
    }

    if (showExplore) {
        ExploreDialog(
            entries = entries,
            darkTheme = darkTheme,
            onToggleTheme = { darkTheme = !darkTheme },
            onRestore = { restored -> entries.clear(); entries.addAll(restored); saveEntries(context, entries) },
            onDismiss = { showExplore = false }
        )
    }
    }
}

@Composable
private fun VeeraLogo(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Violet, VioletDeep))), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(25.dp))
        Box(Modifier.size(7.dp).align(Alignment.TopEnd).offset((-6).dp, 6.dp).clip(CircleShape).background(Gold))
    }
}

@Composable
private fun HomeScreen(entries: List<MoneyEntry>, onIncome: () -> Unit, onExpense: () -> Unit, onTransfer: () -> Unit, onEntryClick: (MoneyEntry) -> Unit) {
    val income = entries.filter { it.type == EntryType.INCOME }.sumOf { it.amount }
    val expense = entries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
    val balance = income - expense
    val grouped = entries.sortedByDescending { it.time }.groupBy { dayKey(it.time) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp)
    ) {
        item { BalanceHero(balance, income, expense) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction("Income", Icons.Default.ArrowDownward, Mint, onIncome, Modifier.weight(1f))
                QuickAction("Expense", Icons.Default.ArrowUpward, Coral, onExpense, Modifier.weight(1f))
                QuickAction("Transfer", Icons.Default.SwapHoriz, Violet, onTransfer, Modifier.weight(1f))
            }
        }
        item { SpendingOverview(income, expense) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recent activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Grouped by transaction date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (entries.isEmpty()) { item { EmptyState() } }
        grouped.entries.take(8).forEach { (day, dayEntries) ->
            item {
                Text(dayLabel(dayEntries.first().time), fontWeight = FontWeight.Bold, color = Violet, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            }
            items(dayEntries.take(8), key = { it.id }) { EntryRow(it, onClick = onEntryClick) }
        }
    }
}

@Composable
private fun BalanceHero(balance: Double, income: Double, expense: Double) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Midnight, VioletDeep)))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val path = Path().apply {
                moveTo(size.width * .55f, size.height)
                cubicTo(size.width * .68f, size.height * .55f, size.width * .78f, size.height * .95f, size.width, size.height * .32f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, Brush.linearGradient(listOf(Color(0x336C5CE7), Color(0x555B4BDB))))
            drawCircle(Color(0x33FFC857), radius = 90.dp.toPx(), center = Offset(size.width * .88f, 20.dp.toPx()))
        }
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = .10f)) {
                    Text("TOTAL BALANCE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White.copy(.75f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.AutoGraph, null, tint = Gold)
            }
            Spacer(Modifier.height(12.dp))
            Text(money(balance), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (balance >= 0) "You're on track this period" else "Review your spending", color = Color.White.copy(.70f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetric("Income", income, Mint, Modifier.weight(1f))
                HeroMetric("Expenses", expense, Coral, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, amount: Double, color: Color, modifier: Modifier) {
    Surface(modifier, color = Color.White.copy(.09f), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(7.dp))
                Text(label, color = Color.White.copy(.72f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(money(amount), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier, color = Color.White, shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SpendingOverview(income: Double, expense: Double) {
    val total = (income + expense).coerceAtLeast(1.0)
    val ratio = (expense / total).coerceIn(0.0, 1.0).toFloat()
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cash flow", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Income vs. expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.TrendingUp, null, tint = Mint)
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFECEEF5))) {
                Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Coral, Violet))))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Legend("Income", money(income), Mint)
                Legend("Expenses", money(expense), Coral)
            }
        }
    }
}

@Composable
private fun Legend(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyState() {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEAFE))) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Violet.copy(.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Savings, null, tint = Violet)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Build your money story", fontWeight = FontWeight.Bold)
                Text("Add your first income or expense to unlock insights.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Violet)
        }
    }
}

@Composable
private fun EntryRow(entry: MoneyEntry, onClick: (MoneyEntry) -> Unit = {}) {
    val isIncome = entry.type == EntryType.INCOME
    val isTransfer = entry.type == EntryType.TRANSFER
    val accent = when { isIncome -> Mint; isTransfer -> Violet; else -> Coral }
    val context = LocalContext.current
    val bitmap = remember(entry.photoUri) {
        entry.photoUri?.let {
            runCatching { context.contentResolver.openInputStream(Uri.parse(it))?.use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    Card(onClick = { onClick(entry) }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = "Receipt photo", modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(.11f)), contentAlignment = Alignment.Center) {
                    Icon(if (isIncome) Icons.Default.ArrowDownward else if (isTransfer) Icons.Default.SwapHoriz else Icons.Default.ArrowUpward, null, tint = accent)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${entry.category} • ${entry.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDateTime(entry.time), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if (isIncome) "+" else if (isTransfer) "↔" else "-") + money(entry.amount), fontWeight = FontWeight.ExtraBold, color = if (isIncome) Color(0xFF159B74) else if (isTransfer) Violet else Ink)
                if (entry.photoUri != null) {
                    Text("Receipt attached", fontSize = 10.sp, color = Violet, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(entries: List<MoneyEntry>, onEntryClick: (MoneyEntry) -> Unit) {
    var mode by remember { mutableStateOf("Month") }
    var selectedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) writePdf(context, uri, filteredEntries(entries, mode, selectedTime))
    }
    val filtered = filteredEntries(entries, mode, selectedTime)
    val income = filtered.filter { it.type == EntryType.INCOME }.sumOf { it.amount }
    val expense = filtered.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
    val balance = income - expense

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Insights", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Filter, review and export your money story", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { picker.launch("veera-${mode.lowercase(Locale.getDefault())}-report.pdf") }, shape = RoundedCornerShape(14.dp)) { Text("PDF") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Day", "Month", "All").forEach { option ->
                    FilterChip(selected = mode == option, onClick = { mode = option }, label = { Text(option) })
                }
                if (mode != "All") {
                    OutlinedButton(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = selectedTime }
                        DatePickerDialog(context, { _, y, m, d ->
                            selectedTime = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text(if (mode == "Day") "📅 ${dayLabel(selectedTime)}" else "🗓️ ${monthLabel(selectedTime)}", maxLines = 1)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Income", money(income), Mint, Modifier.weight(1f))
                StatCard("Expense", money(expense), Coral, Modifier.weight(1f))
            }
        }
        item { ReportCard("Net balance", balance, Violet) }
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Midnight)) {
                Column(Modifier.padding(22.dp)) {
                    Text("Financial pulse", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${filtered.size} transaction${if (filtered.size == 1) "" else "s"} in this view", color = Color.White.copy(.6f), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(18.dp)); MiniChart()
                }
            }
        }
        filtered.sortedByDescending { it.time }.groupBy { dayKey(it.time) }.forEach { (_, list) ->
            item { Text(dayLabel(list.first().time), fontWeight = FontWeight.Bold, color = Violet) }
            items(list, key = { it.id }) { EntryRow(it, onClick = onEntryClick) }
        }
        if (filtered.isEmpty()) item { EmptyState() }
    }
}

@Composable
private fun StatCard(title: String, value: String, accent: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun MiniChart() {
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        val points = listOf(.18f, .42f, .28f, .58f, .48f, .76f, .68f, .92f)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.lastIndex.toFloat())
            val y = size.height * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = Gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { index, value ->
            drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(size.width * index / points.lastIndex.toFloat(), size.height * (1f - value)))
        }
    }
}

@Composable
private fun ReportCard(title: String, amount: Double, accent: Color) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(money(amount), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MoreScreen(onExplore: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("More", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("Make Veera feel like yours", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Midnight)) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VeeraLogo(Modifier.size(46.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("VEERA PREMIUM", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                        Text("Clarity. Control. Confidence.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Premium reports, smart insights, exports, themes and savings goals.", color = Color.White.copy(.68f))
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onExplore, border = BorderStroke(1.dp, Color.White.copy(.2f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Explore") }
            }
        }
    }
}

@Composable
private fun ExploreDialog(
    entries: List<MoneyEntry>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onRestore: (List<MoneyEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    var active by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var goal by remember { mutableStateOf(loadGoal(context)) }
    var goalSaved by remember { mutableStateOf(loadGoalSaved(context)) }
    var goalText by remember { mutableStateOf(if (goal > 0) goal.toString() else "") }
    var goalSavedText by remember { mutableStateOf(if (goalSaved > 0) goalSaved.toString() else "") }
    var investmentTarget by remember { mutableStateOf(loadInvestmentTarget(context)) }
    var investmentSaved by remember { mutableStateOf(loadInvestmentSaved(context)) }
    var investmentTargetText by remember { mutableStateOf(if (investmentTarget > 0) investmentTarget.toString() else "") }
    var investmentSavedText by remember { mutableStateOf(if (investmentSaved > 0) investmentSaved.toString() else "") }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) context.contentResolver.openOutputStream(uri)?.use { it.write(buildBackupJson(entries).toByteArray()) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val restored = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.let(::parseBackupJson) ?: emptyList() }.getOrDefault(emptyList())
            if (restored.isNotEmpty()) onRestore(restored)
        }
    }
    val income = entries.filter { it.type == EntryType.INCOME }.sumOf { it.amount }
    val expense = entries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
    val balance = income - expense
    val suggestion = when {
        entries.isEmpty() -> "Start with today's income and expenses. Veera will build your personal money pattern."
        expense > income -> "Your recorded expenses are above income. Review the largest categories and set a daily spending limit."
        goal > 0 && goalSaved >= goal -> "🎉 Your savings goal is reached. You can now create the next milestone."
        investmentTarget > 0 && investmentSaved >= investmentTarget -> "📈 Your investment target is reached. Consider setting your next long-term milestone."
        goal > 0 -> "You have ₹${String.format(Locale.getDefault(), "%,.0f", (goal - goalSaved).coerceAtLeast(0.0))} left on your savings goal. Add actual savings contributions here; your balance is kept separate."
        investmentTarget > 0 -> "You have ₹${String.format(Locale.getDefault(), "%,.0f", (investmentTarget - investmentSaved).coerceAtLeast(0.0))} left on your investment target. Record contributions separately from your cash balance."
        balance >= 0 -> "Your recorded balance is positive. Set separate savings and investment targets to give your surplus a purpose."
        else -> "Your balance is negative for the recorded period. Review expenses before increasing your savings or investment target."
    }
    AlertDialog(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp),
        title = { Column { Text("Veera Premium", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp); Text("Tools that make your money clearer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumAction("Settings", "Preferences, security & app controls", Icons.Default.Settings) { active = "Settings" }
                PremiumAction("App Lock", if (isAppLockEnabled(context)) "Fingerprint / device lock enabled" else "Protect Veera with biometrics", Icons.Default.Settings) { active = "App Lock" }
                PremiumAction("Smart Insights", "Personal spending suggestions", Icons.Default.AutoAwesome) { active = "Smart Insights" }
                PremiumAction("Themes", if (darkTheme) "Dark theme enabled" else "Light theme enabled", Icons.Default.Palette) { onToggleTheme(); active = "Themes" }
                PremiumAction("Exports", "CSV or filtered PDF reports", Icons.Default.FileDownload) { active = "Exports" }
                PremiumAction("Savings Goal", if (goal > 0) "Target ₹${String.format(Locale.getDefault(), "%,.0f", goal)} • Saved ₹${String.format(Locale.getDefault(), "%,.0f", goalSaved)}" else "Set target + add savings separately", Icons.Default.Flag) { active = "Savings Goal" }
                PremiumAction("Investment Goal", if (investmentTarget > 0) "Target ₹${String.format(Locale.getDefault(), "%,.0f", investmentTarget)} • Added ₹${String.format(Locale.getDefault(), "%,.0f", investmentSaved)}" else "Set an investment target separately", Icons.Default.TrendingUp) { active = "Investment Goal" }
                if (active == "Settings") {
                    PremiumInfoCard("Settings", "Your transactions, dates, times and receipt photos stay on this device. Day / Month / All filters and PDF export are available in Insights.")
                    PremiumInfoCard("Developer", "Srinivasan\nsrinimmb@gmail.com")
                    OutlinedButton(onClick = { backupPicker.launch("veera-backup.json") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Backup data") }
                    OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Restore data") }
                }
                if (active == "App Lock") {
                    val enabled = isAppLockEnabled(context)
                    PremiumInfoCard("App Lock", if (enabled) "Veera is protected with fingerprint / device credentials." else "Turn on biometric or device-credential protection for your financial data.")
                    Button(onClick = {
                        if (enabled) {
                            setAppLockEnabled(context, false)
                            active = "App Lock"
                        } else {
                            setAppLockEnabled(context, true)
                            active = "App Lock"
                            (context as? MainActivity)?.enableAndShowBiometricLock()
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(if (enabled) "Disable App Lock" else "Enable App Lock & Test Now") }
                }
                if (active == "Smart Insights") PremiumInfoCard("Smart Insights", suggestion)
                if (active == "Themes") PremiumInfoCard("Theme", "Switch between a bright professional workspace and a premium dark mode.")
                if (active == "Exports") PremiumInfoCard("Exports", "Open Insights → choose Day, Month or All → tap PDF to create a shareable PDF. CSV export is also available below.")
                if (active == "Exports") {
                    OutlinedButton(onClick = {
                        val csv = buildCsv(entries); val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Veera transactions"); putExtra(Intent.EXTRA_TEXT, csv) }; context.startActivity(Intent.createChooser(intent, "Export Veera CSV"))
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Export CSV") }
                }
                if (active == "Savings Goal") {
                    OutlinedTextField(goalText, { goalText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Savings target ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(goalSavedText, { goalSavedText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount actually saved ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        goal = goalText.toDoubleOrNull() ?: 0.0
                        goalSaved = goalSavedText.toDoubleOrNull() ?: 0.0
                        saveGoal(context, goal)
                        saveGoalSaved(context, goalSaved)
                        if (goal > 0) { scheduleGoalReminder(context, goal); requestNotificationPermission(context) } else cancelGoalReminder(context)
                        active = "Savings Goal"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save savings goal") }
                    if (goal > 0) {
                        val pct = ((goalSaved / goal).coerceIn(0.0, 1.0) * 100).toInt()
                        PremiumInfoCard("Savings progress", "₹${String.format(Locale.getDefault(), "%,.0f", goalSaved)} / ₹${String.format(Locale.getDefault(), "%,.0f", goal)} • $pct% • Balance is not used as saved amount.")
                    }
                }
                if (active == "Investment Goal") {
                    OutlinedTextField(investmentTargetText, { investmentTargetText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Investment target ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(investmentSavedText, { investmentSavedText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount invested so far ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        investmentTarget = investmentTargetText.toDoubleOrNull() ?: 0.0
                        investmentSaved = investmentSavedText.toDoubleOrNull() ?: 0.0
                        saveInvestmentTarget(context, investmentTarget)
                        saveInvestmentSaved(context, investmentSaved)
                        if (investmentTarget > 0) { scheduleInvestmentReminder(context, investmentTarget); requestNotificationPermission(context) } else cancelInvestmentReminder(context)
                        active = "Investment Goal"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save investment goal") }
                    if (investmentTarget > 0) {
                        val pct = ((investmentSaved / investmentTarget).coerceIn(0.0, 1.0) * 100).toInt()
                        PremiumInfoCard("Investment progress", "₹${String.format(Locale.getDefault(), "%,.0f", investmentSaved)} / ₹${String.format(Locale.getDefault(), "%,.0f", investmentTarget)} • $pct% • This is separate from Veera's cash balance.")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun PremiumInfoCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun PremiumAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Violet.copy(.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Violet)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun buildCsv(entries: List<MoneyEntry>): String {
    val header = "Title,Amount,Type,Category,Payment Method,Date,Time,Receipt\n"
    return header + entries.joinToString("\n") { e ->
        val c = Calendar.getInstance().apply { timeInMillis = e.time }
        val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH))
        val time = String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        "\"${e.title.replace("\"", "\"\"")}\",${e.amount},${e.type},\"${e.category.replace("\"", "\"\"")}\",$date,$time,${if (e.photoUri != null) "Yes" else "No"}"
    }
}


@Composable
private fun EntryDetailDialog(
    entry: MoneyEntry,
    onDismiss: () -> Unit,
    onEdit: (MoneyEntry) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(entry.photoUri) {
        entry.photoUri?.let { uri ->
            runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transaction details", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(entry.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    (if (entry.type == EntryType.INCOME) "+" else if (entry.type == EntryType.TRANSFER) "↔" else "-") + money(entry.amount),
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (entry.type == EntryType.INCOME) Mint else if (entry.type == EntryType.TRANSFER) Violet else Coral
                )
                PremiumInfoCard("Category", entry.category)
                PremiumInfoCard("Account / payment", entry.paymentMethod)
                PremiumInfoCard("Date & time", formatDateTime(entry.time))
                if (bitmap != null) {
                    Image(
                        bitmap.asImageBitmap(),
                        contentDescription = "Receipt",
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                if (entry.photoUri != null) {
                    Text("Receipt attached", color = Violet, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Delete")
                }
                Button(onClick = { onEdit(entry) }, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit")
                }
            }
        }
    )
}

@Composable
private fun EditEntryDialog(
    entry: MoneyEntry,
    onDismiss: () -> Unit,
    onSave: (MoneyEntry) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(entry.title) }
    var amount by remember { mutableStateOf(entry.amount.toString()) }
    var category by remember { mutableStateOf(entry.category) }
    var paymentMethod by remember { mutableStateOf(entry.paymentMethod) }
    var timestamp by remember { mutableLongStateOf(entry.time) }
    var photoUri by remember { mutableStateOf(entry.photoUri) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) photoUri = persistPhoto(context, uri.toString()) ?: photoUri
    }
    val cal = remember(timestamp) { Calendar.getInstance().apply { timeInMillis = timestamp } }
    val dateLabel = String.format(Locale.getDefault(), "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR))
    val timeLabel = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Edit transaction", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(paymentMethod, { paymentMethod = it }, label = { Text("Account / payment method") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        DatePickerDialog(context, { _, y, m, d ->
                            timestamp = Calendar.getInstance().apply { timeInMillis = timestamp; set(y,m,d) }.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("📅 $dateLabel", fontSize = 12.sp) }
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            timestamp = Calendar.getInstance().apply { timeInMillis = timestamp; set(Calendar.HOUR_OF_DAY,h); set(Calendar.MINUTE,m) }.timeInMillis
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("🕐 $timeLabel", fontSize = 12.sp) }
                }
                OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text(if (photoUri == null) "📷 Add / replace receipt" else "✓ Receipt attached")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = { onSave(entry.copy(title = title.trim(), amount = amount.toDouble(), category = category.ifBlank { "Other" }, paymentMethod = paymentMethod.ifBlank { "Cash" }, time = timestamp, photoUri = photoUri)) }
            ) { Text("Save changes", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(
    type: EntryType,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, Long, String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var fromAccount by remember { mutableStateOf("Cash") }
    var toAccount by remember { mutableStateOf("Bank") }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }
    val categories = if (type == EntryType.INCOME) listOf("Salary", "Business", "Gift", "Refund", "Other") else listOf("Food", "Travel", "Bills", "Shopping", "Fuel", "Medical", "Entertainment", "Other")
    val paymentMethods = listOf("Cash", "UPI", "Bank", "Card", "Wallet")
    val accounts = listOf("Cash", "Bank", "UPI", "Card", "Wallet")
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    val selectedCalendar = remember(timestamp) { Calendar.getInstance().apply { timeInMillis = timestamp } }
    val dateLabel = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedCalendar.get(Calendar.DAY_OF_MONTH), selectedCalendar.get(Calendar.MONTH) + 1, selectedCalendar.get(Calendar.YEAR))
    val timeLabel = String.format(Locale.getDefault(), "%02d:%02d", selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(when (type) { EntryType.INCOME -> "Add income"; EntryType.TRANSFER -> "Transfer money"; else -> "Add expense" }, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (type == EntryType.TRANSFER) {
                    ExposedDropdownMenuBox(expanded = showFromMenu, onExpandedChange = { showFromMenu = !showFromMenu }) {
                        OutlinedTextField(fromAccount, {}, readOnly = true, label = { Text("From account") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showFromMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = showFromMenu, onDismissRequest = { showFromMenu = false }) {
                            accounts.forEach { a -> DropdownMenuItem(text = { Text(a) }, onClick = { fromAccount = a; showFromMenu = false }) }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = showToMenu, onExpandedChange = { showToMenu = !showToMenu }) {
                        OutlinedTextField(toAccount, {}, readOnly = true, label = { Text("To account") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showToMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = showToMenu, onDismissRequest = { showToMenu = false }) {
                            accounts.forEach { a -> DropdownMenuItem(text = { Text(a) }, onClick = { toAccount = a; showToMenu = false }) }
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(expanded = showCategoryMenu, onExpandedChange = { showCategoryMenu = !showCategoryMenu }) {
                        OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) { categories.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; showCategoryMenu = false }) } }
                    }
                    ExposedDropdownMenuBox(expanded = showPaymentMenu, onExpandedChange = { showPaymentMenu = !showPaymentMenu }) {
                        OutlinedTextField(paymentMethod, {}, readOnly = true, label = { Text("Payment method") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showPaymentMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = showPaymentMenu, onDismissRequest = { showPaymentMenu = false }) { paymentMethods.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { paymentMethod = p; showPaymentMenu = false }) } }
                    }
                }

                Text("Transaction details", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                val c = Calendar.getInstance().apply { timeInMillis = timestamp; set(y, m, d) }
                                timestamp = c.timeInMillis
                            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("📅 $dateLabel", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                val c = Calendar.getInstance().apply { timeInMillis = timestamp; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                                timestamp = c.timeInMillis
                            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("🕐 $timeLabel", fontSize = 12.sp) }
                }

                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (photoUri == null) "📷 Add receipt / photo (optional)" else "✓ Receipt photo attached")
                }
                if (photoUri != null) {
                    val bitmap = remember(photoUri) {
                        runCatching { context.contentResolver.openInputStream(Uri.parse(photoUri!!))?.use(BitmapFactory::decodeStream) }.getOrNull()
                    }
                    if (bitmap != null) {
                        Image(bitmap.asImageBitmap(), contentDescription = "Selected receipt", modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0, onClick = {
                onSave(
                    title.trim(),
                    amount.toDouble(),
                    if (type == EntryType.TRANSFER) "$fromAccount → $toAccount" else category.ifBlank { if (type == EntryType.INCOME) "Income" else "Purchase" },
                    if (type == EntryType.TRANSFER) "Transfer" else paymentMethod,
                    timestamp,
                    photoUri
                )
            }) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private const val PREFS_NAME = "veera_storage"
private const val ENTRIES_KEY = "entries_json"

private fun saveEntries(context: Context, entries: List<MoneyEntry>) {
    val array = org.json.JSONArray()
    entries.forEach { entry ->
        val obj = org.json.JSONObject()
            .put("id", entry.id)
            .put("title", entry.title)
            .put("amount", entry.amount)
            .put("type", entry.type.name)
            .put("category", entry.category)
            .put("paymentMethod", entry.paymentMethod)
            .put("time", entry.time)
        if (entry.photoUri != null) obj.put("photoUri", entry.photoUri) else obj.put("photoUri", org.json.JSONObject.NULL)
        array.put(obj)
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(ENTRIES_KEY, array.toString())
        .apply()
}

private fun loadEntries(context: Context): List<MoneyEntry> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(ENTRIES_KEY, null) ?: return emptyList()
    return runCatching {
        val array = org.json.JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    MoneyEntry(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        type = EntryType.valueOf(obj.getString("type")),
                        category = obj.getString("category"),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        time = obj.getLong("time"),
                        photoUri = if (obj.isNull("photoUri")) null else obj.getString("photoUri")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun persistPhoto(context: Context, sourceUri: String): String? {
    return runCatching {
        val source = Uri.parse(sourceUri)
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Unable to read selected image" }
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file).toString()
    }.getOrNull()
}

private fun buildBackupJson(entries: List<MoneyEntry>): String {
    val array = org.json.JSONArray()
    entries.forEach { e ->
        array.put(org.json.JSONObject().put("id", e.id).put("title", e.title).put("amount", e.amount).put("type", e.type.name).put("category", e.category).put("paymentMethod", e.paymentMethod).put("time", e.time).put("photoUri", e.photoUri ?: org.json.JSONObject.NULL))
    }
    return org.json.JSONObject().put("app", "Veera").put("version", 2).put("entries", array).toString(2)
}

private fun parseBackupJson(raw: String): List<MoneyEntry> {
    val array = org.json.JSONObject(raw).getJSONArray("entries")
    return buildList { for (i in 0 until array.length()) {
        val o = array.getJSONObject(i)
        add(MoneyEntry(o.getLong("id"), o.getString("title"), o.getDouble("amount"), EntryType.valueOf(o.getString("type")), o.getString("category"), o.optString("paymentMethod", "Cash"), o.getLong("time"), if (o.isNull("photoUri")) null else o.getString("photoUri")))
    } }
}

private fun dayKey(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

private fun dayLabel(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        dayKey(timestamp) == dayKey(today.timeInMillis) -> "Today"
        dayKey(timestamp) == dayKey(yesterday.timeInMillis) -> "Yesterday"
        else -> String.format(Locale.getDefault(), "%02d %s %04d", c.get(Calendar.DAY_OF_MONTH), c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), c.get(Calendar.YEAR))
    }
}

private fun monthLabel(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    return String.format(Locale.getDefault(), "%s %04d", c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()), c.get(Calendar.YEAR))
}

private fun filteredEntries(entries: List<MoneyEntry>, mode: String, selectedTime: Long): List<MoneyEntry> {
    if (mode == "All") return entries
    val selected = Calendar.getInstance().apply { timeInMillis = selectedTime }
    return entries.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.time }
        if (mode == "Day") {
            c.get(Calendar.YEAR) == selected.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
        } else {
            c.get(Calendar.YEAR) == selected.get(Calendar.YEAR) && c.get(Calendar.MONTH) == selected.get(Calendar.MONTH)
        }
    }
}

private fun isAppLockEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("app_lock", false)
private fun setAppLockEnabled(context: Context, enabled: Boolean) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("app_lock", enabled).apply() }

private fun loadGoal(context: Context): Double = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("goal", "0")?.toDoubleOrNull() ?: 0.0
private fun saveGoal(context: Context, goal: Double) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("goal", goal.toString()).apply() }
private fun loadGoalSaved(context: Context): Double = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("goal_saved", "0")?.toDoubleOrNull() ?: 0.0
private fun saveGoalSaved(context: Context, amount: Double) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("goal_saved", amount.toString()).apply() }
private fun loadInvestmentTarget(context: Context): Double = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("investment_target", "0")?.toDoubleOrNull() ?: 0.0
private fun saveInvestmentTarget(context: Context, amount: Double) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("investment_target", amount.toString()).apply() }
private fun loadInvestmentSaved(context: Context): Double = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("investment_saved", "0")?.toDoubleOrNull() ?: 0.0
private fun saveInvestmentSaved(context: Context, amount: Double) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("investment_saved", amount.toString()).apply() }

private const val GOAL_CHANNEL = "veera_goal"
private const val GOAL_REQUEST = 7744
private const val INVESTMENT_REQUEST = 7745

private fun requestNotificationPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= 33 && context is Activity && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        context.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7745)
    }
}

private fun scheduleGoalReminder(context: Context, goal: Double) {
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, GoalReminderReceiver::class.java).putExtra("goal", goal)
    val pending = PendingIntent.getBroadcast(context, GOAL_REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1) }
    alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
}

private fun scheduleInvestmentReminder(context: Context, target: Double) {
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, GoalReminderReceiver::class.java).putExtra("investment", true).putExtra("goal", target)
    val pending = PendingIntent.getBroadcast(context, INVESTMENT_REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 30); set(Calendar.SECOND, 0); if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1) }
    alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
}

private fun cancelInvestmentReminder(context: Context) {
    val intent = Intent(context, GoalReminderReceiver::class.java)
    val pending = PendingIntent.getBroadcast(context, INVESTMENT_REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
}

private fun cancelGoalReminder(context: Context) {
    val intent = Intent(context, GoalReminderReceiver::class.java)
    val pending = PendingIntent.getBroadcast(context, GOAL_REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
}

class GoalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val goal = intent?.getDoubleExtra("goal", loadGoal(context)) ?: loadGoal(context)
        val investment = intent?.getBooleanExtra("investment", false) ?: false
        if (goal <= 0) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(GOAL_CHANNEL, "Veera Savings Goals", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = Notification.Builder(context, GOAL_CHANNEL)
            .setSmallIcon(com.veera.expense.R.drawable.ic_veera_logo)
            .setContentTitle(if (investment) "Veera • Investment goal" else "Veera • Savings goal")
            .setContentText(if (investment) "Investment target ₹${String.format(Locale.getDefault(), "%,.0f", goal)}. Update your invested amount separately from your cash balance." else "Savings target ₹${String.format(Locale.getDefault(), "%,.0f", goal)}. Update your saved amount separately from your cash balance.")
            .setAutoCancel(true)
            .build()
        manager.notify(GOAL_REQUEST, notification)
    }
}

private fun writePdf(context: Context, uri: Uri, entries: List<MoneyEntry>) {
    runCatching {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(22, 26, 43); textSize = 22f; isFakeBoldText = true }
        canvas.drawText("VEERA • Financial Report", 36f, 50f, paint)
        paint.textSize = 11f; paint.isFakeBoldText = false
        canvas.drawText("Generated ${formatDateTime(System.currentTimeMillis())}", 36f, 70f, paint)
        var y = 102f
        entries.sortedByDescending { it.time }.take(45).forEach { e ->
            val line = "${formatDateTime(e.time)}  •  ${e.type}  •  ${e.title.take(28)}  •  ₹${String.format(Locale.getDefault(), "%,.2f", e.amount)}"
            canvas.drawText(line, 36f, y, paint); y += 16f
            if (y > 805f) return@forEach
        }
        doc.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
    }
}

private fun formatDateTime(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    val date = String.format(Locale.getDefault(), "%02d/%02d/%04d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
    val time = String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    return "$date • $time"
}

private fun money(value: Double): String = "₹" + String.format(Locale.getDefault(), "%,.2f", value)
