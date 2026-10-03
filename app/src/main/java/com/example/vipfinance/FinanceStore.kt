package com.example.vipfinance

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FinanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("vip_finance", Context.MODE_PRIVATE)

    fun loadAccounts(): List<Account> {
        val raw = prefs.getString("accounts", "[]") ?: "[]"
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Account(
                name = o.getString("name"),
                balance = o.getDouble("balance"),
                hidden = o.optBoolean("hidden", false),
                type = o.optString("type", "Счёт")
            )
        }
    }

    fun saveAccounts(list: List<Account>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("name", it.name)
                put("balance", it.balance)
                put("hidden", it.hidden)
                put("type", it.type)
            })
        }
        prefs.edit().putString("accounts", a.toString()).apply()
    }

    fun loadTransactions(): List<Transaction> {
        val raw = prefs.getString("transactions", "[]") ?: "[]"
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Transaction(
                title = o.getString("title"),
                amount = o.getDouble("amount"),
                income = o.getBoolean("income"),
                accountName = o.optString("accountName", ""),
                category = o.optString("category", "Без категории"),
                timestamp = o.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }

    fun loadCurrency(): String = prefs.getString("currency", "GBP") ?: "GBP"

    fun saveCurrency(currency: String) {
        prefs.edit().putString("currency", currency).apply()
    }

    fun saveTransactions(list: List<Transaction>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("title", it.title)
                put("amount", it.amount)
                put("income", it.income)
                put("accountName", it.accountName)
                put("category", it.category)
                put("timestamp", it.timestamp)
            })
        }
        prefs.edit().putString("transactions", a.toString()).apply()
    }
}
