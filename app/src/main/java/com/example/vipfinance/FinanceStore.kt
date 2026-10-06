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
            for(i in 0 until a.length()) { val o=a.getJSONObject(i); add(Transaction(o.optLong("id"),o.optString("title"),o.optDouble("amount"),o.optBoolean("income"),o.optString("accountName"),o.optString("category","Без категории"),o.optLong("timestamp",System.currentTimeMillis()),o.optString("currency","RUB"),o.optString("operationType",if(o.optBoolean("income"))"income" else "expense"),o.optString("toAccountName",""),o.optString("note",""),o.optString("repeat","Не повторять"),o.optString("tags",""))) }
        }
    }.getOrDefault(emptyList())

    fun saveTransactions(items: List<Transaction>) {
        val a=JSONArray()
        items.forEach { x -> a.put(JSONObject().apply { put("id",x.id);put("title",x.title);put("amount",x.amount);put("income",x.income);put("accountName",x.accountName);put("category",x.category);put("timestamp",x.timestamp);put("currency",x.currency);put("operationType",x.operationType);put("toAccountName",x.toAccountName);put("note",x.note);put("repeat",x.repeat);put("tags",x.tags) }) }
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
        buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(Debt(o.optLong("id"),o.optString("person"),o.optDouble("amount"),o.optBoolean("mine"),o.optBoolean("interest"),o.optDouble("interestRate",0.0),o.optString("dueDate"),o.optString("note"),o.optDouble("paid",0.0))) } }
    }.getOrDefault(emptyList())

    fun saveDebts(items: List<Debt>) {
        val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("person",x.person);put("amount",x.amount);put("mine",x.mine);put("interest",x.interest);put("interestRate",x.interestRate);put("dueDate",x.dueDate);put("note",x.note);put("paid",x.paid)})};prefs.edit().putString("debts",a.toString()).apply()
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
    fun loadBudgets(): List<Budget> = runCatching {
        val a=JSONArray(prefs.getString("budgets","[]"))
        buildList { for(i in 0 until a.length()){val o=a.getJSONObject(i);add(Budget(o.optLong("id"),o.optString("name"),o.optString("category"),o.optString("accountName"),o.optDouble("limit"),o.optString("currency","RUB"),o.optString("period","Месяц"))) } }
    }.getOrDefault(emptyList())

    fun saveBudgets(items: List<Budget>) {
        val a=JSONArray();items.forEach{x->a.put(JSONObject().apply{put("id",x.id);put("name",x.name);put("category",x.category);put("accountName",x.accountName);put("limit",x.limit);put("currency",x.currency);put("period",x.period)})};prefs.edit().putString("budgets",a.toString()).apply()
    }

    fun loadPin(): String = prefs.getString("app_pin", "") ?: ""
    fun savePin(v: String) = prefs.edit().putString("app_pin", v).apply()

    fun importBackupJson(json: String): Boolean = runCatching {
        val root = JSONObject(json)
        require(root.optString("format") == "VIP Finance backup")
        val a = root.optJSONArray("accounts") ?: JSONArray()
        saveAccounts(buildList { for (i in 0 until a.length()) { val o=a.getJSONObject(i); add(Account(o.optString("name"),o.optDouble("balance"),o.optBoolean("hidden"),o.optString("type","Счёт"),o.optString("currency","RUB"),o.optString("icon","account_balance"),o.optString("iconColor","#5B35F5"))) } })
        val t = root.optJSONArray("transactions") ?: JSONArray()
        saveTransactions(buildList { for (i in 0 until t.length()) { val o=t.getJSONObject(i); add(Transaction(o.optLong("id"),o.optString("title"),o.optDouble("amount"),o.optBoolean("income"),o.optString("accountName"),o.optString("category","Без категории"),o.optLong("timestamp",System.currentTimeMillis()),o.optString("currency","RUB"),o.optString("operationType",if(o.optBoolean("income"))"income" else "expense"),o.optString("toAccountName",""),o.optString("note",""),o.optString("repeat","Не повторять"),o.optString("tags",""))) } })
        val c = root.optJSONArray("categories") ?: JSONArray()
        saveCategories(buildList { for (i in 0 until c.length()) { val o=c.getJSONObject(i); add(Category(o.optLong("id"),o.optString("name"),o.optString("kind","expense"),o.optString("icon","category"),o.optString("color","#5B35F5"))) } })
        val b = root.optJSONArray("budgets") ?: JSONArray()
        saveBudgets(buildList { for (i in 0 until b.length()) { val o=b.getJSONObject(i); add(Budget(o.optLong("id"),o.optString("name"),o.optString("category"),o.optString("accountName"),o.optDouble("limit"),o.optString("currency","RUB"),o.optString("period","Месяц"))) } })
        val d = root.optJSONArray("debts") ?: JSONArray()
        saveDebts(buildList { for (i in 0 until d.length()) { val o=d.getJSONObject(i); add(Debt(o.optLong("id"),o.optString("person"),o.optDouble("amount"),o.optBoolean("mine"),o.optBoolean("interest"),o.optDouble("interestRate"),o.optString("dueDate"),o.optString("note"),o.optDouble("paid",0.0))) } })
        val g = root.optJSONArray("goals") ?: JSONArray()
        saveGoals(buildList { for (i in 0 until g.length()) { val o=g.getJSONObject(i); add(Goal(o.optLong("id"),o.optString("name"),o.optDouble("target"),o.optDouble("saved"),o.optString("currency","RUB"),o.optString("deadline"))) } })
        val rr = root.optJSONArray("reminders") ?: JSONArray()
        saveReminders(buildList { for (i in 0 until rr.length()) { val o=rr.getJSONObject(i); add(Reminder(o.optLong("id"),o.optString("title"),o.optDouble("amount"),o.optString("date"),o.optString("repeat","Один раз"),o.optBoolean("done"))) } })
        root.optJSONObject("settings")?.let { q ->
            saveCurrency(q.optString("currency","RUB")); saveAutoConversion(q.optBoolean("autoConversion",true)); saveTheme(q.optString("theme","system")); saveStyle(q.optString("style","platinum"))
            val menuArr=q.optJSONArray("menu"); if(menuArr!=null){ saveMenu(buildSet { for(i in 0 until menuArr.length()) add(menuArr.getString(i)) }) }
            val ratesObj=q.optJSONObject("rates"); if(ratesObj!=null){ val map=buildMap<String,Double>{ ratesObj.keys().forEach{ k->put(k,ratesObj.optDouble(k)) } }; saveRates(map) }
        }
        true
    }.getOrDefault(false)

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("format", "VIP Finance backup")
        root.put("version", 1)
        fun arr(block: (JSONArray) -> Unit): JSONArray = JSONArray().also(block)

        root.put("accounts", arr { a -> loadAccounts().forEach { x ->
            a.put(JSONObject().apply { put("name",x.name);put("balance",x.balance);put("hidden",x.hidden);put("type",x.type);put("currency",x.currency);put("icon",x.icon);put("iconColor",x.iconColor) })
        }})
        root.put("transactions", arr { a -> loadTransactions().forEach { x ->
            a.put(JSONObject().apply { put("id",x.id);put("title",x.title);put("amount",x.amount);put("income",x.income);put("accountName",x.accountName);put("category",x.category);put("timestamp",x.timestamp);put("currency",x.currency);put("operationType",x.operationType);put("toAccountName",x.toAccountName);put("note",x.note);put("repeat",x.repeat);put("tags",x.tags) })
        }})
        root.put("categories", arr { a -> loadCategories().forEach { x ->
            a.put(JSONObject().apply { put("id",x.id);put("name",x.name);put("kind",x.kind);put("icon",x.icon);put("color",x.color) })
        }})
        root.put("budgets", arr { a -> loadBudgets().forEach { x ->
            a.put(JSONObject().apply { put("id",x.id);put("name",x.name);put("category",x.category);put("accountName",x.accountName);put("limit",x.limit);put("currency",x.currency);put("period",x.period) })
        }})
        root.put("debts", arr { a -> loadDebts().forEach { x ->
            a.put(JSONObject().apply { put("id",x.id);put("person",x.person);put("amount",x.amount);put("mine",x.mine);put("interest",x.interest);put("interestRate",x.interestRate);put("note",x.note) })
        }})
        root.put("goals", arr { a -> loadGoals().forEach { x ->
            a.put(JSONObject().apply { put("id",x.id);put("name",x.name);put("target",x.target);put("saved",x.saved);put("currency",x.currency);put("deadline",x.deadline) })
        }})
        root.put("reminders", arr { a -> loadReminders().forEach { x ->
            a.put(JSONObject().apply { put("id",x.id);put("title",x.title);put("amount",x.amount);put("date",x.date);put("repeat",x.repeat);put("done",x.done) })
        }})
        root.put("settings", JSONObject().apply {
            put("currency", loadCurrency())
            put("autoConversion", loadAutoConversion())
            put("theme", loadTheme())
            put("style", loadStyle())
            put("menu", JSONArray(loadMenu().toList()))
            put("rates", JSONObject().apply { loadRates().forEach { (k,v) -> put(k,v) } })
            put("ratesTime", loadRatesTime())
        })
        return root.toString(2)
    }


}
