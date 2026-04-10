package io.cadence.music.audio;

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
public final class PlayerNotification_Factory implements Factory<PlayerNotification> {
  @Override
  public PlayerNotification get() {
    return newInstance();
  }

  public static PlayerNotification_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PlayerNotification newInstance() {
    return new PlayerNotification();
  }

  private static final class InstanceHolder {
    private static final PlayerNotification_Factory INSTANCE = new PlayerNotification_Factory();
  }
}
