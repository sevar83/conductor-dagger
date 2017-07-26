package com.christianbahl.conductor.dagger.sample.simple;

import com.christianbahl.conductor.ContributesControllerInjector;
import com.christianbahl.conductor.dagger.sample.di.ScreenScope;

import dagger.Module;

@Module
public abstract class ControllerBindModule {

  @ScreenScope
  @ContributesControllerInjector(modules = {SimpleModule.class})
  abstract SimpleController simpleController();
}
