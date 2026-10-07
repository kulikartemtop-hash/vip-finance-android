package com.example.vipfinance

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import org.json.JSONArray
import java.util.Locale

class BalanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }
    companion object {
        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs=context.getSharedPreferences("vip_finance",Context.MODE_PRIVATE)
            val raw=prefs.getString("accounts","[]") ?: "[]"
            var total=0.0
            runCatching{
                val a=JSONArray(raw)
                for(i in 0 until a.length()){
                    val o=a.getJSONObject(i)
                    if(!o.optBoolean("hidden")) total+=o.optDouble("balance")
                }
            }
            val currency=prefs.getString("currency","RUB") ?: "RUB"
            val symbol=when(currency){"RUB"->"₽";"GBP"->"£";"EUR"->"€";"USD"->"$";"CNY","JPY"->"¥";else->currency}
            val views=RemoteViews(context.packageName,R.layout.widget_balance)
            views.setTextViewText(R.id.widget_title,"VIP Finance")
            views.setTextViewText(R.id.widget_balance,symbol+"%.2f".format(Locale.getDefault(),total))
            manager.updateAppWidget(id,views)
        }
    }
}