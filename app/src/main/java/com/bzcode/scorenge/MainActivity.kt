package com.bzcode.scorenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bzcode.scorenge.ui.ScorengeApp
import com.bzcode.scorenge.ui.theme.ScorengeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScorengeTheme {
                ScorengeApp()
            }
        }
    }
}
