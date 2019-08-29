//
//  Flutter plugin for audio playback on Android
//  Created by Karol Wąsowski (karol@tailosive.net) on June 23rd 2019
//  Licensed under GPLv3
//

package net.tailosive.flutter_audio_as_service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;

import androidx.annotation.Nullable;

import java.io.File;

public class AudioService extends Service {
    Context context = this;
    public static AudioService runningService;
    // private Handler handler;

    private static Cache cache;
    public SimpleExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private String PLAYBACK_CHANNEL_ID = "playback channel";
    private int PLAYBACK_NOTIFICATION_ID = 1;
    MediaSessionCompat mediaSession;
    private String MEDIA_SESSION_TAG = "AudioPlaybackSession";
    MediaSessionConnector mediaSessionConnector;

    private String nowPlayingUrl;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        runningService = this;

        final String title = intent.getStringExtra("title");
        final String channel = intent.getStringExtra("channel");
        final String url = intent.getStringExtra("url");
        nowPlayingUrl = url;

        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                context,
                Util.getUserAgent(context, "Tailosive"));

        CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(getCache(this), dataSourceFactory);
        MediaSource audioSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(Uri.parse(url));
/*
        SimpleExoPlayer.EventListener audioEventListener = new SimpleExoPlayer.EventListener() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                mainActivity.onPlayerStateChanged(playWhenReady, playbackState);
            }
        };

        player.addListener(audioEventListener); */
        player.prepare(audioSource);
        player.setPlayWhenReady(true);

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                context,
                PLAYBACK_CHANNEL_ID,
                R.string.playback_channel_name,
                R.string.channel_description,
                PLAYBACK_NOTIFICATION_ID,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return title;
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        /*
                        Intent intent = new Intent(context, MainActivity.class);
                        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        */
                        return null;
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        return channel;
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        if (channel.equals("Tailosive Tech")) {
                            return BitmapFactory.decodeResource(getResources(), R.drawable.ptech);
                        } else {
                            return BitmapFactory.decodeResource(getResources(), R.drawable.ptalks);
                        }
                    }
                },
                new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        stopSelf();
                    }

                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        startForeground(notificationId, notification);
                    }
                }
        );

        playerNotificationManager.setSmallIcon(R.drawable.app_icon);
        playerNotificationManager.setUseStopAction(true);
        playerNotificationManager.setRewindIncrementMs(30000);  // 30s
        playerNotificationManager.setFastForwardIncrementMs(30000);

        playerNotificationManager.setPlayer(player);

        mediaSession = new MediaSessionCompat(
                context,
                MEDIA_SESSION_TAG
        );
        mediaSession.setActive(true);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        mediaSessionConnector = new MediaSessionConnector(mediaSession);

        mediaSessionConnector.setPlayer(player);
/*
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!(player == null)) {
                    if (player.getPlayWhenReady()) {
                        mainActivity.onAudioPositionChanged(player.getCurrentPosition());
                    }
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
*/
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        runningService = null;
        // handler = null;

        mediaSession.release();
        mediaSessionConnector.setPlayer(null);
        playerNotificationManager.setPlayer(null);
        player.release();
        player = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void pauseAudio() {
        player.setPlayWhenReady(false);
    }

    public void resumeAudio() {
        player.setPlayWhenReady(true);
    }

    public void serviceStop() {
        stopSelf();
    }

    public String getUrlPlaying() {
        return nowPlayingUrl;
    }

    public long getPlayerAudioLength() {
        return player.getDuration();
    }

    public void seekBy(int seekByInMs) {
        player.seekTo(player.getCurrentPosition() + seekByInMs);
    }

    @Deprecated
    public static synchronized Cache getCache(Context context) {
        if (cache == null) {
            File cacheDirectory = new File(context.getCacheDir(), "audio");
            cache = new SimpleCache(cacheDirectory, new LeastRecentlyUsedCacheEvictor(300 * 1024 * 1024));
        }
        return cache;
    }
}