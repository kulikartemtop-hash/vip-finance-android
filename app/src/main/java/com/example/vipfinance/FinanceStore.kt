package com.example.vipfinance

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FinanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("vip_finance", Context.MODE_PRIVATE)

    fun loadAccounts(): List<Account> {
        val raw = prefs.getString("accounts", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Account(
                            name = o.optString("name"),
                            balance = o.optDouble("balance", 0.0),
                            hidden = o.optBoolean("hidden", false),
                            type = o.optString("type", "Счёт"),
                            currency = o.optString("currency", "GBP")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveAccounts(accounts: List<Account>) {
        val array = JSONArray()
        accounts.forEach {
            array.put(
                JSONObject().apply {
                    put("name", it.name)
                    put("balance", it.balance)
                    put("hidden", it.hidden)
                    put("type", it.type)
                    put("currency", it.currency)
                }
            )
        }
        prefs.edit().putString("accounts", array.toString()).apply()
    }

    fun loadTransactions(): List<Transaction> {
        val raw = prefs.getString("transactions", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Transaction(
                            title = o.optString("title"),
                            amount = o.optDouble("amount", 0.0),
                            income = o.optBoolean("income", false),
                            accountName = o.optString("accountName", ""),
                            category = o.optString("category", "Без категории"),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            currency = o.optString("currency", "GBP")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val array = JSONArray()
        transactions.forEach {
            array.put(
                JSONObject().apply {
                    put("title", it.title)
                    put("amount", it.amount)
                    put("income", it.income)
                    put("accountName", it.accountName)
                    put("category", it.category)
                    put("timestamp", it.timestamp)
                    put("currency", it.currency)
                }
            )
        }
        prefs.edit().putString("transactions", array.toString()).apply()
    }

    fun loadCurrency(): String = prefs.getString("currency", "GBP") ?: "GBP"

    fun saveCurrency(currency: String) {
        prefs.edit().putString("currency", currency).apply()
    }

    fun loadAutoConversion(): Boolean = prefs.getBoolean("auto_conversion", true)

    fun saveAutoConversion(enabled: Boolean) {
        prefs.edit().putBoolean("auto_conversion", enabled).apply()
    }

    fun saveRates(rates: Map<String, Double>) {
        val json = JSONObject()
        rates.forEach { (code, rate) -> json.put(code, rate) }
        prefs.edit()
            .putString("rates", json.toString())
            .putLong("rates_time", System.currentTimeMillis())
            .apply()
    }

    fun loadRates(): Map<String, Double> {
        val raw = prefs.getString("rates", null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key -> put(key, json.optDouble(key)) }
            }
        }.getOrDefault(emptyMap())
    }

    fun loadRatesTime(): Long = prefs.getLong("rates_time", 0L)
}
