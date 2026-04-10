-if class io.cadence.music.data.sensor.WeatherResponse
-keepnames class io.cadence.music.data.sensor.WeatherResponse
-if class io.cadence.music.data.sensor.WeatherResponse
-keep class io.cadence.music.data.sensor.WeatherResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
