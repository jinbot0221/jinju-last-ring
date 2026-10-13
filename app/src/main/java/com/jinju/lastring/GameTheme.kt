package com.jinju.lastring

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun GameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme=lightColorScheme(
            primary=Color(0xFF245A4C),onPrimary=Color.White,
            primaryContainer=Color(0xFFDCEBE1),onPrimaryContainer=Color(0xFF173D31),
            secondary=Color(0xFF715531),secondaryContainer=Color(0xFFF0E5D4),
            background=Color(0xFFF8F5ED),onBackground=Color(0xFF202C26),
            surface=Color(0xFFFFFCF6),onSurface=Color(0xFF202C26),
            surfaceVariant=Color(0xFFECEFE7),onSurfaceVariant=Color(0xFF414D44),
            outline=Color(0xFF738075)
        ),
        typography=Typography(
            headlineLarge=TextStyle(fontSize=30.sp,lineHeight=39.sp,fontWeight=FontWeight.Bold),
            headlineMedium=TextStyle(fontSize=25.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold),
            headlineSmall=TextStyle(fontSize=22.sp,lineHeight=31.sp,fontWeight=FontWeight.Bold),
            titleLarge=TextStyle(fontSize=20.sp,lineHeight=29.sp,fontWeight=FontWeight.SemiBold),
            titleMedium=TextStyle(fontSize=18.sp,lineHeight=26.sp,fontWeight=FontWeight.SemiBold),
            bodyLarge=TextStyle(fontSize=17.sp,lineHeight=28.sp),
            bodyMedium=TextStyle(fontSize=16.sp,lineHeight=25.sp),
            bodySmall=TextStyle(fontSize=14.sp,lineHeight=22.sp),
            labelLarge=TextStyle(fontSize=16.sp,lineHeight=23.sp,fontWeight=FontWeight.SemiBold)
        ),content=content
    )
}
