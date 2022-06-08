/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */
package com.adobe.marketing.optimizeapp

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adobe.marketing.optimizeapp.viewmodels.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    var appBarTitle by remember {
        mutableStateOf("Welcome to Optimize Demo")
    }
    val bottomNavigationItems = listOf(
        BottomNavigationScreen.OffersScreen,
        BottomNavigationScreen.SettingsScreen
    )

    Scaffold(bottomBar = {
        OffersBottomNavigation(navController = navController, items = bottomNavigationItems, onNavigationChange = {
            appBarTitle = when(it){
                BottomNavigationScreen.SettingsScreen -> "Settings"
                BottomNavigationScreen.OffersScreen -> "Welcome to Optimize Demo"
            }
        })
    },
        topBar = {
            TopAppBar {
                Text(
                    text = "$appBarTitle",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h6
                )
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            NavigationConfiguration(navController = navController, viewModel = viewModel)
        }
    }
}

@Composable
private fun OffersBottomNavigation(
    navController: NavHostController,
    items: List<BottomNavigationScreen>,
    onNavigationChange: (BottomNavigationScreen) -> Unit
) {
    BottomNavigation {
        items.forEach { screen ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.iconResId),
                        contentDescription = null
                    )
                },
                label = { Text(text = stringResource(id = screen.titleResID)) },
                selected = false,
                onClick = {
                    navController.navigate(screen.route)
                    onNavigationChange(screen)
                }
            )
        }
    }
}

@Composable
private fun NavigationConfiguration(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(
        navController = navController,
        startDestination = BottomNavigationScreen.OffersScreen.route
    ) {
        composable(BottomNavigationScreen.OffersScreen.route) {
            OffersView(viewModel = viewModel)
        }

        composable(BottomNavigationScreen.SettingsScreen.route) {
            SettingsView(viewModel = viewModel)
        }
    }
}

sealed class BottomNavigationScreen(
    val route: String,
    @StringRes val titleResID: Int,
    @DrawableRes val iconResId: Int
) {
    object OffersScreen : BottomNavigationScreen("offers", R.string.offers, R.drawable.offer)
    object SettingsScreen :
        BottomNavigationScreen("settings", R.string.settings, R.drawable.settings)

}