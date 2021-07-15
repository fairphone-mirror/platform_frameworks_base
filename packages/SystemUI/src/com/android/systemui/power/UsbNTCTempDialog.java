package com.android.systemui.power;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.View;
import android.os.SystemProperties;
import android.view.WindowManager;
import com.android.systemui.R;
import android.util.Log;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import java.io.IOException;
import android.os.PowerManager;
import static android.os.PowerManager.WAKE_REASON_UNKNOWN;
import android.os.SystemClock;


/**
 * Created by shaochuanzhi on 21/07/12.
 */

public class UsbNTCTempDialog extends AlertDialog {
    private static final String TAG = "UsbNTCTempDialog";
    String mTitleName;
    private Context mContext;
    private MediaPlayer playPhoneMusic;
    private AudioManager audioManager;
    private PowerManager mPowerManager;
    private boolean setPower = false;



    public UsbNTCTempDialog(Context context) {
        super(context, R.style.usbntc_dialog_toast);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usb_ntc_temp_dialog);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setCanceledOnTouchOutside(false);
        hideNavigationBar();
        SystemProperties.set("dev.tct.MMITestPower", "true");
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
    }



    private void hideNavigationBar() {
        final View mDecorView = getWindow().getDecorView();
        final int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        mDecorView.setSystemUiVisibility(flags);
        mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                            mDecorView.setSystemUiVisibility(flags);
                        }
                    }
                });

    }

    public void onDismissDialog(boolean dismissDialog,boolean speakerNoise){
        if (!setPower) {
            setPower = true;
            mPowerManager.wakeUp(SystemClock.uptimeMillis(), WAKE_REASON_UNKNOWN, "android.policy:USBNTC_TEMP");
        }
        if (dismissDialog) {
            SystemProperties.set("dev.tct.MMITestPower", "false");            
        }

        if (speakerNoise) {
            playMusic();
        }else{
            stopMusic();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_POWER == keyCode) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    private void setSpeakerMode() {
      int maxVolume = audioManager
              .getStreamMaxVolume(AudioManager.STREAM_MUSIC);

      Log.d(TAG, "STREAM_MUSIC maxVolume=" + maxVolume);

      audioManager.setMode(AudioManager.MODE_NORMAL);
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
    }

    private void stopMusic(){
        if (playPhoneMusic != null) {
            Log.d(TAG, "=======stopMusic()========== ");
            playPhoneMusic.stop();
            playPhoneMusic.release();
            playPhoneMusic = null;
        }
    }


    private void playMusic() {
        Log.d(TAG, "=======playMusic()========== ");

        if (playPhoneMusic != null) {
            return;
        }

        setSpeakerMode();


        playPhoneMusic = new MediaPlayer();
        
        setDataSourceFromResource(playPhoneMusic, R.raw.usb_cesium);

        playPhoneMusic.setVolume(1f, 1f);

        playPhoneMusic.setLooping(true);
        playPhoneMusic.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "playPhoneMusic onPrepared,playPhoneMusic = "+playPhoneMusic);
                playPhoneMusic.start();
            }
        });
        playPhoneMusic.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "playPhoneMusic had error " + what + ", " + extra);
                return true;
            }
        });
        try {
            playPhoneMusic .prepare();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "playPhoneMusic prepare fail, " + e.getMessage());
        }
    }

    private void setDataSourceFromResource(MediaPlayer player, int res) {
      AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(res);
      if (afd != null) {
          try {
              player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
              afd.close();
          } catch (IOException e) {
              // MMILog.e(TAG, e);
          }
      }
    }

}
