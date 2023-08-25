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
import android.content.Intent;
import android.os.UserHandle;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;


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

    private int mLastMode = -1;
    private boolean isBluetoothA2dpOn = false;



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

    private boolean requestAudioFocus() {
          // Bluetooth A2DP may carry Music, Audio Books, Navigation, or other sounds so mark content
          // type unknown.
          AudioAttributes streamAttributes =
                  new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                          .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                          .build();
          // Bluetooth ducking is handled at the native layer at the request of AudioManager.
          AudioFocusRequest focusRequest =
                  new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                          streamAttributes)
                          .setOnAudioFocusChangeListener(mAudioFocusListener, new Handler())
                          .build();
        int focusRequestStatus = audioManager.requestAudioFocus(focusRequest);

        Log.d(TAG, "requestAudioFocus focusRequestStatus " + focusRequestStatus);

        if (focusRequestStatus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }
        return false;
      }

      // Focus changes when we are currently holding focus.
      private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
          @Override
          public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "onAudioFocusChangeListener focuschange " + focusChange);
          }
      };

    private void setSpeakerMode() {

      int maxVolume = audioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume-3,
                0);

        mLastMode = audioManager.getMode();

        isBluetoothA2dpOn = audioManager.isBluetoothA2dpOn();

        audioManager.setMode(isBluetoothA2dpOn ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);

        if (isBluetoothA2dpOn) {
            audioManager.stopBluetoothSco();

            audioManager.setBluetoothScoOn(false);
        }

        audioManager.setSpeakerphoneOn(true);

        Log.e(TAG, "setSpeakerMode maxVolume = "+maxVolume+" ;isBluetoothA2dpOn = "+isBluetoothA2dpOn+" ; mLastMode = "+mLastMode);
    }

    private void stopMusic(){
        if (playPhoneMusic != null) {
            Log.d(TAG, "=======stopMusic()========== ");
            playPhoneMusic.stop();
            playPhoneMusic.release();
            playPhoneMusic = null;
        }
        audioManager.abandonAudioFocus(mAudioFocusListener,null);
        if (isBluetoothA2dpOn) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            Log.d(TAG, "=======stopMusic()========== reset BluetoothA2dp");
        }else{
            audioManager.setMode(mLastMode);            
        }
        audioManager.setSpeakerphoneOn(false);
    }


    private void playMusic() {
        Log.d(TAG, "=======playMusic()========== ");

        if (playPhoneMusic != null) {
            return;
        }
        requestAudioFocus();

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
