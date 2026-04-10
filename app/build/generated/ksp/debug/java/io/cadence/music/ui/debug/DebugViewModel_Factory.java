package io.cadence.music.ui.debug;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.cadence.music.audio.MusicOrchestrator;
import io.cadence.music.data.sensor.SensorStateCollector;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DebugViewModel_Factory implements Factory<DebugViewModel> {
  private final Provider<SensorStateCollector> sensorStateCollectorProvider;

  private final Provider<MusicOrchestrator> orchestratorProvider;

  public DebugViewModel_Factory(Provider<SensorStateCollector> sensorStateCollectorProvider,
      Provider<MusicOrchestrator> orchestratorProvider) {
    this.sensorStateCollectorProvider = sensorStateCollectorProvider;
    this.orchestratorProvider = orchestratorProvider;
  }

  @Override
  public DebugViewModel get() {
    return newInstance(sensorStateCollectorProvider.get(), orchestratorProvider.get());
  }

  public static DebugViewModel_Factory create(
      Provider<SensorStateCollector> sensorStateCollectorProvider,
      Provider<MusicOrchestrator> orchestratorProvider) {
    return new DebugViewModel_Factory(sensorStateCollectorProvider, orchestratorProvider);
  }

  public static DebugViewModel newInstance(SensorStateCollector sensorStateCollector,
      MusicOrchestrator orchestrator) {
    return new DebugViewModel(sensorStateCollector, orchestrator);
  }
}
