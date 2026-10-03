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
private val pages=listOf("Главная","Операции","Счета","Категории","Аналитика","Конвертер","Долги","Цели","Напоминания","Чеки")
private val defaultCategories=listOf(
    Category(name="Продукты",kind="expense",icon="shopping_cart",color="#43A047"),
    Category(name="Транспорт",kind="expense",icon="directions_car",color="#1E88E5"),
    Category(name="Жильё",kind="expense",icon="home",color="#8E24AA"),
    Category(name="Развлечения",kind="expense",icon="movie",color="#FB8C00"),
    Category(name="Здоровье",kind="expense",icon="favorite",color="#E53935"),
    Category(name="Покупки",kind="expense",icon="shopping_bag",color="#6D4C41"),
    Category(name="Связь",kind="expense",icon="phone",color="#3949AB"),
    Category(name="Подписки",kind="expense",icon="subscriptions",color="#5E35B1"),
    Category(name="Образование",kind="expense",icon="school",color="#039BE5"),
    Category(name="Путешествия",kind="expense",icon="flight",color="#00ACC1"),
    Category(name="Автомобиль",kind="expense",icon="directions_car",color="#546E7A"),
    Category(name="Коммунальные услуги",kind="expense",icon="home",color="#7B1FA2"),
    Category(name="Другое",kind="expense",icon="category",color="#757575"),
    Category(name="Зарплата",kind="income",icon="payments",color="#00897B"),
    Category(name="Подработка",kind="income",icon="payments",color="#2E7D32"),
    Category(name="Премия",kind="income",icon="savings",color="#F9A825"),
    Category(name="Подарок",kind="income",icon="favorite",color="#D81B60"),
    Category(name="Возврат денег",kind="income",icon="wallet",color="#1565C0"),
    Category(name="Проценты",kind="income",icon="savings",color="#00838F"),
    Category(name="Продажа",kind="income",icon="shopping_bag",color="#6D4C41"),
    Category(name="Инвестиционный доход",kind="income",icon="trending_up",color="#00796B"),
    Category(name="Другое",kind="income",icon="category",color="#757575")
)
private val iconChoices=listOf("account_balance","credit_card","wallet","savings","shopping_cart","home","directions_car","payments","favorite","phone","school","flight","movie","subscriptions","trending_up","category")
private val colorChoices=listOf("#5B35F5","#00A7B5","#007A5A","#E53935","#FB8C00","#1E88E5","#8E24AA","#D81B60","#6D4C41","#757575")
private fun iconText(icon:String)=when(icon){"account_balance"->"▥";"credit_card"->"▣";"wallet"->"◫";"savings"->"◉";"shopping_cart"->"🛒";"home"->"⌂";"directions_car"->"🚗";"payments"->"₽";"favorite"->"♥";"phone"->"☎";"school"->"◆";"flight"->"✈";"movie"->"▶";"subscriptions"->"◉";"shopping_bag"->"▱";"trending_up"->"↗";else->"•"}
private fun uiColor(hex:String)=runCatching{Color(android.graphics.Color.parseColor(hex))}.getOrDefault(Color(0xFF5B35F5))

private fun sym(c:String)=when(c){"GBP"->"£";"USD"->"$";"EUR"->"€";"RUB"->"₽";"CNY"->"¥";"JPY"->"¥";"CHF"->"Fr";"CAD"->"C$";"AUD"->"A$";"PLN"->"zł";else->c}
private fun money(v:Double,c:String)=sym(c)+"%.2f".format(Locale.getDefault(),v)
private fun conv(v:Double,from:String,to:String,auto:Boolean,r:Map<String,Double>)=if(auto)ExchangeRates.convert(v,from,to,r) else v

@Composable
private fun PremiumDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 14.dp,
        shadowElevation = 22.dp,
        title = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) { title() }
        },
        text = {
            Box(
                Modifier.fillMaxWidth().padding(top = 2.dp)
            ) { text() }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
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
    search: String, setSearch: (String) -> Unit, newest: Boolean, setNewest: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Операции", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${ts.size} операций", style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = add) { Text("+ Добавить") }
        }
        OutlinedTextField(search, setSearch, label = { Text("Поиск операций") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Сначала новые", modifier = Modifier.weight(1f)); Switch(newest, setNewest) }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FilterChip(filter == null, { setFilter(null) }, label = { Text("Все") })
            accounts.forEach { a -> FilterChip(filter == a.name, { setFilter(a.name) }, label = { Text(a.name) }) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(if (newest) ts.sortedByDescending { it.timestamp } else ts.sortedBy { it.timestamp }) { t ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (t.income) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (t.income) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) {
                            Text(if (t.income) "↗" else "↘", color = if (t.income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontWeight = FontWeight.SemiBold)
                            Text(t.category + " • " + t.accountName, style = MaterialTheme.typography.bodySmall)
                            Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(t.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text((if (t.income) "+" else "−") + " " + money(conv(t.amount, t.currency, c, auto, r), c), fontWeight = FontWeight.Bold, color = if (t.income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            TextButton(onClick = { remove(t) }) { Text("Удалить") }
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
    var tab by remember { mutableStateOf("expense") }
    val filtered = items.filter { it.kind == tab }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(tab == "expense", { tab = "expense" }, label = { Text("Расходы") })
            FilterChip(tab == "income", { tab = "income" }, label = { Text("Доходы") })
        }
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (tab == "expense") "Категории расходов" else "Категории доходов",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (tab == "expense") "На что уходят деньги" else "Откуда приходят деньги",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(onClick = add, modifier = Modifier.fillMaxWidth()) {
            Text(if (tab == "expense") "＋ Добавить категорию расхода" else "＋ Добавить категорию дохода")
        }
        if (filtered.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text("Категорий пока нет", Modifier.padding(18.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { c ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(uiColor(c.color)),
                                contentAlignment = Alignment.Center
                            ) { Text(iconText(c.icon), color = Color.White, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (c.kind == "income") "Доход" else "Расход",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { edit(c) }) { Text("Изменить") }
                            TextButton(onClick = { remove(c) }) { Text("Удалить") }
                        }
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

@Composable private fun Debts(items:List<Debt>,add:()->Unit,remove:(Debt)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Долги",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}};items.forEach{d->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.padding(12.dp)){Text(if(d.mine)"Я должен: "+d.person else "Мне должны: "+d.person,fontWeight=FontWeight.Bold);Text("%.2f".format(d.amount));Text(if(d.interest)"Проценты включены" else "Без процентов");if(d.note.isNotBlank())Text(d.note);TextButton(onClick={remove(d)}){Text("Удалить")}}}}
}

@Composable private fun Goals(items:List<Goal>,add:()->Unit,remove:(Goal)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Цели",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}};items.forEach{g->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.padding(12.dp)){Text(g.name,fontWeight=FontWeight.Bold);Text(money(g.saved,g.currency)+" / "+money(g.target,g.currency));LinearProgressIndicator({if(g.target>0)(g.saved/g.target).toFloat().coerceIn(0f,1f) else 0f},Modifier.fillMaxWidth());if(g.deadline.isNotBlank())Text("Срок: "+g.deadline);TextButton(onClick={remove(g)}){Text("Удалить")}}}}
}

@Composable private fun Reminders(items:List<Reminder>,add:()->Unit,toggle:(Reminder)->Unit,remove:(Reminder)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Напоминания",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}};items.forEach{r->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(r.title,fontWeight=FontWeight.Bold);Text(r.date+" • "+r.repeat)};Switch(r.done,{toggle(r)});TextButton(onClick={remove(r)}){Text("Удалить")}}}}
}

@Composable private fun Receipt(uri:Uri?,text:String,setUri:(Uri?)->Unit,setText:(String)->Unit,pick:(Uri)->Unit){
 val context=LocalContext.current
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){u->if(u!=null){pick(u);runCatching{InputImage.fromFilePath(context,u)}.onSuccess{image->TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).addOnSuccessListener{result->setText(result.text.ifBlank{"Текст не найден."})}.addOnFailureListener{setText("Не удалось распознать текст.")}}}}
 Text("Чеки и OCR",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button({launcher.launch("image/*")}){Text("Выбрать фото чека")};if(uri!=null)Text("Фото выбрано: "+uri.lastPathSegment);Card(Modifier.fillMaxWidth()){Text(if(text.isBlank())"Фото подготовлено для OCR." else text,Modifier.padding(12.dp))}
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

@Composable private fun SettingsDialog(c: String, auto: Boolean, theme: String, style: String, menu: Set<String>, time: Long, save: (String, Boolean, String, String, Set<String>) -> Unit, close: () -> Unit) {
    PremiumDialog(
        onDismissRequest = close,
        title = { Text("Настройки") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 520.dp)) {
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
    PremiumDialog(
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
    close: () -> Unit,
    save: (Transaction) -> Unit
) {
    var n by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var inc by remember { mutableStateOf(defaultIncome) }
    var cat by remember { mutableStateOf(categories.firstOrNull { it.kind == if (defaultIncome) "income" else "expense" }?.name ?: "") }
    var acc by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "") }
    val c = accounts.firstOrNull { it.name == acc }?.currency ?: "RUB"
    val v = a.replace(',', '.').toDoubleOrNull()
    PremiumDialog(
        onDismissRequest = close,
        title = { Text(if (inc) "Новый доход" else "Новый расход") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(n, { n = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(a, { a = it }, label = { Text("Сумма $c") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Тип операции", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(inc, { inc = true }, label = { Text("Доход") })
                    FilterChip(!inc, { inc = false }, label = { Text("Расход") })
                }
                Text("Категория", fontWeight = FontWeight.Bold)
                OutlinedTextField(cat, { cat = it }, label = { Text("Можно ввести свою") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(categories.filter { it.kind == if (inc) "income" else "expense" }) { x ->
                        FilterChip(cat == x.name, { cat = x.name }, label = { Text(iconText(x.icon) + " " + x.name) })
                    }
                }
                Text("Счёт / карта", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(accounts) { x ->
                        FilterChip(acc == x.name, { acc = x.name }, label = { Text(iconText(x.icon) + " " + x.name) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { save(Transaction(title = n.trim(), amount = v ?: 0.0, income = inc, accountName = acc, category = cat.ifBlank { "Без категории" }, currency = c)) },
                enabled = n.isNotBlank() && v != null && v > 0 && acc.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(close) { Text("Отмена") } }
    )
}

@Composable
private fun CategoryDialog(existing: Category?, close: () -> Unit, save: (Category) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var kind by remember { mutableStateOf(existing?.kind ?: "expense") }
    var icon by remember { mutableStateOf(existing?.icon ?: "category") }
    var color by remember { mutableStateOf(existing?.color ?: colorChoices.first()) }
    PremiumDialog(
        onDismissRequest = close,
        title = { Text(if (existing == null) "Новая категория" else "Изменить категорию") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Название категории") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Тип категории", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(kind == "expense", { kind = "expense" }, label = { Text("Расход") })
                    FilterChip(kind == "income", { kind = "income" }, label = { Text("Доход") })
                }
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
        confirmButton = { Button({ save(Category(existing?.id ?: System.currentTimeMillis(), name.trim(), kind, icon, color)) }, enabled = name.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(close) { Text("Отмена") } }
    )
}

@Composable
private fun DebtDialog(close:()->Unit,save:(Debt)->Unit){
 var p by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var mine by remember{mutableStateOf(false)};var interest by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")}
 PremiumDialog(onDismissRequest=close,title={Text("Новый долг")},text={
  Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   OutlinedTextField(p,{p=it},label={Text("Человек")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(a,{a=it},label={Text("Сумма RUB")},modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(mine,{mine=true},label={Text("Я должен")});FilterChip(!mine,{mine=false},label={Text("Мне должны")})}
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Начислять проценты",Modifier.weight(1f));Switch(interest,{interest=it})}
   OutlinedTextField(note,{note=it},label={Text("Заметка")},modifier=Modifier.fillMaxWidth())
  }
 },confirmButton={Button({save(Debt(person=p,amount=a.replace(',','.').toDoubleOrNull()?:0.0,mine=mine,interest=interest,note=note))},enabled=p.isNotBlank()&&a.replace(',','.').toDoubleOrNull()!=null){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}

@Composable private fun GoalDialog(c:String,close:()->Unit,save:(Goal)->Unit){
 var n by remember{mutableStateOf("")};var t by remember{mutableStateOf("")};var d by remember{mutableStateOf("")}
 PremiumDialog(onDismissRequest=close,title={Text("Новая цель")},text={Column{OutlinedTextField(n,{n=it},label={Text("Название")});OutlinedTextField(t,{t=it},label={Text("Цель "+c)});OutlinedTextField(d,{d=it},label={Text("Срок")})}},confirmButton={Button({save(Goal(name=n,target=t.replace(',','.').toDoubleOrNull()?:0.0,currency=c,deadline=d))},enabled=n.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun ReminderDialog(close:()->Unit,save:(Reminder)->Unit){
 var n by remember{mutableStateOf("")};var d by remember{mutableStateOf("")};var rep by remember{mutableStateOf("Один раз")}
 PremiumDialog(onDismissRequest=close,title={Text("Напоминание")},text={
  Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   OutlinedTextField(n,{n=it},label={Text("Что напомнить")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(d,{d=it},label={Text("Дата")},modifier=Modifier.fillMaxWidth())
   Text("Повтор",fontWeight=FontWeight.Bold)
   LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("Один раз","Еженедельно","Ежемесячно")){x->FilterChip(rep==x,{rep=x},label={Text(x)})}}
  }
 },confirmButton={Button({save(Reminder(title=n,date=d,repeat=rep))},enabled=n.isNotBlank()&&d.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}


