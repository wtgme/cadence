package io.cadence.music;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.squareup.moshi.Moshi;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import io.cadence.music.audio.AudioBufferManager;
import io.cadence.music.audio.MusicOrchestrator;
import io.cadence.music.audio.MusicPlayerService;
import io.cadence.music.audio.MusicPlayerService_MembersInjector;
import io.cadence.music.audio.PlayerNotification;
import io.cadence.music.data.api.LyriaMusicRepository;
import io.cadence.music.data.api.PromptTranslator;
import io.cadence.music.data.sensor.HealthDataManager;
import io.cadence.music.data.sensor.HealthExtrasRepository;
import io.cadence.music.data.sensor.LocationRepository;
import io.cadence.music.data.sensor.LocationService;
import io.cadence.music.data.sensor.LocationService_MembersInjector;
import io.cadence.music.data.sensor.SensorStateCollector;
import io.cadence.music.data.sensor.SleepRepository;
import io.cadence.music.data.sensor.WeatherRepository;
import io.cadence.music.di.AppModule_ProvideAudioCacheDirFactory;
import io.cadence.music.di.AppModule_ProvideCoroutineDispatcherFactory;
import io.cadence.music.di.AppModule_ProvideFusedLocationClientFactory;
import io.cadence.music.di.NetworkModule_ProvideMoshiFactory;
import io.cadence.music.di.NetworkModule_ProvideOkHttpClientFactory;
import io.cadence.music.di.NetworkModule_ProvideWeatherRetrofitFactory;
import io.cadence.music.domain.PromptBuilder;
import io.cadence.music.domain.SceneDetector;
import io.cadence.music.domain.SceneStateMachine;
import io.cadence.music.ui.debug.DebugViewModel;
import io.cadence.music.ui.debug.DebugViewModel_HiltModules;
import io.cadence.music.ui.player.PlayerViewModel;
import io.cadence.music.ui.player.PlayerViewModel_HiltModules;
import java.io.File;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerCadenceApplication_HiltComponents_SingletonC {
  private DaggerCadenceApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public CadenceApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements CadenceApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public CadenceApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements CadenceApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public CadenceApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements CadenceApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public CadenceApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements CadenceApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CadenceApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements CadenceApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CadenceApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements CadenceApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public CadenceApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements CadenceApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public CadenceApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends CadenceApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends CadenceApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends CadenceApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends CadenceApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>of(LazyClassKeyProvider.io_cadence_music_ui_debug_DebugViewModel, DebugViewModel_HiltModules.KeyModule.provide(), LazyClassKeyProvider.io_cadence_music_ui_player_PlayerViewModel, PlayerViewModel_HiltModules.KeyModule.provide()));
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String io_cadence_music_ui_debug_DebugViewModel = "io.cadence.music.ui.debug.DebugViewModel";

      static String io_cadence_music_ui_player_PlayerViewModel = "io.cadence.music.ui.player.PlayerViewModel";

      @KeepFieldType
      DebugViewModel io_cadence_music_ui_debug_DebugViewModel2;

      @KeepFieldType
      PlayerViewModel io_cadence_music_ui_player_PlayerViewModel2;
    }
  }

  private static final class ViewModelCImpl extends CadenceApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<DebugViewModel> debugViewModelProvider;

    private Provider<PlayerViewModel> playerViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.debugViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.playerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>of(LazyClassKeyProvider.io_cadence_music_ui_debug_DebugViewModel, ((Provider) debugViewModelProvider), LazyClassKeyProvider.io_cadence_music_ui_player_PlayerViewModel, ((Provider) playerViewModelProvider)));
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String io_cadence_music_ui_player_PlayerViewModel = "io.cadence.music.ui.player.PlayerViewModel";

      static String io_cadence_music_ui_debug_DebugViewModel = "io.cadence.music.ui.debug.DebugViewModel";

      @KeepFieldType
      PlayerViewModel io_cadence_music_ui_player_PlayerViewModel2;

      @KeepFieldType
      DebugViewModel io_cadence_music_ui_debug_DebugViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // io.cadence.music.ui.debug.DebugViewModel 
          return (T) new DebugViewModel(singletonCImpl.sensorStateCollectorProvider.get(), singletonCImpl.musicOrchestratorProvider.get());

          case 1: // io.cadence.music.ui.player.PlayerViewModel 
          return (T) new PlayerViewModel(singletonCImpl.musicOrchestratorProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends CadenceApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends CadenceApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectMusicPlayerService(MusicPlayerService arg0) {
      injectMusicPlayerService2(arg0);
    }

    @Override
    public void injectLocationService(LocationService arg0) {
      injectLocationService2(arg0);
    }

    @CanIgnoreReturnValue
    private MusicPlayerService injectMusicPlayerService2(MusicPlayerService instance) {
      MusicPlayerService_MembersInjector.injectBufferManager(instance, singletonCImpl.audioBufferManagerProvider.get());
      MusicPlayerService_MembersInjector.injectPlayerNotification(instance, singletonCImpl.playerNotificationProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private LocationService injectLocationService2(LocationService instance) {
      LocationService_MembersInjector.injectFusedLocationClient(instance, singletonCImpl.provideFusedLocationClientProvider.get());
      LocationService_MembersInjector.injectLocationRepository(instance, singletonCImpl.locationRepositoryProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends CadenceApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<LocationRepository> locationRepositoryProvider;

    private Provider<HealthDataManager> healthDataManagerProvider;

    private Provider<SleepRepository> sleepRepositoryProvider;

    private Provider<HealthExtrasRepository> healthExtrasRepositoryProvider;

    private Provider<Moshi> provideMoshiProvider;

    private Provider<Retrofit> provideWeatherRetrofitProvider;

    private Provider<WeatherRepository> weatherRepositoryProvider;

    private Provider<SensorStateCollector> sensorStateCollectorProvider;

    private Provider<SceneDetector> sceneDetectorProvider;

    private Provider<CoroutineDispatcher> provideCoroutineDispatcherProvider;

    private Provider<SceneStateMachine> sceneStateMachineProvider;

    private Provider<File> provideAudioCacheDirProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<PromptTranslator> promptTranslatorProvider;

    private Provider<LyriaMusicRepository> lyriaMusicRepositoryProvider;

    private Provider<PromptBuilder> promptBuilderProvider;

    private Provider<AudioBufferManager> audioBufferManagerProvider;

    private Provider<MusicOrchestrator> musicOrchestratorProvider;

    private Provider<PlayerNotification> playerNotificationProvider;

    private Provider<FusedLocationProviderClient> provideFusedLocationClientProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.locationRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LocationRepository>(singletonCImpl, 1));
      this.healthDataManagerProvider = DoubleCheck.provider(new SwitchingProvider<HealthDataManager>(singletonCImpl, 2));
      this.sleepRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SleepRepository>(singletonCImpl, 3));
      this.healthExtrasRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<HealthExtrasRepository>(singletonCImpl, 4));
      this.provideMoshiProvider = DoubleCheck.provider(new SwitchingProvider<Moshi>(singletonCImpl, 7));
      this.provideWeatherRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 6));
      this.weatherRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<WeatherRepository>(singletonCImpl, 5));
      this.sensorStateCollectorProvider = DoubleCheck.provider(new SwitchingProvider<SensorStateCollector>(singletonCImpl, 0));
      this.sceneDetectorProvider = DoubleCheck.provider(new SwitchingProvider<SceneDetector>(singletonCImpl, 9));
      this.provideCoroutineDispatcherProvider = DoubleCheck.provider(new SwitchingProvider<CoroutineDispatcher>(singletonCImpl, 11));
      this.sceneStateMachineProvider = DoubleCheck.provider(new SwitchingProvider<SceneStateMachine>(singletonCImpl, 10));
      this.provideAudioCacheDirProvider = DoubleCheck.provider(new SwitchingProvider<File>(singletonCImpl, 14));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 16));
      this.promptTranslatorProvider = DoubleCheck.provider(new SwitchingProvider<PromptTranslator>(singletonCImpl, 15));
      this.lyriaMusicRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LyriaMusicRepository>(singletonCImpl, 13));
      this.promptBuilderProvider = DoubleCheck.provider(new SwitchingProvider<PromptBuilder>(singletonCImpl, 17));
      this.audioBufferManagerProvider = DoubleCheck.provider(new SwitchingProvider<AudioBufferManager>(singletonCImpl, 12));
      this.musicOrchestratorProvider = DoubleCheck.provider(new SwitchingProvider<MusicOrchestrator>(singletonCImpl, 8));
      this.playerNotificationProvider = DoubleCheck.provider(new SwitchingProvider<PlayerNotification>(singletonCImpl, 18));
      this.provideFusedLocationClientProvider = DoubleCheck.provider(new SwitchingProvider<FusedLocationProviderClient>(singletonCImpl, 19));
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @Override
    public void injectCadenceApplication(CadenceApplication cadenceApplication) {
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // io.cadence.music.data.sensor.SensorStateCollector 
          return (T) new SensorStateCollector(singletonCImpl.locationRepositoryProvider.get(), singletonCImpl.healthDataManagerProvider.get(), singletonCImpl.sleepRepositoryProvider.get(), singletonCImpl.healthExtrasRepositoryProvider.get(), singletonCImpl.weatherRepositoryProvider.get());

          case 1: // io.cadence.music.data.sensor.LocationRepository 
          return (T) new LocationRepository();

          case 2: // io.cadence.music.data.sensor.HealthDataManager 
          return (T) new HealthDataManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // io.cadence.music.data.sensor.SleepRepository 
          return (T) new SleepRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // io.cadence.music.data.sensor.HealthExtrasRepository 
          return (T) new HealthExtrasRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // io.cadence.music.data.sensor.WeatherRepository 
          return (T) new WeatherRepository(singletonCImpl.provideWeatherRetrofitProvider.get());

          case 6: // @javax.inject.Named("WeatherRetrofit") retrofit2.Retrofit 
          return (T) NetworkModule_ProvideWeatherRetrofitFactory.provideWeatherRetrofit(singletonCImpl.provideMoshiProvider.get());

          case 7: // com.squareup.moshi.Moshi 
          return (T) NetworkModule_ProvideMoshiFactory.provideMoshi();

          case 8: // io.cadence.music.audio.MusicOrchestrator 
          return (T) new MusicOrchestrator(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.sensorStateCollectorProvider.get(), singletonCImpl.sceneDetectorProvider.get(), singletonCImpl.sceneStateMachineProvider.get(), singletonCImpl.audioBufferManagerProvider.get());

          case 9: // io.cadence.music.domain.SceneDetector 
          return (T) new SceneDetector();

          case 10: // io.cadence.music.domain.SceneStateMachine 
          return (T) new SceneStateMachine(singletonCImpl.sceneDetectorProvider.get(), singletonCImpl.provideCoroutineDispatcherProvider.get());

          case 11: // kotlinx.coroutines.CoroutineDispatcher 
          return (T) AppModule_ProvideCoroutineDispatcherFactory.provideCoroutineDispatcher();

          case 12: // io.cadence.music.audio.AudioBufferManager 
          return (T) new AudioBufferManager(singletonCImpl.lyriaMusicRepositoryProvider.get(), singletonCImpl.promptBuilderProvider.get());

          case 13: // io.cadence.music.data.api.LyriaMusicRepository 
          return (T) new LyriaMusicRepository(singletonCImpl.provideAudioCacheDirProvider.get(), singletonCImpl.promptTranslatorProvider.get(), singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideMoshiProvider.get());

          case 14: // java.io.File 
          return (T) AppModule_ProvideAudioCacheDirFactory.provideAudioCacheDir(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 15: // io.cadence.music.data.api.PromptTranslator 
          return (T) new PromptTranslator(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideMoshiProvider.get());

          case 16: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 17: // io.cadence.music.domain.PromptBuilder 
          return (T) new PromptBuilder();

          case 18: // io.cadence.music.audio.PlayerNotification 
          return (T) new PlayerNotification();

          case 19: // com.google.android.gms.location.FusedLocationProviderClient 
          return (T) AppModule_ProvideFusedLocationClientFactory.provideFusedLocationClient(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
