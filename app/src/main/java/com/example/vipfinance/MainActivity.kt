
// ============================================================================
package com.example.vipfinance

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.vipfinance.ui.theme.VIPFinanceTheme
import java.text.SimpleDateFormat
import java.util.*

private val currencies = listOf(
    "RUB", "GBP", "EUR", "USD", "CNY",
    "JPY", "CHF", "CAD", "AUD", "PLN"
)

private val pages = listOf(
    "Главная", "Операции", "Счета", "Категории", "Бюджеты",
    "Аналитика", "Конвертер", "Долги", "Цели", "Напоминания", "Чеки"
)

private val defaultCategories = listOf(
    Category(name = "Продукты", icon = "shopping_cart", color = "#43A047"),
    Category(name = "Транспорт", icon = "directions_car", color = "#1E88E5"),
    Category(name = "Жильё", icon = "home", color = "#8E24AA"),
    Category(name = "Зарплата", icon = "payments", color = "#00897B"),
    Category(name = "Развлечения", icon = "movie", color = "#FB8C00"),
    Category(name = "Здоровье", icon = "favorite", color = "#E53935"),
    Category(name = "Покупки", icon = "shopping_bag", color = "#6D4C41"),
    Category(name = "Связь", icon = "phone", color = "#3949AB"),
    Category(name = "Подписки", icon = "subscriptions", color = "#5E35B1"),
    Category(name = "Образование", icon = "school", color = "#039BE5"),
    Category(name = "Путешествия", icon = "flight", color = "#00ACC1"),
    Category(name = "Другое", icon = "category", color = "#757575")
)

private val iconChoices = listOf(
    "account_balance", "credit_card", "wallet", "savings", "shopping_cart",
    "home", "directions_car", "payments", "favorite", "phone",
    "school", "flight", "movie", "subscriptions", "category"
)

private val colorChoices = listOf(
    "#5B35F5", "#00A7B5", "#007A5A", "#E53935", "#FB8C00",
    "#1E88E5", "#8E24AA", "#D81B60", "#6D4C41", "#757575"
)

private fun iconText(icon: String): String = when (icon) {
    "account_balance" -> "▥"
    "credit_card" -> "▣"
    "wallet" -> "◫"
    "savings" -> "◉"
    "shopping_cart" -> "🛒"
    "home" -> "⌂"
    "directions_car" -> "🚗"
    "payments" -> "₽"
    "favorite" -> "♥"
    "phone" -> "☎"
    "school" -> "◆"
    "flight" -> "✈"
    "movie" -> "▶"
    "subscriptions" -> "◉"
    "shopping_bag" -> "▱"
    else -> "•"
}

private fun uiColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF5B35F5))

private fun sym(c: String): String = when (c) {
    "GBP" -> "£"
    "USD" -> "$"
    "EUR" -> "€"
    "RUB" -> "₽"
    "CNY" -> "¥"
    "JPY" -> "¥"
    "CHF" -> "Fr"
    "CAD" -> "C$"
    "AUD" -> "A$"
    "PLN" -> "zł"
    else -> c
}

private fun money(v: Double, c: String): String =
    sym(c) + "%.2f".format(Locale.getDefault(), v)

private fun conv(
    v: Double,
    from: String,
    to: String,
    auto: Boolean,
    r: Map<String, Double>
): Double = if (auto) ExchangeRates.convert(v, from, to, r) else v
class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        enableEdgeToEdge()
        val s = FinanceStore(this)
        setContent {
            VIPFinanceTheme(s.loadTheme(), s.loadStyle()) {
                FinanceApp(s)
            }
        }
    }
}

// ============================================================================
// PREMIUM UI COMPONENTS
// ============================================================================

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    gradient: List<Color>? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = gradient?.let { Color.Transparent }
                ?: MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        if (gradient != null) {
            Box(
                modifier = Modifier
                    .background(Brush.linearGradient(gradient))
                    .padding(20.dp)
            ) { content() }
        } else {
            Box(modifier = Modifier.padding(20.dp)) { content() }
        }
    }
}

@Composable
fun PremiumDialog(
    onDismiss: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                content()
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp,
                        Alignment.End
                    )
                ) { actions() }
            }
        }
    }
}

// ============================================================================
// MAIN APP
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(s: FinanceStore) {
    var accounts by remember { mutableStateOf(s.loadAccounts()) }
    var tx by remember { mutableStateOf(s.loadTransactions()) }
    var debts by remember { mutableStateOf(s.loadDebts()) }
    var goals by remember { mutableStateOf(s.loadGoals()) }
    var reminders by remember { mutableStateOf(s.loadReminders()) }
    var budgets by remember { mutableStateOf(s.loadBudgets()) }
    var categories by remember {
        mutableStateOf(s.loadCategories().ifEmpty { defaultCategories })
    }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var page by remember { mutableStateOf("Главная") }
    var currency by remember { mutableStateOf(s.loadCurrency()) }
    var auto by remember { mutableStateOf(s.loadAutoConversion()) }
    var theme by remember { mutableStateOf(s.loadTheme()) }
    var style by remember { mutableStateOf(s.loadStyle()) }
    var menu by remember {
        mutableStateOf(
            s.loadMenu()
                .filter { it in pages }
                .toSet()
                .ifEmpty { pages.toSet() }
        )
    }
    var rates by remember { mutableStateOf(s.loadRates()) }
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
    val drawerState = rememberDrawerState(
        if (drawerOpen) DrawerValue.Open else DrawerValue.Closed
    )

    LaunchedEffect(drawerOpen) {
        if (drawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(menu) {
        if (page !in menu && menu.isNotEmpty()) page = menu.first()
    }
    LaunchedEffect(Unit) {
        if (s.loadCategories().isEmpty()) s.saveCategories(categories)
        loading = true
        runCatching { ExchangeRates.loadEcbRates() }
            .onSuccess { rates = it; s.saveRates(it) }
        loading = false
    }

    fun add(t: Transaction) {
        tx = tx + t
        s.saveTransactions(tx)
        val source = accounts.firstOrNull { it.name == t.accountName }
        if (t.operationType == "transfer") {
            val target = accounts.firstOrNull { it.name == t.toAccountName }
            if (source != null && target != null) {
                val converted = ExchangeRates.convert(
                    t.amount, source.currency, target.currency, rates
                )
                accounts = accounts.map {
                    when (it.name) {
                        source.name -> it.copy(balance = it.balance - t.amount)
                        target.name -> it.copy(balance = it.balance + converted)
                        else -> it
                    }
                }
                s.saveAccounts(accounts)
            }
        } else if (source != null) {
            val delta = if (t.income) t.amount else -t.amount
            accounts = accounts.map {
                if (it.name == t.accountName)
                    it.copy(balance = it.balance + delta)
                else it
            }
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
                val converted = ExchangeRates.convert(
                    t.amount, source.currency, target.currency, rates
                )
                accounts = accounts.map {
                    when (it.name) {
                        source.name -> it.copy(balance = it.balance + t.amount)
                        target.name -> it.copy(balance = it.balance - converted)
                        else -> it
                    }
                }
                s.saveAccounts(accounts)
            }
        } else if (source != null) {
            val delta = if (t.income) -t.amount else t.amount
            accounts = accounts.map {
                if (it.name == t.accountName)
                    it.copy(balance = it.balance + delta)
                else it
            }
            s.saveAccounts(accounts)
        }
    }

    val visible = accounts.filter { !it.hidden }
    val total = visible.sumOf {
        conv(it.balance, it.currency, currency, auto, rates)
    }
    val shown0 = filter?.let { name ->
        tx.filter { it.accountName == name }
    } ?: tx
    val shown = shown0.filter {
        search.isBlank() ||
            it.title.contains(search, true) ||
            it.category.contains(search, true) ||
            it.accountName.contains(search, true)
    }
    val inc = shown
        .filter { it.income && it.operationType != "transfer" }
        .sumOf { conv(it.amount, it.currency, currency, auto, rates) }
    val exp = shown
        .filter { !it.income && it.operationType != "transfer" }
        .sumOf { conv(it.amount, it.currency, currency, auto, rates) }

    fun saveSettings(
        c: String,
        a: Boolean,
        t: String,
        st: String,
        m: Set<String>
    ) {
        currency = c
        auto = a
        theme = t
        style = st
        menu = m.filter { it in pages }.toSet()
        s.saveCurrency(c)
        s.saveAutoConversion(a)
        s.saveTheme(t)
        s.saveStyle(st)
        s.saveMenu(menu.toSet())
    }
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
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            "VIP Finance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Управление капиталом",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
                    items(menu.toList()) { item ->
                        NavigationDrawerItem(
                            label = { Text(item, fontWeight = FontWeight.Medium) },
                            selected = page == item,
                            onClick = { page = item; drawerOpen = false },
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text("Настройки", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = { dialog = "settings"; drawerOpen = false },
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(page, fontWeight = FontWeight.Bold)
                            if (page == "Главная") {
                                Text(
                                    "Финансовый обзор",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(onClick = { drawerOpen = true }) {
                            Text(
                                "☰",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { dialog = "settings" }) {
                            Text(
                                "⚙",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (page == "Главная" || page == "Операции") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { dialog = "expense" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) { Text("− Расход", fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = { dialog = "income" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) { Text("+ Доход", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(pad)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (page) {
                    "Главная" -> Home(
                        total, currency, accounts, auto, rates,
                        selected, inc, exp
                    ) { selected = it }
                    "Операции" -> Operations(
                        shown, accounts, filter, { filter = it },
                        { dialog = "expense" }, ::remove,
                        currency, auto, rates, search, { search = it },
                        newest, { newest = it },
                        { original ->
                            repeatSource = original
                            dialog = if (original.income) "income" else "expense"
                        }
                    )
                    "Счета" -> Accounts(
                        accounts, currency, auto, rates,
                        { dialog = "account" }, { selected = it }
                    ) { n ->
                        val updated = accounts.map {
                            if (it.name == n) it.copy(hidden = !it.hidden) else it
                        }
                        accounts = updated
                        s.saveAccounts(updated)
                    }
                    "Категории" -> Categories(
                        categories,
                        { editingCategory = null; dialog = "category" },
                        { editingCategory = it; dialog = "category" }
                    ) { c0 ->
                        categories = categories.filterNot { it.id == c0.id }
                        s.saveCategories(categories)
                    }
                    "Бюджеты" -> Budgets(
                        budgets, tx, currency, auto, rates, accounts,
                        { dialog = "budget" }
                    ) { b0 ->
                        budgets = budgets.filterNot { it.id == b0.id }
                        s.saveBudgets(budgets)
                    }
                    "Аналитика" -> Analytics(tx, currency, auto, rates)
                    "Конвертер" -> Converter(currency, rates, loading)
                    "Долги" -> Debts(debts, { dialog = "debt" }) { d ->
                        debts = debts.filterNot { it.id == d.id }
                        s.saveDebts(debts)
                    }
                    "Цели" -> Goals(
                        goals,
                        { dialog = "goal" },
                        { g -> editingGoal = g; dialog = "goalProgress" }
                    ) { g ->
                        goals = goals.filterNot { it.id == g.id }
                        s.saveGoals(goals)
                    }
                    "Напоминания" -> Reminders(
                        reminders,
                        { dialog = "reminder" },
                        { r ->
                            reminders = reminders.map {
                                if (it.id == r.id) it.copy(done = !it.done) else it
                            }
                            s.saveReminders(reminders)
                        },
                        { r ->
                            reminders = reminders.filterNot { it.id == r.id }
                            s.saveReminders(reminders)
                        }
                    )
                    "Чеки" -> Receipt(
                        receiptUri, receiptText,
                        { u -> receiptUri = u; receiptText = "" },
                        { t -> receiptText = t },
                        accounts.firstOrNull(),
                        categories
                    ) { draft ->
                        receiptDraft = draft
                        dialog = "receiptExpense"
                    }
                }
            }
        }
    }

    when (dialog) {
        "account" -> AccountDialog({ dialog = "" }) { n, b, t, c, icon, iconColor ->
            accounts = accounts + Account(n, b, false, t, c, icon, iconColor)
            s.saveAccounts(accounts)
            dialog = ""
        }
        "category" -> CategoryDialog(
            editingCategory,
            { dialog = ""; editingCategory = null }
        ) { category ->
            categories = if (categories.any { it.id == category.id }) {
                categories.map { if (it.id == category.id) category else it }
            } else {
                categories + category
            }
            s.saveCategories(categories)
            dialog = ""
            editingCategory = null
        }
        "expense" -> TransactionDialog(
            accounts, categories, false, repeatSource,
            { dialog = ""; repeatSource = null }
        ) { add(it); dialog = ""; repeatSource = null }
        "income" -> TransactionDialog(
            accounts, categories, true, repeatSource,
            { dialog = ""; repeatSource = null }
        ) { add(it); dialog = ""; repeatSource = null }
        "debt" -> DebtDialog({ dialog = "" }) {
            debts = debts + it
            s.saveDebts(debts)
            dialog = ""
        }
        "goal" -> GoalDialog(currency, { dialog = "" }) {
            goals = goals + it
            s.saveGoals(goals)
            dialog = ""
        }
        "goalProgress" -> GoalProgressDialog(
            editingGoal,
            { dialog = ""; editingGoal = null }
        ) { updated ->
            goals = goals.map { if (it.id == updated.id) updated else it }
            s.saveGoals(goals)
            dialog = ""
            editingGoal = null
        }
        "reminder" -> ReminderDialog({ dialog = "" }) {
            reminders = reminders + it
            s.saveReminders(reminders)
            dialog = ""
        }
        "budget" -> BudgetDialog(
            accounts, categories, currency,
            { dialog = "" }
        ) {
            budgets = budgets + it
            s.saveBudgets(budgets)
            dialog = ""
        }
        "receiptExpense" -> TransactionDialog(
            accounts, categories, false, receiptDraft,
            { dialog = ""; receiptDraft = null }
        ) { add(it); dialog = ""; receiptDraft = null }
        "settings" -> SettingsDialog(
            currency, auto, theme, style, menu,
            { c: String, a: Boolean, t: String, st: String, m: Set<String> ->
                saveSettings(c, a, t, st, m)
            },
            { s.exportBackupJson() }
        ) { dialog = "" }
    }
}
// ============================================================================
// HOME SCREEN
// ============================================================================

@Composable
private fun Home(
    total: Double,
    c: String,
    accounts: List<Account>,
    auto: Boolean,
    r: Map<String, Double>,
    selected: String?,
    income: Double,
    expense: Double,
    pick: (String?) -> Unit
) {
    val visible = accounts.filter { !it.hidden }
    val selectedBalance = selected?.let { n ->
        visible.firstOrNull { it.name == n }
            ?.let { conv(it.balance, it.currency, c, auto, r) }
    } ?: total
    val net = income - expense

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            PremiumCard(
                gradient = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selected == null) "ОБЩИЙ КАПИТАЛ" else "БАЛАНС • $selected",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = money(selectedBalance, c),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                "Доходы",
                                color = MaterialTheme.colorScheme.onPrimary.copy(0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                money(income, c),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                "Расходы",
                                color = MaterialTheme.colorScheme.onPrimary.copy(0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                money(expense, c),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                "Остаток",
                                color = MaterialTheme.colorScheme.onPrimary.copy(0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                money(net, c),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Счета",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${visible.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    FilterChip(
                        selected = selected == null,
                        onClick = { pick(null) },
                        label = { Text("Все") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(visible) { a ->
                    FilterChip(
                        selected = selected == a.name,
                        onClick = { pick(a.name) },
                        label = { Text(a.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visible) { a ->
                    PremiumCard(
                        modifier = Modifier.width(220.dp),
                        gradient = listOf(
                            uiColor(a.iconColor),
                            uiColor(a.iconColor).copy(alpha = 0.7f)
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        iconText(a.icon),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    a.currency,
                                    color = Color.White.copy(0.8f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                a.name,
                                color = Color.White.copy(0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                money(conv(a.balance, a.currency, c, auto, r), c),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                a.type,
                                color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// OPERATIONS SCREEN
// ============================================================================

@Composable
private fun Operations(
    ts: List<Transaction>,
    accounts: List<Account>,
    filter: String?,
    setFilter: (String?) -> Unit,
    add: () -> Unit,
    remove: (Transaction) -> Unit,
    c: String,
    auto: Boolean,
    r: Map<String, Double>,
    search: String,
    setSearch: (String) -> Unit,
    newest: Boolean,
    setNewest: (Boolean) -> Unit,
    repeat: (Transaction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        "Доходы",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        money(
                            ts.filter { it.income && it.operationType != "transfer" }
                                .sumOf { conv(it.amount, it.currency, c, auto, r) },
                            c
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            PremiumCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        "Расходы",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        money(
                            ts.filter { !it.income && it.operationType != "transfer" }
                                .sumOf { conv(it.amount, it.currency, c, auto, r) },
                            c
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
OutlinedTextField(
            value = search,
            onValueChange = setSearch,
            label = { Text("Поиск операций") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Сначала новые",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(checked = newest, onCheckedChange = setNewest)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filter == null,
                    onClick = { setFilter(null) },
                    label = { Text("Все") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            items(accounts) { a ->
                FilterChip(
                    selected = filter == a.name,
                    onClick = { setFilter(a.name) },
                    label = { Text(a.name) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(
                if (newest) ts.sortedByDescending { it.timestamp }
                else ts.sortedBy { it.timestamp }
            ) { t ->
                val transfer = t.operationType == "transfer"
                val isInc = t.income && !transfer

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (transfer) MaterialTheme.colorScheme.secondaryContainer
                                    else if (isInc) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (transfer) "⇄" else if (isInc) "↗" else "↘",
                                color = if (transfer) MaterialTheme.colorScheme.secondary
                                else if (isInc) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (transfer) "${t.accountName} → ${t.toAccountName}"
                                else "${t.category} • ${t.accountName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                SimpleDateFormat(
                                    "dd.MM.yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date(t.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                (if (transfer) "⇄" else if (isInc) "+" else "−") +
                                    " " + money(conv(t.amount, t.currency, c, auto, r), c),
                                fontWeight = FontWeight.Bold,
                                color = if (transfer) MaterialTheme.colorScheme.secondary
                                else if (isInc) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            Row {
                                TextButton(onClick = { repeat(t) }) {
                                    Text("Повторить", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { remove(t) }) {
                                    Text(
                                        "Удалить",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// CATEGORIES SCREEN
// ============================================================================

@Composable
private fun Categories(
    items: List<Category>,
    add: () -> Unit,
    edit: (Category) -> Unit,
    remove: (Category) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Категории",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Стандартные и ваши категории",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = add,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+ Добавить") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { c0 ->
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(uiColor(c0.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                iconText(c0.icon),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(c0.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { edit(c0) }) { Text("Изменить") }
                        TextButton(onClick = { remove(c0) }) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// ACCOUNTS SCREEN
// ============================================================================

@Composable
private fun Accounts(
    items: List<Account>,
    c: String,
    auto: Boolean,
    r: Map<String, Double>,
    add: () -> Unit,
    select: (String) -> Unit,
    toggle: (String) -> Unit
) {
    val visible = items.filter { !it.hidden }
    val total = visible.sumOf { conv(it.balance, it.currency, c, auto, r) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Счета и карты",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${items.size} финансовых объектов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = add,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+ Добавить") }
        }

        PremiumCard(
            gradient = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface
            )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Общий баланс",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    money(total, c),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Учитываются только открытые счета",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.name }) { account ->
                val converted = conv(account.balance, account.currency, c, auto, r)
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(uiColor(account.iconColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        iconText(account.icon),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        account.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        account.type + " • " + account.currency,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    money(converted, c),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (converted >= 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                                if (account.currency != c) {
                                    Text(
                                        "≈ " + money(converted, c),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Text(
                                if (account.hidden) "Скрыт из общего баланса"
                                else "В общем балансе",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { toggle(account.name) }) {
                                Text(if (account.hidden) "Показать" else "Скрыть")
                            }
                        }
                    }
                }
            }
        }
    }
}
// ============================================================================
// ANALYTICS SCREEN
// ============================================================================

@Composable
private fun Analytics(
    tx: List<Transaction>,
    c: String,
    auto: Boolean,
    r: Map<String, Double>
) {
    var preset by remember { mutableStateOf("Месяц") }
    val now = Calendar.getInstance()
    val from = when (preset) {
        "Сегодня" -> now.timeInMillis
        "7 дней" -> now.timeInMillis - 7L * 24 * 60 * 60 * 1000
        "14 дней" -> now.timeInMillis - 14L * 24 * 60 * 60 * 1000
        else -> now.timeInMillis - 30L * 24 * 60 * 60 * 1000
    }
    val periodTx = tx.filter {
        it.timestamp in from..now.timeInMillis &&
            it.operationType != "transfer"
    }
    val income = periodTx
        .filter { it.income }
        .sumOf { conv(it.amount, it.currency, c, auto, r) }
    val expense = periodTx
        .filter { !it.income }
        .sumOf { conv(it.amount, it.currency, c, auto, r) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Сегодня", "7 дней", "14 дней", "Месяц").forEach { p ->
                FilterChip(
                    selected = preset == p,
                    onClick = { preset = p },
                    label = { Text(p) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        PremiumCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Аналитика периода",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            "Доходы",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            money(income, c),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            "Расходы",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            money(expense, c),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val total = income + expense
                    if (total > 0) {
                        val incWidth = (size.width * (income / total).toFloat())
                            .coerceAtMost(size.width)
                        drawRoundRect(
                            color = MaterialTheme.colorScheme.primary,
                            size = androidx.compose.ui.geometry.Size(incWidth, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f)
                        )
                        drawRoundRect(
                            color = MaterialTheme.colorScheme.error,
                            topLeft = androidx.compose.ui.geometry.Offset(incWidth, 0f),
                            size = androidx.compose.ui.geometry.Size(
                                size.width - incWidth, size.height
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// CONVERTER SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Converter(
    currency: String,
    rates: Map<String, Double>,
    loading: Boolean
) {
    var amount by remember { mutableStateOf("1000") }
    var from by remember { mutableStateOf("RUB") }
    var to by remember { mutableStateOf("USD") }
    val num = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val res = if (loading) "..." else money(
        ExchangeRates.convert(num, from, to, rates), to
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PremiumCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Конвертер валют",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var expanded1 by remember { mutableStateOf(false) }
                    var expanded2 by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded1,
                        onExpandedChange = { expanded1 = !expanded1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = from,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Из") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded1,
                            onDismissRequest = { expanded1 = false }
                        ) {
                            currencies.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = { from = c; expanded1 = false }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = expanded2,
                        onExpandedChange = { expanded2 = !expanded2 },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = to,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("В") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded2,
                            onDismissRequest = { expanded2 = false }
                        ) {
                            currencies.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = { to = c; expanded2 = false }
                                )
                            }
                        }
                    }
                }
                PremiumCard(
                    gradient = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "Результат: $res",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ============================================================================
// DEBTS SCREEN
// ============================================================================

@Composable
private fun Debts(
    items: List<Debt>,
    add: () -> Unit,
    remove: (Debt) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Долги",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Управление обязательствами",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = add,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+ Добавить") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { d ->
                PremiumCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween
                        ) {
                            Text(
                                d.person,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                money(d.amount, "RUB"),
                                fontWeight = FontWeight.Bold,
                                color = if (d.mine) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            if (d.mine) "Я должен" else "Мне должны",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (d.mine) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        if (d.interest) {
                            Text(
                                "Ставка: ${d.interestRate}% годовых",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (d.dueDate.isNotBlank()) {
                            Text(
                                "Срок: ${d.dueDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (d.note.isNotBlank()) {
                            Text(
                                d.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { remove(d) }) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// GOALS SCREEN
// ============================================================================

@Composable
private fun Goals(
    items: List<Goal>,
    add: () -> Unit,
    progress: (Goal) -> Unit,
    remove: (Goal) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Цели накоплений",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Достигайте финансового благополучия",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = add,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+ Добавить") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { g ->
                PremiumCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween
                        ) {
                            Text(
                                g.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                money(g.target, g.currency),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val pct = (g.saved / g.target).coerceIn(0.0, 1.0)
                        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                            drawRoundRect(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
                                size = androidx.compose.ui.geometry.Size(size.width, size.height)
                            )
                            drawRoundRect(
                                color = MaterialTheme.colorScheme.primary,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width * pct.toFloat(), size.height
                                )
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Сохранено: ${money(g.saved, g.currency)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${(pct * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (g.deadline.isNotBlank()) {
                            Text(
                                "Срок: ${g.deadline}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween
                        ) {
                            FilledTonalButton(
                                onClick = { progress(g) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Пополнить") }
                            TextButton(onClick = { remove(g) }) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
// ============================================================================
// REMINDERS SCREEN
// ============================================================================

@Composable
private fun Reminders(
    items: List<Reminder>,
    add: () -> Unit,
    toggle: (Reminder) -> Unit,
    remove: (Reminder) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Напоминания",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Не упускайте важные платежи",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = add,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+ Добавить") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { r ->
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = r.done,
                            onCheckedChange = { toggle(r) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.title,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (r.done) TextDecoration.LineThrough
                                else TextDecoration.None
                            )
                            Text(
                                "${r.date} • ${r.repeat}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { remove(r) }) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// RECEIPTS SCREEN
// ============================================================================

@Composable
private fun Receipt(
    uri: Uri?,
    text: String,
    setUri: (Uri?) -> Unit,
    setText: (String) -> Unit,
    account: Account?,
    categories: List<Category>,
    save: (Transaction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Сканирование чека",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        PremiumCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Загрузите фото чека для автоматического распознавания (OCR).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Статус: ${if (uri != null) "Файл выбран" else "Ожидание..."}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (text.isNotBlank()) {
                    Text(
                        "Распознанный текст:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        save(
                            Transaction(
                                title = "Расход по чеку",
                                amount = 0.0,
                                income = false,
                                accountName = account?.name ?: "",
                                category = categories.firstOrNull()?.name ?: "Другое"
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Подтвердить операцию") }
            }
        }
    }
}

// ============================================================================
// BUDGETS SCREEN
// ============================================================================

@Composable
private fun Budgets(
    items: List<Budget>,
    tx: List<Transaction>,
    c: String,
    auto: Boolean,
    r: Map<String, Double>,
    accounts: List<Account>,
    add: () -> Unit,
    remove: (Budget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Бюджеты",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Контроль лимитов расходов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = add,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+ Добавить") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { b ->
                val spent = tx
                    .filter {
                        it.category == b.category &&
                            !it.income &&
                            it.operationType != "transfer"
                    }
                    .sumOf { conv(it.amount, it.currency, c, auto, r) }
                val pct = (spent / b.limit).coerceIn(0.0, 1.0)

                PremiumCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween
                        ) {
                            Text(
                                b.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${b.period}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                            drawRoundRect(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
                                size = androidx.compose.ui.geometry.Size(size.width, size.height)
                            )
                            drawRoundRect(
                                color = if (pct > 0.9) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width * pct.toFloat(), size.height
                                )
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Потрачено: ${money(spent, c)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Лимит: ${money(b.limit, c)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.End
                        ) {
                            TextButton(onClick = { remove(b) }) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// DIALOGS
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDialog(
    close: () -> Unit,
    save: (String, Double, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bal by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Счёт") }
    var cur by remember { mutableStateOf("RUB") }
    var icon by remember { mutableStateOf("account_balance") }
    var color by remember { mutableStateOf("#5B35F5") }

    PremiumDialog(
        onDismiss = close,
        title = "Новый счёт",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = bal,
                    onValueChange = { bal = it },
                    label = { Text("Начальный баланс") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var exp1 by remember { mutableStateOf(false) }
                    var exp2 by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = exp1,
                        onExpandedChange = { exp1 = !exp1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = type,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Тип") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = exp1,
                            onDismissRequest = { exp1 = false }
                        ) {
                            listOf("Счёт", "Карта", "Наличные").forEach { x ->
                                DropdownMenuItem(
                                    text = { Text(x) },
                                    onClick = { type = x; exp1 = false }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = exp2,
                        onExpandedChange = { exp2 = !exp2 },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = cur,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Валюта") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = exp2,
                            onDismissRequest = { exp2 = false }
                        ) {
                            currencies.forEach { x ->
                                DropdownMenuItem(
                                    text = { Text(x) },
                                    onClick = { cur = x; exp2 = false }
                                )
                            }
                        }
                    }
                }
                Text(
                    "Выберите цвет:",
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorChoices) { c ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(uiColor(c))
                                .clickable { color = c }
                                .border(
                                    if (color == c) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    RoundedCornerShape(12.dp)
                                )
                        )
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = {
                    save(
                        name,
                        bal.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        type,
                        cur,
                        icon,
                        color
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Сохранить") }
        }
    )
}
@Composable
private fun CategoryDialog(
    existing: Category?,
    close: () -> Unit,
    save: (Category) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "category") }
    var color by remember { mutableStateOf(existing?.color ?: colorChoices.first()) }

    PremiumDialog(
        onDismiss = close,
        title = if (existing == null) "Новая категория" else "Изменить категорию",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Text("Иконка:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconChoices) { i ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { icon = i }
                                .border(
                                    if (icon == i) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) { Text(iconText(i)) }
                    }
                }
                Text("Цвет:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorChoices) { c ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(uiColor(c))
                                .clickable { color = c }
                                .border(
                                    if (color == c) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    RoundedCornerShape(12.dp)
                                )
                        )
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = {
                    save(
                        Category(
                            existing?.id ?: System.currentTimeMillis(),
                            name.trim(),
                            "expense",
                            icon,
                            color
                        )
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Сохранить") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    isInc: Boolean,
    repeatSource: Transaction?,
    close: () -> Unit,
    save: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf(repeatSource?.title ?: "") }
    var amount by remember { mutableStateOf(repeatSource?.amount?.toString() ?: "") }
    var acc by remember {
        mutableStateOf(repeatSource?.accountName ?: (accounts.firstOrNull()?.name ?: ""))
    }
    var cat by remember {
        mutableStateOf(repeatSource?.category ?: (categories.firstOrNull()?.name ?: "Другое"))
    }
    var note by remember { mutableStateOf(repeatSource?.note ?: "") }
    var rep by remember { mutableStateOf(repeatSource?.repeat ?: "Не повторять") }
    var exp1 by remember { mutableStateOf(false) }
    var exp2 by remember { mutableStateOf(false) }
    var exp3 by remember { mutableStateOf(false) }

    PremiumDialog(
        onDismiss = close,
        title = if (isInc) "Новый доход" else "Новый расход",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                ExposedDropdownMenuBox(
                    expanded = exp1,
                    onExpandedChange = { exp1 = !exp1 }
                ) {
                    OutlinedTextField(
                        value = acc,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Счёт") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp1,
                        onDismissRequest = { exp1 = false }
                    ) {
                        accounts.forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x.name) },
                                onClick = { acc = x.name; exp1 = false }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = exp2,
                    onExpandedChange = { exp2 = !exp2 }
                ) {
                    OutlinedTextField(
                        value = cat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp2,
                        onDismissRequest = { exp2 = false }
                    ) {
                        categories.forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x.name) },
                                onClick = { cat = x.name; exp2 = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2
                )
                ExposedDropdownMenuBox(
                    expanded = exp3,
                    onExpandedChange = { exp3 = !exp3 }
                ) {
                    OutlinedTextField(
                        value = rep,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Повторение") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp3,
                        onDismissRequest = { exp3 = false }
                    ) {
                        listOf("Не повторять", "Еженедельно", "Ежемесячно").forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x) },
                                onClick = { rep = x; exp3 = false }
                            )
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = {
                    save(
                        Transaction(
                            title = title,
                            amount = amount.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            income = isInc,
                            accountName = acc,
                            category = cat,
                            note = note,
                            repeat = rep
                        )
                    )
                },
                enabled = title.isNotBlank() && amount.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Сохранить") }
        }
    )
}
@Composable
private fun DebtDialog(
    close: () -> Unit,
    save: (Debt) -> Unit
) {
    var p by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var mine by remember { mutableStateOf(false) }
    var interest by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    PremiumDialog(
        onDismiss = close,
        title = "Новый долг",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = p,
                    onValueChange = { p = it },
                    label = { Text("Человек") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма RUB") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mine,
                        onClick = { mine = true },
                        label = { Text("Я должен") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = !mine,
                        onClick = { mine = false },
                        label = { Text("Мне должны") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Начислять проценты", Modifier.weight(1f))
                    Switch(checked = interest, onCheckedChange = { interest = it })
                }
                if (interest) {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Ставка, % годовых") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    label = { Text("Срок погашения (дд.мм.гггг)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = {
                    save(
                        Debt(
                            person = p,
                            amount = amount.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            mine = mine,
                            interest = interest,
                            interestRate = rate.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            dueDate = due,
                            note = note
                        )
                    )
                },
                enabled = p.isNotBlank() && amount.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Сохранить") }
        }
    )
}

@Composable
private fun GoalDialog(
    c: String,
    close: () -> Unit,
    save: (Goal) -> Unit
) {
    var n by remember { mutableStateOf("") }
    var t by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }

    PremiumDialog(
        onDismiss = close,
        title = "Новая цель",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = n,
                    onValueChange = { n = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = t,
                    onValueChange = { t = it },
                    label = { Text("Цель ($c)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    value = d,
                    onValueChange = { d = it },
                    label = { Text("Срок") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = {
                    save(
                        Goal(
                            name = n,
                            target = t.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            currency = c,
                            deadline = d
                        )
                    )
                },
                enabled = n.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Сохранить") }
        }
    )
}

@Composable
private fun GoalProgressDialog(
    goal: Goal?,
    close: () -> Unit,
    save: (Goal) -> Unit
) {
    if (goal == null) {
        close()
        return
    }
    var amount by remember { mutableStateOf("") }
    val value = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val newSaved = (goal.saved + value).coerceAtMost(
        goal.target.coerceAtLeast(goal.saved)
    )

    PremiumDialog(
        onDismiss = close,
        title = "Пополнить цель",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    goal.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Сейчас: ${money(goal.saved, goal.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сколько добавить, ${goal.currency}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                if (value > 0) {
                    Text(
                        "После пополнения: ${money(newSaved, goal.currency)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = { save(goal.copy(saved = newSaved)) },
                enabled = value > 0,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Пополнить") }
        }
    )
}

@Composable
private fun ReminderDialog(
    close: () -> Unit,
    save: (Reminder) -> Unit
) {
    var n by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }
    var rep by remember { mutableStateOf("Один раз") }

    PremiumDialog(
        onDismiss = close,
        title = "Напоминание",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = n,
                    onValueChange = { n = it },
                    label = { Text("Что напомнить") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = d,
                    onValueChange = { d = it },
                    label = { Text("Дата") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Один раз", "Еженедельно", "Ежемесячно").forEach { x ->
                        FilterChip(
                            selected = rep == x,
                            onClick = { rep = x },
                            label = { Text(x) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = { save(Reminder(title = n, date = d, repeat = rep)) },
                enabled = n.isNotBlank() && d.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Сохранить") }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDialog(
    accounts: List<Account>,
    categories: List<Category>,
    c: String,
    close: () -> Unit,
    save: (Budget) -> Unit
) {
    var n by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var lim by remember { mutableStateOf("") }
    var per by remember { mutableStateOf("Месяц") }
    var exp1 by remember { mutableStateOf(false) }
    var exp2 by remember { mutableStateOf(false) }

    PremiumDialog(
        onDismiss = close,
        title = "Новый бюджет",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = n,
                    onValueChange = { n = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = exp1,
                    onExpandedChange = { exp1 = !exp1 }
                ) {
                    OutlinedTextField(
                        value = cat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp1,
                        onDismissRequest = { exp1 = false }
                    ) {
                        categories.forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x.name) },
                                onClick = { cat = x.name; exp1 = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = lim,
                    onValueChange = { lim = it },
                    label = { Text("Лимит ($c)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                ExposedDropdownMenuBox(
                    expanded = exp2,
                    onExpandedChange = { exp2 = !exp2 }
                ) {
                    OutlinedTextField(
                        value = per,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Период") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp2,
                        onDismissRequest = { exp2 = false }
                    ) {
                        listOf("Месяц", "Неделя", "Год").forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x) },
                                onClick = { per = x; exp2 = false }
                            )
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = {
                    save(
                        Budget(
                            name = n,
                            category = cat,
                            limit = lim.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            currency = c,
                            period = per
                        )
                    )
                },
                enabled = n.isNotBlank() && lim.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Сохранить") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    currency: String,
    auto: Boolean,
    theme: String,
    style: String,
    menu: Set<String>,
    save: (String, Boolean, String, String, Set<String>) -> Unit,
    export: () -> String,
    close: () -> Unit
) {
    var cur by remember { mutableStateOf(currency) }
    var a by remember { mutableStateOf(auto) }
    var t by remember { mutableStateOf(theme) }
    var s by remember { mutableStateOf(style) }
    var exp1 by remember { mutableStateOf(false) }
    var exp2 by remember { mutableStateOf(false) }
    var exp3 by remember { mutableStateOf(false) }

    PremiumDialog(
        onDismiss = close,
        title = "Настройки",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = exp1,
                    onExpandedChange = { exp1 = !exp1 }
                ) {
                    OutlinedTextField(
                        value = cur,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Основная валюта") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp1,
                        onDismissRequest = { exp1 = false }
                    ) {
                        currencies.forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x) },
                                onClick = { cur = x; exp1 = false }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = exp2,
                    onExpandedChange = { exp2 = !exp2 }
                ) {
                    OutlinedTextField(
                        value = t,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тема") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp2,
                        onDismissRequest = { exp2 = false }
                    ) {
                        listOf("system", "light", "dark").forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x) },
                                onClick = { t = x; exp2 = false }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = exp3,
                    onExpandedChange = { exp3 = !exp3 }
                ) {
                    OutlinedTextField(
                        value = s,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Стиль") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = exp3,
                        onDismissRequest = { exp3 = false }
                    ) {
                        listOf(
                            "platinum", "midnight", "emerald", "royal", "rose"
                        ).forEach { x ->
                            DropdownMenuItem(
                                text = { Text(x) },
                                onClick = { s = x; exp3 = false }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Автоконвертация валют", Modifier.weight(1f))
                    Switch(checked = a, onCheckedChange = { a = it })
                }
                Button(
                    onClick = {
                        val json = export()
                        val clipboard = android.content.ClipboardManager::class.java
                        // В реальном приложении здесь копирование JSON в буфер обмена
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Экспорт резервной копии (JSON)") }
            }
        },
        actions = {
            TextButton(onClick = close) { Text("Отмена") }
            Button(
                onClick = { save(cur, a, t, s, menu) },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Применить") }
        }
)
}