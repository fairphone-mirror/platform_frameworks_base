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

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;
import android.media.ImageReader;
import java.util.Arrays;
import android.os.SystemProperties;
import android.widget.Button;
import android.util.Size;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;

/**
 * Created by shaochuanzhi on 22/01/18.
 */

public class CameraOISTempDialog extends AlertDialog {
    private static final String TAG = "CameraOISTempDialog";
    String mTitleName;
    private Context mContext;
    private MediaPlayer playPhoneMusic;
    private AudioManager audioManager;
    private PowerManager mPowerManager;
    private boolean setPower = false;

    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private String mCameraId = "0";
    private UIHandler mUiHandler = null;

    private static final int UI_UPDATE_OIS_STOP = 1026;
    private static final int UI_UPDATE_OIS_START = 1027;
    private static final int UI_UPDATE_OIS_DESTORY = 1028;
    private static final int UI_UPDATE_OIS_START_2 = 1020;
    private Scene mState = Scene.OIS_SET;

    // An additional thread for running tasks that shouldn't block the UI.
    private HandlerThread mBackgroundThread;
    // Handler for running tasks in the background.
    private Handler mBackgroundHandler = null;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private Button mButton;
    private TextView mTitleTips;
    private TextView mMessageTips;
    private boolean isFir = false;


    public CameraOISTempDialog(Context context) {
        super(context, R.style.Theme_SystemUI_Dialog);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_ois_cail);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setCanceledOnTouchOutside(false);
        hideNavigationBar();
        SystemProperties.set("dev.tct.MMITestPower", "true");
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mTitleTips = (TextView)findViewById(R.id.tips_title);
        mMessageTips = (TextView)findViewById(R.id.tips_message);

        mButton = (Button)findViewById(R.id.tips_ok);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFir) {
                    isFir = true;
                    mButton.setEnabled(false);
                    startCameraOis();
                }else{
                    dismiss();
                }
            }
        });

        setPower();

    }

    public void setPower(){
        if (!setPower) {
            setPower = true;
            mPowerManager.wakeUp(SystemClock.uptimeMillis(), WAKE_REASON_UNKNOWN, "android.policy:USBNTC_TEMP");
        }
    }

    private void dismissAll(){
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        SystemProperties.set("dev.tct.MMITestPower", "false");
    }

    private void startCameraOis(){
        initCamera();
        if (mCameraId != null) {
            start();
        }
        mUiHandler = new UIHandler();
        Message.obtain(mUiHandler, UI_UPDATE_OIS_START_2, true).sendToTarget();
    }

    private void initCamera() {

        Log.i(TAG, "initCamera");

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        // Should have mCameraId as this point.
        if (!findCamera(mCameraId)) {
            return;
        }
    }

    private boolean findCamera(String targetId) {
        mCameraId = null;

        String[] cameralist;
        try {
            cameralist = mCameraManager.getCameraIdList();
            for (String id : cameralist) {
                Log.d(TAG, "cameralist id = " + id);
            }
            for (String id : cameralist) {
                if (id.equals(targetId)) {
                    mCameraId = id;
                    Log.i(TAG, "mCameraId---->" + id);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "findCamera fail", e);
        }

        Log.e(TAG, "Could not find id = " + targetId);

        return false;
    }

    private void start() {
        Log.d(TAG, "start");

        startBackgroundThread();

        try {
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        Log.d(TAG, "stop");

        closeCamera();
        stopBackgroundThread();
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened, " + Thread.currentThread());

            mCameraDevice = cameraDevice;
            if (cameraDevice != null) {
                createCameraPreviewSession();
            } else {
                Log.e(TAG, "cameraDevice is null");
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "onDisconnected, " + Thread.currentThread());
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.d(TAG, "onError=" + error);
            cameraDevice.close();
            mCameraDevice = null;
            // if(mCaliBtn!=null) {
            //     mContext.runOnUiThread(new Runnable() {
            //         @Override
            //         public void run() {
            //             mCaliBtn.setEnabled(false);
            //         }
            //     });
            // }
        }
    };

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        if (mBackgroundThread == null) {
            Log.i(TAG, "Start Bg Thread");
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            if (mBackgroundHandler == null) {
                mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
            }
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            Log.i(TAG, "Stop Bg Thread");
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Stop Bg Thread fail", e);
            }
        }
    }

    private void closeCamera() {
        Log.i(TAG, "closeCamera START");

        if (null != mCameraDevice) {
            Log.i(TAG, "close mCameraDevice");
            mCameraDevice.close();
            mCameraDevice = null;

            
        }


        Log.i(TAG, "closeCamera FINISH");
    }

    private void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "sleep fail", e);
        }
    }

    private void createCameraPreviewSession() {
        Log.i(TAG, "createCameraPreviewSession START");
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size targetSize = null;
            if (map != null) {
                // List<Size> sizes = CameraUtil.getAllSizes(map,ImageFormat.JPEG);
                Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
                for (Size s : sizes) {
                    Log.i(TAG, String.format("%dx%d", s.getWidth(), s.getHeight()));
                }

                for (Size s : sizes) {
                    if (s.getWidth() == 800 && s.getHeight() == 600) {
                        targetSize = s;
                        Log.i(TAG, "found 800x600");
                        break;
                    }
                }

                if (targetSize == null) {
                    targetSize = sizes[sizes.length / 2];
                }
            }

            if (targetSize == null) {
                return;
            }

            Log.i(TAG, String.format("targetSize=%dx%d", targetSize.getWidth(), targetSize.getHeight()));

            SurfaceTexture texture = new SurfaceTexture(/* random texture ID */1);
            texture.setDefaultBufferSize(targetSize.getWidth(), targetSize.getHeight());
            Surface surface = new Surface(texture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            ImageReader mImageReader = ImageReader.newInstance(targetSize.getWidth(), targetSize.getHeight(), ImageFormat.JPEG, 1);

            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(
                                CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "onConfigured");

                            if (mCameraDevice == null) {
                                Log.e(TAG, "mCameraDevice is null");
                                return;
                            }

                            try {
                                //mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                                CaptureRequest mPreviewRequest = mPreviewRequestBuilder.build();
                                cameraCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG,"createCameraPreviewSession fail,　", e);
                                return;
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG,"createCameraPreviewSession fail,　", e);
                                return;
                            }

                            openCloseCamera();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreviewSession CameraAccessException,　"
                    + e);
        }
        Log.i(TAG, "createCameraPreviewSession FINISH");
    }

    private boolean writeFile(String value, String path) {
          Log.d(TAG, "writeFile, " + path);
          Log.d(TAG, "write data: " + value);
          FileWriter mFileWriter = null;
          boolean ret = false;
          try {
              mFileWriter = new FileWriter(path);
              mFileWriter.write(value);
              ret = true;
          } catch (Exception e) {
              e.printStackTrace();
          } finally {
              try {
                  if (mFileWriter != null) {
                      mFileWriter.close();
                  }
              } catch (Exception e) {
                  e.printStackTrace();
              }
              Log.d(TAG, "write data finish");
          }
          return ret;
    }

    private String readFile(String path) {
          Log.i(TAG, "readFile, " + path);
          if(path == null) {
              return  null;
          }
          BufferedReader readbuffer = null;
          String bufferValue = null;
          try {
              File file = new File(path);
              if (file.exists()) {
                  readbuffer = new BufferedReader(new FileReader(file));
                  bufferValue = readbuffer.readLine();
                  String line;
                  while ((line = readbuffer.readLine()) != null) {
                      bufferValue += line;
                  }
                  Log.i(TAG, "File node value is " + bufferValue.toString().trim());
              } else {
                  Log.w(TAG, "File node not exist");
              }
          } catch (IOException e) {
              Log.i(TAG, "readFileNote() throws IOException: " + e);
          } finally {
              try {
                  if (readbuffer != null) {
                      readbuffer.close();
                  }
              } catch (IOException e) {
                  Log.i(TAG, "readFileNote() throws IOException: " + e);
              }
          }
          return bufferValue;
      }

    private boolean setOisCail() {
  
        String path = "/sys/devices/platform/soc/ac4a000.qcom,cci0/ac4a000.qcom,cci0:qcom,ois@0/ois_gyro_cali_data";
  
        if (writeFile("0x9b2c:0x0002:w", path)) {
            return true;
        }  
        return false;
    }

    private boolean checkOisCail() {
        String path = "/sys/devices/platform/soc/ac4a000.qcom,cci0/ac4a000.qcom,cci0:qcom,ois@0/ois_gyro_cali_data";
        if (writeFile("0x9fb6:0x9fb8:r", path)) {
            return true;
        }  
        return false;
    }

    private boolean getOisCail() {
  
        String path = "/sys/devices/platform/soc/ac4a000.qcom,cci0/ac4a000.qcom,cci0:qcom,ois@0/ois_gyro_cali_data";
  
          try {
            String oisCail = readFile(path);  
            Log.d(TAG, "getOisCail oisCail =" + oisCail);
  
            if ("1".equals(oisCail)) {
                return true;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "getOisCail fail", e);
            return false;
        }
        return false;
    }

    private void openCloseCamera(){
        if (mState == Scene.OIS_SET) {
            setOisCail();
            Log.e(TAG, "openCloseCamera OIS_SET ,time delay 0.1 seconds");

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1,100);
        }else if (mState == Scene.OIS_CHECK) {
            checkOisCail();

            boolean checkOisCail = getOisCail();
            // Message.obtain(mUiHandler, UI_UPDATE_OIS_START_3, true).sendToTarget();
            if (checkOisCail) {
                Log.e(TAG, "ois cail sucessed");
                SystemProperties.set("persist.sys.cameraOis", "false");
            }else{
                Log.e(TAG, "ois cail failed");
                // Message.obtain(mUiHandler, UI_UPDATE_OIS_GET, false).sendToTarget();
            }
            dismissAll();
            mState = Scene.OIS_GET;
            Message.obtain(mUiHandler, UI_UPDATE_OIS_DESTORY, checkOisCail).sendToTarget();
        }
    }





    private class UIHandler extends Handler {
          @Override
          public void handleMessage(Message msg) {
              switch (msg.what) {
                    case UI_UPDATE_OIS_START_2:
                        mTitleTips.setText(mContext.getResources().getString(R.string.camera_ois_calibration_title2));
                        mMessageTips.setText(mContext.getResources().getString(R.string.camera_ois_calibration_message2));
                        break;
                    case UI_UPDATE_OIS_STOP:
                      stop();
                      break;
                    case UI_UPDATE_OIS_START:
                      start();
                      break;
                    case UI_UPDATE_OIS_DESTORY:
                        stop();
                        Boolean setOk = (Boolean)msg.obj;
                        if (setOk) {
                            mTitleTips.setText(mContext.getResources().getString(R.string.camera_ois_calibration_title3));
                            mMessageTips.setText(mContext.getResources().getString(R.string.camera_ois_calibration_message3));
                        }else{
                            mTitleTips.setText(mContext.getResources().getString(R.string.camera_ois_calibration_title4));
                            mMessageTips.setText(mContext.getResources().getString(R.string.camera_ois_calibration_message4));
                        }
                        mButton.setEnabled(true);
                      // dismiss();
                      break;
              }
              super.handleMessage(msg);
          }
    }

    private void setDelay(){
        Message.obtain(mUiHandler, UI_UPDATE_OIS_STOP, true).sendToTarget();
        Log.e(TAG, "openCloseCamera OIS_SET start");
        Message.obtain(mUiHandler, UI_UPDATE_OIS_START, true).sendToTarget();
        mState = Scene.OIS_CHECK;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            setDelay();
        }
    };

    enum Scene {
OIS_SET,OIS_CHECK,OIS_GET }



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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_POWER == keyCode) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

}
