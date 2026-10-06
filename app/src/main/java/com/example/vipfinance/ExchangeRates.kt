package com.example.vipfinance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

object ExchangeRates {
    private fun parseEcbRates(): Map<String, Double> {
        val connection = URL("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml").openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.useCaches = false
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
            return result
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Bank of Russia daily rates are used to provide a live RUB base
     * and the Belarusian ruble (BYN), which are not supplied by the
     * current ECB reference-rate feed.
     *
     * CBR values are quoted as RUB per nominal unit. We normalize them
     * to the same EUR=1 representation used by convert().
     */
    private fun parseCbrRates(): Map<String, Double> {
        val connection = URL("https://www.cbr.ru/scripts/XML_daily.asp").openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.useCaches = false
        try {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.inputStream)
            val nodes = document.getElementsByTagName("Valute")
            val rubPerUnit = mutableMapOf<String, Double>()

            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val code = node.childNodes.let { children ->
                    (0 until children.length)
                        .map { children.item(it) }
                        .firstOrNull { it.nodeName == "CharCode" }
                        ?.textContent
                        ?.trim()
                } ?: continue

                val nominalText = node.childNodes.let { children ->
                    (0 until children.length)
                        .map { children.item(it) }
                        .firstOrNull { it.nodeName == "Nominal" }
                        ?.textContent
                        ?.trim()
                } ?: "1"

                val valueText = node.childNodes.let { children ->
                    (0 until children.length)
                        .map { children.item(it) }
                        .firstOrNull { it.nodeName == "Value" }
                        ?.textContent
                        ?.trim()
                } ?: continue

                val nominal = nominalText.toDoubleOrNull() ?: continue
                val value = valueText.replace(',', '.').toDoubleOrNull() ?: continue
                if (nominal > 0.0) rubPerUnit[code] = value / nominal
            }

            val eurRub = rubPerUnit["EUR"] ?: return emptyMap()
            if (eurRub <= 0.0) return emptyMap()

            return buildMap {
                put("EUR", 1.0)
                put("RUB", eurRub)
                rubPerUnit.forEach { (code, rubRate) ->
                    if (code != "EUR" && rubRate > 0.0) {
                        put(code, eurRub / rubRate)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun loadEcbRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        val ecb = runCatching { parseEcbRates() }.getOrDefault(mapOf("EUR" to 1.0))
        val cbr = runCatching { parseCbrRates() }.getOrDefault(emptyMap())

        // Prefer CBR values when available so RUB, BYN and the other
        // supported currencies are all calculated from one RUB quote set.
        ecb.toMutableMap().apply { putAll(cbr) }
    }

    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double {
        if (from == to) return amount
        val fromRate = rates[from] ?: return amount
        val toRate = rates[to] ?: return amount
        return amount / fromRate * toRate
    }
}
