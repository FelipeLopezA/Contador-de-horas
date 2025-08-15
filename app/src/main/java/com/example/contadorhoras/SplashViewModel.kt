package com.example.contadorhoras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SplashViewModel : ViewModel() {
    var isLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            // TODO: reemplazar con carga real
            delay(1500)
            isLoading = false
        }
    }
}
