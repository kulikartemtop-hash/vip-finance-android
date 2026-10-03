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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val visibleAccounts = accounts.filter { !it.hidden }
    val total = visibleAccounts.sumOf { it.balance }
    val income = transactions.filter { it.income }.sumOf { it.amount }
    val expense = transactions.filter { !it.income }.sumOf { it.amount }

    fun addTransaction(transaction: Transaction) {
        transactions = transactions + transaction
        store.saveTransactions(transactions)

        if (transaction.accountName.isNotBlank()) {
            val delta = if (transaction.income) transaction.amount else -transaction.amount
            accounts = accounts.map { account ->
                if (account.name == transaction.accountName) {
                    account.copy(balance = account.balance + delta)
                } else account
            }
            store.saveAccounts(accounts)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        transactions = transactions.filterNot { it === transaction }
        store.saveTransactions(transactions)

        if (transaction.accountName.isNotBlank()) {
            val delta = if (transaction.income) -transaction.amount else transaction.amount
            accounts = accounts.map { account ->
                if (account.name == transaction.accountName) {
                    account.copy(balance = account.balance + delta)
                } else account
            }
            store.saveAccounts(accounts)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val tabs = listOf("Главная", "Операции", "Счета", "Аналитика", "Ещё")
                val icons = listOf("⌂", "↕", "▣", "◔", "⋯")
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Text(icons[index]) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "VIP Finance",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            when (selected) {
                0 -> HomeScreen(total, income, expense, accounts)
                1 -> TransactionsScreen(
                    transactions = transactions,
                    onAdd = { showTransactionDialog = true },
                    onDelete = ::deleteTransaction
                )
                2 -> AccountsScreen(
                    accounts = accounts,
                    onAdd = { showAccountDialog = true },
                    onToggle = { index ->
                        accounts = accounts.mapIndexed { i, account ->
                            if (i == index) account.copy(hidden = !account.hidden) else account
                        }
                        store.saveAccounts(accounts)
                    }
                )
                3 -> AnalyticsScreen(income, expense, transactions)
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
            accounts = accounts,
            onDismiss = { showTransactionDialog = false },
            onSave = { transaction ->
                addTransaction(transaction)
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
            Text(
                "£%.2f".format(total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(it.name)
                Text("£%.2f".format(it.balance), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onAdd: () -> Unit,
    onToggle: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Счета и карты",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Button(onClick = onAdd) { Text("+ Счёт") }
    }

    Spacer(Modifier.height(8.dp))

    if (accounts.isEmpty()) {
        Text("Добавь первый счёт или карту.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts.indices.toList()) { index ->
                val account = accounts[index]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(account.name, fontWeight = FontWeight.Bold)
                                Text("£%.2f".format(account.balance))
                            }
                            FilterChip(
                                selected = account.hidden,
                                onClick = { onToggle(index) },
                                label = { Text(if (account.hidden) "Скрыт" else "Виден") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    onAdd: () -> Unit,
    onDelete: (Transaction) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Операции",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Button(onClick = onAdd) { Text("+ Операция") }
    }

    Spacer(Modifier.height(8.dp))

    if (transactions.isEmpty()) {
        Text("Операций пока нет.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transactions.asReversed()) { transaction ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(transaction.title, fontWeight = FontWeight.Bold)
                                Text(
                                    transaction.category +
                                        if (transaction.accountName.isNotBlank()) {
                                            " • " + transaction.accountName
                                        } else ""
                                )
                                Text(
                                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                        .format(Date(transaction.timestamp))
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    (if (transaction.income) "+" else "−") +
                                        " £%.2f".format(transaction.amount),
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { onDelete(transaction) }) {
                                    Text("Удалить")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(
    income: Double,
    expense: Double,
    transactions: List<Transaction>
) {
    Text(
        "Аналитика",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
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

    Spacer(Modifier.height(12.dp))
    Text("По категориям", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    val categories = transactions
        .filter { !it.income }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }

    if (categories.isEmpty()) {
        Text("Пока нет расходов по категориям.")
    } else {
        categories.entries.sortedByDescending { it.value }.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(entry.key)
                Text("£%.2f".format(entry.value), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MoreScreen() {
    Text(
        "Ещё",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))

    listOf(
        "Долги",
        "Накопления и цели",
        "Чеки и OCR",
        "Напоминания",
        "Настройки",
        "Темы оформления"
    ).forEach {
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(it, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый счёт") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Начальный баланс") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        balance.replace(',', '.').toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "") }

    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая операция") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Описание") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = income,
                        onClick = { income = true },
                        label = { Text("Доход") }
                    )
                    Spacer(Modifier.padding(4.dp))
                    FilterChip(
                        selected = !income,
                        onClick = { income = false },
                        label = { Text("Расход") }
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                if (accounts.isEmpty()) {
                    Text("Сначала добавь счёт.")
                } else {
                    Text("Счёт", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.take(3).forEach { account ->
                            FilterChip(
                                selected = accountName == account.name,
                                onClick = { accountName = account.name },
                                label = { Text(account.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Transaction(
                            title = title.trim(),
                            amount = parsedAmount ?: 0.0,
                            income = income,
                            accountName = accountName,
                            category = category.trim().ifBlank { "Без категории" }
                        )
                    )
                },
                enabled = title.isNotBlank() &&
                    parsedAmount != null &&
                    parsedAmount > 0.0 &&
                    (accounts.isEmpty() || accountName.isNotBlank())
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
