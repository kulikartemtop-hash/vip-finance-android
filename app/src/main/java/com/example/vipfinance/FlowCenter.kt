package com.example.vipfinance

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FlowCenter(
    incomeMode: Boolean,
    transactions: List<Transaction>,
    accounts: List<Account>,
    categories: List<Category>,
    currency: String,
    auto: Boolean,
    rates: Map<String, Double>,
    onAdd: () -> Unit,
    onVoiceAdd: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onRemove: (Transaction) -> Unit,
    onUpdate: (Transaction, Transaction) -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var range by remember { mutableStateOf("Месяц") }
    var categoryFilter by remember { mutableStateOf("Все") }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }
    var attachTarget by remember { mutableStateOf<Transaction?>(null) }
    var exportText by remember { mutableStateOf("") }
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val original = attachTarget
        if (uri != null && original != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val newNote = listOf(original.note.trim(), "Чек: ${uri}").filter { it.isNotBlank() }.joinToString("\n")
            onUpdate(original, original.copy(note = newNote))
        }
        attachTarget = null
    }
    var voiceMessage by remember { mutableStateOf("") }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val candidates = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        val spoken = candidates.firstOrNull().orEmpty()
        val parsed = parseVoiceTransaction(spoken, accounts, categories, System.currentTimeMillis())
        when {
            parsed == null -> voiceMessage = "Не удалось определить операцию. Назовите сумму, категорию и счёт или карту."
            parsed.income != incomeMode -> voiceMessage = "Фраза распознана как " + (if (parsed.income) "доход" else "расход") + ". Откройте соответствующую вкладку."
            else -> { onVoiceAdd(parsed); voiceMessage = "Добавлено: " + parsed.title + " • " + money(parsed.amount, parsed.currency) + " • " + parsed.category }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(exportText) }
        }
    }
    val now = Calendar.getInstance()
    val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val rangeStart = when (range) {
        "7 дней" -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        "30 дней" -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -29); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        else -> monthStart
    }
    val typed = transactions.filter { it.operationType != "transfer" && it.income == incomeMode }
    val recurring = typed.filter { it.repeat.isNotBlank() && it.repeat != "Не повторять" }
    val period = typed.filter { it.timestamp >= rangeStart && it.timestamp <= now.timeInMillis }
    val filtered = (if (showRecurring) recurring else period).filter {
        (search.isBlank() || it.title.contains(search, true) || it.category.contains(search, true) || it.accountName.contains(search, true) || it.note.contains(search, true)) &&
        (categoryFilter == "Все" || it.category == categoryFilter)
    }.sortedByDescending { it.timestamp }
    val total = period.sumOf { conv(it.amount, it.currency, currency, auto, rates) }
    val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val todayTotal = typed.filter { it.timestamp >= todayStart && it.timestamp <= now.timeInMillis }.sumOf { conv(it.amount, it.currency, currency, auto, rates) }
    val categoryTotals = period.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { conv(it.amount, it.currency, currency, auto, rates) } }.toList().sortedByDescending { it.second }
    val peak = categoryTotals.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val elapsedDays = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysRemaining = (daysInMonth - elapsedDays).coerceAtLeast(0)
    val projectedTotal = if (total > 0) total / elapsedDays * daysInMonth else 0.0
    val projectedDaily = if (elapsedDays > 0) total / elapsedDays else 0.0
    val balance = accounts.filter { !it.hidden }.sumOf { conv(it.balance, it.currency, currency, auto, rates) }
    val projectedBalance = if (incomeMode) balance + projectedDaily * daysRemaining else balance - projectedDaily * daysRemaining
    val kindLabel = if (incomeMode) "дохода" else "расхода"
    val accent = if (incomeMode) Color(0xFF16845B) else MaterialTheme.colorScheme.error
    val title = if (incomeMode) "Центр доходов" else "Центр расходов"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Управление и аналитика ${if (incomeMode) "поступлений" else "трат"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(15.dp), color = accent.copy(alpha = 0.13f)) {
                            Text(if (incomeMode) "↗" else "↘", modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp), color = accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(money(total, currency), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = accent)
                    Text("За выбранный период • сегодня: ${money(todayTotal, currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text(if (incomeMode) "+ Добавить доход" else "− Добавить расход") }
                        OutlinedButton(onClick = {
                            val rows = filtered
                            exportText = buildString {
                                appendLine("Дата;Название;Сумма;Валюта;Категория;Счёт/карта;Комментарий;Повтор")
                                rows.forEach { t ->
                                    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(t.timestamp))
                                    appendLine(listOf(date, t.title, t.amount.toString(), t.currency, t.category, t.accountName, t.note.replace("\n", " "), t.repeat).joinToString(";") { it.replace(";", ",") })
                                }
                            }
                            exportPicker.launch(if (incomeMode) "VIP-Finance-income.csv" else "VIP-Finance-expenses.csv")
                        }, modifier = Modifier.weight(1f)) { Text("Экспорт CSV") }
                    }
                    OutlinedButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, if (incomeMode) "Скажите: получил зарплату 50000 на счёт" else "Скажите: потратил 2800 на бензин с карты")
                        }
                        runCatching { voiceLauncher.launch(intent) }.onFailure { voiceMessage = "Голосовой ввод недоступен на устройстве." }
                    }, modifier = Modifier.fillMaxWidth()) { Text("🎙 Добавить голосом") }
                    if (voiceMessage.isNotBlank()) Text(voiceMessage, style = MaterialTheme.typography.bodySmall, color = accent)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Месяц", "7 дней", "30 дней").forEach { preset ->
                    FilterChip(selected = range == preset && !showRecurring, onClick = { range = preset; showRecurring = false }, label = { Text(preset) })
                }
                FilterChip(selected = showRecurring, onClick = { showRecurring = !showRecurring }, label = { Text("Регулярные") })
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowMetricCard("Операций", if (showRecurring) recurring.size.toString() else period.size.toString(), accent, Modifier.weight(1f))
                FlowMetricCard("Средняя сумма", money(if (period.isEmpty()) 0.0 else total / period.size, currency), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Прогноз до конца месяца", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(money(projectedTotal, currency), style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Bold)
                    Text("Оценка по среднему ${kindLabel} в день за текущий месяц. Это прогноз, а не гарантированное поступление или списание.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!incomeMode) Text("Примерный баланс после расходов по текущему темпу: ${money(projectedBalance, currency)}", fontWeight = FontWeight.SemiBold, color = if (projectedBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    else Text("До конца месяца осталось дней: $daysRemaining", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!showRecurring && categoryTotals.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (incomeMode) "Источники дохода" else "Куда уходят деньги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        categoryTotals.take(6).forEach { (name, amount) ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontWeight = FontWeight.SemiBold)
                                    Text(money(amount, currency), fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(progress = { (amount / peak).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = accent)
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Умные подсказки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val top = categoryTotals.firstOrNull()
                        if (top != null) Text((if (incomeMode) "Главный источник" else "Самая затратная категория") + ": ${top.first} • ${money(top.second, currency)}")
                        if (!incomeMode && total > 0 && projectedTotal > total * 1.2) Text("При сохранении темпа расходы могут превысить текущую сумму ещё на ${money(projectedTotal - total, currency)}.", color = MaterialTheme.colorScheme.error)
                        if (incomeMode && total > 0) Text("Планируйте бюджет на основе уже полученных сумм; прогноз не меняет баланс счёта.")
                        if (categoryTotals.size <= 1) Text("Добавляйте операции с разными категориями, чтобы получить более точную аналитику.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Поиск по операциям") }, placeholder = { Text("Название, категория, счёт или комментарий") })
        }
        item {
            val categoryOptions = (categories.filter { it.kind == if (incomeMode) "income" else "expense" }.map { it.name } + typed.map { it.category })
                .filter { it.isNotBlank() }.distinct()
            val topCategoryNames = categoryTotals.take(3).map { it.first }.filter { it in categoryOptions }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Фильтр по категории", fontWeight = FontWeight.Bold)
                            Text(if (categoryFilter == "Все") "Показываются все категории" else "Выбрано: $categoryFilter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { showCategoryPicker = true }, shape = RoundedCornerShape(14.dp)) {
                            Text("⌕ Найти")
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = categoryFilter == "Все", onClick = { categoryFilter = "Все" }, label = { Text("Все") })
                        topCategoryNames.forEach { name ->
                            FilterChip(selected = categoryFilter == name, onClick = { categoryFilter = name }, label = { Text(name) })
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (showRecurring) "Регулярные операции" else "История операций", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${filtered.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (filtered.isEmpty()) item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(if (showRecurring) "Регулярных операций пока нет" else "Операций пока нет", fontWeight = FontWeight.SemiBold)
                    Text(if (showRecurring) "При добавлении операции выберите периодичность, чтобы она появилась здесь." else "Добавьте первую операцию — здесь появятся история, категории и прогноз.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onAdd) { Text(if (incomeMode) "Добавить доход" else "Добавить расход") }
                }
            }
        }
        items(filtered, key = { "${it.id}-${it.timestamp}" }) { t ->
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title.ifBlank { t.category }, fontWeight = FontWeight.Bold)
                            Text("${t.category} • ${t.accountName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(t.timestamp)) + if (t.repeat != "Не повторять") " • ${t.repeat}" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(conv(t.amount, t.currency, currency, auto, rates), currency), color = accent, fontWeight = FontWeight.Bold)
                    }
                    if (t.note.isNotBlank()) Text(t.note.substringBefore("\nЧек:"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val receiptUri = Regex("Чек:\\s*(content://[^\\s]+|file://[^\\s]+)").find(t.note)?.groupValues?.getOrNull(1)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onEdit(t) }) { Text("Изменить") }
                        TextButton(onClick = { attachTarget = t; receiptPicker.launch(arrayOf("image/*", "application/pdf")) }) { Text(if (receiptUri != null) "Заменить чек" else "Прикрепить чек") }
                        if (receiptUri != null) TextButton(onClick = {
                            runCatching {
                                val uri = Uri.parse(receiptUri)
                                val mime = context.contentResolver.getType(uri) ?: "*/*"
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            }
                        }) { Text("Открыть чек") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { onRemove(t) }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (showCategoryPicker) {
        val options = (categories.filter { it.kind == if (incomeMode) "income" else "expense" }.map { it.name } + typed.map { it.category })
            .filter { it.isNotBlank() }.distinct()
        val counts = period.groupBy { it.category }.mapValues { (_, rows) -> rows.sumOf { conv(it.amount, it.currency, currency, auto, rates) } }
        CategoryFilterDialog(
            incomeMode = incomeMode,
            categories = options,
            selectedName = categoryFilter,
            amounts = counts,
            onClose = { showCategoryPicker = false },
            onSelect = { categoryFilter = it; showCategoryPicker = false }
        )
    }
}


@Composable
private fun CategoryFilterDialog(
    incomeMode: Boolean,
    categories: List<String>,
    selectedName: String,
    amounts: Map<String, Double>,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val aliases = mapOf(
        "Продукты" to listOf("еда", "магазин", "супермаркет", "питание", "покупка еды"),
        "Топливо" to listOf("бензин", "заправка", "дизель", "азс", "газ"),
        "Автомобиль" to listOf("авто", "машина", "мойка", "страховка", "транспорт"),
        "Здоровье" to listOf("аптека", "врач", "лекарства", "медицина"),
        "Жильё" to listOf("аренда", "квартира", "коммунальные", "жкх", "свет", "вода"),
        "Одежда и обувь" to listOf("одежда", "обувь", "куртка", "кроссовки"),
        "Рестораны и кафе" to listOf("кафе", "ресторан", "обед", "доставка еды"),
        "Транспорт" to listOf("такси", "автобус", "метро", "проезд"),
        "Зарплата" to listOf("оклад", "аванс", "работа", "зарплатный перевод"),
        "Премия" to listOf("бонус", "премиальные"),
        "Подработка" to listOf("фриланс", "халтура", "дополнительная работа"),
        "Бизнес" to listOf("выручка", "прибыль", "предпринимательство"),
        "Инвестиции" to listOf("акции", "дивиденды", "вложения"),
        "Проценты по вкладам" to listOf("вклад", "депозит", "проценты"),
        "Кэшбэк" to listOf("кешбек", "cashback", "возврат банка"),
        "Подарки" to listOf("подарок", "дарение"),
        "Возврат средств" to listOf("возврат", "вернули деньги", "рефанд"),
        "Продажа вещей" to listOf("продажа", "авито", "продал")
    )
    val q = query.trim().lowercase(Locale.getDefault())
    val filtered = categories.filter { name ->
        q.isBlank() || name.contains(q, true) ||
            aliases[name].orEmpty().any { it.contains(q, true) || q.contains(it.lowercase(Locale.getDefault())) }
    }.sortedWith(compareByDescending<String> { amounts[it] ?: 0.0 }.thenBy { it.lowercase(Locale.getDefault()) })
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.93f).heightIn(max = 690.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (incomeMode) "Категории доходов" else "Категории расходов", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Сначала самые используемые. Ищите по названию или смыслу — например, «бензин» или «зарплата».", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Найти категорию") },
                    placeholder = { Text(if (incomeMode) "Зарплата, бонус, возврат…" else "Еда, бензин, аптека…") },
                    trailingIcon = { if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Очистить") } }
                )
                Text("${filtered.size} найдено", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect("Все") },
                            shape = RoundedCornerShape(15.dp),
                            color = if (selectedName == "Все") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Все категории", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                if (selectedName == "Все") Text("✓", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(filtered) { name ->
                        val selected = selectedName == name
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(name) },
                            shape = RoundedCornerShape(15.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(name, fontWeight = FontWeight.SemiBold)
                                    Text(if ((amounts[name] ?: 0.0) > 0.0) "За период: " + money(amounts[name] ?: 0.0, "RUB") else "Нажмите, чтобы показать только эту категорию",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Закрыть") }
            }
        }
    }
}


@Composable
private fun FlowMetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}
