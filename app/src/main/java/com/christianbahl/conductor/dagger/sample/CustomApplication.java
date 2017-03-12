package com.christianbahl.conductor.dagger.sample;

import android.app.Application;
import com.bluelinelabs.conductor.Controller;
import com.christianbahl.conductor.HasDispatchingControllerInjector;
import com.christianbahl.conductor.dagger.sample.di.DaggerAppComponent;
import dagger.android.DispatchingAndroidInjector;
import javax.inject.Inject;

/**
 * Created by cbahl on 12.03.17.
 */

public class CustomApplication extends Application implements HasDispatchingControllerInjector {

  @Inject protected DispatchingAndroidInjector<Controller> dispatchingControllerInjector;

  @Override public void onCreate() {
    super.onCreate();

    DaggerAppComponent.create().inject(this);
  }

  @Override public DispatchingAndroidInjector<Controller> controllerInjector() {
    return dispatchingControllerInjector;
  }
}