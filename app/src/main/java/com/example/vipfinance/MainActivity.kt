package com.example.vipfinance

import android.content.Context
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vipfinance.ui.theme.VIPFinanceTheme
import org.json.JSONArray
import org.json.JSONObject

data class Account(val name: String, val balance: Double, val hidden: Boolean = false)
data class Transaction(val title: String, val amount: Double, val income: Boolean)

class FinanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("vip_finance", Context.MODE_PRIVATE)

    fun loadAccounts(): List<Account> {
        val raw = prefs.getString("accounts", "[]") ?: "[]"
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Account(o.getString("name"), o.getDouble("balance"), o.optBoolean("hidden"))
        }
    }

    fun saveAccounts(list: List<Account>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("name", it.name)
                put("balance", it.balance)
                put("hidden", it.hidden)
            })
        }
        prefs.edit().putString("accounts", a.toString()).apply()
    }

    fun loadTransactions(): List<Transaction> {
        val raw = prefs.getString("transactions", "[]") ?: "[]"
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Transaction(o.getString("title"), o.getDouble("amount"), o.getBoolean("income"))
        }
    }

    fun saveTransactions(list: List<Transaction>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("title", it.title)
                put("amount", it.amount)
                put("income", it.income)
            })
        }
        prefs.edit().putString("transactions", a.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = FinanceStore(this)
        setContent {
            VIPFinanceTheme {
                FinanceApp(store)
            }
        }
    }
}

@Composable
fun FinanceApp(store: FinanceStore) {
    var accounts by remember { mutableStateOf(store.loadAccounts()) }
    var transactions by remember { mutableStateOf(store.loadTransactions()) }
    var selected by remember { mutableStateOf(0) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf(false) }

    val total = accounts.filter { !it.hidden }.sumOf { it.balance }
    val income = transactions.filter { it.income }.sumOf { it.amount }
    val expense = transactions.filter { !it.income }.sumOf { it.amount }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val tabs = listOf("Главная", "Операции", "Счета", "Аналитика", "Ещё")
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Text(listOf("⌂", "↕", "▣", "◔", "⋯")[index]) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("VIP Finance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            when (selected) {
                0 -> HomeScreen(total, income, expense, accounts)
                1 -> TransactionsScreen(transactions, onAdd = { showTransactionDialog = true })
                2 -> AccountsScreen(
                    accounts,
                    onAdd = { showAccountDialog = true },
                    onToggle = { index ->
                        accounts = accounts.mapIndexed { i, a -> if (i == index) a.copy(hidden = !a.hidden) else a }
                        store.saveAccounts(accounts)
                    }
                )
                3 -> AnalyticsScreen(income, expense)
                else -> MoreScreen()
            }
        }
    }

    if (showAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAccountDialog = false },
            onSave = { name, balance ->
                accounts = accounts + Account(name, balance)
                store.saveAccounts(accounts)
                showAccountDialog = false
            }
        )
    }

    if (showTransactionDialog) {
        AddTransactionDialog(
            onDismiss = { showTransactionDialog = false },
            onSave = { title, amount, isIncome ->
                transactions = transactions + Transaction(title, amount, isIncome)
                store.saveTransactions(transactions)
                showTransactionDialog = false
            }
        )
    }
}

@Composable
fun HomeScreen(total: Double, income: Double, expense: Double, accounts: List<Account>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Общий баланс", style = MaterialTheme.typography.titleMedium)
            Text("£%.2f".format(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Доходы\n£%.2f".format(income))
                Text("Расходы\n£%.2f".format(expense))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("Счета", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    accounts.filter { !it.hidden }.take(4).forEach {
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(it.name)
                Text("£%.2f".format(it.balance), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AccountsScreen(accounts: List<Account>, onAdd: () -> Unit, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Счета и карты", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = onAdd) { Text("+ Счёт") }
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts.indices.toList()) { index ->
            val a = accounts[index]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(a.name, fontWeight = FontWeight.Bold)
                            Text("£%.2f".format(a.balance))
                        }
                        FilterChip(selected = a.hidden, onClick = { onToggle(index) }, label = { Text(if (a.hidden) "Скрыт" else "Виден") })
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(items: List<Transaction>, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Операции", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = onAdd) { Text("+ Операция") }
    }
    Spacer(Modifier.height(8.dp))
    if (items.isEmpty()) {
        Text("Операций пока нет.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items.asReversed()) { t ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t.title)
                        Text(
                            (if (t.income) "+" else "−") + " £%.2f".format(t.amount),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(income: Double, expense: Double) {
    Text("Аналитика", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Доходы", fontWeight = FontWeight.Bold)
            Text("£%.2f".format(income), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Расходы", fontWeight = FontWeight.Bold)
            Text("£%.2f".format(expense), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Разница", fontWeight = FontWeight.Bold)
            Text("£%.2f".format(income - expense), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun MoreScreen() {
    Text("Ещё", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    listOf("Долги", "Накопления и цели", "Чеки и OCR", "Напоминания", "Настройки", "Темы оформления").forEach {
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый счёт") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(balance, { balance = it }, label = { Text("Баланс") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim(), balance.replace(',', '.').toDoubleOrNull() ?: 0.0) }, enabled = name.isNotBlank()) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onSave: (String, Double, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая операция") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Описание") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = income, onClick = { income = true }, label = { Text("Доход") })
                    Spacer(Modifier.padding(4.dp))
                    FilterChip(selected = !income, onClick = { income = false }, label = { Text("Расход") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title.trim(), amount.replace(',', '.').toDoubleOrNull() ?: 0.0, income) }, enabled = title.isNotBlank() && amount.replace(',', '.').toDoubleOrNull() != null) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
