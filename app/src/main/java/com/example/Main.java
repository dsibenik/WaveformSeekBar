package com.example;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.android.custom.widget.waveformseekbar.WaveformSeekBar;
import com.example.davor.testapp2.R;

import java.io.IOException;


public class Main extends Activity {

    private static final int UPDATE_FREQUENCY_MS = 5;
    private WaveformSeekBar seekbar = null;
    private MediaPlayer player = null;
    private ImageButton playButton = null;
    private boolean isStarted = false;
    private boolean isMoveingSeekBar = false;
    private final Handler handler = new Handler();
    private final Runnable updatePositionRunnable = new Runnable() {
        public void run() {
            updatePosition();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = (ImageButton) findViewById(R.id.play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (player.isPlaying()) {
                    handler.removeCallbacks(updatePositionRunnable);
                    player.pause();
                    playButton.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    if (isStarted) {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);

                        updatePosition();
                    } else {
                        startPlay();
                    }
                }
            }
        });

        player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlay();
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
                // returning false will call the OnCompletionListener
            }
        });


        seekbar = (WaveformSeekBar) findViewById(R.id.seekbar);
        try {
            seekbar.setAudio(this.getAssets().open("audio.wav"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isMoveingSeekBar = false;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isMoveingSeekBar = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isMoveingSeekBar) {
                    player.seekTo(progress);
                }
            }
        });

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        if (null != cursor) {
            cursor.moveToFirst();
        }
    }


    private void startPlay() {
        seekbar.setProgress(0);
        player.stop();
        player.reset();

        AssetFileDescriptor afd = null;
        try {
            afd = getAssets().openFd("audio.wav");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            player.prepare();
            player.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        seekbar.setMax(player.getDuration());
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        updatePosition();

        isStarted = true;
    }


    private void stopPlay() {
        player.stop();
        player.reset();
        playButton.setImageResource(android.R.drawable.ic_media_play);
        handler.removeCallbacks(updatePositionRunnable);
        seekbar.setProgress(0);

        isStarted = false;
    }


    private void updatePosition() {
        handler.removeCallbacks(updatePositionRunnable);

        seekbar.setProgress(player.getCurrentPosition());

        handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY_MS);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(updatePositionRunnable);
        player.stop();
        player.reset();
        player.release();
        player = null;
    }

}