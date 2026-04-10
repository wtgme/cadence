package io.cadence.music.data.sensor;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class HealthDataManager_Factory implements Factory<HealthDataManager> {
  private final Provider<Context> contextProvider;

  public HealthDataManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HealthDataManager get() {
    return newInstance(contextProvider.get());
  }

  public static HealthDataManager_Factory create(Provider<Context> contextProvider) {
    return new HealthDataManager_Factory(contextProvider);
  }

  public static HealthDataManager newInstance(Context context) {
    return new HealthDataManager(context);
  }
}
