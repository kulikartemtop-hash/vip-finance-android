package com.example.vipfinance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun VIPFinanceTheme(theme:String="system",style:String="classic",content: @Composable () -> Unit){
    val dark=when(theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
    val base=when(style){"ocean"->if(dark)darkColorScheme(primary=Purple80,secondary=PurpleGrey80,tertiary=Pink80)else lightColorScheme(primary=Purple40,secondary=PurpleGrey40,tertiary=Pink40)
        "graphite"->if(dark)darkColorScheme(primary=PurpleGrey80,secondary=Purple80,tertiary=Pink80)else lightColorScheme(primary=PurpleGrey40,secondary=Purple40,tertiary=Pink40)
        else->if(dark)darkColorScheme(primary=Purple80,secondary=PurpleGrey80,tertiary=Pink80)else lightColorScheme(primary=Purple40,secondary=PurpleGrey40,tertiary=Pink40)}
    MaterialTheme(colorScheme=base,typography=Typography(),content=content)
}
