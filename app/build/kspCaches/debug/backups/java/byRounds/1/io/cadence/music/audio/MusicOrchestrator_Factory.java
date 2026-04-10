package io.cadence.music.audio;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.cadence.music.data.sensor.SensorStateCollector;
import io.cadence.music.domain.SceneDetector;
import io.cadence.music.domain.SceneStateMachine;
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
public final class MusicOrchestrator_Factory implements Factory<MusicOrchestrator> {
  private final Provider<Context> contextProvider;

  private final Provider<SensorStateCollector> sensorStateCollectorProvider;

  private final Provider<SceneDetector> sceneDetectorProvider;

  private final Provider<SceneStateMachine> sceneStateMachineProvider;

  private final Provider<AudioBufferManager> bufferManagerProvider;

  public MusicOrchestrator_Factory(Provider<Context> contextProvider,
      Provider<SensorStateCollector> sensorStateCollectorProvider,
      Provider<SceneDetector> sceneDetectorProvider,
      Provider<SceneStateMachine> sceneStateMachineProvider,
      Provider<AudioBufferManager> bufferManagerProvider) {
    this.contextProvider = contextProvider;
    this.sensorStateCollectorProvider = sensorStateCollectorProvider;
    this.sceneDetectorProvider = sceneDetectorProvider;
    this.sceneStateMachineProvider = sceneStateMachineProvider;
    this.bufferManagerProvider = bufferManagerProvider;
  }

  @Override
  public MusicOrchestrator get() {
    return newInstance(contextProvider.get(), sensorStateCollectorProvider.get(), sceneDetectorProvider.get(), sceneStateMachineProvider.get(), bufferManagerProvider.get());
  }

  public static MusicOrchestrator_Factory create(Provider<Context> contextProvider,
      Provider<SensorStateCollector> sensorStateCollectorProvider,
      Provider<SceneDetector> sceneDetectorProvider,
      Provider<SceneStateMachine> sceneStateMachineProvider,
      Provider<AudioBufferManager> bufferManagerProvider) {
    return new MusicOrchestrator_Factory(contextProvider, sensorStateCollectorProvider, sceneDetectorProvider, sceneStateMachineProvider, bufferManagerProvider);
  }

  public static MusicOrchestrator newInstance(Context context,
      SensorStateCollector sensorStateCollector, SceneDetector sceneDetector,
      SceneStateMachine sceneStateMachine, AudioBufferManager bufferManager) {
    return new MusicOrchestrator(context, sensorStateCollector, sceneDetector, sceneStateMachine, bufferManager);
  }
}
