package io.cadence.music.data.api;

import com.squareup.moshi.Moshi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.io.File;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class LyriaMusicRepository_Factory implements Factory<LyriaMusicRepository> {
  private final Provider<File> cacheDirProvider;

  private final Provider<PromptTranslator> promptTranslatorProvider;

  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Moshi> moshiProvider;

  public LyriaMusicRepository_Factory(Provider<File> cacheDirProvider,
      Provider<PromptTranslator> promptTranslatorProvider, Provider<OkHttpClient> clientProvider,
      Provider<Moshi> moshiProvider) {
    this.cacheDirProvider = cacheDirProvider;
    this.promptTranslatorProvider = promptTranslatorProvider;
    this.clientProvider = clientProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public LyriaMusicRepository get() {
    return newInstance(cacheDirProvider.get(), promptTranslatorProvider.get(), clientProvider.get(), moshiProvider.get());
  }

  public static LyriaMusicRepository_Factory create(Provider<File> cacheDirProvider,
      Provider<PromptTranslator> promptTranslatorProvider, Provider<OkHttpClient> clientProvider,
      Provider<Moshi> moshiProvider) {
    return new LyriaMusicRepository_Factory(cacheDirProvider, promptTranslatorProvider, clientProvider, moshiProvider);
  }

  public static LyriaMusicRepository newInstance(File cacheDir, PromptTranslator promptTranslator,
      OkHttpClient client, Moshi moshi) {
    return new LyriaMusicRepository(cacheDir, promptTranslator, client, moshi);
  }
}
