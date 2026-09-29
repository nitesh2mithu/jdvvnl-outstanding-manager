package com.example.outstandingmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.outstandingmanager.theme.OutstandingManagerTheme

import androidx.compose.runtime.remember
import com.example.outstandingmanager.data.repository.OutstandingRepository

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      OutstandingManagerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          val repository = remember { OutstandingRepository(applicationContext) }
          OutstandingApp(repository = repository)
        }
      }
    }
  }
}
