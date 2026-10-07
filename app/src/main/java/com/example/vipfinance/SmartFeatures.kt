package com.example.vipfinance

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun SmartCenter(
    accounts: List<Account>, tx: List<Transaction>, debts: List<Debt>, goals: List<Goal>,
    budgets: List<Budget>, currency: String, auto: Boolean, rates: Map<String,Double>,
    onVoiceTransaction: (Transaction) -> Unit,
    onImportTransactions: (List<Transaction>) -> Unit,
    backupJson: () -> String
) {
    val visible=accounts.filter{!it.hidden}
    val balance=visible.sumOf{conv(it.balance,it.currency,currency,auto,rates)}
    val expenses=tx.filter{!it.income&&it.operationType!="transfer"}
    val incomes=tx.filter{it.income&&it.operationType!="transfer"}
    val month=Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis
    val monthExpenses=expenses.filter{it.timestamp>=month}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}
    val monthIncome=incomes.filter{it.timestamp>=month}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}
    val net=monthIncome-monthExpenses
    val recentMonths=(0..2).map{offset->
        val c=Calendar.getInstance().apply{add(Calendar.MONTH,-offset)}
        val start=(c.clone() as Calendar).apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis
        val end=(c.clone() as Calendar).apply{add(Calendar.MONTH,1);set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis
        val i=incomes.filter{it.timestamp>=start&&it.timestamp<end}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}
        val e=expenses.filter{it.timestamp>=start&&it.timestamp<end}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}
        i-e
    }
    val avgNet=recentMonths.average()
    val recurring=expenses.groupBy{it.title.ifBlank{it.category}.trim().lowercase(Locale.getDefault())}.mapNotNull{(name,list)->
        if(list.size<2)null else {
            val sorted=list.sortedBy{it.timestamp}
            val gaps=sorted.zipWithNext().map{(a,b)->(b.timestamp-a.timestamp)/86400000.0}
            val gap=gaps.average()
            if(gap in 20.0..100.0) name to list.sumOf{conv(it.amount,it.currency,currency,auto,rates)}/list.size else null
        }
    }.sortedByDescending{it.second}
    val overspend=expenses.filter{it.timestamp>=month}.groupBy{it.category}.mapValues{(_,v)->v.sumOf{conv(it.amount,it.currency,currency,auto,rates)}}.maxByOrNull{it.value}
    val debtOutstanding=debts.sumOf{d->(d.amount+(if(d.interest)d.amount*d.interestRate/100 else 0.0)-d.paid).coerceAtLeast(0.0)}
    val warnings=budgets.mapNotNull{b->
        val spent=expenses.filter{it.category==b.category&&it.timestamp>=month}.sumOf{conv(it.amount,it.currency,b.currency,auto,rates)}
        val limit=conv(b.limit,b.currency,currency,auto,rates)
        if(limit>0&&spent/limit>=.8)b.name to spent/limit*100 else null
    }
    val context=androidx.compose.ui.platform.LocalContext.current
    var voiceText by remember{mutableStateOf("")}
    var voiceMessage by remember{mutableStateOf("")}
    val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val text=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        voiceText=text
        val amount=Regex("""(?i)(\d+(?:[.,]\d+)?)""").find(text)?.value?.replace(',','.')?.toDoubleOrNull()
        val account=accounts.firstOrNull{!it.hidden}
        if(amount!=null&&account!=null){
            onVoiceTransaction(Transaction(id=System.currentTimeMillis(),title=text,amount=amount,income=false,accountName=account.name,category="Другое",timestamp=System.currentTimeMillis(),currency=account.currency))
            voiceMessage="Расход на $amount ${account.currency} добавлен."
        }else voiceMessage="Не удалось определить сумму или доступный счёт."
    }
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null) runCatching{
            val raw=context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
            val result=raw.lines().mapNotNull{line->
                val p=line.split(';',',','	').map{it.trim().trim('"')}
                if(p.size<2)null else {
                    val amount=p.mapNotNull{it.replace(" ","").replace(",",".").toDoubleOrNull()}.firstOrNull()
                    val title=p.firstOrNull{it.toDoubleOrNull()==null&&!it.matches(Regex("""\d{1,2}[./]\d{1,2}[./]\d{2,4}"""))}
                    if(amount!=null&&title!=null) Transaction(id=System.currentTimeMillis()+p.hashCode(),title=title,amount=amount,income=false,accountName=accounts.firstOrNull()?.name.orEmpty(),category="Импорт",timestamp=System.currentTimeMillis(),currency=currency) else null
                }
            }
            onImportTransactions(result)
            voiceMessage="Импортировано операций: ${result.size}"
        }.onFailure{voiceMessage="Не удалось прочитать файл."}
    }
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null) runCatching{context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(backupJson())};voiceMessage="Резервная копия сохранена."}.onFailure{voiceMessage="Ошибка сохранения."}
    }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)){
        item{SmartCard("🤖 Финансовый помощник","Локальный анализ без отправки финансовых данных"){
            Text(when{
                monthIncome<=0&&monthExpenses>0->"В этом месяце есть расходы без зафиксированного дохода."
                net<0->"Расходы превышают доходы на ${money(abs(net),currency)}."
                warnings.isNotEmpty()->"Бюджеты близки к лимиту: ${warnings.joinToString(", "){it.first}}."
                recurring.isNotEmpty()->"Найдено ${recurring.size} вероятных регулярных платежа."
                else->"Финансовый темп выглядит устойчиво."
            })
        }}
        item{SmartCard("📈 Прогноз баланса","На основе среднего результата последних трёх месяцев"){
            Text("Через 3 месяца: ${money(balance+avgNet*3,currency)}")
            Text("Через 6 месяцев: ${money(balance+avgNet*6,currency)}")
            Text("Через 12 месяцев: ${money(balance+avgNet*12,currency)}",fontWeight=FontWeight.Bold)
            Text(if(avgNet>=0)"Тренд: +${money(avgNet,currency)} в месяц" else "Тренд: ${money(avgNet,currency)} в месяц")
        }}
        item{SmartCard("🔁 Подписки и регулярные платежи","Автоматический поиск повторяющихся расходов"){
            if(recurring.isEmpty())Text("Пока не найдено достаточно повторений.")
            recurring.take(8).forEach{(name,amount)->Text("${name.replaceFirstChar{it.uppercase()}} • ${money(amount,currency)} в среднем")}
            if(recurring.isNotEmpty())Text("Примерная сумма: ${money(recurring.sumOf{it.second},currency)}",fontWeight=FontWeight.Bold)
        }}
        item{SmartCard("🚨 Умные предупреждения","Проверка перерасхода и денежного дефицита"){
            if(net<0)Text("⚠️ Текущий месяц уходит в минус.")
            if(overspend!=null)Text("Крупнейшая категория: ${overspend.key} — ${money(overspend.value,currency)}")
            warnings.forEach{Text("Бюджет «${it.first}» использован на ${"%.0f".format(it.second)}%")}
            if(net>=0&&warnings.isEmpty())Text("Критических предупреждений нет.")
        }}
        item{SmartCard("🎯 Цели","Автоматическая оценка достижения целей"){
            val monthly=avgNet.coerceAtLeast(0.0)
            goals.filter{it.saved<it.target}.take(8).forEach{g->
                val left=(g.target-g.saved).coerceAtLeast(0.0)
                val months=if(monthly>0)max(1.0,left/monthly)else Double.POSITIVE_INFINITY
                Text("${g.name}: осталось ${money(left,g.currency)} • "+if(months.isFinite())"≈ ${"%.1f".format(months)} мес." else "нужны дополнительные накопления")
            }
            if(goals.none{it.saved<it.target})Text("Все текущие цели выполнены или отсутствуют.")
        }}
        item{SmartCard("💳 Долги","Контроль долговой нагрузки"){
            Text("Осталось по долгам: ${money(debtOutstanding,"RUB")}",fontWeight=FontWeight.Bold)
            Text(if(debtOutstanding>balance)"⚠️ Долговая нагрузка выше доступного баланса." else "Нагрузка по долгам в пределах текущего баланса.")
        }}
        item{SmartCard("🧠 Финансовое здоровье","Оценка по текущему поведению"){
            val score=(100-(if(net<0)30 else 0)-(if(monthIncome>0&&monthExpenses/monthIncome>.8)20 else 0)-(if(warnings.isNotEmpty())15 else 0)-(if(debtOutstanding>balance&&balance>0)15 else 0)).coerceIn(0,100)
            Text("$score / 100",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold)
            Text(when{score>=80->"Отличное состояние";score>=60->"Нормальное состояние";else->"Есть зоны для улучшения"})
        }}
        item{SmartCard("📉 Оптимизатор долгов","Сравнение ускоренного погашения"){
            val open=debts.map{d->d to (d.amount+(if(d.interest)d.amount*d.interestRate/100 else 0.0)-d.paid).coerceAtLeast(0.0)}.filter{it.second>0}
            if(open.isEmpty()) Text("Активных долгов нет.")
            else {
                val avalanche=open.sortedByDescending{it.first.interestRate}
                Text("Сначала выгоднее гасить: ${avalanche.first().first.person}",fontWeight=FontWeight.Bold)
                Text("Причина: ставка ${"%.1f".format(avalanche.first().first.interestRate)}%")
                Text("Стратегия: направляйте минимум на остальные долги, а свободные деньги — на самый дорогой по ставке.")
            }
        }}
        item{SmartCard("📈 Инвестиции","Учет инвестиционных счетов внутри общей картины"){
            val investments=accounts.filter{it.type.contains("инвест",true)||it.type.contains("брок",true)}
            Text("Инвестиционных счетов: ${investments.size}")
            investments.forEach{a->Text("${a.name}: ${money(a.balance,a.currency)}")}
            if(investments.isEmpty())Text("Создайте счёт с типом «Инвестиции» или «Брокер», чтобы он появился здесь.")
        }}
        item{SmartCard("👨‍👩‍👧 Семейный режим","Локальный безопасный обзор без облачной передачи"){
            var family by remember{mutableStateOf(false)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(if(family)"Семейный обзор включён" else "Личный режим")
                Switch(family,{family=it})
            }
            Text(if(family)"Показываем только общие финансовые показатели, без списка операций." else "Включите режим для совместного просмотра основных итогов на одном устройстве.")
            if(family)Text("Баланс семьи: ${money(balance,currency)}",fontWeight=FontWeight.Bold)
        }}
        item{SmartCard("💎 Чистый капитал","Активы минус обязательства"){
            val netWorth=balance-debtOutstanding
            Text("Активы: ${money(balance,currency)}")
            Text("Обязательства: ${money(debtOutstanding,"RUB")}")
            Text("Чистый капитал: ${money(netWorth,currency)}",fontWeight=FontWeight.ExtraBold)
        }}
        item{SmartCard("📊 История капитала","Оценка динамики по денежному потоку"){
            Text("Изменение за месяц: ${money(avgNet,currency)}")
            Text(if(avgNet>=0)"Капитал растёт по текущему тренду." else "Капитал снижается — стоит пересмотреть расходы.")
        }}
        item{SmartCard("🧪 Сценарии «Что если?»","Влияние сокращения расходов"){
            val save10=monthExpenses*.10
            val save20=monthExpenses*.20
            Text("−10% расходов: +${money(save10,currency)} в месяц")
            Text("−20% расходов: +${money(save20,currency)} в месяц")
            Text("За год при −20%: +${money(save20*12,currency)}")
        }}
        item{SmartCard("🛟 Финансовая подушка","Покрытие текущих расходов"){
            val runway=if(monthExpenses>0)balance/monthExpenses else Double.POSITIVE_INFINITY
            Text(if(runway.isFinite())"Запас: ${"%.1f".format(runway)} месяца" else "Расходы пока не определены")
            Text(if(runway>=6)"Подушка сильная." else if(runway>=3)"Подушка приемлемая." else "Запас небольшой — резерв стоит увеличить.")
        }}
        item{SmartCard("🎙️ Голосовой ввод","Скажите: «потратил 1250 на продукты»"){
            Button(onClick={
                val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
                }
                runCatching{voiceLauncher.launch(intent)}.onFailure{voiceMessage="Голосовой ввод недоступен на устройстве."}
            }){Text("🎤 Говорить")}
            if(voiceText.isNotBlank())Text("Распознано: $voiceText")
            if(voiceMessage.isNotBlank())Text(voiceMessage,color=MaterialTheme.colorScheme.primary)
        }}
        item{SmartCard("🏦 Импорт выписки","CSV / TXT с операциями"){
            Button(onClick={importLauncher.launch("text/*")}){Text("Выбрать файл")}
            Text("Приложение ищет сумму и описание в каждой строке и добавляет найденные операции.")
        }}
        item{SmartCard("💾 Экспорт и резервная копия","Полная локальная копия данных"){
            Button(onClick={exportLauncher.launch("VIP-Finance-backup.json")}){Text("Сохранить резервную копию")}
            Text("Файл можно хранить отдельно и восстановить через настройки.")
        }}
    }
}
@Composable private fun SmartCard(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){
    Card(shape=MaterialTheme.shapes.large){
        Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement=Arrangement.spacedBy(5.dp),content=content)
        }
    }
}