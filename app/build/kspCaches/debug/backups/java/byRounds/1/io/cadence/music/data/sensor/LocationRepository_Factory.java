package io.cadence.music.data.sensor;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class LocationRepository_Factory implements Factory<LocationRepository> {
  @Override
  public LocationRepository get() {
    return newInstance();
  }

  public static LocationRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LocationRepository newInstance() {
    return new LocationRepository();
  }

  private static final class InstanceHolder {
    private static final LocationRepository_Factory INSTANCE = new LocationRepository_Factory();
  }
}
