package com.example.taller3.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.taller3.screens.InicioScreen
import com.example.taller3.screens.RegistroScreen
import com.example.taller3.screens.MainScreen
import com.example.taller3.screens.LoginScreen
import com.example.taller3.screens.PerfilScreen
import com.example.taller3.screens.MapaScreen // 👈 nuevo import

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "inicio"
    ) {
        composable("inicio") { InicioScreen(navController) }
        composable("registro") { RegistroScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable("perfil") { PerfilScreen(navController) }
        composable("mapa") { MapaScreen(navController) } // 👈 nueva ruta para el mapa
    }
}
