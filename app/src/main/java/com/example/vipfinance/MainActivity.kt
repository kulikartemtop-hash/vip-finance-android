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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.vipfinance.ui.theme.VIPFinanceTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val currencies=listOf("RUB","GBP","EUR","USD","CNY","JPY","CHF","CAD","AUD","PLN")
private val pages=listOf("Главная","Операции","Счета","Категории","Бюджеты","Аналитика","Конвертер","Долги","Цели","Напоминания","Чеки")
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

private fun sym(c:String)=when(c){"GBP"->"£";"USD"->"$";"EUR"->"€";"RUB"->"₽";"CNY"->"¥";"JPY"->"¥";"CHF"->"Fr";"CAD"->"C$";"AUD"->"A$";"PLN"->"zł";else->c}
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
    val shown0 = filter?.let { name -> tx.filter { it.accountName == name } } ?: tx
    val shown = shown0.filter { search.isBlank() || it.title.contains(search, true) || it.category.contains(search, true) || it.accountName.contains(search, true) }
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
                        "Операции" -> Operations(shown, accounts, filter, { filter = it }, { dialog = "expense" }, ::remove, currency, auto, rates, search, { search = it }, newest, { newest = it }, { original -> repeatSource = original; dialog = if (original.income) "income" else "expense" })
                        "Счета" -> Accounts(accounts, currency, auto, rates, { dialog = "account" }, { selected = it }) { n -> val updated = accounts.map { if (it.name == n) it.copy(hidden = !it.hidden) else it }; accounts = updated; s.saveAccounts(updated) }
                        "Категории" -> Categories(categories, { editingCategory = null; dialog = "category" }, { editingCategory = it; dialog = "category" }, { c0 -> categories = categories.filterNot { it.id == c0.id }; s.saveCategories(categories) })
                        "Бюджеты" -> Budgets(budgets, tx, currency, auto, rates, accounts, { dialog = "budget" }) { b0 -> budgets = budgets.filterNot { it.id == b0.id }; s.saveBudgets(budgets) }
                        "Аналитика" -> Analytics(tx, currency, auto, rates)
                        "Конвертер" -> Converter(currency, rates, loading)
                        "Долги" -> Debts(debts, { dialog = "debt" }) { d -> debts = debts.filterNot { it.id == d.id }; s.saveDebts(debts) }
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
    total: Double, c: String, accounts: List<Account>, auto: Boolean, r: Map<String, Double>,
    selected: String?, income: Double, expense: Double, pick: (String?) -> Unit
) {
    val selectedBalance = selected?.let { n -> accounts.firstOrNull { it.name == n }?.let { conv(it.balance, it.currency, c, auto, r) } } ?: total
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (selected == null) "Общий баланс" else "Баланс • $selected", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge)
                    Text(money(selectedBalance, c), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(if (auto) "Автоконвертация включена" else "Показ исходных валют", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Доходы", money(income, c), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricCard("Расходы", money(expense, c), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }
        item {
            Text("Фильтр счетов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected == null, { pick(null) }, label = { Text("Все") })
                accounts.filter { !it.hidden }.forEach { a -> FilterChip(selected == a.name, { pick(a.name) }, label = { Text(a.name) }) }
            }
        }
        item { Text("Мои счета", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(accounts.filter { !it.hidden }) { a ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(uiColor(a.iconColor)), contentAlignment = Alignment.Center) {
                            Text(iconText(a.icon), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column { Text(a.name, fontWeight = FontWeight.Bold); Text(a.type + " • " + a.currency, style = MaterialTheme.typography.bodySmall) }
                    }
                    Text(money(conv(a.balance, a.currency, c, auto, r), c), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
    search: String, setSearch: (String) -> Unit, newest: Boolean, setNewest: (Boolean) -> Unit,
    repeat: (Transaction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Операции", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold); Text("${ts.size} операций", style=MaterialTheme.typography.bodySmall) }
            FilledTonalButton(onClick=add){Text("+ Добавить")}
        }
        OutlinedTextField(search,setSearch,label={Text("Поиск операций")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(verticalAlignment=Alignment.CenterVertically){Text("Сначала новые",Modifier.weight(1f));Switch(newest,setNewest)}
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){FilterChip(filter==null,{setFilter(null)},label={Text("Все")});accounts.forEach{a->FilterChip(filter==a.name,{setFilter(a.name)},label={Text(a.name)})}}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(if(newest)ts.sortedByDescending{it.timestamp}else ts.sortedBy{it.timestamp}){t->
                val transfer=t.operationType=="transfer"
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(transfer)MaterialTheme.colorScheme.secondaryContainer.copy(.45f)else if(t.income)MaterialTheme.colorScheme.primaryContainer.copy(.42f)else MaterialTheme.colorScheme.surface)){
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if(transfer)MaterialTheme.colorScheme.secondaryContainer else if(t.income)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),contentAlignment=Alignment.Center){Text(if(transfer)"⇄"else if(t.income)"↗"else"↘",color=if(transfer)MaterialTheme.colorScheme.secondary else if(t.income)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.SemiBold);Text(if(transfer)"${t.accountName} → ${t.toAccountName}" else "${t.category} • ${t.accountName}",style=MaterialTheme.typography.bodySmall);if(t.note.isNotBlank())Text(t.note,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(t.timestamp)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);if(t.repeat!="Не повторять")Text("↻ ${t.repeat}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)}
                        Column(horizontalAlignment=Alignment.End){Text((if(transfer)"⇄"else if(t.income)"+"else"−")+" "+money(conv(t.amount,t.currency,c,auto,r),c),fontWeight=FontWeight.Bold,color=if(transfer)MaterialTheme.colorScheme.secondary else if(t.income)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error);Row{TextButton(onClick={repeat(t)}){Text("Повторить")};TextButton(onClick={remove(t)}){Text("Удалить")}}}
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
private fun Accounts(
    items: List<Account>, c: String, auto: Boolean, r: Map<String, Double>,
    add: () -> Unit, select: (String) -> Unit, toggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Счета и карты", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${items.size} подключённых", style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = add) { Text("+ Добавить") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { a ->
                ElevatedCard(onClick = { select(a.name) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(uiColor(a.iconColor)), contentAlignment = Alignment.Center) {
                                    Text(iconText(a.icon), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(a.name, fontWeight = FontWeight.Bold)
                                    Text(a.type + " • " + a.currency + if (a.hidden) " • скрыт" else "", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(money(conv(a.balance, a.currency, c, auto, r), c), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(if (a.hidden) "Скрыт из общего баланса" else "Учитывается в общем балансе", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { toggle(a.name) }) { Text(if (a.hidden) "Показать" else "Скрыть") }
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
    val periodTx = tx.filter { it.timestamp in from..to }
    val income = periodTx.filter { it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) }
    val expense = periodTx.filter { !it.income }.sumOf { conv(it.amount, it.currency, c, auto, r) }
    val cats = periodTx.filter { !it.income }.groupBy { it.category }.mapValues { (_, values) -> values.sumOf { conv(it.amount, it.currency, c, auto, r) } }
    val max = cats.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text("Аналитика", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Финансовый отчёт за выбранный период", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Сегодня", "7 дней", "14 дней", "Месяц", "Свои даты").forEach { p -> FilterChip(preset == p, { preset = p }, label = { Text(p) }) }
            }
        }
        if (preset == "Свои даты") item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(fromText, { fromText = it }, label = { Text("С даты") }, placeholder = { Text("дд.мм.гггг") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(toText, { toText = it }, label = { Text("По дату") }, placeholder = { Text("дд.мм.гггг") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Доходы", money(income, c), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricCard("Расходы", money(expense, c), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }
        item {
            Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))).padding(18.dp)) {
                Column {
                    Text("Итог за период", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(money(income - expense, c), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${periodTx.size} операций", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Расходы по категориям", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(cats.entries.sortedByDescending { it.value }) { entry ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(entry.key, fontWeight = FontWeight.SemiBold); Text(money(entry.value, c), fontWeight = FontWeight.Bold) }
                    LinearProgressIndicator(progress = { (entry.value / max).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
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
@Composable private fun Converter(c:String,r:Map<String,Double>,loading:Boolean){
 var amount by remember{mutableStateOf("")};var from by remember{mutableStateOf(c)};var to by remember{mutableStateOf(if(c=="EUR")"GBP" else "EUR")};val v=amount.replace(',','.').toDoubleOrNull();val out=v?.let{ExchangeRates.convert(it,from,to,r)}
 Text("Конвертер",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedTextField(amount,{amount=it},label={Text("Сумма")},modifier=Modifier.fillMaxWidth());Text("Из");Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){currencies.forEach{x->FilterChip(from==x,{from=x},label={Text(x)})}};Text("В");Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){currencies.forEach{x->FilterChip(to==x,{to=x},label={Text(x)})}};if(loading)Text("Обновляю курсы…");if(out!=null)Text(money(out,to),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("ECB: справочные курсы. RUB не входит в актуальный набор ECB.",style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun Budgets(budgets:List<Budget>,transactions:List<Transaction>,c:String,auto:Boolean,rates:Map<String,Double>,accounts:List<Account>,add:()->Unit,remove:(Budget)->Unit){
 val cal=Calendar.getInstance();val month=(cal.clone() as Calendar).apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis;val week=(cal.clone() as Calendar).apply{add(Calendar.DAY_OF_MONTH,-6);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Column{Text("Бюджеты",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Лимиты по расходам",style=MaterialTheme.typography.bodySmall)};FilledTonalButton(onClick=add){Text("+ Бюджет")}}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(budgets,key={it.id}){x->val start=if(x.period=="Неделя")week else month;val spent=transactions.filter{!it.income&&it.operationType!="transfer"&&it.timestamp>=start&&(x.category.isBlank()||it.category==x.category)&&(x.accountName.isBlank()||it.accountName==x.accountName)}.sumOf{conv(it.amount,it.currency,x.currency,auto,rates)};val p=if(x.limit>0)(spent/x.limit).coerceIn(0.0,1.0).toFloat()else 0f
   ElevatedCard(Modifier.fillMaxWidth(),elevation=CardDefaults.elevatedCardElevation(5.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(x.name,fontWeight=FontWeight.Bold);Text(listOf(x.period,x.category.ifBlank{"Все категории"},x.accountName.ifBlank{"Все счета"}).joinToString(" • "),style=MaterialTheme.typography.bodySmall)};TextButton(onClick={remove(x)}){Text("Удалить")}};LinearProgressIndicator(progress={p},Modifier.fillMaxWidth());Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(money(spent,x.currency));Text(money(x.limit,x.currency),fontWeight=FontWeight.Bold)};Text(if(spent>=x.limit)"Лимит превышен"else if(spent>=x.limit*.9)"Осталось меньше 10%"else if(spent>=x.limit*.75)"Использовано больше 75%"else"В норме",color=if(spent>=x.limit)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)}}}
  }
 }
}
@Composable
private fun BudgetDialog(accounts:List<Account>,categories:List<Category>,c:String,close:()->Unit,save:(Budget)->Unit){
 var n by remember{mutableStateOf("")};var lim by remember{mutableStateOf("")};var cat by remember{mutableStateOf("")};var acc by remember{mutableStateOf("")};var per by remember{mutableStateOf("Месяц")}
 AlertDialog(onDismissRequest=close,title={Text("Новый бюджет")},text={Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(n,{n=it},label={Text("Название")},modifier=Modifier.fillMaxWidth(),singleLine=true);OutlinedTextField(lim,{lim=it},label={Text("Лимит $c")},modifier=Modifier.fillMaxWidth(),singleLine=true);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(per=="Месяц",{per="Месяц"},label={Text("Месяц")});FilterChip(per=="Неделя",{per="Неделя"},label={Text("Неделя")})};Text("Категория",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(categories){z->FilterChip(cat==z.name,{cat=z.name},label={Text(iconText(z.icon)+" "+z.name)})}};Text("Счёт",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(accounts){z->FilterChip(acc==z.name,{acc=z.name},label={Text(iconText(z.icon)+" "+z.name)})}}}},confirmButton={Button(onClick={save(Budget(name=n.trim(),category=cat,accountName=acc,limit=lim.replace(',','.').toDoubleOrNull()?:0.0,currency=c,period=per))},enabled=n.isNotBlank()&&(lim.replace(',','.').toDoubleOrNull()?:0.0)>0){Text("Создать")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun Debts(items:List<Debt>,add:()->Unit,remove:(Debt)->Unit){
    Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){
        Text("Долги",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Button(add){Text("+")}
    }
    items.forEach{d->
        Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(if(d.mine)"Я должен: "+d.person else "Мне должны: "+d.person,fontWeight=FontWeight.Bold)
                Text("%.2f RUB".format(d.amount),style=MaterialTheme.typography.titleMedium)
                Text(if(d.interest)"Проценты: %.2f%%".format(d.interestRate) else "Без процентов",color=MaterialTheme.colorScheme.primary)
                if(d.note.isNotBlank())Text(d.note)
                TextButton(onClick={remove(d)}){Text("Удалить")}
            }
        }
    }
}
@Composable private fun Goals(items:List<Goal>,add:()->Unit,progress:(Goal)->Unit,remove:(Goal)->Unit){
    Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){
        Text("Цели",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Button(add){Text("+")}
    }
    items.forEach{g->
        Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(g.name,fontWeight=FontWeight.Bold)
                Text(money(g.saved,g.currency)+" / "+money(g.target,g.currency))
                LinearProgressIndicator({if(g.target>0)(g.saved/g.target).toFloat().coerceIn(0f,1f) else 0f},Modifier.fillMaxWidth())
                Text(String.format(Locale.getDefault(),"%.1f%%",if(g.target>0)g.saved/g.target*100 else 0.0),style=MaterialTheme.typography.bodySmall)
                if(g.deadline.isNotBlank())Text("Срок: "+g.deadline)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    TextButton(onClick={progress(g)}){Text("Пополнить")}
                    TextButton(onClick={remove(g)}){Text("Удалить")}
                }
            }
        }
    }
}
@Composable private fun Reminders(items:List<Reminder>,add:()->Unit,toggle:(Reminder)->Unit,remove:(Reminder)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Напоминания",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}};items.forEach{r->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(r.title,fontWeight=FontWeight.Bold);Text(r.date+" • "+r.repeat)};Switch(r.done,{toggle(r)});TextButton(onClick={remove(r)}){Text("Удалить")}}}}
}

private fun parseReceiptDraft(text:String, account:Account?, categories:List<Category>):Transaction? {
    if(account==null) return null
    val amountRegex=Regex("""(?i)(итого|к оплате|сумма|total|amount)[^0-9]{0,20}([0-9]{1,6}(?:[ .][0-9]{3})*(?:[.,][0-9]{2})?)""")
    val fallbackRegex=Regex("""[0-9]{1,6}(?:[ .][0-9]{3})*(?:[.,][0-9]{2})""")
    val keyword=amountRegex.findAll(text).lastOrNull()?.groupValues?.getOrNull(2)
    val raw=keyword ?: fallbackRegex.findAll(text).lastOrNull()?.value ?: return null
    val amount=raw.replace(" ","").replace(".","").replace(',','.').toDoubleOrNull() ?: return null
    val lines=text.lines().map{it.trim()}.filter{it.isNotBlank()}
    val title=lines.firstOrNull()?.take(60)?.ifBlank{"Покупка"} ?: "Покупка"
    val dateMatch=Regex("""([0-3][0-9])[./-]([0-1][0-9])[./-]([0-9]{4})""").find(text)
    val timestamp=dateMatch?.let{
        runCatching{SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).parse(it.value.replace('/','.').replace('-','.'))?.time}.getOrNull()
    } ?: System.currentTimeMillis()
    val category=categories.firstOrNull{it.name.equals("Покупки",true)}?.name ?: categories.firstOrNull()?.name ?: "Без категории"
    return Transaction(title=title,amount=amount,income=false,accountName=account.name,category=category,timestamp=timestamp,currency=account.currency,operationType="expense")
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
                        label = { Text(code) }
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

@Composable private fun SettingsDialog(c: String, auto: Boolean, theme: String, style: String, menu: Set<String>, time: Long, save: (String, Boolean, String, String, Set<String>) -> Unit, backup: () -> String, close: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Настройки") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 560.dp)) {
                item {
                    Text("Основная валюта", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { currencies.forEach { code -> FilterChip(c == code, { save(code, auto, theme, style, menu) }, label = { Text(code) }) } }
                }
                item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Автоконвертация"); Switch(auto, { save(c, it, theme, style, menu) }) } }
                item {
                    Text("Тема", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная").forEach { (v, label) -> FilterChip(theme == v, { save(c, auto, v, style, menu) }, label = { Text(label) }) }
                    }
                }
                item {
                    Text("Стиль", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("midnight" to "Midnight", "platinum" to "Platinum", "emerald" to "Emerald").forEach { (v, label) -> FilterChip(style == v, { save(c, auto, theme, v, menu) }, label = { Text(label) }) }
                    }
                }
                item {
                    Text("Резервная копия", fontWeight = FontWeight.Bold)
                    Text("Экспортирует счета, операции, категории, бюджеты, долги, цели, напоминания и настройки в JSON.",style=MaterialTheme.typography.bodySmall)
                    Button(onClick={
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{
                            type="application/json"
                            putExtra(Intent.EXTRA_TEXT,backup())
                        },"Сохранить резервную копию"))
                    }){Text("Экспортировать JSON")}
                }
                item { Text("Функции в меню", fontWeight = FontWeight.Bold) }
                items(pages) { p ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(p)
                        Switch(p in menu, { enabled ->
                            val updated = menu.toMutableSet()
                            if (enabled) updated.add(p) else updated.remove(p)
                            save(c, auto, theme, style, updated)
                        })
                    }
                }
                if (time > 0) item { Text("Курсы обновлены: " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(time)), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(close) { Text("Закрыть") } }
    )
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
private fun TransactionDialog(accounts: List<Account>,categories: List<Category>,defaultIncome:Boolean,source:Transaction?,close:()->Unit,save:(Transaction)->Unit){
 var n by remember{mutableStateOf(source?.title?:"")};var a by remember{mutableStateOf(source?.amount?.toString()?:"")}
 var type by remember{mutableStateOf(source?.operationType?:if(defaultIncome)"income"else"expense")};var cat by remember{mutableStateOf(source?.category?:categories.firstOrNull()?.name.orEmpty())}
 var acc by remember{mutableStateOf(source?.accountName?:accounts.firstOrNull()?.name.orEmpty())};var toAcc by remember{mutableStateOf(source?.toAccountName?:accounts.firstOrNull{it.name!=acc}?.name.orEmpty())}
 var note by remember{mutableStateOf(source?.note?:"")};var rep by remember{mutableStateOf(source?.repeat?:"Не повторять")}
 var dateText by remember{mutableStateOf(source?.timestamp?.let{SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(it))}?:SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date()))}
 val cc=accounts.firstOrNull{it.name==acc}?.currency?:"RUB";val v=a.replace(',','.').toDoubleOrNull();val parsed=runCatching{SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).parse(dateText)?.time}.getOrNull()?:System.currentTimeMillis();val other=accounts.filter{it.name!=acc}
 AlertDialog(onDismissRequest=close,title={Text(if(type=="transfer")"Новый перевод"else if(type=="income")"Новый доход"else"Новый расход")},text={
  Column(Modifier.heightIn(max=580.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   OutlinedTextField(n,{n=it},label={Text("Описание")},modifier=Modifier.fillMaxWidth(),singleLine=true);OutlinedTextField(a,{a=it},label={Text("Сумма $cc")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   Text("Тип операции",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){FilterChip(type=="expense",{type="expense"},label={Text("Расход")});FilterChip(type=="income",{type="income"},label={Text("Доход")});FilterChip(type=="transfer",{type="transfer"},label={Text("Перевод")})}
   Text("Счёт / карта",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(accounts){x->FilterChip(acc==x.name,{acc=x.name},label={Text(iconText(x.icon)+" "+x.name)})}}
   if(type=="transfer"){Text("Куда",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(other){x->FilterChip(toAcc==x.name,{toAcc=x.name},label={Text(iconText(x.icon)+" "+x.name)})}}}
   else{Text("Категория",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){items(categories){x->FilterChip(cat==x.name,{cat=x.name},label={Text(iconText(x.icon)+" "+x.name)})}};OutlinedTextField(cat,{cat=it},label={Text("Своя категория")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
   OutlinedTextField(note,{note=it},label={Text("Комментарий / заметка")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=3);OutlinedTextField(dateText,{dateText=it},label={Text("Дата и время")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   Text("Повтор",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("Не повторять","Ежедневно","Еженедельно","Ежемесячно","Ежегодно")){x->FilterChip(rep==x,{rep=x},label={Text(x)})}}
  }
},confirmButton={Button(onClick={save(Transaction(id=source?.id?:System.currentTimeMillis(),title=n.trim(),amount=v?:0.0,income=type=="income",accountName=acc,category=if(type=="transfer")"Перевод"else cat.ifBlank{"Без категории"},timestamp=parsed,currency=cc,operationType=type,toAccountName=if(type=="transfer")toAcc else "",note=note.trim(),repeat=rep))},enabled=n.isNotBlank()&&v!=null&&v>0&&acc.isNotBlank()&&(type!="transfer"||toAcc.isNotBlank())){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
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
private fun DebtDialog(close:()->Unit,save:(Debt)->Unit){
 var p by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var mine by remember{mutableStateOf(false)};var interest by remember{mutableStateOf(false)};var rate by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
 val amount=a.replace(',','.').toDoubleOrNull();val rateValue=rate.replace(',','.').toDoubleOrNull()?:0.0
 AlertDialog(onDismissRequest=close,title={Text("Новый долг")},text={
  Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   OutlinedTextField(p,{p=it},label={Text("Человек")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(a,{a=it},label={Text("Сумма RUB")},modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(mine,{mine=true},label={Text("Я должен")});FilterChip(!mine,{mine=false},label={Text("Мне должны")})}
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Начислять проценты",Modifier.weight(1f));Switch(interest,{interest=it})}
   if(interest) OutlinedTextField(rate,{rate=it},label={Text("Ставка, % годовых")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   OutlinedTextField(note,{note=it},label={Text("Заметка")},modifier=Modifier.fillMaxWidth())
  }
 },confirmButton={Button({save(Debt(person=p,amount=amount?:0.0,mine=mine,interest=interest,interestRate=rateValue,note=note.trim()))},enabled=p.isNotBlank()&&amount!=null&&amount>0&&(!interest||rateValue>=0)){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
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


