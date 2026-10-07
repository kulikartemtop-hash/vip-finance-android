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
import kotlin.math.pow

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
        val candidates=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        val text=candidates.maxByOrNull { voiceCandidateScore(it) }.orEmpty().trim()
        voiceText=text
        val account=accounts.firstOrNull{!it.hidden}
        if(account==null){
            voiceMessage="Сначала создайте доступный счёт."
        }else{
            val parsed=parseVoiceExpense(text, account, System.currentTimeMillis())
            if(parsed!=null){
                onVoiceTransaction(parsed)
                voiceMessage="Добавлен расход: \${money(parsed.amount,parsed.currency)} • \${parsed.category}."
            }else{
                voiceMessage="Не смог уверенно определить сумму. Скажите, например: «потратил две тысячи восемьсот рублей на продукты»."
            }
        }
    }
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null) runCatching{context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(backupJson())}}
    }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)){
        item { PremiumSuite(context, accounts, tx, debts, goals, budgets, currency, auto, rates) }
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
        item{SmartCard("🎙️ Голосовой ввод","Скажите: «потратил 2800 на продукты» или «купил продукты за две тысячи восемьсот»"){
            Button(onClick={
                val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ru-RU")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,"ru-RU")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
                    putExtra(RecognizerIntent.EXTRA_PROMPT,"Скажите сумму и категорию, например: потратил 2800 на продукты")
                }
                runCatching{voiceLauncher.launch(intent)}.onFailure{voiceMessage="Голосовой ввод недоступен на устройстве."}
            }){Text("🎤 Говорить")}
            if(voiceText.isNotBlank())Text("Распознано: $voiceText")
            if(voiceMessage.isNotBlank())Text(voiceMessage,color=MaterialTheme.colorScheme.primary)
        }}
        item{SmartCard("💾 Экспорт и резервная копия","Полная локальная копия данных"){
            Button(onClick={exportLauncher.launch("VIP-Finance-backup.json")}){Text("Сохранить резервную копию")}
            Text("Файл можно хранить отдельно и восстановить через настройки.")
        }}
    }
}
@Composable
fun VoiceInputCard(accounts: List<Account>, onVoiceTransaction: (Transaction) -> Unit) {
    var voiceText by remember { mutableStateOf("") }
    var voiceMessage by remember { mutableStateOf("") }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val candidates = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        val text = candidates.maxByOrNull { voiceCandidateScore(it) }.orEmpty().trim()
        voiceText = text
        val account = accounts.firstOrNull { !it.hidden }
        if (account == null) {
            voiceMessage = "Сначала создайте доступный счёт."
        } else {
            val parsed = parseVoiceTransaction(text, account, System.currentTimeMillis())
            if (parsed != null) {
                onVoiceTransaction(parsed)
                voiceMessage = if (parsed.income) "Добавлен доход: ${money(parsed.amount, parsed.currency)} • ${parsed.category}." else "Добавлен расход: ${money(parsed.amount, parsed.currency)} • ${parsed.category}."
            } else {
                voiceMessage = "Не смог определить операцию. Скажите: «потратил 2800 на продукты» или «получил зарплату 50000»."
            }
        }
    }
    SmartCard("🎙️ Голосовой ввод", "Расход или доход: «потратил 2800 на продукты» или «получил зарплату 50000»") {
        Button(onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите операцию: потратил 2800 на продукты или получил зарплату 50000")
            }
            runCatching { voiceLauncher.launch(intent) }
                .onFailure { voiceMessage = "Голосовой ввод недоступен на устройстве." }
        }) { Text("🎤 Говорить") }
        if (voiceText.isNotBlank()) Text("Распознано: $voiceText")
        if (voiceMessage.isNotBlank()) Text(voiceMessage, color = MaterialTheme.colorScheme.primary)
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
private fun voiceCandidateScore(text:String):Int{
    val t=text.lowercase(Locale.getDefault())
    var score=0
    if(Regex("""\d""").containsMatchIn(t)) score+=5
    if(listOf("тысяч","руб","рублей","потрат","купил","оплат","расход","заработ","получил","получила","зарплат","доход","поступил","поступление","начисли").any{t.contains(it)}) score+=3
    if(listOf("на ","за ","в ","от ","с ").any{t.contains(it)}) score+=1
    if(t.length>4) score+=1
    return score
}

private fun parseVoiceTransaction(text:String,account:Account,timestamp:Long):Transaction?{
    if(text.isBlank()) return null
    val normalized=text.lowercase(Locale.getDefault()).replace('ё','е').replace(Regex("""\s+""")," ").trim()
    val amount=extractVoiceAmount(normalized) ?: return null
    val incomeWords=listOf("получил","получила","получено","заработал","заработала","зарплата","зарплату","доход","поступил","поступила","поступление","начисли","начислили","аванс","премия","кэшбэк","кешбек","вернули","возврат","продал","продала","фриланс")
    val expenseWords=listOf("потратил","потратила","потратить","потрачено","купил","купила","оплатил","оплатила","заплатил","заплатила","расход","покупка")
    val isIncome=incomeWords.any{normalized.contains(it)} && !expenseWords.any{normalized.contains(it)}
    val category=if(isIncome) extractVoiceIncomeCategory(normalized) else extractVoiceCategory(normalized)
    val title=if(isIncome){
        normalized.replace(Regex("""\b(я|сегодня|вчера|мне)\b""")," ")
            .replace(Regex("""\b(получил|получила|получено|заработал|заработала|получить|заработать|поступил|поступила|поступление|начисли|начислили|получение|доход|зарплата|зарплату|аванс|премия|кэшбэк|кешбек|вернули|возврат|продал|продала|фриланс)\b""")," ")
            .replace(Regex("""\d[\d\s.,]*""")," ")
            .replace(Regex("""\s+""")," ").trim().ifBlank { category ?: "Доход" }
    }else{
        normalized.replace(Regex("""\b(я|сегодня|вчера)\b""")," ")
            .replace(Regex("""\b(потратил|потратила|потратить|потрачено|купил|купила|оплатил|оплатила|заплатил|заплатила|расход|покупка)\b""")," ")
            .replace(Regex("""\b(на|за|в)\s+(продукты|продукт|транспорт|такси|жилье|квартиру|зарплату|зарплата|развлечения|одежду|здоровье|связь|подписки)\b""")," ")
            .replace(Regex("""\d[\d\s.,]*""")," ")
            .replace(Regex("""\s+""")," ").trim().ifBlank { category ?: "Расход" }
    }
    return Transaction(id=timestamp,title=title.replaceFirstChar{it.uppercase()},amount=amount,income=isIncome,accountName=account.name,category=category ?: if(isIncome) "Доход" else "Другое",timestamp=timestamp,currency=account.currency,operationType=if(isIncome) "income" else "expense")
}

private fun parseVoiceExpense(text:String,account:Account,timestamp:Long):Transaction? =
    parseVoiceTransaction(text,account,timestamp)?.takeIf{!it.income}

private fun extractVoiceIncomeCategory(text:String):String?{
    val map=linkedMapOf("зарплат" to "Зарплата","аванс" to "Зарплата","преми" to "Зарплата","фриланс" to "Другое","кэшбэк" to "Другое","кешбек" to "Другое","возврат" to "Другое","вернули" to "Другое","продал" to "Другое","продала" to "Другое")
    return map.entries.firstOrNull{text.contains(it.key)}?.value
}

private fun extractVoiceCategory(text:String):String?{
    val map=linkedMapOf("продукты" to "Продукты","продукт" to "Продукты","магазин" to "Продукты","транспорт" to "Транспорт","такси" to "Транспорт","бензин" to "Транспорт","топливо" to "Транспорт","жилье" to "Жильё","квартиру" to "Жильё","квартплата" to "Жильё","развлечения" to "Развлечения","кино" to "Развлечения","игры" to "Развлечения","одежда" to "Одежда","одежду" to "Одежда","здоровье" to "Здоровье","аптека" to "Здоровье","лекарства" to "Здоровье","связь" to "Связь","телефон" to "Связь","интернет" to "Связь","подписка" to "Подписки","подписки" to "Подписки")
    return map.entries.firstOrNull{text.contains(it.key)}?.value
}

private fun extractVoiceAmount(text:String):Double?{
    val numeric=Regex("""(?<!\d)\d{1,3}(?:[ .]\d{3})+(?:[.,]\d{1,2})?(?!\d)|(?<!\d)\d+(?:[.,]\d+)?(?!\d)""").findAll(text).map{it.value}.toList()
    numeric.firstOrNull{token->
        val compact=token.replace(" ","")
        val separator=compact.lastIndexOfAny(charArrayOf('.',','))
        val fraction=if(separator>=0) compact.length-separator-1 else 0
        fraction==3 && separator>=0
    }?.let{token->token.replace(" ","").replace(".","").replace(",","").toDoubleOrNull()?.let{v->if(v>=1.0)return v}}
    numeric.firstOrNull()?.let{token->
        val compact=token.replace(" ","")
        val comma=compact.lastIndexOf(',')
        val dot=compact.lastIndexOf('.')
        return when{
            comma>=0&&dot>=0->{val last=maxOf(comma,dot);val frac=compact.length-last-1;if(frac==3)compact.replace(",","").replace(".","").toDoubleOrNull() else compact.substring(0,last).replace(",","").replace(".","").toDoubleOrNull()?.let{whole->compact.substring(last+1).toDoubleOrNull()?.let{part->whole+part/10.0.pow(frac.toDouble())}}}
            comma>=0->{val frac=compact.length-comma-1;if(frac==3)compact.replace(",","").toDoubleOrNull() else compact.replace(',','.').toDoubleOrNull()}
            dot>=0->{val frac=compact.length-dot-1;if(frac==3)compact.replace(".","").toDoubleOrNull() else compact.toDoubleOrNull()}
            else->compact.toDoubleOrNull()
        }
    }
    return parseRussianNumber(text)
}

private fun parseRussianNumber(text:String):Double?{
    val ones=mapOf("ноль" to 0,"один" to 1,"одна" to 1,"два" to 2,"две" to 2,"три" to 3,"четыре" to 4,"пять" to 5,"шесть" to 6,"семь" to 7,"восемь" to 8,"девять" to 9)
    val teens=mapOf("десять" to 10,"одиннадцать" to 11,"двенадцать" to 12,"тринадцать" to 13,"четырнадцать" to 14,"пятнадцать" to 15,"шестнадцать" to 16,"семнадцать" to 17,"восемнадцать" to 18,"девятнадцать" to 19)
    val tens=mapOf("двадцать" to 20,"тридцать" to 30,"сорок" to 40,"пятьдесят" to 50,"шестьдесят" to 60,"семьдесят" to 70,"восемьдесят" to 80,"девяносто" to 90)
    val hundreds=mapOf("сто" to 100,"двести" to 200,"триста" to 300,"четыреста" to 400,"пятьсот" to 500,"шестьсот" to 600,"семьсот" to 700,"восемьсот" to 800,"девятьсот" to 900)
    val words=text.split(Regex("""[^а-я0-9]+""")).filter{it.isNotBlank()}
    var total=0;var current=0;var found=false
    for(w in words) when{
        ones[w]!=null->{current+=ones[w]!!;found=true}
        teens[w]!=null->{current+=teens[w]!!;found=true}
        tens[w]!=null->{current+=tens[w]!!;found=true}
        hundreds[w]!=null->{current+=hundreds[w]!!;found=true}
        w=="тысяча"||w=="тысячи"||w=="тысяч"->{total+=if(current==0)1000 else current*1000;current=0;found=true}
        w=="миллион"||w=="миллиона"||w=="миллионов"->{total+=if(current==0)1000000 else current*1000000;current=0;found=true}
    }
    val result=total+current
    return if(found&&result>0)result.toDouble() else null
}
