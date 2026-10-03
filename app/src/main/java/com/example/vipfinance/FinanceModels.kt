package com.example.vipfinance

data class Account(
    val name: String,
    val balance: Double,
    val hidden: Boolean = false,
    val type: String = "Счёт",
    val currency: String = "GBP"
)

data class Transaction(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val amount: Double,
    val income: Boolean,
    val accountName: String = "",
    val category: String = "Без категории",
    val timestamp: Long = System.currentTimeMillis(),
    val currency: String = "GBP"
)

data class Debt(
    val id: Long = System.currentTimeMillis(),
    val person: String,
    val amount: Double,
    val mine: Boolean,
    val interest: Boolean = false,
    val note: String = ""
)

data class Goal(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val target: Double,
    val saved: Double = 0.0,
    val currency: String = "GBP",
    val deadline: String = ""
)

data class Reminder(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val amount: Double = 0.0,
    val date: String,
    val repeat: String = "Один раз",
    val done: Boolean = false
)
