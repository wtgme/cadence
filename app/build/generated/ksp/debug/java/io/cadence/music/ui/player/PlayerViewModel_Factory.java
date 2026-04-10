package io.cadence.music.ui.player;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.cadence.music.audio.MusicOrchestrator;
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
public final class PlayerViewModel_Factory implements Factory<PlayerViewModel> {
  private final Provider<MusicOrchestrator> orchestratorProvider;

  public PlayerViewModel_Factory(Provider<MusicOrchestrator> orchestratorProvider) {
    this.orchestratorProvider = orchestratorProvider;
  }

  @Override
  public PlayerViewModel get() {
    return newInstance(orchestratorProvider.get());
  }

  public static PlayerViewModel_Factory create(Provider<MusicOrchestrator> orchestratorProvider) {
    return new PlayerViewModel_Factory(orchestratorProvider);
  }

  public static PlayerViewModel newInstance(MusicOrchestrator orchestrator) {
    return new PlayerViewModel(orchestrator);
  }
}
