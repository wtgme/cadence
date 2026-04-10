package io.cadence.music.data.sensor;

import com.google.android.gms.location.FusedLocationProviderClient;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LocationService_MembersInjector implements MembersInjector<LocationService> {
  private final Provider<FusedLocationProviderClient> fusedLocationClientProvider;

  private final Provider<LocationRepository> locationRepositoryProvider;

  public LocationService_MembersInjector(
      Provider<FusedLocationProviderClient> fusedLocationClientProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    this.fusedLocationClientProvider = fusedLocationClientProvider;
    this.locationRepositoryProvider = locationRepositoryProvider;
  }

  public static MembersInjector<LocationService> create(
      Provider<FusedLocationProviderClient> fusedLocationClientProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    return new LocationService_MembersInjector(fusedLocationClientProvider, locationRepositoryProvider);
  }

  @Override
  public void injectMembers(LocationService instance) {
    injectFusedLocationClient(instance, fusedLocationClientProvider.get());
    injectLocationRepository(instance, locationRepositoryProvider.get());
  }

  @InjectedFieldSignature("io.cadence.music.data.sensor.LocationService.fusedLocationClient")
  public static void injectFusedLocationClient(LocationService instance,
      FusedLocationProviderClient fusedLocationClient) {
    instance.fusedLocationClient = fusedLocationClient;
  }

  @InjectedFieldSignature("io.cadence.music.data.sensor.LocationService.locationRepository")
  public static void injectLocationRepository(LocationService instance,
      LocationRepository locationRepository) {
    instance.locationRepository = locationRepository;
  }
}
