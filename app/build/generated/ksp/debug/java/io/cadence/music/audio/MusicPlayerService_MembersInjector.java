package io.cadence.music.audio;

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
public final class MusicPlayerService_MembersInjector implements MembersInjector<MusicPlayerService> {
  private final Provider<AudioBufferManager> bufferManagerProvider;

  private final Provider<PlayerNotification> playerNotificationProvider;

  public MusicPlayerService_MembersInjector(Provider<AudioBufferManager> bufferManagerProvider,
      Provider<PlayerNotification> playerNotificationProvider) {
    this.bufferManagerProvider = bufferManagerProvider;
    this.playerNotificationProvider = playerNotificationProvider;
  }

  public static MembersInjector<MusicPlayerService> create(
      Provider<AudioBufferManager> bufferManagerProvider,
      Provider<PlayerNotification> playerNotificationProvider) {
    return new MusicPlayerService_MembersInjector(bufferManagerProvider, playerNotificationProvider);
  }

  @Override
  public void injectMembers(MusicPlayerService instance) {
    injectBufferManager(instance, bufferManagerProvider.get());
    injectPlayerNotification(instance, playerNotificationProvider.get());
  }

  @InjectedFieldSignature("io.cadence.music.audio.MusicPlayerService.bufferManager")
  public static void injectBufferManager(MusicPlayerService instance,
      AudioBufferManager bufferManager) {
    instance.bufferManager = bufferManager;
  }

  @InjectedFieldSignature("io.cadence.music.audio.MusicPlayerService.playerNotification")
  public static void injectPlayerNotification(MusicPlayerService instance,
      PlayerNotification playerNotification) {
    instance.playerNotification = playerNotification;
  }
}
