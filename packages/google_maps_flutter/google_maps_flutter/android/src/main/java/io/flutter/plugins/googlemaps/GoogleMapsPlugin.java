// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import static androidx.lifecycle.Lifecycle.State.CREATED;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.RESUMED;
import static androidx.lifecycle.Lifecycle.State.STARTED;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;

/**
 * Plugin for controlling a set of GoogleMap views to be shown as overlays on top of the Flutter
 * view. The overlay should be hidden during transformations or while Flutter is rendering on top of
 * the map. A Texture drawn using GoogleMap bitmap snapshots can then be shown instead of the
 * overlay.
 */
public class GoogleMapsPlugin implements FlutterPlugin, ActivityAware, LifecycleProvider {
  @Nullable private Lifecycle lifecycle;

  private static final String VIEW_TYPE = "plugins.flutter.io/google_maps";

  @SuppressWarnings("deprecation")
  public static void registerWith(io.flutter.plugin.common.PluginRegistry.Registrar registrar) {
    final Activity activity = registrar.activity();
    if (activity == null) {
      // When a background flutter view tries to register the plugin, the registrar has no activity.
      // We stop the registration process as this plugin is foreground only.
      return;
    }
    if (activity instanceof LifecycleOwner) {
      registrar
          .platformViewRegistry()
          .registerViewFactory(
              VIEW_TYPE,
              new GoogleMapFactory(
                  registrar.messenger(),
                  new LifecycleProvider() {
                    @Override
                    public Lifecycle getLifecycle() {
                      return ((LifecycleOwner) activity).getLifecycle();
                    }
                  }));
    } else {
      ProxyLifecycleProvider proxyLifecycleProvider = new ProxyLifecycleProvider(activity);
      activity.getApplication().registerActivityLifecycleCallbacks(proxyLifecycleProvider);
      registrar
          .platformViewRegistry()
          .registerViewFactory(
              VIEW_TYPE, new GoogleMapFactory(registrar.messenger(), proxyLifecycleProvider));
    }
  }

  public GoogleMapsPlugin() {}

  // FlutterPlugin

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    binding
        .getPlatformViewRegistry()
        .registerViewFactory(VIEW_TYPE, new GoogleMapFactory(binding.getBinaryMessenger(), this));
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding binding) {}

  // ActivityAware

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    lifecycle = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Nullable
  @Override
  public Lifecycle getLifecycle() {
    return lifecycle;
  }

  private static final class ProxyLifecycleProvider
      implements ActivityLifecycleCallbacks, LifecycleOwner, LifecycleProvider {

    private final LifecycleRegistry lifecycle = new LifecycleRegistry(this);
    private final int registrarActivityHashCode;

    private ProxyLifecycleProvider(Activity activity) {
      this.registrarActivityHashCode = activity.hashCode();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      if (activity.hashCode() != registrarActivityHashCode) {
        return;
      }
      lifecycle.setCurrentState(CREATED);
    }

    @Override
    public void onActivityStarted(Activity activity) {
      if (activity.hashCode() != registrarActivityHashCode) {
        return;
      }
      lifecycle.setCurrentState(STARTED);
    }

    @Override
    public void onActivityResumed(Activity activity) {
      if (activity.hashCode() != registrarActivityHashCode) {
        return;
      }
      lifecycle.setCurrentState(RESUMED);
    }

    @Override
    public void onActivityPaused(Activity activity) {
      if (activity.hashCode() != registrarActivityHashCode) {
        return;
      }
      lifecycle.setCurrentState(STARTED);
    }

    @Override
    public void onActivityStopped(Activity activity) {
      if (activity.hashCode() != registrarActivityHashCode) {
        return;
      }
      lifecycle.setCurrentState(CREATED);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
      if (activity.hashCode() != registrarActivityHashCode) {
        return;
      }
      activity.getApplication().unregisterActivityLifecycleCallbacks(this);
      lifecycle.setCurrentState(DESTROYED);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
      return lifecycle;
    }
  }
}
