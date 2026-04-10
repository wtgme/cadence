package io.cadence.music.di;

import com.squareup.moshi.Moshi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class NetworkModule_ProvideWeatherRetrofitFactory implements Factory<Retrofit> {
  private final Provider<Moshi> moshiProvider;

  public NetworkModule_ProvideWeatherRetrofitFactory(Provider<Moshi> moshiProvider) {
    this.moshiProvider = moshiProvider;
  }

  @Override
  public Retrofit get() {
    return provideWeatherRetrofit(moshiProvider.get());
  }

  public static NetworkModule_ProvideWeatherRetrofitFactory create(Provider<Moshi> moshiProvider) {
    return new NetworkModule_ProvideWeatherRetrofitFactory(moshiProvider);
  }

  public static Retrofit provideWeatherRetrofit(Moshi moshi) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWeatherRetrofit(moshi));
  }
}
