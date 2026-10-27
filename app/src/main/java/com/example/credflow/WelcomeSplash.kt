package com.example.credflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeSplash(onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShowChart, null, tint = Blue, modifier = Modifier.size(42.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Text("Credflow", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Simple Loan Collection Manager", color = Color(0xFFCAD9EF), fontSize = 14.sp)
            Spacer(Modifier.height(42.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text("Get Started")
            }
        }
    }
}
