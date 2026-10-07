package com.example.vipfinance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.Locale

class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages=Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val text=messages.joinToString(" "){it.messageBody.orEmpty()}
        val lower=text.lowercase(Locale.getDefault())
        val bankWords=listOf("банк","оплата","покупка","списание","зачисление","card","payment","purchase","debit","credit")
        if(bankWords.none{lower.contains(it)}) return
        val amount=Regex("""(?i)(\d+(?:[.,]\d+)?)\s*(?:₽|руб|rub|р\.)?""").find(text)?.value?.replace(Regex("""[^0-9,.]"""),"")?.replace(',','.')?.toDoubleOrNull() ?: return
        val store=FinanceStore(context)
        val accounts=store.loadAccounts()
        val account=accounts.firstOrNull{!it.hidden} ?: return
        val income=lower.contains("зачис")||lower.contains("credit")||lower.contains("пополн")
        val tx=store.loadTransactions().toMutableList()
        tx.add(Transaction(id=System.currentTimeMillis(),title="Банковское SMS",amount=amount,income=income,accountName=account.name,category=if(income)"Доход" else "Импорт SMS",timestamp=System.currentTimeMillis(),currency=account.currency,note=text.take(160)))
        store.saveTransactions(tx)
        val updated=accounts.map{if(it.name==account.name)it.copy(balance=it.balance+if(income)amount else -amount)else it}
        store.saveAccounts(updated)
    }
}