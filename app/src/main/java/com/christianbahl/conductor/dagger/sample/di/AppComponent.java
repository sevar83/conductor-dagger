package com.christianbahl.conductor.dagger.sample.di;

import com.christianbahl.conductor.ConductorInjectionModule;
import com.christianbahl.conductor.dagger.sample.CustomApplication;
import com.christianbahl.conductor.dagger.sample.controller.MainModule;
import com.christianbahl.conductor.dagger.sample.simple.ControllerBindModule_SimpleController;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by cbahl on 12.03.17.
 */
@Singleton
@Component(modules = {
        ConductorInjectionModule.class,
        MainModule.class,
        ControllerBindModule_SimpleController.class
})
public interface AppComponent {
  void inject(CustomApplication application);
}
