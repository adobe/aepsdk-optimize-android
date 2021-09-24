package com.adobe.marketing.mobile.optimize

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.BottomNavigation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.adobe.marketing.mobile.optimize.ui.theme.OptimizeTheme
import com.adobe.marketing.mobile.optimize.viewmodels.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationItemView

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OptimizeTheme {
                MainScreen(viewModel)
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OptimizeTheme {
//        MainScreen()
    }
}