package io.cadence.music.data.sensor

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current") val current: CurrentWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "weather_code") val weatherCode: Int
)

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "weather_code"
    ): WeatherResponse
}

@Singleton
class WeatherRepository @Inject constructor(
    @Named("WeatherRetrofit") private val retrofit: Retrofit
) {
    private val _weather = MutableStateFlow("Clear")
    val weather: StateFlow<String> = _weather

    private val api = retrofit.create(WeatherApi::class.java)

    private var lastRefreshTime = 0L
    private var lastLat = 0.0
    private var lastLon = 0.0

    suspend fun refresh(lat: Double, lon: Double, force: Boolean = false) {
        if (lat == 0.0 && lon == 0.0) return
        
        // Throttle: only refresh if moved > 1km or 15 minutes passed
        val now = System.currentTimeMillis()
        val movedEnough = Math.abs(lat - lastLat) > 0.01 || Math.abs(lon - lastLon) > 0.01
        val timePassed = now - lastRefreshTime > 15 * 60 * 1000 // 15 mins
        
        if (!force && !movedEnough && !timePassed && lastRefreshTime != 0L) return

        try {
            val response = api.getCurrentWeather(lat, lon)
            val code = response.current?.weatherCode ?: 0
            _weather.value = mapWeatherCode(code)
            lastRefreshTime = now
            lastLat = lat
            lastLon = lon
            Log.d(TAG, "Weather updated: ${_weather.value} (code: $code)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather", e)
        }
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            77 -> "Snowy"
            80, 81, 82 -> "Rainy"
            85, 86 -> "Snowy"
            95, 96, 99 -> "Stormy"
            else -> "Clear"
        }
    }

    companion object {
        private const val TAG = "WeatherRepository"
    }
}
