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
public final class PromptBuilder_Factory implements Factory<PromptBuilder> {
  @Override
  public PromptBuilder get() {
    return newInstance();
  }

  public static PromptBuilder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PromptBuilder newInstance() {
    return new PromptBuilder();
  }

  private static final class InstanceHolder {
    private static final PromptBuilder_Factory INSTANCE = new PromptBuilder_Factory();
  }
}
