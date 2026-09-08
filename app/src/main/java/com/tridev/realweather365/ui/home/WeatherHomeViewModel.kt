package com.tridev.realweather365.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherHomeUiState())
    val uiState: StateFlow<WeatherHomeUiState> = _uiState.asStateFlow()
}
