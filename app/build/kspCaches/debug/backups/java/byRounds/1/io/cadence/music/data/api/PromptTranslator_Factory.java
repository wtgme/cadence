package io.cadence.music.data.api;

import com.squareup.moshi.Moshi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class PromptTranslator_Factory implements Factory<PromptTranslator> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Moshi> moshiProvider;

  public PromptTranslator_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Moshi> moshiProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public PromptTranslator get() {
    return newInstance(okHttpClientProvider.get(), moshiProvider.get());
  }

  public static PromptTranslator_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Moshi> moshiProvider) {
    return new PromptTranslator_Factory(okHttpClientProvider, moshiProvider);
  }

  public static PromptTranslator newInstance(OkHttpClient okHttpClient, Moshi moshi) {
    return new PromptTranslator(okHttpClient, moshi);
  }
}
