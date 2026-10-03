package com.example.vipfinance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

object ExchangeRates {
    suspend fun loadEcbRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        val connection = URL("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml").openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.useCaches = true
        try {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.inputStream)
            val result = mutableMapOf("EUR" to 1.0)
            val nodes = document.getElementsByTagName("Cube")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val currency = node.attributes?.getNamedItem("currency")?.nodeValue
                val rate = node.attributes?.getNamedItem("rate")?.nodeValue?.toDoubleOrNull()
                if (currency != null && rate != null) result[currency] = rate
            }
            result
        } finally {
            connection.disconnect()
        }
    }

    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double {
        if (from == to) return amount
        val fromRate = rates[from] ?: return amount
        val toRate = rates[to] ?: return amount
        return amount / fromRate * toRate
    }
}
