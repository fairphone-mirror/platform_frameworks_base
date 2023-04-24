package android.mymodule.mmitest;

import android.mymodule.mmitest.IT2MmiTestManager;
import android.util.Log;
import android.content.Context;
import android.os.RemoteException;
import android.annotation.SystemService;

public class T2MmiTestManager{

	private static final String TAG = "T2MmiTestManager";

	private IT2MmiTestManager manager;
	private Context mContext;


	public T2MmiTestManager(Context ctx,IT2MmiTestManager manager){
		this.manager = manager;
		mContext = ctx;
	}

	public String t2GetNodeString(String action){
		Log.d(TAG,"t2GetNodeString action = "+action);
		try{
			return manager.t2GetNodeString(action);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return "-1";
	}

	public boolean t2SetNodeString(String action,String value){
		Log.d(TAG,"t2SetNodeString action = "+action);
		try{
			return manager.t2SetNodeString(action,value);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return false;
	}

	public String t2TestNodeRead(String node){
		Log.d(TAG,"t2TestNodeRead node = "+node);
		try{
			return manager.t2TestNodeRead(node);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return "null";
	}

	public boolean t2TestNodeWrite(String node,String value){
		Log.d(TAG,"t2TestNodeWrite node = "+node+" ; value = "+value);
		try{
			return manager.t2TestNodeWrite(node,value);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return false;
	}

	public String t2RunShellCmd(String cmd){
		Log.d(TAG,"t2RunShellCmd cmd = "+cmd);
		try{
			return manager.t2RunShellCmd(cmd);
		}catch(RemoteException e){
			e.printStackTrace();
		}

		return "fail";
	}

}
