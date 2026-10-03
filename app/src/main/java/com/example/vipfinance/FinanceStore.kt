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
                add(Account(o.optString("name"), o.optDouble("balance"), o.optBoolean("hidden"), o.optString("type","Счёт"), o.optString("currency","GBP")))
            }
        }
    }.getOrDefault(emptyList())

    fun saveAccounts(items: List<Account>) {
        val a=JSONArray()
        items.forEach { x -> a.put(JSONObject().apply { put("name",x.name);put("balance",x.balance);put("hidden",x.hidden);put("type",x.type);put("currency",x.currency) }) }
        prefs.edit().putString("accounts",a.toString()).apply()
    }

    fun loadTransactions(): List<Transaction> = runCatching {
        val a=JSONArray(prefs.getString("transactions","[]"))
        buildList {
            for(i in 0 until a.length()) { val o=a.getJSONObject(i); add(Transaction(o.optLong("id"),o.optString("title"),o.optDouble("amount"),o.optBoolean("income"),o.optString("accountName"),o.optString("category","Без категории"),o.optLong("timestamp",System.currentTimeMillis()),o.optString("currency","GBP"))) }
        }
    }.getOrDefault(emptyList())

    fun saveTransactions(items: List<Transaction>) {
        val a=JSONArray()
        items.forEach { x -> a.put(JSONObject().apply { put("id",x.id);put("title",x.title);put("amount",x.amount);put("income",x.income);put("accountName",x.accountName);put("category",x.category);put("timestamp",x.timestamp);put("currency",x.currency) }) }
        prefs.edit().putString("transactions",a.toString()).apply()
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
        buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(Goal(o.optLong("id"),o.optString("name"),o.optDouble("target"),o.optDouble("saved"),o.optString("currency","GBP"),o.optString("deadline"))) } }
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

    fun loadCurrency(): String = prefs.getString("currency","GBP") ?: "GBP"
    fun saveCurrency(v:String)=prefs.edit().putString("currency",v).apply()
    fun loadAutoConversion():Boolean=prefs.getBoolean("auto_conversion",true)
    fun saveAutoConversion(v:Boolean)=prefs.edit().putBoolean("auto_conversion",v).apply()
    fun loadTheme():String=prefs.getString("theme","system") ?: "system"
    fun saveTheme(v:String)=prefs.edit().putString("theme",v).apply()
    fun loadStyle():String=prefs.getString("style","classic") ?: "classic"
    fun saveStyle(v:String)=prefs.edit().putString("style",v).apply()
    fun loadMenu():Set<String>=prefs.getStringSet("menu",setOf("Главная","Операции","Счета","Аналитика","Конвертер","Долги","Цели","Напоминания","Чеки","Ещё")) ?: emptySet()
    fun saveMenu(v:Set<String>)=prefs.edit().putStringSet("menu",v).apply()
    fun saveRates(rates:Map<String,Double>){val j=JSONObject();rates.forEach{(k,v)->j.put(k,v)};prefs.edit().putString("rates",j.toString()).putLong("rates_time",System.currentTimeMillis()).apply()}
    fun loadRates():Map<String,Double>=runCatching{val j=JSONObject(prefs.getString("rates","{}"));buildMap{j.keys().forEach{k->put(k,j.optDouble(k))}}}.getOrDefault(emptyMap())
    fun loadRatesTime():Long=prefs.getLong("rates_time",0L)
}
