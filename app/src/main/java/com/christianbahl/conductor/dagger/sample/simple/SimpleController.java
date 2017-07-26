package com.christianbahl.conductor.dagger.sample.simple;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bluelinelabs.conductor.Controller;
import com.christianbahl.conductor.ConductorInjection;
import com.christianbahl.conductor.dagger.sample.R;

import javax.inject.Inject;
import javax.inject.Named;


public class SimpleController extends Controller {
  @Inject @Named("controllerName") String controllerName;

  @NonNull @Override protected View onCreateView(@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup) {
    ConductorInjection.inject(this);
    View view = layoutInflater.inflate(R.layout.controller_simple, viewGroup, false);
    ((TextView) view.findViewById(R.id.textView)).setText("Injected value = " + controllerName);
    return view;
  }
}
