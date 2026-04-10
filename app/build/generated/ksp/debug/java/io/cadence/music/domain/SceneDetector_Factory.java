package io.cadence.music.domain;

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
public final class SceneDetector_Factory implements Factory<SceneDetector> {
  @Override
  public SceneDetector get() {
    return newInstance();
  }

  public static SceneDetector_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SceneDetector newInstance() {
    return new SceneDetector();
  }

  private static final class InstanceHolder {
    private static final SceneDetector_Factory INSTANCE = new SceneDetector_Factory();
  }
}
