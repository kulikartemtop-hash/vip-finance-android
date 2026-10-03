package com.example.vipfinance

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FinanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("vip_finance", Context.MODE_PRIVATE)

    fun loadAccounts(): List<Account> = runCatching {
        val a = JSONArray(prefs.getString("accounts", "[]"))
        buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Account(o.optString("name"), o.optDouble("balance"), o.optBoolean("hidden"), o.optString("type","Счёт"), o.optString("currency","RUB"),o.optString("icon","account_balance"),o.optString("iconColor","#5B35F5")))
            }
        }
    }.getOrDefault(emptyList())

    fun saveAccounts(items: List<Account>) {
        val a=JSONArray()
        items.forEach { x -> a.put(JSONObject().apply { put("name",x.name);put("balance",x.balance);put("hidden",x.hidden);put("type",x.type);put("currency",x.currency);put("icon",x.icon);put("iconColor",x.iconColor) }) }
        prefs.edit().putString("accounts",a.toString()).apply()
    }

    fun loadTransactions(): List<Transaction> = runCatching {
        val a=JSONArray(prefs.getString("transactions","[]"))
        buildList {
            for(i in 0 until a.length()) { val o=a.getJSONObject(i); add(Transaction(o.optLong("id"),o.optString("title"),o.optDouble("amount"),o.optBoolean("income"),o.optString("accountName"),o.optString("category","Без категории"),o.optLong("timestamp",System.currentTimeMillis()),o.optString("currency","RUB"),o.optString("operationType",if(o.optBoolean("income"))"income" else "expense"),o.optString("toAccountName",""),o.optString("note",""),o.optString("repeat","Не повторять"))) }
        }
    }.getOrDefault(emptyList())

    fun saveTransactions(items: List<Transaction>) {
        val a=JSONArray()
        items.forEach { x -> a.put(JSONObject().apply { put("id",x.id);put("title",x.title);put("amount",x.amount);put("income",x.income);put("accountName",x.accountName);put("category",x.category);put("timestamp",x.timestamp);put("currency",x.currency);put("operationType",x.operationType);put("toAccountName",x.toAccountName);put("note",x.note);put("repeat",x.repeat) }) }
        prefs.edit().putString("transactions",a.toString()).apply()
    }

    fun loadCategories(): List<Category> = runCatching {
        val a=JSONArray(prefs.getString("categories","[]"))
        buildList { for(i in 0 until a.length()){ val o=a.getJSONObject(i); add(Category(o.optLong("id"),o.optString("name"),o.optString("kind","expense"),o.optString("icon","category"),o.optString("color","#5B35F5"))) } }
    }.getOrDefault(emptyList())

    fun saveCategories(items: List<Category>) {
        val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("name",x.name);put("kind",x.kind);put("icon",x.icon);put("color",x.color)})};prefs.edit().putString("categories",a.toString()).apply()
    }

    fun loadDebts(): List<Debt> = runCatching {
        val a=JSONArray(prefs.getString("debts","[]"))
        buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(Debt(o.optLong("id"),o.optString("person"),o.optDouble("amount"),o.optBoolean("mine"),o.optBoolean("interest"),o.optString("note"))) } }
    }.getOrDefault(emptyList())

    fun saveDebts(items: List<Debt>) {
        val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("person",x.person);put("amount",x.amount);put("mine",x.mine);put("interest",x.interest);put("note",x.note)})};prefs.edit().putString("debts",a.toString()).apply()
    }

    fun loadGoals(): List<Goal> = runCatching {
        val a=JSONArray(prefs.getString("goals","[]"))
        buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(Goal(o.optLong("id"),o.optString("name"),o.optDouble("target"),o.optDouble("saved"),o.optString("currency","RUB"),o.optString("deadline"))) } }
    }.getOrDefault(emptyList())

    fun saveGoals(items: List<Goal>) {
        val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("name",x.name);put("target",x.target);put("saved",x.saved);put("currency",x.currency);put("deadline",x.deadline)})};prefs.edit().putString("goals",a.toString()).apply()
    }

    fun loadReminders(): List<Reminder> = runCatching {
        val a=JSONArray(prefs.getString("reminders","[]"))
        buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(Reminder(o.optLong("id"),o.optString("title"),o.optDouble("amount"),o.optString("date"),o.optString("repeat","Один раз"),o.optBoolean("done"))) } }
    }.getOrDefault(emptyList())

    fun saveReminders(items: List<Reminder>) {
        val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("title",x.title);put("amount",x.amount);put("date",x.date);put("repeat",x.repeat);put("done",x.done)})};prefs.edit().putString("reminders",a.toString()).apply()
    }

    fun loadCurrency(): String = prefs.getString("currency","RUB") ?: "RUB"
    fun saveCurrency(v:String)=prefs.edit().putString("currency",v).apply()
    fun loadAutoConversion():Boolean=prefs.getBoolean("auto_conversion",true)
    fun saveAutoConversion(v:Boolean)=prefs.edit().putBoolean("auto_conversion",v).apply()
    fun loadTheme():String=prefs.getString("theme","system") ?: "system"
    fun saveTheme(v:String)=prefs.edit().putString("theme",v).apply()
    fun loadStyle():String=when(prefs.getString("style","platinum") ?: "platinum"){ "classic" -> "platinum"; "ocean" -> "emerald"; "graphite" -> "platinum"; else -> prefs.getString("style","platinum") ?: "platinum" }
    fun saveStyle(v:String)=prefs.edit().putString("style",v).apply()
    fun loadMenu(): Set<String> =prefs.getStringSet("menu",setOf("Главная","Операции","Счета","Аналитика","Конвертер","Долги","Цели","Напоминания","Чеки","Ещё")) ?: emptySet()
    fun saveMenu(v:Set<String>)=prefs.edit().putStringSet("menu",v).apply()
    fun saveRates(rates:Map<String,Double>){val j=JSONObject();rates.forEach{(k,v)->j.put(k,v)};prefs.edit().putString("rates",j.toString()).putLong("rates_time",System.currentTimeMillis()).apply()}
    fun loadRates(): Map<String, Double> =runCatching{val j=JSONObject(prefs.getString("rates","{}"));buildMap{j.keys().forEach{k->put(k,j.optDouble(k))}}}.getOrDefault(emptyMap())
    fun loadRatesTime():Long=prefs.getLong("rates_time",0L)
}


    fun loadBudgets(): List<Budget> = runCatching {
        val a=JSONArray(prefs.getString("budgets","[]"))
        buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(Budget(o.optLong("id"),o.optString("name"),o.optString("category"),o.optString("accountName"),o.optDouble("limit"),o.optString("currency","RUB"),o.optString("period","Месяц"))) } }
    }.getOrDefault(emptyList())

    fun saveBudgets(items: List<Budget>) {
        val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("name",x.name);put("category",x.category);put("accountName",x.accountName);put("limit",x.limit);put("currency",x.currency);put("period",x.period)})};prefs.edit().putString("budgets",a.toString()).apply()
    }
