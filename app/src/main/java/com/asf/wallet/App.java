package com.asf.wallet;

import android.app.Activity;
import android.support.multidex.MultiDexApplication;
import com.asf.wallet.di.DaggerAppComponent;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import io.realm.Realm;
import javax.inject.Inject;

public class App extends MultiDexApplication implements HasActivityInjector {

  @Inject DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

  @Override public void onCreate() {
    super.onCreate();
    Realm.init(this);
    DaggerAppComponent.builder()
        .application(this)
        .build()
        .inject(this);

    // enable pin code for the application
    //		LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
    //		lockManager.enableAppLock(this, CustomPinActivity.class);
    //		lockManager.getAppLock().setShouldShowForgot(false);
  }

  @Override public AndroidInjector<Activity> activityInjector() {
    return dispatchingAndroidInjector;
  }
}
