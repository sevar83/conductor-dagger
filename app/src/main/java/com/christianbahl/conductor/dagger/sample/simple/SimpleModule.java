package com.christianbahl.conductor.dagger.sample.simple;

import com.christianbahl.conductor.dagger.sample.di.ScreenScope;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module public class SimpleModule {

  @Provides @ScreenScope @Named("controllerName") String getControllerName() {
    return "SimpleModule";
  }
}
