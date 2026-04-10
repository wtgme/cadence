package io.cadence.music.data.sensor;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class WeatherRepository_Factory implements Factory<WeatherRepository> {
  private final Provider<Retrofit> retrofitProvider;

  public WeatherRepository_Factory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public WeatherRepository get() {
    return newInstance(retrofitProvider.get());
  }

  public static WeatherRepository_Factory create(Provider<Retrofit> retrofitProvider) {
    return new WeatherRepository_Factory(retrofitProvider);
  }

  public static WeatherRepository newInstance(Retrofit retrofit) {
    return new WeatherRepository(retrofit);
  }
}
