package io.cadence.music.data.sensor;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class SensorStateCollector_Factory implements Factory<SensorStateCollector> {
  private final Provider<LocationRepository> locationRepositoryProvider;

  private final Provider<HealthDataManager> healthDataManagerProvider;

  private final Provider<SleepRepository> sleepRepositoryProvider;

  private final Provider<HealthExtrasRepository> healthExtrasRepositoryProvider;

  private final Provider<WeatherRepository> weatherRepositoryProvider;

  public SensorStateCollector_Factory(Provider<LocationRepository> locationRepositoryProvider,
      Provider<HealthDataManager> healthDataManagerProvider,
      Provider<SleepRepository> sleepRepositoryProvider,
      Provider<HealthExtrasRepository> healthExtrasRepositoryProvider,
      Provider<WeatherRepository> weatherRepositoryProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
    this.healthDataManagerProvider = healthDataManagerProvider;
    this.sleepRepositoryProvider = sleepRepositoryProvider;
    this.healthExtrasRepositoryProvider = healthExtrasRepositoryProvider;
    this.weatherRepositoryProvider = weatherRepositoryProvider;
  }

  @Override
  public SensorStateCollector get() {
    return newInstance(locationRepositoryProvider.get(), healthDataManagerProvider.get(), sleepRepositoryProvider.get(), healthExtrasRepositoryProvider.get(), weatherRepositoryProvider.get());
  }

  public static SensorStateCollector_Factory create(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<HealthDataManager> healthDataManagerProvider,
      Provider<SleepRepository> sleepRepositoryProvider,
      Provider<HealthExtrasRepository> healthExtrasRepositoryProvider,
      Provider<WeatherRepository> weatherRepositoryProvider) {
    return new SensorStateCollector_Factory(locationRepositoryProvider, healthDataManagerProvider, sleepRepositoryProvider, healthExtrasRepositoryProvider, weatherRepositoryProvider);
  }

  public static SensorStateCollector newInstance(LocationRepository locationRepository,
      HealthDataManager healthDataManager, SleepRepository sleepRepository,
      HealthExtrasRepository healthExtrasRepository, WeatherRepository weatherRepository) {
    return new SensorStateCollector(locationRepository, healthDataManager, sleepRepository, healthExtrasRepository, weatherRepository);
  }
}
