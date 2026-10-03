package com.example.vipfinance

data class Account(
    val name: String,
    val balance: Double,
    val hidden: Boolean = false,
    val type: String = "Счёт",
    val currency: String = "RUB",
    val icon: String = "account_balance",
    val iconColor: String = "#5B35F5"
)

data class Category(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val icon: String = "category",
    val color: String = "#5B35F5"
)

data class Transaction(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val amount: Double,
    val income: Boolean,
    val accountName: String = "",
    val category: String = "Без категории",
    val timestamp: Long = System.currentTimeMillis(),
    val currency: String = "RUB"
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
    val currency: String = "RUB",
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
