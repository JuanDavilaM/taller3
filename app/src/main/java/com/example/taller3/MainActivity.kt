package com.example.taller3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.taller3.navigation.AppNavGraph
import com.example.taller3.ui.theme.Taller3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Taller3Theme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
