package io.cadence.music.di;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.io.File;
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
public final class AppModule_ProvideAudioCacheDirFactory implements Factory<File> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideAudioCacheDirFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public File get() {
    return provideAudioCacheDir(contextProvider.get());
  }

  public static AppModule_ProvideAudioCacheDirFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideAudioCacheDirFactory(contextProvider);
  }

  public static File provideAudioCacheDir(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAudioCacheDir(context));
  }
}
