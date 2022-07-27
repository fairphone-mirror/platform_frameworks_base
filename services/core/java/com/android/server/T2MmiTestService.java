package com.android.server;

import android.mymodule.mmitest.IT2MmiTestManager;
import android.util.Log;
import android.content.Context;
import android.os.RemoteException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.io.FileNotFoundException;
import java.io.IOException;

public class T2MmiTestService extends IT2MmiTestManager.Stub {
	private static final String TAG = "T2MmiTestService";

	private static final String FTS_TEST_SEATTLE = "/proc/android_touch/self_test";
	private static final String LCDBACKLIGHT_NODE = "/sys/class/backlight/backlight/brightness";
	private static final String BackFlash_White = "/sys/class/leds/led:torch_1/brightness";
    private static final String BackFlash_Yellow = "/sys/class/leds/led:torch_0/brightness";
    private static final String BackFlash_Switch0 = "/sys/class/leds/led:switch_0/brightness";
    private static final String BackFlash_Switch1 = "/sys/class/leds/led:switch_1/brightness";
    private static final String CHARGE_STATUS = "/sys/class/power_supply/battery/status";
    private static final String CHARGE_NOW = "/sys/class/power_supply/battery/current_now";
    private static final String USBPlugStatusFile = "/sys/class/power_supply/usb/typec_cc_orientation";
    private static final String UsbModeStatusFile = "/sys/class/udc/a600000.dwc3/current_speed";
    private static final String UsbNTCPath = "/sys/class/power_supply/usb/connector_temp";
    private static final String EfuseCheckPath = "/sys/module/msm_poweroff/parameters/download_mode";
    private static final String NPI_DOWNLOAD = "/sys/class/npi_down_status/status";
    private static final String TP_android_touch = "/proc/android_touch/vendor";

	private static final String RAWDATA_TEST = "mmitest_rawdata_test";
	private static final String LCDBACKLIGHT_TEST = "mmitest_lcdbacklight_test";
	private static final String BackFlash_White_TEST = "backflash_white_test";
	private static final String BackFlash_Yellow_TEST = "backflash_yellow_test";
	private static final String BackFlash_Switch0_TEST = "backflash_switch0_test";
	private static final String BackFlash_Switch1_TEST = "backflash_switch1_test";
	private static final String CHARGE_STATUS_TEST = "charge_status_test";
	private static final String CHARGE_NOW_TEST = "charge_now_test";
	private static final String USBPlugStatusFile_TEST = "usb_plug_status_test";
	private static final String UsbModeStatusFile_TEST = "usb_mode_status_test";
	private static final String UsbNTCPath_TEST = "usb_ntc_path_test";
	private static final String EfuseCheckPath_TEST = "efuse_check_path_test";
	private static final String NPI_DOWNLOAD_TEST = "npi_download_test";
	private static final String TP_android_touch_TEST = "tp_android_touch_test";


	private Context mContext;

	public T2MmiTestService(Context ctx){
		mContext = ctx;
	}


	@Override
	public String t2GetNodeString(String action){
		Log.e(TAG,"t2GetNodeString action = "+action);
		if (RAWDATA_TEST.equals(action)) {
			return readNodeString(FTS_TEST_SEATTLE);
		}else if (LCDBACKLIGHT_TEST.equals(action)) {
			return readNodeString(LCDBACKLIGHT_NODE);
		}else if (CHARGE_STATUS_TEST.equals(action)) {
			return readNodeString(CHARGE_STATUS);
		}else if (CHARGE_NOW_TEST.equals(action)) {
			return readNodeString(CHARGE_NOW);
		}else if (USBPlugStatusFile_TEST.equals(action)) {
			return readNodeString(USBPlugStatusFile);
		}else if (UsbModeStatusFile_TEST.equals(action)) {
			return readNodeString(UsbModeStatusFile);
		}else if (UsbNTCPath_TEST.equals(action)) {
			return readNodeString(UsbNTCPath);
		}else if (EfuseCheckPath_TEST.equals(action)) {
			return readNodeString(EfuseCheckPath);
		}else if (NPI_DOWNLOAD_TEST.equals(action)) {
			return readNodeString(NPI_DOWNLOAD);
		}else if (TP_android_touch_TEST.equals(action)) {
			return readNodeString(TP_android_touch);
		}

		return "1";
	}

	@Override
	public boolean t2SetNodeString(String action,String value){
		Log.e(TAG,"t2SetNodeString action = "+action);
		if (LCDBACKLIGHT_TEST.equals(action)) {
			return writeNodeString(LCDBACKLIGHT_NODE,value);
		}else if (BackFlash_White_TEST.equals(action)) {
			return writeNodeString(BackFlash_White,value);
		}else if (BackFlash_Yellow_TEST.equals(action)) {
			return writeNodeString(BackFlash_Yellow,value);
		}else if (BackFlash_Switch0_TEST.equals(action)) {
			return writeNodeString(BackFlash_Switch0,value);
		}else if (BackFlash_Switch1_TEST.equals(action)) {
			return writeNodeString(BackFlash_Switch1,value);
		}

		return false;
	}

	@Override
	public String t2TestNodeRead(String node){
		Log.e(TAG,"t2TestNodeRead node = "+node);
		return readNodeString(node);
	}

	@Override
	public boolean t2TestNodeWrite(String node,String value){
		Log.e(TAG,"t2TestNodeWrite node = "+node);
		return writeNodeString(node,value);
	}

	private String readNodeString(String node){
		FileInputStream fis = null;
		try {
            File file = new File(node);
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        	Log.e(TAG,"openInputStream error: "+node);
            e.printStackTrace();
        }
        if (fis != null) {
        	byte[] range = new byte[256];
            int ret = -1;
            try {
                ret = fis.read(range);
                if (ret <= 0) {
                	closeFileInputStream(fis);
                    return "FAIL";
                }
            } catch (IOException e) {
            	Log.e(TAG,"InputStream read error: "+node);
                e.printStackTrace();
                closeFileInputStream(fis);
                return "FAIL";
            }
            byte[] readBytes = new byte[ret];
            System.arraycopy(range, 0, readBytes, 0, ret);
            range = null;
            closeFileInputStream(fis);            
	        if (readBytes == null) {
	            return "FAIL";
	        } else {
	            String test_result = new String(readBytes).trim();
	            Log.i(TAG, "readFile...test_result =  " + test_result);
	            return test_result;
	        }

        }
        return "FAIL";
	}

	private void closeFileInputStream(FileInputStream fis){
		if (fis != null) {
			try {
                fis.close();
                fis = null;
            } catch (IOException e) {
                Log.e(TAG, "inputStreamClose error");
                e.printStackTrace();
            }
		}
	}

	private boolean writeNodeString(String path,String value){
		FileOutputStream fos = null;
          try {
            File file = new File(path);
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "openOutputStream error: "+path);
            e.printStackTrace();
        }

        if (fos != null) {
            try {
                fos.write(value.getBytes());
                fileOutputClose(fos);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        } else {
            Log.e(TAG, "fileOutputStream is null");
            return false;
        }
	}

	private void fileOutputClose(FileOutputStream fileOutputStream) {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
                fileOutputStream = null;
            } catch (IOException e) {
                Log.e(TAG, "outputStreamClose error");
                e.printStackTrace();
            }
        }
    }





}
