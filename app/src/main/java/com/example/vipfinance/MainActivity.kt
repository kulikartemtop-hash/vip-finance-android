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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
private val pages=listOf("Главная","Операции","Счета","Аналитика","Конвертер","Долги","Цели","Напоминания","Чеки")
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
    var page by remember { mutableStateOf("Главная") }
    var currency by remember { mutableStateOf(s.loadCurrency()) }
    var auto by remember { mutableStateOf(s.loadAutoConversion()) }
    var theme by remember { mutableStateOf(s.loadTheme()) }
    var style by remember { mutableStateOf(s.loadStyle()) }
    var menu by remember { mutableStateOf(s.loadMenu().filter { it in pages }.ifEmpty { pages.toSet() }) }
    var rates by remember { mutableStateOf(s.loadRates()) }
    var rateTime by remember { mutableStateOf(s.loadRatesTime()) }
    var loading by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<String?>(null) }
    var newest by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var receiptText by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(if (drawerOpen) DrawerValue.Open else DrawerValue.Closed)

    LaunchedEffect(drawerOpen) { if (drawerOpen) drawerState.open() else drawerState.close() }
    LaunchedEffect(menu) { if (page !in menu && menu.isNotEmpty()) page = menu.first() }
    LaunchedEffect(Unit) {
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
        if (t.accountName.isNotBlank()) {
            val delta = if (t.income) t.amount else -t.amount
            accounts = accounts.map { if (it.name == t.accountName) it.copy(balance = it.balance + delta) else it }
            s.saveAccounts(accounts)
        }
    }
    fun remove(t: Transaction) {
        tx = tx.filterNot { it.id == t.id }
        s.saveTransactions(tx)
        if (t.accountName.isNotBlank()) {
            val delta = if (t.income) -t.amount else t.amount
            accounts = accounts.map { if (it.name == t.accountName) it.copy(balance = it.balance + delta) else it }
            s.saveAccounts(accounts)
        }
    }

    val visible = accounts.filter { !it.hidden }
    val total = visible.sumOf { conv(it.balance, it.currency, currency, auto, rates) }
    val shown0 = filter?.let { name -> tx.filter { it.accountName == name } } ?: tx
    val shown = shown0.filter { search.isBlank() || it.title.contains(search, true) || it.category.contains(search, true) || it.accountName.contains(search, true) }
    val inc = shown.filter { it.income }.sumOf { conv(it.amount, it.currency, currency, auto, rates) }
    val exp = shown.filter { !it.income }.sumOf { conv(it.amount, it.currency, currency, auto, rates) }

    fun saveSettings(c: String, a: Boolean, t: String, st: String, m: Set<String>) {
        currency = c; auto = a; theme = t; style = st; menu = m.filter { it in pages }.toSet()
        s.saveCurrency(c); s.saveAutoConversion(a); s.saveTheme(t); s.saveStyle(st); s.saveMenu(menu.toSet())
    }

    VIPFinanceTheme(theme = theme, style = style) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("VIP Finance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
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
                        title = { Text(page) },
                        navigationIcon = { IconButton(onClick = { drawerOpen = true }) { Text("☰", style = MaterialTheme.typography.titleLarge) } },
                        actions = { IconButton(onClick = { dialog = "settings" }) { Text("⚙", style = MaterialTheme.typography.titleLarge) } }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { dialog = "expense" }, modifier = Modifier.weight(1f)) { Text("− Расход") }
                            Button(onClick = { dialog = "income" }, modifier = Modifier.weight(1f)) { Text("+ Доход") }
                        }
                    }
                }
            ) { pad ->
                Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    when (page) {
                        "Главная" -> Home(total, currency, accounts, auto, rates, selected, inc, exp) { selected = it }
                        "Операции" -> Operations(shown, accounts, filter, { filter = it }, { dialog = "expense" }, ::remove, currency, auto, rates, search, { search = it }, newest) { newest = it }
                        "Счета" -> Accounts(accounts, currency, auto, rates, { dialog = "account" }, { selected = it }) { n -> val updated = accounts.map { if (it.name == n) it.copy(hidden = !it.hidden) else it }; accounts = updated; s.saveAccounts(updated) }
                        "Аналитика" -> Analytics(tx, currency, auto, rates)
                        "Конвертер" -> Converter(currency, rates, loading)
                        "Долги" -> Debts(debts, { dialog = "debt" }) { d -> debts = debts.filterNot { it.id == d.id }; s.saveDebts(debts) }
                        "Цели" -> Goals(goals, { dialog = "goal" }) { g -> goals = goals.filterNot { it.id == g.id }; s.saveGoals(goals) }
                        "Напоминания" -> Reminders(reminders, { dialog = "reminder" }, { r -> reminders = reminders.map { if (it.id == r.id) it.copy(done = !it.done) else it }; s.saveReminders(reminders) }, { r -> reminders = reminders.filterNot { it.id == r.id }; s.saveReminders(reminders) })
                        "Чеки" -> Receipt(receiptUri, receiptText, { u -> receiptUri = u; receiptText = "" }, { t -> receiptText = t }) { u -> receiptUri = u }
                    }
                }
            }
        }
    }

    when (dialog) {
        "account" -> AccountDialog({ dialog = "" }) { n, b, t, c -> accounts = accounts + Account(n, b, false, t, c); s.saveAccounts(accounts); dialog = "" }
        "expense" -> TransactionDialog(accounts, false, { dialog = "" }) { add(it); dialog = "" }
        "income" -> TransactionDialog(accounts, true, { dialog = "" }) { add(it); dialog = "" }
        "debt" -> DebtDialog({ dialog = "" }) { debts = debts + it; s.saveDebts(debts); dialog = "" }
        "goal" -> GoalDialog(currency, { dialog = "" }) { goals = goals + it; s.saveGoals(goals); dialog = "" }
        "reminder" -> ReminderDialog({ dialog = "" }) { reminders = reminders + it; s.saveReminders(reminders); dialog = "" }
        "settings" -> SettingsDialog(currency, auto, theme, style, menu, rateTime, { c: String, a: Boolean, t: String, st: String, m: Set<String> -> saveSettings(c, a, t, st, m) }) { dialog = "" }
    }
}

@Composable private fun Home(total:Double,c:String,accounts:List<Account>,auto:Boolean,r:Map<String,Double>,selected:String?,income:Double,expense:Double,pick:(String?)->Unit){
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text(if(selected==null)"Общий баланс" else "Баланс: "+selected);val v=selected?.let{n->accounts.firstOrNull{it.name==n}?.let{conv(it.balance,it.currency,c,auto,r)}}?:total;Text(money(v,c),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text(if(auto)"Автоконвертация включена" else "Показ исходных валют");Spacer(Modifier.height(8.dp));Text("Доходы: "+money(income,c));Text("Расходы: "+money(expense,c))}}
 Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){FilterChip(selected==null,{pick(null)},label={Text("Все")});accounts.filter{!it.hidden}.forEach{a->FilterChip(selected==a.name,{pick(a.name)},label={Text(a.name)})}}
 Spacer(Modifier.height(10.dp));accounts.filter{!it.hidden}.forEach{a->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column{Text(a.name,fontWeight=FontWeight.Bold);Text(a.type+" • "+a.currency)};Text(money(conv(a.balance,a.currency,c,auto,r),c),fontWeight=FontWeight.Bold)}}}
}

@Composable private fun Operations(ts:List<Transaction>,accounts:List<Account>,filter:String?,setFilter:(String?)->Unit,add:()->Unit,remove:(Transaction)->Unit,c:String,auto:Boolean,r:Map<String,Double>,search:String,setSearch:(String)->Unit,newest:Boolean,setNewest:(Boolean)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("Операции",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}}
 OutlinedTextField(search,setSearch,label={Text("Поиск операций")},singleLine=true,modifier=Modifier.fillMaxWidth());Row(verticalAlignment=Alignment.CenterVertically){Text("Сначала новые");Switch(newest,setNewest)};Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){FilterChip(filter==null,{setFilter(null)},label={Text("Все")});accounts.forEach{a->FilterChip(filter==a.name,{setFilter(a.name)},label={Text(a.name)})}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(if(newest)ts.sortedByDescending{it.timestamp}else ts.sortedBy{it.timestamp}){t->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.Bold);Text(t.category+" • "+t.accountName);Text(SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(t.timestamp)))};Column(horizontalAlignment=Alignment.End){Text((if(t.income)"+" else "−")+" "+money(conv(t.amount,t.currency,c,auto,r),c),fontWeight=FontWeight.Bold);TextButton(onClick={remove(t)}){Text("Удалить")}}}}}}
}

@Composable private fun Accounts(items:List<Account>,c:String,auto:Boolean,r:Map<String,Double>,add:()->Unit,select:(String)->Unit,toggle:(String)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("Счета и карты",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(items){a->Card(onClick={select(a.name)},modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column{Text(a.name,fontWeight=FontWeight.Bold);Text(a.type+" • "+a.currency+if(a.hidden)" • скрыт" else "")};Column(horizontalAlignment=Alignment.End){Text(money(conv(a.balance,a.currency,c,auto,r),c),fontWeight=FontWeight.Bold);TextButton(onClick={toggle(a.name)}){Text(if(a.hidden)"Показать" else "Скрыть")}}}}}}
}

@Composable private fun Analytics(tx: List<Transaction>, c: String, auto: Boolean, r: Map<String, Double>) {
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Аналитика", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Отчёт за выбранный период", style = MaterialTheme.typography.bodySmall) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Сегодня", "7 дней", "14 дней", "Месяц", "Свои даты").forEach { p -> FilterChip(preset == p, { preset = p }, label = { Text(p) }) } } }
        if (preset == "Свои даты") item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(fromText, { fromText = it }, label = { Text("С даты") }, placeholder = { Text("дд.мм.гггг") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(toText, { toText = it }, label = { Text("По дату") }, placeholder = { Text("дд.мм.гггг") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("Доходы"); Text(money(income, c), fontWeight = FontWeight.Bold) } }
                Card(Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("Расходы"); Text(money(expense, c), fontWeight = FontWeight.Bold) } }
            }
        }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("Итог", style = MaterialTheme.typography.titleMedium); Text(money(income - expense, c), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Операций: " + periodTx.size) } } }
        item { Text("Расходы по категориям", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(cats.entries.sortedByDescending { it.value }) { entry ->
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(entry.key); Text(money(entry.value, c), fontWeight = FontWeight.Bold) }
                LinearProgressIndicator({ (entry.value / max).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
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
                listOf("classic", "ocean", "graphite").forEach { value ->
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
    AlertDialog(
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
                        listOf("classic" to "Classic", "ocean" to "Ocean", "graphite" to "Graphite").forEach { (v, label) -> FilterChip(style == v, { save(c, auto, theme, v, menu) }, label = { Text(label) }) }
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

@Composable private fun AccountDialog(close:()->Unit,save:(String,Double,String,String)->Unit){
 var n by remember{mutableStateOf("")};var b by remember{mutableStateOf("")};var t by remember{mutableStateOf("Счёт")};var c by remember{mutableStateOf("GBP")}
 AlertDialog(onDismissRequest=close,title={Text("Новый счёт")},text={Column{OutlinedTextField(n,{n=it},label={Text("Название")});OutlinedTextField(b,{b=it},label={Text("Баланс")});Row{FilterChip(t=="Счёт",{t="Счёт"},label={Text("Счёт")});FilterChip(t=="Карта",{t="Карта"},label={Text("Карта")})};Row{currencies.forEach{x->FilterChip(c==x,{c=x},label={Text(x)})}}}},confirmButton={Button({save(n.trim(),b.replace(',','.').toDoubleOrNull()?:0.0,t,c)},enabled=n.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun TransactionDialog(accounts: List<Account>, defaultIncome: Boolean, close: () -> Unit, save: (Transaction) -> Unit) {
    var n by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var inc by remember { mutableStateOf(defaultIncome) }
    var cat by remember { mutableStateOf("") }
    var acc by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "") }
    val c = accounts.firstOrNull { it.name == acc }?.currency ?: "RUB"
    val v = a.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (inc) "Новый доход" else "Новый расход") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(n, { n = it }, label = { Text("Описание") })
                OutlinedTextField(a, { a = it }, label = { Text("Сумма $c") })
                Row { FilterChip(inc, { inc = true }, label = { Text("Доход") }); FilterChip(!inc, { inc = false }, label = { Text("Расход") }) }
                OutlinedTextField(cat, { cat = it }, label = { Text("Категория") })
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Продукты", "Транспорт", "Жильё", "Зарплата", "Развлечения", "Другое").forEach { x -> FilterChip(cat == x, { cat = x }, label = { Text(x) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { accounts.forEach { x -> FilterChip(acc == x.name, { acc = x.name }, label = { Text(x.name) }) } }
            }
        },
        confirmButton = { Button({ save(Transaction(title = n.trim(), amount = v ?: 0.0, income = inc, accountName = acc, category = cat.ifBlank { "Без категории" }, currency = c)) }, enabled = n.isNotBlank() && v != null && v > 0 && acc.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(close) { Text("Отмена") } }
    )
}

@Composable private fun DebtDialog(close:()->Unit,save:(Debt)->Unit){
 var p by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var mine by remember{mutableStateOf(false)};var interest by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Новый долг")},text={Column{OutlinedTextField(p,{p=it},label={Text("Человек")});OutlinedTextField(a,{a=it},label={Text("Сумма")});Row{FilterChip(mine,{mine=true},label={Text("Я должен")});FilterChip(!mine,{mine=false},label={Text("Мне должны")})};Row(verticalAlignment=Alignment.CenterVertically){Text("Проценты");Switch(interest,{interest=it})};OutlinedTextField(note,{note=it},label={Text("Заметка")})}},confirmButton={Button({save(Debt(person=p,amount=a.replace(',','.').toDoubleOrNull()?:0.0,mine=mine,interest=interest,note=note))},enabled=p.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun GoalDialog(c:String,close:()->Unit,save:(Goal)->Unit){
 var n by remember{mutableStateOf("")};var t by remember{mutableStateOf("")};var d by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Новая цель")},text={Column{OutlinedTextField(n,{n=it},label={Text("Название")});OutlinedTextField(t,{t=it},label={Text("Цель "+c)});OutlinedTextField(d,{d=it},label={Text("Срок")})}},confirmButton={Button({save(Goal(name=n,target=t.replace(',','.').toDoubleOrNull()?:0.0,currency=c,deadline=d))},enabled=n.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun ReminderDialog(close:()->Unit,save:(Reminder)->Unit){
 var n by remember{mutableStateOf("")};var d by remember{mutableStateOf("")};var rep by remember{mutableStateOf("Один раз")}
 AlertDialog(onDismissRequest=close,title={Text("Напоминание")},text={Column{OutlinedTextField(n,{n=it},label={Text("Что напомнить")});OutlinedTextField(d,{d=it},label={Text("Дата")});Row{listOf("Один раз","Еженедельно","Ежемесячно").forEach{x->FilterChip(rep==x,{rep=x},label={Text(x)})}}}},confirmButton={Button({save(Reminder(title=n,date=d,repeat=rep))},enabled=n.isNotBlank()&&d.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
