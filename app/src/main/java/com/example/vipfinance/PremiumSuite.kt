package com.example.vipfinance
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

@Composable
fun PremiumSuite(context: Context, accounts: List<Account>, tx: List<Transaction>, debts: List<Debt>, goals: List<Goal>, budgets: List<Budget>, currency: String, auto: Boolean, rates: Map<String,Double>) {
    val visible=accounts.filter{!it.hidden}
    val balance=visible.sumOf{conv(it.balance,it.currency,currency,auto,rates)}
    val expenses=tx.filter{!it.income&&it.operationType!="transfer"}
    val incomes=tx.filter{it.income&&it.operationType!="transfer"}
    val cal=Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
    val start=cal.timeInMillis
    val monthExpenses=expenses.filter{it.timestamp>=start}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}
    val monthIncome=incomes.filter{it.timestamp>=start}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}
    val net=monthIncome-monthExpenses
    val debt=debts.sumOf{(it.amount+(if(it.interest)it.amount*it.interestRate/100 else 0.0)-it.paid).coerceAtLeast(0.0)}
    val netWorth=balance-debt
    val largest=expenses.filter{it.timestamp>=start}.groupBy{it.category}.mapValues{(_,v)->v.sumOf{conv(it.amount,it.currency,currency,auto,rates)}}.maxByOrNull{it.value}
    val recurring=expenses.groupBy{it.title.ifBlank{it.category}.trim().lowercase(Locale.getDefault())}.mapNotNull{(name,list)->
        if(list.size<2)null else { val s=list.sortedBy{it.timestamp}; val gap=s.zipWithNext().map{(a,b)->(b.timestamp-a.timestamp)/86400000.0}.average(); if(gap in 20.0..100.0) name to list.sumOf{conv(it.amount,it.currency,currency,auto,rates)}/list.size else null }
    }
    val yearly=recurring.sumOf{it.second}*12
    val score=(100-(if(net<0)30 else 0)-(if(monthIncome>0&&monthExpenses/monthIncome>.8)20 else 0)-(if(debt>balance&&balance>0)15 else 0)).coerceIn(0,100)
    val prefs=context.getSharedPreferences("vip_premium",Context.MODE_PRIVATE)
    var family by remember{mutableStateOf(prefs.getBoolean("family_mode",false))}
    var conservative by remember{mutableStateOf(prefs.getBoolean("conservative",false))}

    if(BuildConfig.VERSION_NAME>="4.9"){
        PremiumCard("🤖 VIP AI-ассистент","Умный разбор текущей ситуации"){
            Text(when{net<0->"В этом месяце расходы превышают доходы на "+money(-net,currency)+".";largest!=null->"Главная статья расходов — "+largest.key+": "+money(largest.value,currency)+".";else->"Финансовый темп выглядит устойчиво. Добавляйте операции для более точного анализа."})
            Text("Анализ выполняется локально, финансовые данные не отправляются.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PremiumCard("💳 Подписки","Контроль регулярных списаний"){
            Text("Найдено регулярных платежей: "+recurring.size)
            recurring.sortedByDescending{it.second}.take(5).forEach{Text(it.first.replaceFirstChar{c->c.uppercase()}+" • "+money(it.second,currency)+"/период")}
            Text("Ориентировочно за год: "+money(yearly,currency),fontWeight=FontWeight.Bold)
        }
        PremiumCard("🔔 Умные предупреждения","Что стоит проверить сейчас"){
            if(net<0)Text("⚠️ Месяц идёт в минус.")
            if(monthIncome>0&&monthExpenses/monthIncome>=.8)Text("⚠️ Расходы достигли "+("%.0f".format(monthExpenses/monthIncome*100))+"% дохода.")
            if(recurring.isNotEmpty())Text("🔁 Найдены регулярные платежи.")
            if(net>=0&&(monthIncome<=0||monthExpenses/monthIncome<.8))Text("✅ Критических сигналов нет.")
        }
    }
    if(BuildConfig.VERSION_NAME>="5.0"){
        PremiumCard("💰 Чистый капитал","Активы минус обязательства"){
            Text("Активы: "+money(balance,currency));Text("Долги: "+money(debt,currency));Text("Чистый капитал: "+money(netWorth,currency),fontWeight=FontWeight.ExtraBold)
            Text(if(net>=0)"Рост текущего месяца: +"+money(net,currency) else "Изменение текущего месяца: "+money(net,currency))
        }
        PremiumCard("📈 История капитала","Динамика по накопленным операциям"){
            val points=(5 downTo 0).map{off-> val c=Calendar.getInstance().apply{add(Calendar.MONTH,-off);set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}; val e=Calendar.getInstance().apply{timeInMillis=c.timeInMillis;add(Calendar.MONTH,1)}.timeInMillis; incomes.filter{it.timestamp<e}.sumOf{conv(it.amount,it.currency,currency,auto,rates)}-expenses.filter{it.timestamp<e}.sumOf{conv(it.amount,it.currency,currency,auto,rates)} }
            points.forEachIndexed{i,v->Text("Месяц "+(i+1)+": "+money(v,currency))}
            Text("История становится точнее по мере накопления операций.",style=MaterialTheme.typography.bodySmall)
        }
        PremiumCard("🏆 VIP Score","Финансовый рейтинг 0–100"){
            Text(score.toString()+" / 100",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold)
            Text(when{score>=85->"Отлично: система выглядит устойчиво.";score>=65->"Хорошо, но есть точки роста.";else->"Есть зоны для улучшения."})
        }
    }
    if(BuildConfig.VERSION_NAME>="5.1"){
        PremiumCard("🎯 Умные цели","Сценарии достижения быстрее"){
            goals.filter{it.saved<it.target}.take(5).forEach{g->val left=(g.target-g.saved).coerceAtLeast(0.0);val monthly=max(net,0.0);Text(g.name+": осталось "+money(left,g.currency));Text(if(monthly>0)"При текущем темпе ≈ "+("%.1f".format(left/monthly))+" мес." else "Нужен положительный темп накопления.");Text("С дополнительной экономией 10% расходов: "+("%.1f".format(left/max(monthly+monthExpenses*.10,1.0)))+" мес.")}
            if(goals.none{it.saved<it.target})Text("Добавьте цель для персонального сценария.")
        }
        PremiumCard("🧪 Сценарии «Что если?»","Сократите расходы и увидите эффект"){val a=monthExpenses*.10;val b=monthExpenses*.20;Text("−10%: +"+money(a,currency)+"/мес. • +"+money(a*12,currency)+"/год");Text("−20%: +"+money(b,currency)+"/мес. • +"+money(b*12,currency)+"/год");Text("Консервативный режим");Switch(conservative,{conservative=it;prefs.edit().putBoolean("conservative",it).apply()})}
        }
    }
    if(BuildConfig.VERSION_NAME>="5.2"){
        PremiumCard("🧾 Умные чеки","Контроль распознанных покупок"){val c=tx.count{it.title.contains("чек",true)||it.category.contains("чек",true)};Text("Чековых операций: "+c);Text("OCR уже доступен в разделе «Чеки».")}
        PremiumCard("🏦 Выписки","Контроль импорта"){val c=tx.count{it.category.equals("Импорт",true)};Text("Импортировано операций: "+c);Text(if(c>0)"Они участвуют в аналитике." else "Импортируйте CSV/TXT из банковского приложения.")}
    }
    if(BuildConfig.VERSION_NAME>="5.3"){
        PremiumCard("👨‍👩‍👧 Семейные финансы","Общий обзор на одном устройстве"){Text(if(family)"Семейный режим включён" else "Личный режим");Switch(family,{family=it;prefs.edit().putBoolean("family_mode",it).apply()});if(family){Text("Общий баланс: "+money(balance,currency),fontWeight=FontWeight.Bold);Text("Общий капитал: "+money(netWorth,currency))}}
    }
    if(BuildConfig.VERSION_NAME>="5.4"){
        PremiumCard("📱 VIP-виджеты","Быстрый доступ с рабочего стола"){Text("Виджет баланса доступен в Android.");Text("Добавьте его через меню виджетов → VIP Finance.");Text("Он показывает сумму видимых счетов.")}
    }
    if(BuildConfig.VERSION_NAME>="5.5"){
        val keys=listOf("ai" to "AI","subs" to "Подписки","score" to "VIP Score","goals" to "Цели","family" to "Семья","widgets" to "Виджеты")
        PremiumCard("🎛️ Настройка VIP-экрана","Выберите нужные блоки"){Text("Настройки сохраняются на этом устройстве.");keys.forEach{(key,label)->var enabled by remember{mutableStateOf(prefs.getBoolean("dash_"+key,true))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label);Switch(enabled,{enabled=it;prefs.edit().putBoolean("dash_"+key,it).apply()})}}}
        PremiumCard("✨ VIP 5.5","Персональный финансовый центр"){Text("AI • подписки • капитал • цели • семья • виджеты",fontWeight=FontWeight.Bold);Text("Новые модули работают поверх существующих данных.")}
    }
}

@Composable private fun PremiumCard(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){Card(shape=MaterialTheme.shapes.large){Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Column(verticalArrangement=Arrangement.spacedBy(5.dp),content=content)}}}