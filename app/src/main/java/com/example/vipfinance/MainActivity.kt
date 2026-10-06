package com.example.vipfinance

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.vipfinance.ui.theme.VIPFinanceTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val currencies=listOf("RUB","GBP","EUR","USD","CNY","JPY","CHF","CAD","AUD","PLN","BYN")
private val currencyCountries=mapOf(
    "RUB" to "Россия", "GBP" to "Великобритания", "EUR" to "Еврозона", "USD" to "США",
    "CNY" to "Китай", "JPY" to "Япония", "CHF" to "Швейцария", "CAD" to "Канада",
    "AUD" to "Австралия", "PLN" to "Польша", "BYN" to "Беларусь"
)
private fun currencyLabel(code:String)=code+" ("+(currencyCountries[code] ?: "—")+")"
private val pages=listOf("Главная","Операции","Счета","Категории","Бюджеты","Аналитика","Календарь","Конвертер","Долги","Цели","Напоминания","Чеки")
private val defaultCategories=listOf(
    Category(name="Продукты",icon="shopping_cart",color="#43A047"), Category(name="Транспорт",icon="directions_car",color="#1E88E5"),
    Category(name="Жильё",icon="home",color="#8E24AA"), Category(name="Зарплата",icon="payments",color="#00897B"),
    Category(name="Развлечения",icon="movie",color="#FB8C00"), Category(name="Здоровье",icon="favorite",color="#E53935"),
    Category(name="Покупки",icon="shopping_bag",color="#6D4C41"), Category(name="Связь",icon="phone",color="#3949AB"),
    Category(name="Подписки",icon="subscriptions",color="#5E35B1"), Category(name="Образование",icon="school",color="#039BE5"),
    Category(name="Путешествия",icon="flight",color="#00ACC1"), Category(name="Другое",icon="category",color="#757575")
)
private val iconChoices=listOf("account_balance","credit_card","wallet","savings","shopping_cart","home","directions_car","payments","favorite","phone","school","flight","movie","subscriptions","category")
private val colorChoices=listOf("#5B35F5","#00A7B5","#007A5A","#E53935","#FB8C00","#1E88E5","#8E24AA","#D81B60","#6D4C41","#757575")
private fun iconText(icon:String)=when(icon){"account_balance"->"▥";"credit_card"->"▣";"wallet"->"◫";"savings"->"◉";"shopping_cart"->"🛒";"home"->"⌂";"directions_car"->"🚗";"payments"->"₽";"favorite"->"♥";"phone"->"☎";"school"->"◆";"flight"->"✈";"movie"->"▶";"subscriptions"->"◉";"shopping_bag"->"▱";else->"•"}
private fun uiColor(hex:String)=runCatching{Color(android.graphics.Color.parseColor(hex))}.getOrDefault(Color(0xFF5B35F5))

private fun sym(c:String)=when(c){"GBP"->"£";"USD"->"$";"EUR"->"€";"RUB"->"₽";"CNY"->"¥";"JPY"->"¥";"CHF"->"Fr";"CAD"->"C$";"AUD"->"A$";"PLN"->"zł";"BYN"->"Br";else->c}
private fun money(v:Double,c:String)=sym(c)+"%.2f".format(Locale.getDefault(),v)
private fun conv(v:Double,from:String,to:String,auto:Boolean,r:Map<String,Double>)=if(auto)ExchangeRates.convert(v,from,to,r) else v

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);enableEdgeToEdge();val s=FinanceStore(this);setContent{VIPFinanceTheme(s.loadTheme(),s.loadStyle()){FinanceApp(s)}}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(s: FinanceStore) {
    var accounts by remember { mutableStateOf(s.loadAccounts()) }
    var tx by remember { mutableStateOf(s.loadTransactions()) }
    var debts by remember { mutableStateOf(s.loadDebts()) }
    var goals by remember { mutableStateOf(s.loadGoals()) }
    var reminders by remember { mutableStateOf(s.loadReminders()) }
    var budgets by remember { mutableStateOf(s.loadBudgets()) }
    var categories by remember { mutableStateOf(s.loadCategories().ifEmpty { defaultCategories }) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }
    var page by remember { mutableStateOf("Главная") }
    var currency by remember { mutableStateOf(s.loadCurrency()) }
    var auto by remember { mutableStateOf(s.loadAutoConversion()) }
    var theme by remember { mutableStateOf(s.loadTheme()) }
    var style by remember { mutableStateOf(s.loadStyle()) }
    var menu by remember { mutableStateOf(s.loadMenu().filter { it in pages }.toSet().ifEmpty { pages.toSet() }) }
    var rates by remember { mutableStateOf(s.loadRates()) }
    var rateTime by remember { mutableStateOf(s.loadRatesTime()) }
    var loading by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var repeatSource by remember { mutableStateOf<Transaction?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    var newest by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var tagSearch by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("Все") }
    var categoryFilter by remember { mutableStateOf("Все") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var receiptText by remember { mutableStateOf("") }
    var receiptDraft by remember { mutableStateOf<Transaction?>(null) }
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(if (drawerOpen) DrawerValue.Open else DrawerValue.Closed)

    LaunchedEffect(drawerOpen) { if (drawerOpen) drawerState.open() else drawerState.close() }
    LaunchedEffect(menu) { if (page !in menu && menu.isNotEmpty()) page = menu.first() }
    LaunchedEffect(Unit) {
        if (s.loadCategories().isEmpty()) s.saveCategories(categories)
        loading = true
        runCatching { ExchangeRates.loadEcbRates() }.onSuccess {
            rates = it
            s.saveRates(it)
            rateTime = s.loadRatesTime()
        }
        loading = false
    }

    fun add(t: Transaction) {
        tx = tx + t
        s.saveTransactions(tx)
        val source = accounts.firstOrNull { it.name == t.accountName }
        if (t.operationType == "transfer") {
            val target = accounts.firstOrNull { it.name == t.toAccountName }
            if (source != null && target != null) {
                val converted = ExchangeRates.convert(t.amount, source.currency, target.currency, rates)
                accounts = accounts.map { when (it.name) {
                    source.name -> it.copy(balance = it.balance - t.amount)
                    target.name -> it.copy(balance = it.balance + converted)
                    else -> it
                }}
                s.saveAccounts(accounts)
            }
        } else if (source != null) {
            val delta = if (t.income) t.amount else -t.amount
            accounts = accounts.map { if (it.name == t.accountName) it.copy(balance = it.balance + delta) else it }
            s.saveAccounts(accounts)
        }
    }

    fun remove(t: Transaction) {
        tx = tx.filterNot { it.id == t.id }
        s.saveTransactions(tx)
        val source = accounts.firstOrNull { it.name == t.accountName }
        if (t.operationType == "transfer") {
            val target = accounts.firstOrNull { it.name == t.toAccountName }
            if (source != null && target != null) {
                val converted = ExchangeRates.convert(t.amount, source.currency, target.currency, rates)
                accounts = accounts.map { when (it.name) {
                    source.name -> it.copy(balance = it.balance + t.amount)
                    target.name -> it.copy(balance = it.balance - converted)
                    else -> it
                }}
                s.saveAccounts(accounts)
            }
        } else if (source != null) {
            val delta = if (t.income) -t.amount else t.amount
            accounts = accounts.map { if (it.name == t.accountName) it.copy(balance = it.balance + delta) else it }
            s.saveAccounts(accounts)
        }
    }

    val visible = accounts.filter { !it.hidden }
    val total = visible.sumOf { conv(it.balance, it.currency, currency, auto, rates) }
    val fromMillis = parseDateStart(fromDate)
    val toMillis = parseDateEnd(toDate)
    val shown0 = filter?.let { name -> tx.filter { it.accountName == name } } ?: tx
    val shown = shown0.filter { t ->
        val textMatch = search.isBlank() || t.title.contains(search, true) || t.category.contains(search, true) || t.accountName.contains(search, true) || t.note.contains(search, true)
        val tagMatch = tagSearch.isBlank() || t.tags.split(",").map { it.trim() }.any { it.contains(tagSearch.trim(), true) }
        val typeMatch = when (typeFilter) {
            "Доход" -> t.income && t.operationType != "transfer"
            "Расход" -> !t.income && t.operationType != "transfer"
            "Перевод" -> t.operationType == "transfer"
            else -> true
        }
        val categoryMatch = categoryFilter == "Все" || t.category == categoryFilter
        val dateMatch = (fromMillis == null || t.timestamp >= fromMillis) && (toMillis == null || t.timestamp <= toMillis)
        textMatch && tagMatch && typeMatch && categoryMatch && dateMatch
    }
    val inc = shown.filter { it.income && it.operationType != "transfer" }.sumOf { conv(it.amount, it.currency, currency, auto, rates) }
    val exp = shown.filter { !it.income && it.operationType != "transfer" }.sumOf { conv(it.amount, it.currency, currency, auto, rates) }

    fun saveSettings(c: String, a: Boolean, t: String, st: String, m: Set<String>) {
        currency = c; auto = a; theme = t; style = st; menu = m.filter { it in pages }.toSet()
        s.saveCurrency(c); s.saveAutoConversion(a); s.saveTheme(t); s.saveStyle(st); s.saveMenu(menu.toSet())
    }

    VIPFinanceTheme(theme = theme, style = style) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("VIP Finance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Ваши финансы под контролем", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                    Text("Разделы", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                    menu.forEach { item ->
                        NavigationDrawerItem(label = { Text(item) }, selected = page == item, onClick = { page = item; drawerOpen = false }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    NavigationDrawerItem(label = { Text("Настройки") }, selected = false, onClick = { dialog = "settings"; drawerOpen = false }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(page, fontWeight = FontWeight.Bold)
                                if (page == "Главная") Text("Финансовый обзор", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        navigationIcon = {
                            IconButton(onClick = { drawerOpen = true }) {
                                Text("☰", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        actions = {
                            IconButton(onClick = { dialog = "settings" }) {
                                Text("⚙", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 10.dp
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { dialog = "expense" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) { Text("− Расход") }
                            Button(
                                onClick = { dialog = "income" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) { Text("+ Доход") }
                        }
                    }
                }
            ) { pad ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(pad)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    when (page) {
                        "Главная" -> Home(total, currency, accounts, auto, rates, selected, inc, exp) { selected = it }
                        "Операции" -> Operations(shown, accounts, filter, { filter = it }, { dialog = "expense" }, ::remove, currency, auto, rates, search, { search = it }, tagSearch, { tagSearch = it }, newest, { newest = it }, typeFilter, { typeFilter = it }, categoryFilter, { categoryFilter = it }, fromDate, { fromDate = it }, toDate, { toDate = it }, categories.map { it.name }, { original -> repeatSource = original; dialog = if (original.income) "income" else "expense" })
                        "Счета" -> Accounts(accounts, currency, auto, rates, { dialog = "account" }, { selected = it }) { n -> val updated = accounts.map { if (it.name == n) it.copy(hidden = !it.hidden) else it }; accounts = updated; s.saveAccounts(updated) }
                        "Категории" -> Categories(categories, { editingCategory = null; dialog = "category" }, { editingCategory = it; dialog = "category" }, { c0 -> categories = categories.filterNot { it.id == c0.id }; s.saveCategories(categories) })
                        "Бюджеты" -> Budgets(budgets, tx, currency, auto, rates, accounts, { dialog = "budget" }) { b0 -> budgets = budgets.filterNot { it.id == b0.id }; s.saveBudgets(budgets) }
                        "Аналитика" -> Analytics(tx, currency, auto, rates)
                        "Календарь" -> SmartCalendar(tx, reminders, currency, auto, rates)
                        "Конвертер" -> Converter(currency, rates, loading, rateTime)
                        "Долги" -> Debts(debts, { dialog = "debt" }, { d -> editingDebt = d; dialog = "debtProgress" }) { d -> debts = debts.filterNot { it.id == d.id }; s.saveDebts(debts) }
                        "Цели" -> Goals(goals, { dialog = "goal" }, { g -> editingGoal = g; dialog = "goalProgress" }) { g -> goals = goals.filterNot { it.id == g.id }; s.saveGoals(goals) }
                        "Напоминания" -> Reminders(reminders, { dialog = "reminder" }, { r -> reminders = reminders.map { if (it.id == r.id) it.copy(done = !it.done) else it }; s.saveReminders(reminders) }, { r -> reminders = reminders.filterNot { it.id == r.id }; s.saveReminders(reminders) })
                        "Чеки" -> Receipt(receiptUri, receiptText, { u -> receiptUri = u; receiptText = "" }, { t -> receiptText = t }, accounts.firstOrNull(), categories) { draft -> receiptDraft = draft; dialog = "receiptExpense" }
                    }
                }
            }
        }
    }

    VIPFinanceTheme(theme = theme, style = style) {
        when (dialog) {
            "account" -> AccountDialog({ dialog = "" }) { n, b, t, c, icon, iconColor ->
                accounts = accounts + Account(n, b, false, t, c, icon, iconColor); s.saveAccounts(accounts); dialog = ""
            }
            "category" -> CategoryDialog(editingCategory, { dialog = ""; editingCategory = null }) { category ->
                categories = if (categories.any { it.id == category.id }) categories.map { if (it.id == category.id) category else it } else categories + category
                s.saveCategories(categories); dialog = ""; editingCategory = null
            }
            "expense" -> TransactionDialog(accounts, categories, false, repeatSource, { dialog = ""; repeatSource = null }) { add(it); dialog = ""; repeatSource = null }
            "income" -> TransactionDialog(accounts, categories, true, repeatSource, { dialog = ""; repeatSource = null }) { add(it); dialog = ""; repeatSource = null }
            "debt" -> DebtDialog({ dialog = "" }) { debts = debts + it; s.saveDebts(debts); dialog = "" }
            "debtProgress" -> DebtProgressDialog(editingDebt, { dialog = ""; editingDebt = null }) { updated -> debts = debts.map { if (it.id == updated.id) updated else it }; s.saveDebts(debts); dialog = ""; editingDebt = null }
            "goal" -> GoalDialog(currency, { dialog = "" }) { goals = goals + it; s.saveGoals(goals); dialog = "" }
            "goalProgress" -> GoalProgressDialog(editingGoal, { dialog = ""; editingGoal = null }) { updated -> goals = goals.map { if (it.id == updated.id) updated else it }; s.saveGoals(goals); dialog = ""; editingGoal = null }
            "reminder" -> ReminderDialog({ dialog = "" }) { reminders = reminders + it; s.saveReminders(reminders); dialog = "" }
            "budget" -> BudgetDialog(accounts, categories, currency, { dialog = "" }) { budgets = budgets + it; s.saveBudgets(budgets); dialog = "" }
            "receiptExpense" -> TransactionDialog(accounts, categories, false, receiptDraft, { dialog = ""; receiptDraft = null }) { add(it); dialog = ""; receiptDraft = null }
            "settings" -> SettingsDialog(currency, auto, theme, style, menu, rateTime, { c: String, a: Boolean, t: String, st: String, m: Set<String> -> saveSettings(c, a, t, st, m) }, { s.exportBackupJson() }) { dialog = "" }
        }
    }
}

@Composable
private fun Home(
    total: Double, c: String, accounts: List<Account>, auto: Boolean, r: Map<String,Double>,
    selected: String?, income: Double, expense: Double, pick: (String?) -> Unit
) {
    val visible = accounts.filter { !it.hidden }
    val selectedBalance = selected?.let { n -> visible.firstOrNull { it.name == n }?.let { conv(it.balance,it.currency,c,auto,r) } } ?: total
    val net = income - expense
    val spendRate = if (income > 0) (expense / income).coerceIn(0.0,1.0) else 0.0
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val flowColor = MaterialTheme.colorScheme.primary
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),elevation=CardDefaults.cardElevation(10.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.tertiary,MaterialTheme.colorScheme.secondary))).padding(22.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            Column {
                                Text(if(selected==null)"Общий баланс" else "Баланс • $selected",color=Color.White.copy(.78f),style=MaterialTheme.typography.labelLarge)
                                Text(money(selectedBalance,c),color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.ExtraBold)
                            }
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.16f)),contentAlignment=Alignment.Center){Text("₽",color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)}
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Surface(shape=RoundedCornerShape(14.dp),color=Color.White.copy(.12f),modifier=Modifier.weight(1f)){Column(Modifier.padding(12.dp)){Text("Доходы",color=Color.White.copy(.72f),style=MaterialTheme.typography.labelSmall);Text(money(income,c),color=Color.White,fontWeight=FontWeight.Bold)}}
                            Surface(shape=RoundedCornerShape(14.dp),color=Color.White.copy(.12f),modifier=Modifier.weight(1f)){Column(Modifier.padding(12.dp)){Text("Расходы",color=Color.White.copy(.72f),style=MaterialTheme.typography.labelSmall);Text(money(expense,c),color=Color.White,fontWeight=FontWeight.Bold)}}
                            Surface(shape=RoundedCornerShape(14.dp),color=Color.White.copy(.12f),modifier=Modifier.weight(1f)){Column(Modifier.padding(12.dp)){Text("Остаток",color=Color.White.copy(.72f),style=MaterialTheme.typography.labelSmall);Text(money(net,c),color=Color.White,fontWeight=FontWeight.Bold)}}
                        }
                    }
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(24.dp),elevation=CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Column { Text("Финансовый поток",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text("Доходы → расходы → остаток",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(money(net,c),fontWeight=FontWeight.Bold,color=if(net>=0)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    Canvas(modifier=Modifier.fillMaxWidth().height(12.dp)) {
                        val w=size.width
                        drawRoundRect(color=trackColor,cornerRadius=androidx.compose.ui.geometry.CornerRadius(20f),size=androidx.compose.ui.geometry.Size(w,size.height))
                        if(income>0) drawRoundRect(color=flowColor,cornerRadius=androidx.compose.ui.geometry.CornerRadius(20f),size=androidx.compose.ui.geometry.Size(w*spendRate.toFloat(),size.height))
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Расходовано ${(spendRate*100).toInt()}%",style=MaterialTheme.typography.labelSmall);Text("Осталось ${((1-spendRate)*100).toInt()}%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Счета",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("${visible.size}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)}
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){item{FilterChip(selected==null,{pick(null)},label={Text("Все счета")})};items(visible){a->FilterChip(selected==a.name,{pick(a.name)},label={Text(a.name)})}}
            }
        }
        item {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){items(visible){a->
                Card(modifier=Modifier.width(210.dp),shape=RoundedCornerShape(22.dp),elevation=CardDefaults.cardElevation(6.dp),colors=CardDefaults.cardColors(containerColor=uiColor(a.iconColor).copy(alpha=.92f))){
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(.16f)),contentAlignment=Alignment.Center){Text(iconText(a.icon),color=Color.White,fontWeight=FontWeight.Bold)};Text(a.currency,color=Color.White.copy(.78f),style=MaterialTheme.typography.labelSmall)}
                        Text(a.name,color=Color.White.copy(.82f),style=MaterialTheme.typography.bodySmall)
                        Text(money(conv(a.balance,a.currency,c,auto,r),c),color=Color.White,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleLarge)
                        Text(a.type,color=Color.White.copy(.68f),style=MaterialTheme.typography.labelSmall)
                    }
                }
            }}
        }
        item {
            Card(shape=RoundedCornerShape(24.dp),elevation=CardDefaults.cardElevation(4.dp)){
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("Бюджеты",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text("Контроль расходов",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("${(spendRate*100).toInt()}%",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
                    Text("Добавьте бюджет, чтобы видеть лимит, остаток и предупреждения прямо на главном экране.",style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard("Доступно",money(selectedBalance,c),MaterialTheme.colorScheme.primary,Modifier.weight(1f));MetricCard("Чистый поток",money(net,c),if(net>=0)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,Modifier.weight(1f))} }
    }
}

@Composable
private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(value, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun Operations(
    ts: List<Transaction>, accounts: List<Account>, filter: String?, setFilter: (String?) -> Unit,
    add: () -> Unit, remove: (Transaction) -> Unit, c: String, auto: Boolean, r: Map<String, Double>,
    search: String, setSearch: (String) -> Unit, tagSearch: String, setTagSearch: (String) -> Unit,
    newest: Boolean, setNewest: (Boolean) -> Unit, typeFilter: String, setTypeFilter: (String) -> Unit,
    categoryFilter: String, setCategoryFilter: (String) -> Unit, fromDate: String, setFromDate: (String) -> Unit,
    toDate: String, setToDate: (String) -> Unit, categories: List<String>, repeat: (Transaction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Доходы", money(ts.filter { it.income && it.operationType != "transfer" }.sumOf { conv(it.amount, it.currency, c, auto, r) }, c), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricCard("Расходы", money(ts.filter { !it.income && it.operationType != "transfer" }.sumOf { conv(it.amount, it.currency, c, auto, r) }, c), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                MetricCard("Операций", ts.size.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Операции", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold); Text("${ts.size} операций", style=MaterialTheme.typography.bodySmall) }
            FilledTonalButton(onClick=add){Text("+ Добавить")}
        }
        OutlinedTextField(search,setSearch,label={Text("Поиск операций")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(tagSearch,setTagSearch,label={Text("Поиск по тегу")},placeholder={Text("например: Авто")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Text("Тип операции",fontWeight=FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Все","Доход","Расход","Перевод").forEach{v->FilterChip(typeFilter==v,{setTypeFilter(v)},label={Text(v)})}}
        Text("Счёт",fontWeight=FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){FilterChip(filter==null,{setFilter(null)},label={Text("Все")});accounts.forEach{a->FilterChip(filter==a.name,{setFilter(a.name)},label={Text(a.name)})}}
        Text("Категория",fontWeight=FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){FilterChip(categoryFilter=="Все",{setCategoryFilter("Все")},label={Text("Все")});categories.distinct().forEach{v->FilterChip(categoryFilter==v,{setCategoryFilter(v)},label={Text(v)})}}
        Text("Период",fontWeight=FontWeight.SemiBold)
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(fromDate,setFromDate,label={Text("С даты")},placeholder={Text("дд.мм.гггг")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(toDate,setToDate,label={Text("По дату")},placeholder={Text("дд.мм.гггг")},modifier=Modifier.weight(1f),singleLine=true)}
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            val today=Calendar.getInstance()
            val fmt=SimpleDateFormat("dd.MM.yyyy",Locale.getDefault())
            FilterChip(fromDate==fmt.format(today.time)&&toDate==fmt.format(today.time),{val d=fmt.format(today.time);setFromDate(d);setToDate(d)},label={Text("Сегодня")})
            FilterChip(fromDate==fmt.format(Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,-6)}.time),{val start=Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,-6)};setFromDate(fmt.format(start.time));setToDate(fmt.format(today.time))},label={Text("7 дней")})
            FilterChip(fromDate==fmt.format(Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1)}.time),{val start=Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1)};setFromDate(fmt.format(start.time));setToDate(fmt.format(today.time))},label={Text("Месяц")})
            TextButton(onClick={setFromDate("");setToDate("");setTagSearch("");setTypeFilter("Все");setCategoryFilter("Все");setFilter(null)}){Text("Сбросить")}}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Сначала новые",Modifier.weight(1f));Switch(newest,setNewest)}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
            val ordered=if(newest)ts.sortedByDescending{it.timestamp}else ts.sortedBy{it.timestamp}
            itemsIndexed(ordered){index,t->
                val day=SimpleDateFormat("dd MMMM yyyy",Locale.getDefault()).format(Date(t.timestamp))
                val previousDay=ordered.getOrNull(index-1)?.let{SimpleDateFormat("dd MMMM yyyy",Locale.getDefault()).format(Date(it.timestamp))}
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    if(index==0||day!=previousDay) Text(day.replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=4.dp))
                val transfer=t.operationType=="transfer"
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(transfer)MaterialTheme.colorScheme.secondaryContainer.copy(.45f)else if(t.income)MaterialTheme.colorScheme.primaryContainer.copy(.42f)else MaterialTheme.colorScheme.surface)){
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if(transfer)MaterialTheme.colorScheme.secondaryContainer else if(t.income)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),contentAlignment=Alignment.Center){Text(if(transfer)"⇄"else if(t.income)"↗"else"↘",color=if(transfer)MaterialTheme.colorScheme.secondary else if(t.income)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.SemiBold);Text(if(transfer)"${t.accountName} → ${t.toAccountName}" else "${t.category} • ${t.accountName}",style=MaterialTheme.typography.bodySmall);if(t.tags.isNotBlank())Text("🏷 ${t.tags}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);if(t.note.isNotBlank())Text(t.note,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(t.timestamp)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);if(t.repeat!="Не повторять")Text("↻ ${t.repeat}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)}
                        Column(horizontalAlignment=Alignment.End){Text((if(transfer)"⇄"else if(t.income)"+"else"−")+" "+money(conv(t.amount,t.currency,c,auto,r),c),fontWeight=FontWeight.Bold,color=if(transfer)MaterialTheme.colorScheme.secondary else if(t.income)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error);Row{TextButton(onClick={repeat(t)}){Text("Повторить")};TextButton(onClick={remove(t)}){Text("Удалить")}}}
                    }
                }
            }
        }
    }
    }
}
@Composable
private fun Categories(
    items: List<Category>,
    add: () -> Unit,
    edit: (Category) -> Unit,
    remove: (Category) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Категории", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Стандартные и ваши категории", style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = add) { Text("+ Добавить") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { c0 ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(uiColor(c0.color)),
                            contentAlignment = Alignment.Center
                        ) { Text(iconText(c0.icon), color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Text(c0.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { edit(c0) }) { Text("Изменить") }
                        TextButton(onClick = { remove(c0) }) { Text("Удалить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Accounts(items: List<Account>, c: String, auto: Boolean, r: Map<String, Double>, add: () -> Unit, select: (String) -> Unit, toggle: (String) -> Unit) {
    val visible = items.filter { !it.hidden }
    val total = visible.sumOf { conv(it.balance, it.currency, c, auto, r) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Счета и карты", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(items.size.toString() + " финансовых объектов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            FilledTonalButton(onClick = add) { Text("+ Добавить") }
        }
        Card(shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(5.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Общий баланс", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(money(total, c), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Учитываются только открытые счета", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (items.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Счетов пока нет", fontWeight = FontWeight.SemiBold)
                    Text("Добавьте банковский счёт, карту или наличные.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.name }) { account ->
                val converted = conv(account.balance, account.currency, c, auto, r)
                ElevatedCard(onClick = { select(account.name) }, modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(4.dp)) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(uiColor(account.iconColor)), contentAlignment = Alignment.Center) { Text(iconText(account.icon), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
                                Spacer(Modifier.width(12.dp))
                                Column { Text(account.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text(account.type + " • " + account.currency, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(money(converted, c), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (converted >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                if (account.currency != c) Text("≈ " + money(converted, c), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(if (account.hidden) "Скрыт из общего баланса" else "В общем балансе", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { toggle(account.name) }) { Text(if (account.hidden) "Показать" else "Скрыть") }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun Analytics(tx: List<Transaction>, c: String, auto: Boolean, r: Map<String, Double>) {
    var preset by remember { mutableStateOf("Сегодня") }
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    val now = Calendar.getInstance()
    val from = when (preset) {
        "Сегодня" -> startOfDay(now).timeInMillis
        "7 дней" -> daysAgoStart(6).timeInMillis
        "14 дней" -> daysAgoStart(13).timeInMillis
        "Месяц" -> daysAgoStart(29).timeInMillis
        else -> parseDateStart(fromText) ?: startOfDay(now).timeInMillis
    }
    val to = if (preset == "Свои даты") parseDateEnd(toText) ?: endOfDay(now).timeInMillis else endOfDay(now).timeInMillis
    val periodTx = tx.filter { it.timestamp in from..to && it.operationType != "transfer" }
    val income = periodTx.filter { it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) }
    val expense = periodTx.filter { !it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) }
    val cats = periodTx.filter { !it.income }.groupBy { it.category }.mapValues { (_, values) -> values.sumOf { conv(it.amount, it.currency, c, auto, r) } }.toList().sortedByDescending { it.second }
    val maxCat = cats.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val days = (0..6).map { offset ->
        val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
        val key = SimpleDateFormat("dd.MM", Locale.getDefault()).format(day.time)
        val dayTx = tx.filter { it.timestamp in startOfDay(day).timeInMillis..endOfDay(day).timeInMillis && it.operationType != "transfer" }
        Triple(key, dayTx.filter { it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) }, dayTx.filter { !it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) })
    }.reversed()
    val maxDay = days.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1.0) ?: 1.0
    val periodDays = maxOf(1, ((to - from) / 86400000L + 1).toInt())
    val previousFrom = from - periodDays * 86400000L
    val previousTo = from - 1L
    val previousTx = tx.filter { it.timestamp in previousFrom..previousTo && it.operationType != "transfer" }
    val previousIncome = previousTx.filter { it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) }
    val previousExpense = previousTx.filter { !it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) }
    val savingsRate = if (income > 0) ((income - expense) / income * 100.0).coerceIn(-100.0, 100.0) else 0.0
    val healthScore = (50.0 + savingsRate * 0.5 + if (expense <= previousExpense || previousExpense == 0.0) 10 else -10 + if (income >= previousIncome || previousIncome == 0.0) 10 else -10).toInt().coerceIn(0, 100)
    val canSpend = (income - expense).coerceAtLeast(0.0)
    val largestExpense = periodTx.filter { !it.income }.maxByOrNull { conv(it.amount, it.currency, c, auto, r) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text("Аналитика", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Финансовый отчёт за выбранный период", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Сегодня", "7 дней", "14 дней", "Месяц", "Свои даты").forEach { p -> FilterChip(preset == p, { preset = p }, label = { Text(p) }) } } }
        if (preset == "Свои даты") item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(fromText, { fromText = it }, label = { Text("С даты") }, placeholder = { Text("дд.мм.гггг") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(toText, { toText = it }, label = { Text("По дату") }, placeholder = { Text("дд.мм.гггг") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Доходы", money(income, c), MaterialTheme.colorScheme.primary, Modifier.weight(1f)); MetricCard("Расходы", money(expense, c), MaterialTheme.colorScheme.error, Modifier.weight(1f)) } }
        item {
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Финансовое здоровье", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("Оценка", "$healthScore/100", if (healthScore >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, Modifier.weight(1f))
                        MetricCard("Можно потратить", money(canSpend, c), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                    Text("Норма накоплений: %.1f%%".format(savingsRate), style = MaterialTheme.typography.bodySmall)
                    if (largestExpense != null) Text("Самая крупная трата: " + largestExpense.title + " • " + money(conv(largestExpense.amount, largestExpense.currency, c, auto, r), c), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Сравнение с предыдущим периодом", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Доходы: " + money(income, c) + "  •  было " + money(previousIncome, c))
                    Text("Расходы: " + money(expense, c) + "  •  было " + money(previousExpense, c))
                    val delta = if (previousExpense > 0) (expense - previousExpense) / previousExpense * 100 else 0.0
                    Text(if (previousExpense > 0) "Расходы изменились на %.1f%%".format(delta) else "Недостаточно данных для сравнения", color = if (delta <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Финансовый поток", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Доходы и расходы по дням", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().height(145.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                        days.forEach { (label, inc, exp) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(Modifier.height(105.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                                    Box(Modifier.width(7.dp).height((8f + 90f * (inc / maxDay).toFloat()).dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.primary))
                                    Box(Modifier.width(7.dp).height((8f + 90f * (exp / maxDay).toFloat()).dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.error))
                                }
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { Text("Доходы", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text("Расходы", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Итог за период", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(money(income - expense, c), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (income >= expense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Text("${periodTx.size} операций учтено", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Куда уходят деньги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(cats.take(8)) { (category, value) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(category, fontWeight = FontWeight.SemiBold); Text(money(value, c), fontWeight = FontWeight.Bold) }
                    LinearProgressIndicator(progress = { (value / maxCat).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp))
                    Text("${((value / expense.coerceAtLeast(1.0)) * 100).toInt()}% расходов", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            val repeated = periodTx.filter { !it.income }.groupBy { it.title.trim().lowercase(Locale.getDefault()) }.filter { it.key.isNotBlank() && it.value.size >= 2 }.entries.sortedByDescending { it.value.size }.take(3)
            val averageExpense = if (periodTx.count { !it.income } > 0) expense / periodTx.count { !it.income } else 0.0
            val unusual = periodTx.filter { !it.income }.filter { conv(it.amount, it.currency, c, auto, r) > averageExpense * 2 && averageExpense > 0 }.maxByOrNull { it.amount }
            val daysCount = maxOf(1.0, ((to - from).toDouble() / 86400000.0) + 1.0)
            val forecast30 = expense / daysCount * 30.0
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Умные сигналы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (repeated.isNotEmpty()) {
                        val r0 = repeated.first()
                        Text("Повторяющаяся трата: " + r0.value.first().title + " • " + r0.value.size + " раза", color = MaterialTheme.colorScheme.primary)
                    } else Text("Повторяющихся трат за период не найдено.", style = MaterialTheme.typography.bodySmall)
                    if (unusual != null) {
                        Text("Крупная трата: " + unusual.title + " • " + money(conv(unusual.amount, unusual.currency, c, auto, r), c), color = MaterialTheme.colorScheme.error)
                    } else Text("Необычно крупных трат не обнаружено.", style = MaterialTheme.typography.bodySmall)
                    if (expense > 0) {
                        Text("Ориентир расходов на 30 дней: " + money(forecast30, c), fontWeight = FontWeight.SemiBold)
                        Text("Расчёт основан только на расходах выбранного периода.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (periodTx.isEmpty()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Нет операций за выбранный период", fontWeight = FontWeight.SemiBold)
                    Text("Добавьте доход или расход, чтобы увидеть аналитику.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
private fun startOfDay(c: Calendar): Calendar = (c.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
private fun endOfDay(c: Calendar): Calendar = (c.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
private fun daysAgoStart(days: Int): Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
private fun parseDateStart(s: String): Long? = runCatching { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply { isLenient = false }.parse(s)?.let { startOfDay(Calendar.getInstance().apply { time = it }).timeInMillis } }.getOrNull()
private fun parseDateEnd(s: String): Long? = runCatching { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply { isLenient = false }.parse(s)?.let { endOfDay(Calendar.getInstance().apply { time = it }).timeInMillis } }.getOrNull()
@Composable
private fun SmartCalendar(
    tx: List<Transaction>,
    reminders: List<Reminder>,
    c: String,
    auto: Boolean,
    rates: Map<String, Double>
) {
    var monthOffset by remember { mutableStateOf(0) }
    val month = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset); set(Calendar.DAY_OF_MONTH, 1) }
    val year = month.get(Calendar.YEAR)
    val monthIndex = month.get(Calendar.MONTH)
    val monthName = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(month.time).replaceFirstChar { it.uppercase() }
    val firstWeekDay = ((month.get(Calendar.DAY_OF_WEEK) + 5) % 7)
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = ((firstWeekDay + daysInMonth + 6) / 7) * 7
    val dayTx = tx.filter { it.operationType != "transfer" }.groupBy {
        val d = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        if (d.get(Calendar.YEAR) == year && d.get(Calendar.MONTH) == monthIndex) d.get(Calendar.DAY_OF_MONTH) else -1
    }
    val dayRem = reminders.groupBy {
        runCatching {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it.date)?.let { date ->
                Calendar.getInstance().apply { time = date }.let { cal ->
                    if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == monthIndex) cal.get(Calendar.DAY_OF_MONTH) else -1
                }
            } ?: -1
        }.getOrDefault(-1)
    }
    val start = startOfDay(month).timeInMillis
    val end = endOfDay(Calendar.getInstance().apply { set(Calendar.YEAR,year); set(Calendar.MONTH,monthIndex); set(Calendar.DAY_OF_MONTH,daysInMonth) }).timeInMillis
    val income = tx.filter { it.income && it.operationType != "transfer" && it.timestamp in start..end }.sumOf { conv(it.amount,it.currency,c,auto,rates) }
    val expense = tx.filter { !it.income && it.operationType != "transfer" && it.timestamp in start..end }.sumOf { conv(it.amount,it.currency,c,auto,rates) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Календарь денег", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(monthName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row { IconButton(onClick = { monthOffset-- }) { Text("‹", style = MaterialTheme.typography.headlineMedium) }; IconButton(onClick = { monthOffset++ }) { Text("›", style = MaterialTheme.typography.headlineMedium) } }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) } } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until cells / 7) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0 until 7) {
                            val index = row * 7 + col
                            val day = index - firstWeekDay + 1
                            if (day in 1..daysInMonth) {
                                val count = dayTx[day]?.size ?: 0
                                val remCount = dayRem[day]?.size ?: 0
                                Card(Modifier.weight(1f).height(62.dp), shape = RoundedCornerShape(12.dp)) {
                                    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(day.toString(), fontWeight = FontWeight.Bold)
                                        if (count > 0) Text("● $count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        if (remCount > 0) Text("⏰ $remCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            } else Spacer(Modifier.weight(1f).height(62.dp))
                        }
                    }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Доходы", money(income,c), MaterialTheme.colorScheme.primary, Modifier.weight(1f)); MetricCard("Расходы", money(expense,c), MaterialTheme.colorScheme.error, Modifier.weight(1f)) } }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Предстоящие события", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val upcoming = reminders.filter { !it.done }.sortedBy { it.date }.take(8)
                    if (upcoming.isEmpty()) Text("Нет активных напоминаний.", style = MaterialTheme.typography.bodySmall)
                    upcoming.forEach { r0 -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(r0.title, fontWeight = FontWeight.SemiBold); Text(r0.date, style = MaterialTheme.typography.bodySmall) } }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Повторяющиеся операции", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val repeats = tx.filter { it.repeat != "Не повторять" }.take(8)
                    if (repeats.isEmpty()) Text("Повторяющихся операций пока нет.", style = MaterialTheme.typography.bodySmall)
                    repeats.forEach { t -> Text("${t.title} • ${t.repeat} • ${money(conv(t.amount,t.currency,c,auto,rates),c)}", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
@Composable private fun Converter(c:String,r:Map<String,Double>,loading:Boolean,rateTime:Long){
 var amount by remember{mutableStateOf("")};var from by remember{mutableStateOf(c)};var to by remember{mutableStateOf(if(c=="EUR")"GBP" else "EUR")};val v=amount.replace(',','.').toDoubleOrNull();val out=v?.let{ExchangeRates.convert(it,from,to,r)}
 Text("Конвертер",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
 Text("Рубль России и белорусский рубль участвуют в конвертации и получают ежедневные котировки ЦБ РФ.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 Spacer(Modifier.height(6.dp))
 OutlinedTextField(amount,{amount=it},label={Text("Сумма")},modifier=Modifier.fillMaxWidth())
 Text("Из",fontWeight=FontWeight.SemiBold)
 Row(modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){currencies.forEach{x->FilterChip(from==x,{from=x},label={Text(currencyLabel(x))})}}
 Text("В",fontWeight=FontWeight.SemiBold)
 Row(modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){currencies.forEach{x->FilterChip(to==x,{to=x},label={Text(currencyLabel(x))})}}
 if(loading)Text("Обновляю курсы…")
 if(rateTime>0L&&!loading)Text("Курсы обновлены: "+SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(rateTime)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 if(out!=null)Text(money(out,to),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
 Text("Источник: ЦБ РФ для RUB/BYN и поддерживаемых валют; ECB — резервный источник.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Budgets(budgets: List<Budget>, transactions: List<Transaction>, c: String, auto: Boolean, rates: Map<String, Double>, accounts: List<Account>, add: () -> Unit, remove: (Budget) -> Unit) {
    val cal = Calendar.getInstance()
    val month = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val week = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -6); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val current = budgets.map { budget ->
        val start = if (budget.period == "Неделя") week else month
        val spent = transactions.filter { !it.income && it.operationType != "transfer" && it.timestamp >= start && (budget.category.isBlank() || it.category == budget.category) && (budget.accountName.isBlank() || it.accountName == budget.accountName) }.sumOf { conv(it.amount, it.currency, budget.currency, auto, rates) }
        budget to spent
    }
    val totalLimit = current.sumOf { it.first.limit }
    val totalSpent = current.sumOf { it.second }
    val totalRemaining = (totalLimit - totalSpent).coerceAtLeast(0.0)
    val totalPercent = if (totalLimit > 0) totalSpent / totalLimit * 100 else 0.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Бюджеты", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Контроль лимитов и расходов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            FilledTonalButton(onClick = add) { Text("+ Бюджет") }
        }
        if (budgets.isNotEmpty()) {
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Сводка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard("Лимит", money(totalLimit, c), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        MetricCard("Потрачено", money(totalSpent, c), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Осталось", fontWeight = FontWeight.SemiBold); Text(money(totalRemaining, c), fontWeight = FontWeight.Bold, color = if (totalRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                    LinearProgressIndicator(progress = { (totalPercent / 100.0).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(8.dp))
                    Text(totalPercent.toInt().toString() + "% использовано", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (budgets.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Бюджетов пока нет", fontWeight = FontWeight.SemiBold)
                    Text("Создайте первый лимит, чтобы видеть остаток и предупреждения.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(current, key = { it.first.id }) { pair ->
                val x = pair.first
                val spent = pair.second
                val ratio = if (x.limit > 0) spent / x.limit else 0.0
                val progress = ratio.coerceIn(0.0, 1.0).toFloat()
                val remaining = (x.limit - spent).coerceAtLeast(0.0)
                val status = when { spent >= x.limit -> "Лимит превышен"; spent >= x.limit * .9 -> "Осталось меньше 10%"; spent >= x.limit * .75 -> "Использовано больше 75%"; else -> "В норме" }
                val statusColor = if (spent >= x.limit) MaterialTheme.colorScheme.error else if (spent >= x.limit * .75) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(5.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(x.name, fontWeight = FontWeight.Bold); Text(listOf(x.period, x.category.ifBlank { "Все категории" }, x.accountName.ifBlank { "Все счета" }).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(onClick = { remove(x) }) { Text("Удалить") }
                        }
                        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(8.dp), color = statusColor)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column { Text("Потрачено", style = MaterialTheme.typography.labelSmall); Text(money(spent, x.currency), fontWeight = FontWeight.Bold) }
                            Column(horizontalAlignment = Alignment.End) { Text("Осталось", style = MaterialTheme.typography.labelSmall); Text(money(remaining, x.currency), fontWeight = FontWeight.Bold) }
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text((ratio * 100).toInt().toString() + "% использовано", style = MaterialTheme.typography.labelMedium, color = statusColor); Text("из " + money(x.limit, x.currency), style = MaterialTheme.typography.labelMedium) }
                        Text(status, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
@Composable
private fun BudgetDialog(accounts:List<Account>,categories:List<Category>,c:String,close:()->Unit,save:(Budget)->Unit){
 var n by remember{mutableStateOf("")};var lim by remember{mutableStateOf("")};var cat by remember{mutableStateOf("")};var acc by remember{mutableStateOf("")};var per by remember{mutableStateOf("Месяц")}
 AlertDialog(onDismissRequest=close,title={Text("Новый бюджет")},text={Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(n,{n=it},label={Text("Название")},modifier=Modifier.fillMaxWidth(),singleLine=true);OutlinedTextField(lim,{lim=it},label={Text("Лимит $c")},modifier=Modifier.fillMaxWidth(),singleLine=true);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(per=="Месяц",{per="Месяц"},label={Text("Месяц")});FilterChip(per=="Неделя",{per="Неделя"},label={Text("Неделя")})};Text("Категория",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(categories){z->FilterChip(cat==z.name,{cat=z.name},label={Text(iconText(z.icon)+" "+z.name)})}};Text("Счёт",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(accounts){z->FilterChip(acc==z.name,{acc=z.name},label={Text(iconText(z.icon)+" "+z.name)})}}}},confirmButton={Button(onClick={save(Budget(name=n.trim(),category=cat,accountName=acc,limit=lim.replace(',','.').toDoubleOrNull()?:0.0,currency=c,period=per))},enabled=n.isNotBlank()&&(lim.replace(',','.').toDoubleOrNull()?:0.0)>0){Text("Создать")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun Debts(items: List<Debt>, add: () -> Unit, progress: (Debt) -> Unit, remove: (Debt) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Долги", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(items.size.toString() + " обязательств", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            FilledTonalButton(onClick = add) { Text("+ Долг") }
        }
        if (items.isEmpty()) Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Долгов пока нет", fontWeight = FontWeight.SemiBold)
                Text("Добавьте обязательство, чтобы видеть сумму и проценты.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items.forEach { d ->
            val interestAmount = if (d.interest) d.amount * d.interestRate / 100.0 else 0.0
            val total = d.amount + interestAmount
            val remaining = (total - d.paid).coerceAtLeast(0.0)
            val paidRatio = if (total > 0) (d.paid / total).coerceIn(0.0, 1.0) else 0.0
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(if (d.mine) "Я должен" else "Мне должны", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(d.person, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
                        TextButton(onClick = { remove(d) }) { Text("Удалить") }
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        MetricCard("Основной долг", "%.2f RUB".format(d.amount), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        MetricCard("Итого", "%.2f RUB".format(total), if (d.interest) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                    if (d.interest) Text("Проценты за год: %.2f RUB (%.2f%%)".format(interestAmount, d.interestRate), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    Text("Погашено: " + money(d.paid) + " • Осталось: " + money(remaining), style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { paidRatio.toFloat() }, Modifier.fillMaxWidth().height(7.dp))
                    if (d.dueDate.isNotBlank()) Text("Срок: " + d.dueDate, style = MaterialTheme.typography.bodySmall)
                    if (d.note.isNotBlank()) Text(d.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), Arrangement.End) { TextButton(onClick = { progress(d) }, enabled = remaining > 0.0) { Text("Погасить") }; TextButton(onClick = { remove(d) }) { Text("Удалить") } }
                }
            }
        }
    }
}
@Composable private fun Goals(items: List<Goal>, add: () -> Unit, progress: (Goal) -> Unit, remove: (Goal) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Цели", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Накопления и финансовые планы", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            FilledTonalButton(onClick = add) { Text("+ Цель") }
        }
        if (items.isEmpty()) Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Целей пока нет", fontWeight = FontWeight.SemiBold)
                Text("Создайте цель и отслеживайте путь до суммы.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items.forEach { g ->
            val ratio = if (g.target > 0) (g.saved / g.target).coerceIn(0.0, 1.0) else 0.0
            val remaining = (g.target - g.saved).coerceAtLeast(0.0)
            val deadlineMillis = parseDateEnd(g.deadline)
            val daysLeft = deadlineMillis?.let { ((it - System.currentTimeMillis()) / 86400000L).coerceAtLeast(1L) }
            val monthlyNeed = if (daysLeft != null) remaining / (daysLeft.toDouble() / 30.44) else 0.0
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                        Column(Modifier.weight(1f)) { Text(g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); if (g.deadline.isNotBlank()) Text("До " + g.deadline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text((ratio * 100).toInt().toString() + "%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(progress = { ratio.toFloat() }, Modifier.fillMaxWidth().height(9.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column { Text("Накоплено", style = MaterialTheme.typography.labelSmall); Text(money(g.saved, g.currency), fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Осталось", style = MaterialTheme.typography.labelSmall); Text(money(remaining, g.currency), fontWeight = FontWeight.Bold) }
                    }
                    Text("Цель: " + money(g.target, g.currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (daysLeft != null && remaining > 0) Text("Нужно откладывать примерно " + money(monthlyNeed, g.currency) + " в месяц", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(), Arrangement.End) { TextButton(onClick = { progress(g) }) { Text("+ Пополнить") }; TextButton(onClick = { remove(g) }) { Text("Удалить") } }
                }
            }
        }
    }
}
@Composable private fun Reminders(items: List<Reminder>, add: () -> Unit, toggle: (Reminder) -> Unit, remove: (Reminder) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Напоминания", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Платежи и финансовые задачи", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            FilledTonalButton(onClick = add) { Text("+ Напоминание") }
        }
        if (items.isEmpty()) Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Напоминаний пока нет", fontWeight = FontWeight.SemiBold)
                Text("Добавьте дату, чтобы не забыть важный платёж.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items.forEach { item ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (item.done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(if (item.done) "✓" else "!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(item.date + " • " + item.repeat, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.amount > 0) Text(money(item.amount, "RUB"), style = MaterialTheme.typography.labelMedium)
                    }
                    Switch(item.done, { toggle(item) })
                    TextButton(onClick = { remove(item) }) { Text("Удалить") }
                }
            }
        }
    }
}

private fun normalizeReceiptAmount(raw: String): Double? {
    val cleaned = raw.replace(" ", "")
    val comma = cleaned.lastIndexOf(',')
    val dot = cleaned.lastIndexOf('.')
    val normalized = when {
        comma >= 0 && dot >= 0 -> {
            val decimal = maxOf(comma, dot)
            cleaned.mapIndexed { index, ch -> if (index == decimal && ch == cleaned[decimal]) '.' else if (ch.isDigit()) ch else '\u0000' }.filter { it != '\u0000' }.joinToString("")
        }
        comma >= 0 -> cleaned.replace(".", "").replace(',', '.')
        dot >= 0 && cleaned.length - dot - 1 == 2 -> cleaned
        else -> cleaned.replace(".", "")
    }
    return normalized.toDoubleOrNull()
}

private fun parseReceiptDraft(text: String, account: Account?, categories: List<Category>): Transaction? {
    if (account == null) return null
    val amountRegex = Regex("""(?i)(итого|к\s*оплате|сумма|total|amount)[^0-9]{0,20}([0-9]{1,8}(?:[ .][0-9]{3})*(?:[.,][0-9]{2})?)""")
    val fallbackRegex = Regex("""[0-9]{1,8}(?:[ .][0-9]{3})*(?:[.,][0-9]{2})?""")
    val raw = amountRegex.findAll(text).lastOrNull()?.groupValues?.getOrNull(2) ?: fallbackRegex.findAll(text).lastOrNull()?.value ?: return null
    val amount = normalizeReceiptAmount(raw) ?: return null
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val title = lines.firstOrNull { it.length >= 2 && it.any(Char::isLetter) }?.take(60)?.ifBlank { "Покупка" } ?: "Покупка"
    val dateMatch = Regex("""([0-3][0-9])[./-]([0-1][0-9])[./-]([0-9]{4})""").find(text)
    val timestamp = dateMatch?.let { runCatching { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it.value.replace('/', '.').replace('-', '.'))?.time }.getOrNull() } ?: System.currentTimeMillis()
    val lower = text.lowercase(Locale.getDefault())
    val categoryName = when {
        listOf("продукт", "супермаркет", "молоко", "хлеб", "еда").any { it in lower } -> "Продукты"
        listOf("такси", "бензин", "заправ", "метро", "автобус").any { it in lower } -> "Транспорт"
        listOf("аптек", "лекар", "медицин", "врач").any { it in lower } -> "Здоровье"
        listOf("кино", "игр", "театр", "развлеч").any { it in lower } -> "Развлечения"
        else -> "Покупки"
    }
    val category = categories.firstOrNull { it.name.equals(categoryName, true) }?.name ?: categories.firstOrNull()?.name ?: "Без категории"
    return Transaction(title = title, amount = amount, income = false, accountName = account.name, category = category, timestamp = timestamp, currency = account.currency, operationType = "expense")
}

@Composable private fun Receipt(uri:Uri?,text:String,setUri:(Uri?)->Unit,setText:(String)->Unit,account:Account?,categories:List<Category>,prepare:(Transaction)->Unit){
    val context=LocalContext.current
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){u->
        if(u!=null){
            setUri(u)
            runCatching{InputImage.fromFilePath(context,u)}.onSuccess{image->
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener{result->setText(result.text.ifBlank{"Текст не найден."})}
                    .addOnFailureListener{setText("Не удалось распознать текст.")}
            }
        }
    }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Чеки и OCR",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Распознай чек и проверь операцию перед сохранением.",style=MaterialTheme.typography.bodySmall)
        Button({launcher.launch("image/*")}){Text("Выбрать фото чека")}
        if(uri!=null)Text("Фото выбрано: "+uri.lastPathSegment)
        if(text.isNotBlank()){
            Card(Modifier.fillMaxWidth()){Text(text,Modifier.padding(12.dp))}
            val draft=parseReceiptDraft(text,account,categories)
            if(draft!=null){
                Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text("Найдена операция",fontWeight=FontWeight.Bold)
                        Text(draft.title)
                        Text(money(draft.amount,draft.currency),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        Text("Дата: "+SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(Date(draft.timestamp)))
                        Button({prepare(draft)}){Text("Проверить и добавить")}
                    }
                }
            } else {
                Text("Сумма в чеке не найдена. Проверьте распознанный текст.",color=MaterialTheme.colorScheme.error)
            }
        } else Card(Modifier.fillMaxWidth()){Text("После выбора фото здесь появится распознанный текст.",Modifier.padding(12.dp))}
    }
}

@Composable
private fun More(
    c: String,
    auto: Boolean,
    theme: String,
    style: String,
    menu: Set<String>,
    time: Long,
    transactions: List<Transaction>,
    save: (String, Boolean, String, String, Set<String>) -> Unit
) {
    Text(
        "Ещё",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Основная валюта")
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                currencies.forEach { code ->
                    FilterChip(
                        selected = c == code,
                        onClick = { save(code, auto, theme, style, menu) },
                        label = { Text(currencyLabel(code)) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Автоконвертация")
                Switch(
                    checked = auto,
                    onCheckedChange = { value ->
                        save(c, value, theme, style, menu)
                    }
                )
            }

            Text("Тема")
            Row {
                listOf("system", "light", "dark").forEach { value ->
                    FilterChip(
                        selected = theme == value,
                        onClick = { save(c, auto, value, style, menu) },
                        label = { Text(value) }
                    )
                }
            }

            Text("Стиль")
            Row {
                listOf("midnight", "platinum", "emerald").forEach { value ->
                    FilterChip(
                        selected = style == value,
                        onClick = { save(c, auto, theme, value, menu) },
                        label = { Text(value) }
                    )
                }
            }

            if (time > 0) {
                Text(
                    "Курсы: " +
                        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                            .format(Date(time))
                )
            }
        }
    }

    val context = LocalContext.current

    Button(
        onClick = {
            val csv = buildString {
                append("date,title,amount,currency,type,account,category\n")
                transactions.forEach { transaction ->
                    append(
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date(transaction.timestamp))
                    )
                    append(",")
                    append(transaction.title.replace(",", " "))
                    append(",")
                    append(transaction.amount)
                    append(",")
                    append(transaction.currency)
                    append(",")
                    append(if (transaction.income) "income" else "expense")
                    append(",")
                    append(transaction.accountName.replace(",", " "))
                    append(",")
                    append(transaction.category.replace(",", " "))
                    append("\n")
                }
            }

            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, csv)
                    },
                    "Экспорт операций"
                )
            )
        }
    ) {
        Text("Экспорт операций CSV")
    }

    Button(
        onClick = {
            val backup = buildString {
                append("VIP Finance backup\n")
                append("Transactions: ")
                append(transactions.size)
                append("\nAccounts: ")
                append(transactions.map { it.accountName }.distinct().joinToString())
                append("\nItems: ")
                append(transactions.size)
                append("\nDebts/Goals/Reminders: exported in app storage")
            }

            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, backup)
                    },
                    "Резервная копия"
                )
            )
        }
    ) {
        Text("Резервная копия")
    }

    Text("Показывать в меню")

    pages
        .filter { it != "Ещё" }
        .forEach { pageName ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(pageName)
                Switch(
                    checked = pageName in menu,
                    onCheckedChange = { enabled ->
                        val updated = menu.toMutableSet()
                        if (enabled) {
                            updated.add(pageName)
                        } else {
                            updated.remove(pageName)
                        }
                        save(c, auto, theme, style, updated)
                    }
                )
            }
        }
}

@Composable
private fun SettingsDialog(
    c: String,
    auto: Boolean,
    theme: String,
    style: String,
    menu: Set<String>,
    time: Long,
    save: (String, Boolean, String, String, Set<String>) -> Unit,
    backup: () -> String,
    close: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f)
                                    )
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Настройки",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Персонализируйте VIP Finance под себя",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                item {
                    SettingsSection(
                        "Основная валюта",
                        "Нажмите, чтобы выбрать валюту баланса и аналитики"
                    ) {
                        SettingsChoiceDropdown(
                            label = "Валюта",
                            value = c,
                            options = currencies.map { it to it },
                            onSelected = { save(it, auto, theme, style, menu) }
                        )
                    }
                }

                item {
                    SettingsToggleRow(
                        title = "Автоконвертация",
                        subtitle = "Автоматически пересчитывать суммы между валютами",
                        checked = auto,
                        onCheckedChange = { save(c, it, theme, style, menu) }
                    )
                }

                item {
                    SettingsSection(
                        "Внешний вид",
                        "Варианты появляются только после нажатия"
                    ) {
                        SettingsChoiceDropdown(
                            label = "Тема",
                            value = when (theme) {
                                "light" -> "Светлая"
                                "dark" -> "Тёмная"
                                else -> "Системная"
                            },
                            options = listOf(
                                "system" to "Системная",
                                "light" to "Светлая",
                                "dark" to "Тёмная"
                            ),
                            onSelected = { save(c, auto, it, style, menu) }
                        )

                        SettingsChoiceDropdown(
                            label = "Стиль",
                            value = when (style) {
                                "midnight" -> "Midnight"
                                "platinum" -> "Platinum"
                                "emerald" -> "Emerald"
                                "royal" -> "Royal"
                                "rose" -> "Rose"
                                else -> style.replaceFirstChar { it.uppercase() }
                            },
                            options = listOf(
                                "midnight" to "Midnight",
                                "platinum" to "Platinum",
                                "emerald" to "Emerald",
                                "royal" to "Royal",
                                "rose" to "Rose"
                            ),
                            onSelected = { save(c, auto, theme, it, menu) }
                        )
                    }
                }

                item {
                    SettingsSection(
                        "Резервная копия",
                        "Сохраните данные перед переносом или переустановкой"
                    ) {
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_TEXT, backup())
                                        },
                                        "Сохранить резервную копию"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Экспортировать JSON", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    SettingsSection(
                        "Функции в меню",
                        if (menuExpanded) "Нажмите ещё раз, чтобы свернуть список"
                        else "Нажмите, чтобы показать разделы"
                    ) {
                        OutlinedButton(
                            onClick = { menuExpanded = !menuExpanded },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (menuExpanded) "Скрыть разделы" else "Показать разделы")
                        }

                        if (menuExpanded) {
                            pages.forEach { pageName ->
                                SettingsToggleRow(
                                    title = pageName,
                                    subtitle = if (pageName in menu) "Раздел отображается в меню" else "Раздел скрыт из меню",
                                    checked = pageName in menu,
                                    onCheckedChange = { enabled ->
                                        val updated = menu.toMutableSet()
                                        if (enabled) updated.add(pageName) else updated.remove(pageName)
                                        save(c, auto, theme, style, updated)
                                    }
                                )
                            }
                        }
                    }
                }

                if (time > 0) {
                    item {
                        Text(
                            "Курсы обновлены: " +
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(time)),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 22.dp)
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = close,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Закрыть", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsChoiceDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(value, fontWeight = FontWeight.SemiBold)
                    Text(if (expanded) "⌃" else "⌄")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.82f)
            ) {
                options.forEach { (key, title) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                title,
                                fontWeight = if (title == value) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelected(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun AccountDialog(
    close: () -> Unit,
    save: (String, Double, String, String, String, String) -> Unit
) {
    var n by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var t by remember { mutableStateOf("Счёт") }
    var c by remember { mutableStateOf("RUB") }
    var icon by remember { mutableStateOf(if (t == "Карта") "credit_card" else "account_balance") }
    var iconColor by remember { mutableStateOf(colorChoices.first()) }
    val balance = b.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (t == "Карта") "Новая карта" else "Новый счёт") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(n, { n = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text("Начальный баланс $c") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Тип", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(t == "Счёт", { t = "Счёт"; if (icon == "credit_card") icon = "account_balance" }, label = { Text("Счёт") })
                    FilterChip(t == "Карта", { t = "Карта"; if (icon == "account_balance") icon = "credit_card" }, label = { Text("Карта") })
                }
                Text("Валюта", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { items(currencies) { x -> FilterChip(c == x, { c = x }, label = { Text(x) }) } }
                Text("Значок", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconChoices) { x ->
                        FilterChip(icon == x, { icon = x }, label = { Text(iconText(x)) })
                    }
                }
                Text("Цвет значка", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorChoices) { x ->
                        FilterChip(iconColor == x, { iconColor = x }, label = {
                            Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(uiColor(x)))
                        })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { save(n.trim(), balance ?: 0.0, t, c, icon, iconColor) },
                enabled = n.isNotBlank() && balance != null
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(close) { Text("Отмена") } }
    )
}

@Composable
private fun TransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    defaultIncome: Boolean,
    source: Transaction?,
    close: () -> Unit,
    save: (Transaction) -> Unit
) {
    var n by remember { mutableStateOf(source?.title ?: "") }
    var a by remember { mutableStateOf(source?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(source?.operationType ?: if (defaultIncome) "income" else "expense") }
    var cat by remember { mutableStateOf(source?.category ?: categories.firstOrNull()?.name.orEmpty()) }
    var acc by remember { mutableStateOf(source?.accountName ?: accounts.firstOrNull()?.name.orEmpty()) }
    var toAcc by remember { mutableStateOf(source?.toAccountName ?: accounts.firstOrNull { it.name != acc }?.name.orEmpty()) }
    var note by remember { mutableStateOf(source?.note ?: "") }
    var rep by remember { mutableStateOf(source?.repeat ?: "Не повторять") }
    var tags by remember { mutableStateOf(source?.tags ?: "") }
    var dateText by remember {
        mutableStateOf(
            source?.timestamp?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it)) }
                ?: SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        )
    }
    val cc = accounts.firstOrNull { it.name == acc }?.currency ?: "RUB"
    val v = a.replace(',', '.').toDoubleOrNull()
    val parsed = runCatching { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).parse(dateText)?.time }.getOrNull()
        ?: System.currentTimeMillis()
    val other = accounts.filter { it.name != acc }
    val title = if (type == "transfer") "Новый перевод" else if (type == "income") "Новый доход" else "Новый расход"
    val amountPreview = v?.let { money(it, cc) } ?: "0,00 $cc"

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 790.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 14.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (type == "income") Color(0xFF00897B) else if (type == "transfer") MaterialTheme.colorScheme.primary else Color(0xFF8E24AA),
                                        MaterialTheme.colorScheme.primary
                                    )
                                ),
                                RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                            )
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (type == "income") "Добавьте поступление и оно сразу попадёт в баланс"
                                else if (type == "transfer") "Перемещение средств между счетами"
                                else "Зафиксируйте расход и сохраните его в истории",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                amountPreview,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                item {
                    DialogField(
                        label = "Описание",
                        supporting = "Например: продукты, бензин, зарплата",
                        value = n,
                        onValueChange = { n = it }
                    )
                }
                item {
                    DialogField(
                        label = "Сумма $cc",
                        supporting = "Введите сумму операции",
                        value = a,
                        onValueChange = { a = it }
                    )
                }
                item {
                    DialogGroup("Тип операции") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("expense" to "Расход", "income" to "Доход", "transfer" to "Перевод").forEach { (vType, label) ->
                                FilterChip(
                                    selected = type == vType,
                                    onClick = { type = vType },
                                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                item {
                    DialogGroup("Счёт / карта") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                            items(accounts) { x ->
                                FilterChip(
                                    selected = acc == x.name,
                                    onClick = { acc = x.name },
                                    label = { Text(iconText(x.icon) + "  " + x.name) }
                                )
                            }
                        }
                    }
                }
                if (type == "transfer") {
                    item {
                        DialogGroup("Куда перевести") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                                items(other) { x ->
                                    FilterChip(
                                        selected = toAcc == x.name,
                                        onClick = { toAcc = x.name },
                                        label = { Text(iconText(x.icon) + "  " + x.name) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        DialogGroup("Категория") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                                items(categories) { x ->
                                    FilterChip(
                                        selected = cat == x.name,
                                        onClick = { cat = x.name },
                                        label = { Text(iconText(x.icon) + "  " + x.name) }
                                    )
                                }
                            }
                            DialogField(
                                label = "Своя категория",
                                supporting = "Можно заменить выбранную категорию своим названием",
                                value = cat,
                                onValueChange = { cat = it }
                            )
                        }
                    }
                }
                item {
                    DialogGroup("Дополнительно") {
                        DialogField(
                            label = "Комментарий / заметка",
                            supporting = "Необязательно",
                            value = note,
                            onValueChange = { note = it },
                            minLines = 2
                        )
                        DialogField(
                            label = "Теги",
                            supporting = "Через запятую: Авто, Дом, Работа",
                            value = tags,
                            onValueChange = { tags = it }
                        )
                        DialogField(
                            label = "Дата и время",
                            supporting = "Формат: дд.мм.гггг чч:мм",
                            value = dateText,
                            onValueChange = { dateText = it }
                        )
                    }
                }
                item {
                    DialogGroup("Повтор операции") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                            items(listOf("Не повторять", "Ежедневно", "Еженедельно", "Ежемесячно", "Ежегодно")) { x ->
                                FilterChip(selected = rep == x, onClick = { rep = x }, label = { Text(x) })
                            }
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = close, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                            Text("Отмена", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                save(
                                    Transaction(
                                        id = source?.id ?: System.currentTimeMillis(),
                                        title = n.trim(),
                                        amount = v ?: 0.0,
                                        income = type == "income",
                                        accountName = acc,
                                        category = if (type == "transfer") "Перевод" else cat.ifBlank { "Без категории" },
                                        timestamp = parsed,
                                        currency = cc,
                                        operationType = type,
                                        toAccountName = if (type == "transfer") toAcc else "",
                                        note = note.trim(),
                                        repeat = rep,
                                        tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
                                    )
                                )
                            },
                            enabled = n.isNotBlank() && v != null && v > 0 && acc.isNotBlank() && (type != "transfer" || toAcc.isNotBlank()),
                            modifier = Modifier.weight(1.35f),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(21.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
private fun DialogField(
    label: String,
    supporting: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            supportingText = { Text(supporting) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = minLines == 1,
            minLines = minLines,
            maxLines = if (minLines == 1) 1 else 3,
            shape = RoundedCornerShape(17.dp)
        )
    }
}

@Composable
private fun CategoryDialog(existing: Category?, close: () -> Unit, save: (Category) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "category") }
    var color by remember { mutableStateOf(existing?.color ?: colorChoices.first()) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (existing == null) "Новая категория" else "Изменить категорию") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Название категории") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Значок", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(iconChoices) { x -> FilterChip(icon == x, { icon = x }, label = { Text(iconText(x)) }) }
                }
                Text("Цвет", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorChoices) { x ->
                        FilterChip(color == x, { color = x }, label = {
                            Box(Modifier.size(20.dp).clip(RoundedCornerShape(7.dp)).background(uiColor(x)))
                        })
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = uiColor(color).copy(alpha = 0.16f))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(uiColor(color)), contentAlignment = Alignment.Center) {
                            Text(iconText(icon), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(if (name.isBlank()) "Предпросмотр" else name, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = { Button({ save(Category(existing?.id ?: System.currentTimeMillis(), name.trim(), icon, color)) }, enabled = name.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(close) { Text("Отмена") } }
    )
}

@Composable
private fun DebtDialog(close: () -> Unit, save: (Debt) -> Unit) {
    var p by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var mine by remember { mutableStateOf(false) }
    var interest by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val rateValue = rate.replace(',', '.').toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Новый долг") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(p, { p = it }, label = { Text("Человек") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amountText, { amountText = it }, label = { Text("Сумма RUB") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(mine, { mine = true }, label = { Text("Я должен") }); FilterChip(!mine, { mine = false }, label = { Text("Мне должны") }) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Начислять проценты", Modifier.weight(1f)); Switch(interest, { interest = it }) }
                if (interest) OutlinedTextField(rate, { rate = it }, label = { Text("Ставка, % годовых") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(due, { due = it }, label = { Text("Срок погашения") }, placeholder = { Text("дд.мм.гггг") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Заметка") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { save(Debt(person = p, amount = amount ?: 0.0, mine = mine, interest = interest, interestRate = rateValue, dueDate = due.trim(), note = note.trim())) }, enabled = p.isNotBlank() && amount != null && amount > 0 && (!interest || rateValue >= 0)) { Text("Сохранить") } },
        dismissButton = { TextButton(close) { Text("Отмена") } }
    )
}
@Composable private fun GoalDialog(c:String,close:()->Unit,save:(Goal)->Unit){
 var n by remember{mutableStateOf("")};var t by remember{mutableStateOf("")};var d by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Новая цель")},text={Column{OutlinedTextField(n,{n=it},label={Text("Название")});OutlinedTextField(t,{t=it},label={Text("Цель "+c)});OutlinedTextField(d,{d=it},label={Text("Срок")})}},confirmButton={Button({save(Goal(name=n,target=t.replace(',','.').toDoubleOrNull()?:0.0,currency=c,deadline=d))},enabled=n.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun GoalProgressDialog(goal:Goal?,close:()->Unit,save:(Goal)->Unit){
 if(goal==null){close();return}
 var amount by remember{mutableStateOf("")}
 val value=amount.replace(',','.').toDoubleOrNull()?:0.0
 val newSaved=(goal.saved+value).coerceAtMost(goal.target.coerceAtLeast(goal.saved))
 AlertDialog(onDismissRequest=close,title={Text("Пополнить цель")},text={
  Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text(goal.name,fontWeight=FontWeight.Bold)
   Text("Сейчас: "+money(goal.saved,goal.currency))
   OutlinedTextField(amount,{amount=it},label={Text("Сколько добавить, "+goal.currency)},modifier=Modifier.fillMaxWidth(),singleLine=true)
   if(value>0) Text("После пополнения: "+money(newSaved,goal.currency),color=MaterialTheme.colorScheme.primary)
  }
 },confirmButton={Button({save(goal.copy(saved=newSaved))},enabled=value>0){Text("Пополнить")}},dismissButton={TextButton(close){Text("Отмена")}})
}

@Composable private fun ReminderDialog(close:()->Unit,save:(Reminder)->Unit){
 var n by remember{mutableStateOf("")};var d by remember{mutableStateOf("")};var rep by remember{mutableStateOf("Один раз")}
 AlertDialog(onDismissRequest=close,title={Text("Напоминание")},text={
  Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   OutlinedTextField(n,{n=it},label={Text("Что напомнить")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(d,{d=it},label={Text("Дата")},modifier=Modifier.fillMaxWidth())
   Text("Повтор",fontWeight=FontWeight.Bold)
   LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("Один раз","Еженедельно","Ежемесячно")){x->FilterChip(rep==x,{rep=x},label={Text(x)})}}
  }
 },confirmButton={Button({save(Reminder(title=n,date=d,repeat=rep))},enabled=n.isNotBlank()&&d.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}

@Composable
private fun DebtProgressDialog(debt: Debt?, close: () -> Unit, save: (Debt) -> Unit) {
    if (debt == null) { close(); return }
    var amount by remember { mutableStateOf("") }
    val value = amount.replace(',','.').toDoubleOrNull() ?: 0.0
    val interestAmount = if (debt.interest) debt.amount * debt.interestRate / 100.0 else 0.0
    val total = debt.amount + interestAmount
    val newPaid = (debt.paid + value).coerceAtMost(total)
    AlertDialog(onDismissRequest = close, title = { Text("Погашение долга") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(debt.person, fontWeight = FontWeight.Bold)
            Text("Осталось: " + money((total - debt.paid).coerceAtLeast(0.0)))
            OutlinedTextField(amount, { amount = it }, label = { Text("Сумма погашения") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (value > 0) Text("После операции: " + money((total - newPaid).coerceAtLeast(0.0)), color = MaterialTheme.colorScheme.primary)
        }
    }, confirmButton = { Button(onClick = { save(debt.copy(paid = newPaid)) }, enabled = value > 0) { Text("Погасить") } }, dismissButton = { TextButton(close) { Text("Отмена") } })
}

