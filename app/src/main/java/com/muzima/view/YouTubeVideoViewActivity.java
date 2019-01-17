/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.youtube.player.*;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.service.TimeoutPreferenceService;

import java.util.concurrent.TimeUnit;

public class YouTubeVideoViewActivity extends BaseHelpActivity implements YouTubePlayer.OnInitializedListener {

    private static String YOUTUBE_API_KEY = "AIzaSyB95WSRhfe-Pa6ZxU8ZB3C__E51ZQbZdu8";
    private static final int RECOVERY_REQUEST = 1;
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_TITLE = "VIDEO_TITLE";
    private String videoId;
    private YouTubePlayer youTubePalyer;
    private int muzimaTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_video_display);
        setMuzimaTimout();
        setVideoContent();
    }

    private void setVideoContent() {
        setTitle(getIntent().getStringExtra(VIDEO_TITLE));
        YouTubePlayerFragment youTubePlayerFragment = (YouTubePlayerFragment) getYouTubePlayerProvider();
        videoId = getVideoId(getIntent().getStringExtra(VIDEO_PATH));
        youTubePlayerFragment.initialize(YOUTUBE_API_KEY, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //set session timeout back to original
        if (!isUserLoggedOut()) {
            ((MuzimaApplication) getApplication()).resetTimer(muzimaTimeout);
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        if (!wasRestored) {
            youTubePalyer = player;
            youTubePalyer.setPlayerStateChangeListener(new MyPlayerStateChangeListener());
            player.loadVideo(videoId);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
        startVideoWebViewActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_REQUEST) {
            // Retry initialization if user performed a recovery action
            getYouTubePlayerProvider().initialize(YOUTUBE_API_KEY, this);
        }
    }

    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);
    }

    private String getVideoId(String videoUrl) {
        String[] splitUrl = videoUrl.split("/");
        String videoId = splitUrl[splitUrl.length - 1];
        return videoId;
    }

    private void setMuzimaTimout() {
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        muzimaTimeout = new TimeoutPreferenceService(muzimaApplication).getTimeout();
    }

    //WebView
    private void startVideoWebViewActivity() {
        Intent videoIntent = new Intent(this, VideoWebViewActivity.class);
        videoIntent.putExtra(VideoWebViewActivity.VIDEO_PATH, getIntent().getStringExtra(VIDEO_PATH));
        videoIntent.putExtra(VideoWebViewActivity.VIDEO_TITLE, getIntent().getStringExtra(VIDEO_TITLE));
        startActivity(videoIntent);
    }

    private final class MyPlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoaded(String s) {
        }

        @Override
        public void onVideoEnded() {
            //set sessiontimeout back
            onBackPressed();
        }

        @Override
        public void onVideoStarted() {
            if (!isUserLoggedOut()) {
                setTimer();
            }
        }

        @Override
        public void onLoading() {
        }

        @Override
        public void onAdStarted() {
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
        }

        private void setTimer() {
            //set sessiontimeout according to the duration of the video
            int duration = youTubePalyer.getDurationMillis();
            int timeout = muzimaTimeout * 60 * 1000;
            if (timeout < duration) {
                //add 2 at the end to be sure that the timeout is longer than the duration in minutes
                int new_timeout = duration / 60 / 1000 + 2;
                ((MuzimaApplication) getApplication()).resetTimer(new_timeout);
            }
        }
    }
}