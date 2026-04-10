-if class io.cadence.music.data.sensor.CurrentWeather
-keepnames class io.cadence.music.data.sensor.CurrentWeather
-if class io.cadence.music.data.sensor.CurrentWeather
-keep class io.cadence.music.data.sensor.CurrentWeatherJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
