//
//  Flutter plugin for audio playback on Android
//  Created by Karol Wąsowski (karol@tailosive.net) on June 23rd 2019
//  Licensed under the BSD License
//

package net.tailosive.flutter_audio_as_service;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static net.tailosive.flutter_audio_as_service.AudioService.runningService;

/** FlutterAudioAsServicePlugin */
public class FlutterAudioAsServicePlugin implements MethodCallHandler{
  public static PluginRegistry.Registrar pluginRegistrar;
  public static Context context;

  /** Plugin registration. */
  public static void registerWith(PluginRegistry.Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "AudioService");
    channel.setMethodCallHandler(new FlutterAudioAsServicePlugin());

    pluginRegistrar = registrar;
    context = pluginRegistrar.activeContext();
  }

  Intent serviceIntent;

  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {

      case "startService":
        // this really needs some work - it's to long as of now

        String title = call.argument("title");
        String channel = call.argument("channel");
        String url = call.argument("url");
        String smallIcon = call.argument("appIcon");
        String bigIcon = call.argument("albumCover");

        if (!(runningService == null)) {
          if (!(runningService.getUrlPlaying() == url)) {
            runningService.serviceStop();

            serviceIntent = new Intent(context, AudioService.class);

            if (isValidDrawableResource(pluginRegistrar.context(), smallIcon, result, "INVALID_ICON")) {
              serviceIntent.putExtra("appIcon", smallIcon);
            } else {
              serviceIntent.putExtra("appIcon", (String) null);
            }

            if (isValidDrawableResource(pluginRegistrar.context(), bigIcon, result, "INVALID_ICON")) {
              serviceIntent.putExtra("bigIcon", bigIcon);
            } else {
              serviceIntent.putExtra("bigIcon", (String) null);
            }

            serviceIntent.putExtra("title", title);
            serviceIntent.putExtra("channel", channel);
            serviceIntent.putExtra("url", url);
            context.startService(serviceIntent);
          }
        } else {
          serviceIntent = new Intent(context, AudioService.class);

          if (isValidDrawableResource(pluginRegistrar.context(), smallIcon, result, "INVALID_ICON")) {
            serviceIntent.putExtra("appIcon", smallIcon);
          } else {
            serviceIntent.putExtra("appIcon", (String) null);
          }

          if (isValidDrawableResource(pluginRegistrar.context(), bigIcon, result, "INVALID_ICON")) {
            serviceIntent.putExtra("bigIcon", bigIcon);
          } else {
            serviceIntent.putExtra("bigIcon", (String) null);
          }

          serviceIntent.putExtra("title", title);
          serviceIntent.putExtra("channel", channel);
          serviceIntent.putExtra("url", url);
          context.startService(serviceIntent);
        }

        result.success(null);
        break;

      case "stop":
        runningService.serviceStop();

        result.success(null);
        break;

      case "pause":
        runningService.pauseAudio();

        result.success(null);
        break;

      case "resume":
        runningService.resumeAudio();

        result.success(null);
        break;

      case "seekBy":
        int seekByInMs = call.argument("seekByInMs");
        runningService.seekBy(seekByInMs);

        result.success(null);
        break;
/*
      case "getAudioLength":
        if (runningService.player != null) {
          result.success(runningService.getPlayerAudioLength());
        } else {
          result.success(0);
        }
        break;
*/
      case "seekTo":
        long seekTo = 0;
        int seekToInMs = call.argument("seekToInMs");
        runningService.player.seekTo(seekTo + seekToInMs);

      default:
        Log.e("Audio", "Wrong method call");
        result.notImplemented();
        break;
    }
  }

  private static boolean isValidDrawableResource(Context context, String name, Result result, String errorCode) {
      int resourceId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
      if (resourceId == 0) {
          result.error(errorCode, String.format("INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE", name), null);
          return false;
      }
      return true;
  }
}
