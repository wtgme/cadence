-if class io.cadence.music.data.api.SongParams
-keepnames class io.cadence.music.data.api.SongParams
-if class io.cadence.music.data.api.SongParams
-keep class io.cadence.music.data.api.SongParamsJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class io.cadence.music.data.api.SongParams
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class io.cadence.music.data.api.SongParams
-keepclassmembers class io.cadence.music.data.api.SongParams {
    public synthetic <init>(java.lang.String,java.lang.String,float,int,float,float,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
