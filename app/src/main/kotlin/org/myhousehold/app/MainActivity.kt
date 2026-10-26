package org.myhousehold.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.myhousehold.app.ui.AppMyHousehold
import org.myhousehold.app.ui.theme.ThemeMyHousehold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMyHousehold {
                AppMyHousehold()
            }
        }
    }
}
