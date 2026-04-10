package io.cadence.music.audio;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.cadence.music.data.api.GenerationRepository;
import io.cadence.music.domain.PromptBuilder;
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
public final class AudioBufferManager_Factory implements Factory<AudioBufferManager> {
  private final Provider<GenerationRepository> musicRepositoryProvider;

  private final Provider<PromptBuilder> promptBuilderProvider;

  public AudioBufferManager_Factory(Provider<GenerationRepository> musicRepositoryProvider,
      Provider<PromptBuilder> promptBuilderProvider) {
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.promptBuilderProvider = promptBuilderProvider;
  }

  @Override
  public AudioBufferManager get() {
    return newInstance(musicRepositoryProvider.get(), promptBuilderProvider.get());
  }

  public static AudioBufferManager_Factory create(
      Provider<GenerationRepository> musicRepositoryProvider,
      Provider<PromptBuilder> promptBuilderProvider) {
    return new AudioBufferManager_Factory(musicRepositoryProvider, promptBuilderProvider);
  }

  public static AudioBufferManager newInstance(GenerationRepository musicRepository,
      PromptBuilder promptBuilder) {
    return new AudioBufferManager(musicRepository, promptBuilder);
  }
}
