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
public final class SleepRepository_Factory implements Factory<SleepRepository> {
  private final Provider<Context> contextProvider;

  public SleepRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SleepRepository get() {
    return newInstance(contextProvider.get());
  }

  public static SleepRepository_Factory create(Provider<Context> contextProvider) {
    return new SleepRepository_Factory(contextProvider);
  }

  public static SleepRepository newInstance(Context context) {
    return new SleepRepository(context);
  }
}
