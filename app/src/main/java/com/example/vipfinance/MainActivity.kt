package com.example.vipfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vipfinance.ui.theme.VIPFinanceTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val supportedCurrencies = listOf("GBP", "EUR", "USD", "RUB", "CNY", "JPY", "CHF", "CAD", "AUD", "PLN")

private fun currencySymbol(code: String): String = when (code) {
    "GBP" -> "£"; "USD" -> "$"; "EUR" -> "€"; "RUB" -> "₽"; "CNY" -> "¥"; "JPY" -> "¥"
    "CHF" -> "Fr"; "CAD" -> "C$"; "AUD" -> "A$"; "PLN" -> "zł"; else -> code
}

private fun money(value: Double, currency: String): String =
    currencySymbol(currency) + "%.2f".format(Locale.getDefault(), value)

private fun converted(value: Double, from: String, to: String, auto: Boolean, rates: Map<String, Double>): Double =
    if (auto) ExchangeRates.convert(value, from, to, rates) else value

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = FinanceStore(this)
        setContent { VIPFinanceTheme { FinanceApp(store) } }
    }
}

@Composable
fun FinanceApp(store: FinanceStore) {
    var accounts by remember { mutableStateOf(store.loadAccounts()) }
    var transactions by remember { mutableStateOf(store.loadTransactions()) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var transactionAccountFilter by remember { mutableStateOf<String?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var currency by remember { mutableStateOf(store.loadCurrency()) }
    var autoConversion by remember { mutableStateOf(store.loadAutoConversion()) }
    var rates by remember { mutableStateOf(store.loadRates()) }
    var ratesDate by remember { mutableStateOf(store.loadRatesTime()) }
    var ratesLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ratesLoading = true
        runCatching { ExchangeRates.loadEcbRates() }.onSuccess {
            rates = it
            ratesDate = System.currentTimeMillis()
            store.saveRates(it)
        }
        ratesLoading = false
    }

    val visibleAccounts = accounts.filter { !it.hidden }
    val total = visibleAccounts.sumOf { converted(it.balance, it.currency, currency, autoConversion, rates) }
    val selectedBalance = selectedAccount?.let { name ->
        accounts.firstOrNull { it.name == name }?.let { converted(it.balance, it.currency, currency, autoConversion, rates) }
    }

    fun addTransaction(transaction: Transaction) {
        transactions = transactions + transaction
        store.saveTransactions(transactions)
        if (transaction.accountName.isNotBlank()) {
            val delta = if (transaction.income) transaction.amount else -transaction.amount
            accounts = accounts.map { a -> if (a.name == transaction.accountName) a.copy(balance = a.balance + delta) else a }
            store.saveAccounts(accounts)
        }
    }

    fun toggleAccountHidden(name: String) {
        accounts = accounts.map { if (it.name == name) it.copy(hidden = !it.hidden) else it }
        store.saveAccounts(accounts)
    }

    fun deleteTransaction(transaction: Transaction) {
        transactions = transactions.filterNot { it === transaction }
        store.saveTransactions(transactions)
        if (transaction.accountName.isNotBlank()) {
            val delta = if (transaction.income) -transaction.amount else transaction.amount
            accounts = accounts.map { a -> if (a.name == transaction.accountName) a.copy(balance = a.balance + delta) else a }
            store.saveAccounts(accounts)
        }
    }

    val shownTransactions = transactionAccountFilter?.let { name -> transactions.filter { it.accountName == name } } ?: transactions
    val shownIncome = shownTransactions.filter { it.income }.sumOf { converted(it.amount, it.currency, currency, autoConversion, rates) }
    val shownExpense = shownTransactions.filter { !it.income }.sumOf { converted(it.amount, it.currency, currency, autoConversion, rates) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("Главная", "Операции", "Счета", "Аналитика", "Конвертер", "Ещё").forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(listOf("⌂", "↕", "▣", "◔", "⇄", "⋯")[index]) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("VIP Finance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            when (selectedTab) {
                0 -> HomeScreen(total, selectedAccount, selectedBalance, accounts, currency, autoConversion, rates) { selectedAccount = it }
                1 -> TransactionsScreen(shownTransactions, accounts, transactionAccountFilter, { transactionAccountFilter = it }, { showTransactionDialog = true }, ::deleteTransaction, currency, autoConversion, rates)
                2 -> AccountsScreen(accounts, currency, autoConversion, rates, { showAccountDialog = true }, { selectedAccount = it }, ::toggleAccountHidden)
                3 -> AnalyticsScreen(shownIncome, shownExpense, shownTransactions, currency, autoConversion, rates)
                4 -> ConverterScreen(currency, rates, ratesLoading)
                else -> MoreScreen(currency, autoConversion, ratesDate) { newCurrency, newAuto ->
                    currency = newCurrency
                    autoConversion = newAuto
                    store.saveCurrency(newCurrency)
                    store.saveAutoConversion(newAuto)
                }
            }
        }
    }

    if (showAccountDialog) {
        AddAccountDialog({ showAccountDialog = false }) { name, balance, type, accountCurrency ->
            accounts = accounts + Account(name, balance, false, type, accountCurrency)
            store.saveAccounts(accounts)
            showAccountDialog = false
        }
    }
    if (showTransactionDialog) {
        AddTransactionDialog(accounts, { showTransactionDialog = false }) {
            addTransaction(it)
            showTransactionDialog = false
        }
    }
}

@Composable
fun HomeScreen(total: Double, selectedAccount: String?, selectedBalance: Double?, accounts: List<Account>, currency: String, auto: Boolean, rates: Map<String, Double>, onSelect: (String?) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(if (selectedAccount == null) "Общий баланс" else "Баланс: $selectedAccount", style = MaterialTheme.typography.titleMedium)
            Text(money(selectedBalance ?: total, currency), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            if (!auto) Text("Автоконвертация выключена")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selectedAccount == null, { onSelect(null) }, label = { Text("Все") })
                accounts.filter { !it.hidden }.take(3).forEach { account ->
                    FilterChip(selectedAccount == account.name, { onSelect(account.name) }, label = { Text(account.name) })
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("Счета и карты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    accounts.filter { !it.hidden }.take(6).forEach { account ->
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(account.name, fontWeight = FontWeight.Bold)
                    Text(account.type + " • " + account.currency)
                }
                Text(money(converted(account.balance, account.currency, currency, auto, rates), currency), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AccountsScreen(accounts: List<Account>, currency: String, auto: Boolean, rates: Map<String, Double>, onAdd: () -> Unit, onSelect: (String) -> Unit, onToggleHidden: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Счета и карты", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = onAdd) { Text("+ Добавить") }
    }
    Spacer(Modifier.height(8.dp))
    if (accounts.isEmpty()) Text("Добавь первый счёт или карту.")
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts) { account ->
            Card(Modifier.fillMaxWidth(), onClick = { onSelect(account.name) }) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(account.name, fontWeight = FontWeight.Bold)
                        Text(account.type + " • " + account.currency + if (account.hidden) " • скрыт" else "")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(converted(account.balance, account.currency, currency, auto, rates), currency), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onToggleHidden(account.name) }) { Text(if (account.hidden) "Показать" else "Скрыть") }
                    }
                }
        }
    }
}

@Composable
fun TransactionsScreen(
    transactions: List<Transaction>, accounts: List<Account>, selectedFilter: String?, onFilter: (String?) -> Unit,
    onAdd: () -> Unit, onDelete: (Transaction) -> Unit, currency: String, auto: Boolean, rates: Map<String, Double>
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Операции", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = onAdd) { Text("+ Операция") }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selectedFilter == null, { onFilter(null) }, label = { Text("Все") })
        accounts.take(3).forEach { account -> FilterChip(selectedFilter == account.name, { onFilter(account.name) }, label = { Text(account.name) }) }
    }
    Spacer(Modifier.height(8.dp))
    if (transactions.isEmpty()) Text("Операций пока нет.")
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(transactions.asReversed()) { transaction ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(transaction.title, fontWeight = FontWeight.Bold)
                            Text(transaction.category + if (transaction.accountName.isNotBlank()) " • " + transaction.accountName else "")
                            Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text((if (transaction.income) "+" else "−") + " " + money(converted(transaction.amount, transaction.currency, currency, auto, rates), currency), fontWeight = FontWeight.Bold)
                            if (transaction.currency != currency) Text(transaction.currency, style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { onDelete(transaction) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(income: Double, expense: Double, transactions: List<Transaction>, currency: String, auto: Boolean = true, rates: Map<String, Double> = emptyMap()) {
    Text("Аналитика", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Доходы", fontWeight = FontWeight.Bold); Text(money(income, currency), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Расходы", fontWeight = FontWeight.Bold); Text(money(expense, currency), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Разница", fontWeight = FontWeight.Bold); Text(money(income - expense, currency), style = MaterialTheme.typography.titleLarge)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Расходы по категориям", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    val categories = transactions.filter { !it.income }.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { converted(it.amount, it.currency, currency, auto, rates) } }
    if (categories.isEmpty()) Text("Пока нет расходов по категориям.")
    else categories.entries.sortedByDescending { it.value }.forEach { entry ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.key); Text(money(entry.value, currency), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConverterScreen(mainCurrency: String, rates: Map<String, Double>, loading: Boolean) {
    var amount by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(mainCurrency) }
    var to by remember { mutableStateOf(if (mainCurrency == "EUR") "GBP" else "EUR") }
    val value = amount.replace(',', '.').toDoubleOrNull()
    val result = value?.let { ExchangeRates.convert(it, from, to, rates) }
    Text("Конвертер валют", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        supportedCurrencies.take(5).forEach { code -> FilterChip(from == code, { from = code }, label = { Text(code) }) }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        supportedCurrencies.take(5).forEach { code -> FilterChip(to == code, { to = code }, label = { Text(code) }) }
    }
    Spacer(Modifier.height(14.dp))
    if (loading) Text("Обновляю курсы…")
    if (result != null) Text(money(result, to), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    else Text("Введите сумму.")
    Spacer(Modifier.height(8.dp))
    Text("Курс берётся из ежедневных справочных курсов ECB. Для RUB актуальный курс ECB сейчас не публикуется.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun MoreScreen(currency: String, auto: Boolean, ratesDate: Long, onSettings: (String, Boolean) -> Unit) {
    Text("Ещё", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Основная валюта", fontWeight = FontWeight.Bold)
            Text("Сейчас: $currency")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                supportedCurrencies.take(6).forEach { code -> FilterChip(currency == code, { onSettings(code, auto) }, label = { Text(code) }) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Автоконвертация", fontWeight = FontWeight.Bold)
                    Text(if (auto) "Суммы приводятся к основной валюте" else "Суммы показываются в валюте счёта")
                }
                Switch(checked = auto, onCheckedChange = { onSettings(currency, it) })
            }
            if (ratesDate > 0) Text("Последнее обновление курса: " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ratesDate)), style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(Modifier.height(8.dp))
    listOf("Долги", "Накопления и цели", "Чеки и OCR", "Напоминания", "Настройки", "Темы оформления").forEach {
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, Double, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Счёт") }
    var accountCurrency by remember { mutableStateOf("GBP") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новый счёт или карта") }, text = {
        Column {
            OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(balance, { balance = it }, label = { Text("Баланс") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(type == "Счёт", { type = "Счёт" }, label = { Text("Счёт") })
                FilterChip(type == "Карта", { type = "Карта" }, label = { Text("Карта") })
            }
            Spacer(Modifier.height(8.dp))
            Text("Валюта счёта", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                supportedCurrencies.take(5).forEach { code -> FilterChip(accountCurrency == code, { accountCurrency = code }, label = { Text(code) }) }
            }
        }
    }, confirmButton = {
        Button(onClick = { onSave(name.trim(), balance.replace(',', '.').toDoubleOrNull() ?: 0.0, type, accountCurrency) }, enabled = name.isNotBlank()) { Text("Сохранить") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
fun AddTransactionDialog(accounts: List<Account>, onDismiss: () -> Unit, onSave: (Transaction) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "") }
    val accountCurrency = accounts.firstOrNull { it.name == accountName }?.currency ?: "GBP"
    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Новая операция") }, text = {
        Column {
            OutlinedTextField(title, { title = it }, label = { Text("Описание") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(amount, { amount = it }, label = { Text("Сумма ($accountCurrency)") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(income, { income = true }, label = { Text("Доход") })
                FilterChip(!income, { income = false }, label = { Text("Расход") })
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(category, { category = it }, label = { Text("Категория") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Text("Счёт или карта", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                accounts.take(4).forEach { account -> FilterChip(accountName == account.name, { accountName = account.name }, label = { Text(account.name) }) }
            }
        }
    }, confirmButton = {
        Button(onClick = { onSave(Transaction(title.trim(), parsedAmount ?: 0.0, income, accountName, category.trim().ifBlank { "Без категории" }, currency = accountCurrency)) },
            enabled = title.isNotBlank() && parsedAmount != null && parsedAmount > 0.0 && accountName.isNotBlank()) { Text("Сохранить") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}
