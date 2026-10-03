package com.example.vipfinance

import android.net.Uri
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
import com.example.vipfinance.ui.theme.VIPFinanceTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val currencies=listOf("GBP","EUR","USD","RUB","CNY","JPY","CHF","CAD","AUD","PLN")
private val pages=listOf("Главная","Операции","Счета","Аналитика","Конвертер","Долги","Цели","Напоминания","Чеки","Ещё")
private fun sym(c:String)=when(c){"GBP"->"£";"USD"->"$";"EUR"->"€";"RUB"->"₽";"CNY"->"¥";"JPY"->"¥";"CHF"->"Fr";"CAD"->"C$";"AUD"->"A$";"PLN"->"zł";else->c}
private fun money(v:Double,c:String)=sym(c)+"%.2f".format(Locale.getDefault(),v)
private fun conv(v:Double,from:String,to:String,auto:Boolean,r:Map<String,Double>)=if(auto)ExchangeRates.convert(v,from,to,r) else v

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);enableEdgeToEdge();val s=FinanceStore(this);setContent{VIPFinanceTheme(s.loadTheme(),s.loadStyle()){FinanceApp(s)}}}
}

@Composable fun FinanceApp(s:FinanceStore){
 var accounts by remember{mutableStateOf(s.loadAccounts())};var tx by remember{mutableStateOf(s.loadTransactions())}
 var debts by remember{mutableStateOf(s.loadDebts())};var goals by remember{mutableStateOf(s.loadGoals())};var reminders by remember{mutableStateOf(s.loadReminders())}
 var page by remember{mutableStateOf("Главная")};var currency by remember{mutableStateOf(s.loadCurrency())};var auto by remember{mutableStateOf(s.loadAutoConversion())}
 var theme by remember{mutableStateOf(s.loadTheme())};var style by remember{mutableStateOf(s.loadStyle())};var menu by remember{mutableStateOf(s.loadMenu().ifEmpty{pages.toSet()})}
 var rates by remember{mutableStateOf(s.loadRates())};var rateTime by remember{mutableStateOf(s.loadRatesTime())};var loading by remember{mutableStateOf(false)}
 var dialog by remember{mutableStateOf("")};var filter by remember{mutableStateOf<String?>(null)};var search by remember{mutableStateOf("")};var selected by remember{mutableStateOf<String?>(null)}
 var receiptUri by remember{mutableStateOf<Uri?>(null)};var receiptText by remember{mutableStateOf("")}
 LaunchedEffect(Unit){loading=true;runCatching{ExchangeRates.loadEcbRates()}.onSuccess{rates=it;s.saveRates(it);rateTime=s.loadRatesTime()};loading=false}
 val visible=accounts.filter{!it.hidden};val total=visible.sumOf{conv(it.balance,it.currency,currency,auto,rates)}
 val shown0=filter?.let{n->tx.filter{it.accountName==n}}?:tx;val shown=shown0.filter{search.isBlank()||it.title.contains(search,true)||it.category.contains(search,true)||it.accountName.contains(search,true)}
 fun add(t:Transaction){tx=tx+t;s.saveTransactions(tx);if(t.accountName.isNotBlank()){val d=if(t.income)t.amount else -t.amount;accounts=accounts.map{if(it.name==t.accountName)it.copy(balance=it.balance+d)else it};s.saveAccounts(accounts)}}
 fun remove(t:Transaction){tx=tx.filterNot{it.id==t.id};s.saveTransactions(tx);if(t.accountName.isNotBlank()){val d=if(t.income)-t.amount else t.amount;accounts=accounts.map{if(it.name==t.accountName)it.copy(balance=it.balance+d)else it};s.saveAccounts(accounts)}}
 val inc=shown.filter{it.income}.sumOf{conv(it.amount,it.currency,currency,auto,rates)};val exp=shown.filter{!it.income}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}
 Scaffold(bottomBar={NavigationBar{menu.filter{it!="Ещё"}.take(4).forEach{p->NavigationBarItem(selected=page==p,onClick={page=p},icon={Text(p.take(1))},label={Text(p)})};NavigationBarItem(selected=page=="Ещё",onClick={page="Ещё"},icon={Text("⋯")},label={Text("Ещё")})}}){pad->
  Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)){Text("VIP Finance",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp))
   when(page){
    "Главная"->Home(total,currency,accounts,auto,rates,selected){selected=it}
    "Операции"->Operations(shown,accounts,filter,{filter=it},{dialog="tx"},::remove,currency,auto,rates,search){search=it}
    "Счета"->Accounts(accounts,currency,auto,rates,{dialog="account"},{selected=it}){n->accounts=accounts.map{if(it.name==n)it.copy(hidden=!it.hidden)else it};s.saveAccounts(accounts)}
    "Аналитика"->Analytics(inc,exp,shown,currency,auto,rates)
    "Конвертер"->Converter(currency,rates,loading)
    "Долги"->Debts(debts){dialog="debt"}
    "Цели"->Goals(goals){dialog="goal"}
    "Напоминания"->Reminders(reminders,{dialog="reminder"}){r->reminders=reminders.map{if(it.id==r.id)it.copy(done=!it.done)else it};s.saveReminders(reminders)}
    "Чеки"->Receipt(receiptUri,receiptText,{u->receiptUri=u;receiptText=""}){u->receiptUri=u;receiptText="Фото готово к OCR-анализу."}
    else->More(currency,auto,theme,style,menu,rateTime){c,a,t,st,m->currency=c;auto=a;theme=t;style=st;menu=m;s.saveCurrency(c);s.saveAutoConversion(a);s.saveTheme(t);s.saveStyle(st);s.saveMenu(m)}
   }
  }
 }
 if(dialog=="account")AccountDialog({dialog=""}){n,b,t,c->accounts=accounts+Account(n,b,false,t,c);s.saveAccounts(accounts);dialog=""}
 if(dialog=="tx")TransactionDialog(accounts,{dialog=""}){add(it);dialog=""}
 if(dialog=="debt")DebtDialog({dialog=""}){debts=debts+it;s.saveDebts(debts);dialog=""}
 if(dialog=="goal")GoalDialog(currency,{dialog=""}){goals=goals+it;s.saveGoals(goals);dialog=""}
 if(dialog=="reminder")ReminderDialog({dialog=""}){reminders=reminders+it;s.saveReminders(reminders);dialog=""}
}

@Composable private fun Home(total:Double,c:String,accounts:List<Account>,auto:Boolean,r:Map<String,Double>,selected:String?,pick:(String?)->Unit){
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text(if(selected==null)"Общий баланс" else "Баланс: "+selected);val v=selected?.let{n->accounts.firstOrNull{it.name==n}?.let{conv(it.balance,it.currency,c,auto,r)}}?:total;Text(money(v,c),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text(if(auto)"Автоконвертация включена" else "Показ исходных валют")}}
 Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){FilterChip(selected==null,{pick(null)},label={Text("Все")});accounts.filter{!it.hidden}.forEach{a->FilterChip(selected==a.name,{pick(a.name)},label={Text(a.name)})}}
 Spacer(Modifier.height(10.dp));accounts.filter{!it.hidden}.forEach{a->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column{Text(a.name,fontWeight=FontWeight.Bold);Text(a.type+" • "+a.currency)};Text(money(conv(a.balance,a.currency,c,auto,r),c),fontWeight=FontWeight.Bold)}}}
}

@Composable private fun Operations(ts:List<Transaction>,accounts:List<Account>,filter:String?,setFilter:(String?)->Unit,add:()->Unit,remove:(Transaction)->Unit,c:String,auto:Boolean,r:Map<String,Double>){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("Операции",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}}
 OutlinedTextField(search,setSearch,label={Text("Поиск операций")},singleLine=true,modifier=Modifier.fillMaxWidth());Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){FilterChip(filter==null,{setFilter(null)},label={Text("Все")});accounts.forEach{a->FilterChip(filter==a.name,{setFilter(a.name)},label={Text(a.name)})}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(ts.asReversed()){t->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.Bold);Text(t.category+" • "+t.accountName);Text(SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(t.timestamp)))};Column(horizontalAlignment=Alignment.End){Text((if(t.income)"+" else "−")+" "+money(conv(t.amount,t.currency,c,auto,r),c),fontWeight=FontWeight.Bold);TextButton(onClick={remove(t)}){Text("Удалить")}}}}}}
}

@Composable private fun Accounts(items:List<Account>,c:String,auto:Boolean,r:Map<String,Double>,add:()->Unit,select:(String)->Unit,toggle:(String)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text("Счета и карты",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(items){a->Card(Modifier.fillMaxWidth(),onClick={select(a.name)}){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column{Text(a.name,fontWeight=FontWeight.Bold);Text(a.type+" • "+a.currency+if(a.hidden)" • скрыт" else "")};Column(horizontalAlignment=Alignment.End){Text(money(conv(a.balance,a.currency,c,auto,r),c),fontWeight=FontWeight.Bold);TextButton(onClick={toggle(a.name)}){Text(if(a.hidden)"Показать" else "Скрыть")}}}}}}
}

@Composable private fun Analytics(income:Double,expense:Double,ts:List<Transaction>,c:String,auto:Boolean,r:Map<String,Double>){
 Text("Аналитика",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text("Доходы: "+money(income,c));Text("Расходы: "+money(expense,c));Text("Итог: "+money(income-expense,c),fontWeight=FontWeight.Bold)}}
 Spacer(Modifier.height(10.dp));Text("Категории расходов",fontWeight=FontWeight.Bold);val cats=ts.filter{!it.income}.groupBy{it.category}.mapValues{(_,v)->v.sumOf{conv(it.amount,it.currency,c,auto,r)}};val max=cats.values.maxOrNull()?:1.0
 cats.entries.sortedByDescending{it.value}.forEach{e->Text(e.key);LinearProgressIndicator({(e.value/max).toFloat().coerceIn(0f,1f)},Modifier.fillMaxWidth());Text(money(e.value,c));Spacer(Modifier.height(5.dp))}
}

@Composable private fun Converter(c:String,r:Map<String,Double>,loading:Boolean){
 var amount by remember{mutableStateOf("")};var from by remember{mutableStateOf(c)};var to by remember{mutableStateOf(if(c=="EUR")"GBP" else "EUR")};val v=amount.replace(',','.').toDoubleOrNull();val out=v?.let{ExchangeRates.convert(it,from,to,r)}
 Text("Конвертер",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedTextField(amount,{amount=it},label={Text("Сумма")},modifier=Modifier.fillMaxWidth());Text("Из");Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){currencies.forEach{x->FilterChip(from==x,{from=x},label={Text(x)})}};Text("В");Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){currencies.forEach{x->FilterChip(to==x,{to=x},label={Text(x)})}};if(loading)Text("Обновляю курсы…");if(out!=null)Text(money(out,to),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("ECB: справочные курсы. RUB не входит в актуальный набор ECB.",style=MaterialTheme.typography.bodySmall)
}

@Composable private fun Debts(items:List<Debt>,add:()->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Долги",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}};items.forEach{d->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.padding(12.dp)){Text(if(d.mine)"Я должен: "+d.person else "Мне должны: "+d.person,fontWeight=FontWeight.Bold);Text("%.2f".format(d.amount));Text(if(d.interest)"Проценты включены" else "Без процентов");if(d.note.isNotBlank())Text(d.note)}}}
}

@Composable private fun Goals(items:List<Goal>,add:()->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Цели",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}};items.forEach{g->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.padding(12.dp)){Text(g.name,fontWeight=FontWeight.Bold);Text(money(g.saved,g.currency)+" / "+money(g.target,g.currency));LinearProgressIndicator({(g.saved/g.target).toFloat().coerceIn(0f,1f)},Modifier.fillMaxWidth());if(g.deadline.isNotBlank())Text("Срок: "+g.deadline)}}}
}

@Composable private fun Reminders(items:List<Reminder>,add:()->Unit,toggle:(Reminder)->Unit){
 Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Напоминания",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(add){Text("+")}};items.forEach{r->Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Row(Modifier.padding(12.dp).fillMaxWidth(),Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(r.title,fontWeight=FontWeight.Bold);Text(r.date+" • "+r.repeat)};Switch(r.done,{toggle(r)})}}}
}

@Composable private fun Receipt(uri:Uri?,text:String,setUri:(Uri?)->Unit,pick:(Uri)->Unit){
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){u->if(u!=null)pick(u)}
 Text("Чеки и OCR",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button({launcher.launch("image/*")}){Text("Выбрать фото чека")};if(uri!=null)Text("Фото выбрано: "+uri.lastPathSegment);Card(Modifier.fillMaxWidth()){Text(if(text.isBlank())"Фото подготовлено для OCR." else text,Modifier.padding(12.dp))}
}

@Composable private fun More(c:String,auto:Boolean,theme:String,style:String,menu:Set<String>,time:Long,save:(String,Boolean,String,String,Set<String>)->Unit){
 Text("Ещё",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text("Основная валюта");Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){currencies.forEach{x->FilterChip(c==x,{save(x,auto,theme,style,menu)},label={Text(x)})}};Row(Modifier.fillMaxWidth(),Alignment.CenterVertically,Arrangement.SpaceBetween){Text("Автоконвертация");Switch(auto,{save(c,it,theme,style,menu)})};Text("Тема");Row{listOf("system","light","dark").forEach{x->FilterChip(theme==x,{save(c,auto,x,style,menu)},label={Text(x)})}};Text("Стиль");Row{listOf("classic","ocean","graphite").forEach{x->FilterChip(style==x,{save(c,auto,theme,x,menu)},label={Text(x)})}};if(time>0)Text("Курсы: "+SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(time)))}}};Text("Показывать в меню");pages.filter{it!="Ещё"}.forEach{x->Row(Modifier.fillMaxWidth(),Alignment.CenterVertically,Arrangement.SpaceBetween){Text(x);Switch(x in menu,{on->val n=menu.toMutableSet();if(on)n.add(x)else n.remove(x);save(c,auto,theme,style,n)})}}
}

@Composable private fun AccountDialog(close:()->Unit,save:(String,Double,String,String)->Unit){
 var n by remember{mutableStateOf("")};var b by remember{mutableStateOf("")};var t by remember{mutableStateOf("Счёт")};var c by remember{mutableStateOf("GBP")}
 AlertDialog(onDismissRequest=close,title={Text("Новый счёт")},text={Column{OutlinedTextField(n,{n=it},label={Text("Название")});OutlinedTextField(b,{b=it},label={Text("Баланс")});Row{FilterChip(t=="Счёт",{t="Счёт"},label={Text("Счёт")});FilterChip(t=="Карта",{t="Карта"},label={Text("Карта")})};Row{currencies.forEach{x->FilterChip(c==x,{c=x},label={Text(x)})}}}},confirmButton={Button({save(n.trim(),b.replace(',','.').toDoubleOrNull()?:0.0,t,c)},enabled=n.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun TransactionDialog(accounts:List<Account>,close:()->Unit,save:(Transaction)->Unit){
 var n by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var inc by remember{mutableStateOf(false)};var cat by remember{mutableStateOf("")};var acc by remember{mutableStateOf(accounts.firstOrNull()?.name?:"")};val c=accounts.firstOrNull{it.name==acc}?.currency?:"GBP";val v=a.replace(',','.').toDoubleOrNull()
 AlertDialog(onDismissRequest=close,title={Text("Новая операция")},text={Column{OutlinedTextField(n,{n=it},label={Text("Описание")});OutlinedTextField(a,{a=it},label={Text("Сумма "+c)});Row{FilterChip(inc,{inc=true},label={Text("Доход")});FilterChip(!inc,{inc=false},label={Text("Расход")})};OutlinedTextField(cat,{cat=it},label={Text("Категория")});Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Продукты","Транспорт","Жильё","Зарплата","Развлечения","Другое").forEach{x->FilterChip(cat==x,{cat=x},label={Text(x)})}};Row{accounts.forEach{x->FilterChip(acc==x.name,{acc=x.name},label={Text(x.name)})}}}},confirmButton={Button({save(Transaction(title=n.trim(),amount=v?:0.0,income=inc,accountName=acc,category=cat.ifBlank{"Без категории"},currency=c))},enabled=n.isNotBlank()&&v!=null&&v>0&&acc.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun DebtDialog(close:()->Unit,save:(Debt)->Unit){
 var p by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var mine by remember{mutableStateOf(false)};var interest by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Новый долг")},text={Column{OutlinedTextField(p,{p=it},label={Text("Человек")});OutlinedTextField(a,{a=it},label={Text("Сумма")});Row{FilterChip(mine,{mine=true},label={Text("Я должен")});FilterChip(!mine,{mine=false},label={Text("Мне должны")})};Row(Alignment.CenterVertically){Text("Проценты");Switch(interest,{interest=it})};OutlinedTextField(note,{note=it},label={Text("Заметка")})}},confirmButton={Button({save(Debt(person=p,amount=a.replace(',','.').toDoubleOrNull()?:0.0,mine=mine,interest=interest,note=note))},enabled=p.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun GoalDialog(c:String,close:()->Unit,save:(Goal)->Unit){
 var n by remember{mutableStateOf("")};var t by remember{mutableStateOf("")};var d by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Новая цель")},text={Column{OutlinedTextField(n,{n=it},label={Text("Название")});OutlinedTextField(t,{t=it},label={Text("Цель "+c)});OutlinedTextField(d,{d=it},label={Text("Срок")})}},confirmButton={Button({save(Goal(name=n,target=t.replace(',','.').toDoubleOrNull()?:0.0,currency=c,deadline=d))},enabled=n.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
@Composable private fun ReminderDialog(close:()->Unit,save:(Reminder)->Unit){
 var n by remember{mutableStateOf("")};var d by remember{mutableStateOf("")};var rep by remember{mutableStateOf("Один раз")}
 AlertDialog(onDismissRequest=close,title={Text("Напоминание")},text={Column{OutlinedTextField(n,{n=it},label={Text("Что напомнить")});OutlinedTextField(d,{d=it},label={Text("Дата")});Row{listOf("Один раз","Еженедельно","Ежемесячно").forEach{x->FilterChip(rep==x,{rep=x},label={Text(x)})}}}},confirmButton={Button({save(Reminder(title=n,date=d,repeat=rep))},enabled=n.isNotBlank()&&d.isNotBlank()){Text("Сохранить")}},dismissButton={TextButton(close){Text("Отмена")}})
}
