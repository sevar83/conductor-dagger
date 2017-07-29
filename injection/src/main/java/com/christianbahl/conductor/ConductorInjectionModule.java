package com.christianbahl.conductor;

import com.bluelinelabs.conductor.Controller;

import java.util.Map;

import dagger.Module;
import dagger.android.AndroidInjector;
import dagger.multibindings.Multibinds;

/**
 * Created by cbahl on 12.03.17.
 */
@Module public abstract class ConductorInjectionModule {

  private ConductorInjectionModule() {
  }

  @Multibinds abstract Map<Class<? extends Controller>, AndroidInjector.Factory<? extends Controller>> controllerInjectorFactories();
}
