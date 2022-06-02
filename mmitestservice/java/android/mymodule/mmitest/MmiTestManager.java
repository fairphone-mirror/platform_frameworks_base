package android.mymodule.mmitest;

import android.mymodule.mmitest.ImmiTestManager;
import android.util.Log;
import android.content.Context;
import android.os.RemoteException;
import android.annotation.SystemService;

public class MmiTestManager{

	private static final String TAG = "MmiTestManager";

	private ImmiTestManager manager;
	private Context mContext;


	public MmiTestManager(Context ctx,ImmiTestManager manager){
		 android.util.Log.e("chuanzhi","MmiTestManager");
		this.manager = manager;
		mContext = ctx;
	}

	public int getOpen(String action){
		Log.e(TAG,"getOpen action = "+action);
		try{
			return manager.getOpen(action);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return -1;
	}

	public String getNodeString(String action){
		Log.e(TAG,"getNodeString action = "+action);
		try{
			return manager.getNodeString(action);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return "-1";
	}

	public boolean setNodeString(String action,String value){
		Log.e(TAG,"setNodeString action = "+action);
		try{
			return manager.setNodeString(action,value);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return false;
	}

}
