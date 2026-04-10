package io.cadence.music.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

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
public final class AppModule_ProvideCoroutineDispatcherFactory implements Factory<CoroutineDispatcher> {
  @Override
  public CoroutineDispatcher get() {
    return provideCoroutineDispatcher();
  }

  public static AppModule_ProvideCoroutineDispatcherFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CoroutineDispatcher provideCoroutineDispatcher() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCoroutineDispatcher());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideCoroutineDispatcherFactory INSTANCE = new AppModule_ProvideCoroutineDispatcherFactory();
  }
}
