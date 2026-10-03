package com.example.vipfinance

data class Account(
    val name: String,
    val balance: Double,
    val hidden: Boolean = false
)

data class Transaction(
    val title: String,
    val amount: Double,
    val income: Boolean,
    val accountName: String = "",
    val category: String = "Без категории",
    val timestamp: Long = System.currentTimeMillis()
)
