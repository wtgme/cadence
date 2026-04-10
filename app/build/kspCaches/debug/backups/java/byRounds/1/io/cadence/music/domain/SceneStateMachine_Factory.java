package io.cadence.music.domain;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
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
public final class SceneStateMachine_Factory implements Factory<SceneStateMachine> {
  private final Provider<SceneDetector> detectorProvider;

  private final Provider<CoroutineDispatcher> dispatcherProvider;

  public SceneStateMachine_Factory(Provider<SceneDetector> detectorProvider,
      Provider<CoroutineDispatcher> dispatcherProvider) {
    this.detectorProvider = detectorProvider;
    this.dispatcherProvider = dispatcherProvider;
  }

  @Override
  public SceneStateMachine get() {
    return newInstance(detectorProvider.get(), dispatcherProvider.get());
  }

  public static SceneStateMachine_Factory create(Provider<SceneDetector> detectorProvider,
      Provider<CoroutineDispatcher> dispatcherProvider) {
    return new SceneStateMachine_Factory(detectorProvider, dispatcherProvider);
  }

  public static SceneStateMachine newInstance(SceneDetector detector,
      CoroutineDispatcher dispatcher) {
    return new SceneStateMachine(detector, dispatcher);
  }
}
