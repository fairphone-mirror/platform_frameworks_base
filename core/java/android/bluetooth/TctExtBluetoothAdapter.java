/* Copyright (C) 2016 Tcl Corporation Limited */
/*****************************************************************************/
/*                                                           Date:2013/38/11 */
/*                                PRESENTATION                               */
/*                                                                           */
/*       Copyright 2013 TCL Communication Technology Holdings Limited.       */
/*                                                                           */
/* This material is company confidential, cannot be reproduced in any form   */
/* without the written permission of TCL Communication Technology Holdings   */
/* Limited.                                                                  */
/*                                                                           */
/* ------------------------------------------------------------------------- */
/*  Author :                                                                 */
/*  Email  :                                                                 */
/*  Role   :                                                                 */
/*  Reference documents :                                                    */
/* ------------------------------------------------------------------------- */
/*  Comments :                                                               */
/*  File     :                                                               */
/*  Labels   :                                                               */
/* ------------------------------------------------------------------------- */
/* ========================================================================= */
/*     Modifications on Features list / Changes Request / Problems Report    */
/* ------------------------------------------------------------------------- */
/*   date    |   author   |   Key     |                comment               */
/* ----------|------------|-----------|------------------------------------- */
/* 09/09/2013|      Qianbo Pan        |     FR-512357     |Add Bluetooth test*/
/*           |                        |                   |mode api          */
/* ----------|------------|-----------|------------------------------------- */
/*****************************************************************************/
package android.bluetooth;

import android.os.RemoteException;
import android.util.Log;
import android.bluetooth.TctExtIBluetooth;
/**
 *@hide
 */
public class TctExtBluetoothAdapter {
    private String TAG = "TctExtBluetoothAdapter";
    private BluetoothAdapter mBluetoothAdapter = null; //MODIFICATIONS FORBIDDEN
    private static IBluetooth sService;
    private static TctExtIBluetooth sExtService;
    public TctExtBluetoothAdapter(BluetoothAdapter bluetoothadapter) { //MODIFICATIONS FORBIDDEN
        mBluetoothAdapter = bluetoothadapter;
        sService = mBluetoothAdapter.getService();
        sExtService = getTctExtIBluetoothInterface();
    }

    public int setBtTestMode(int mode) {
        if (sExtService == null) {
            return -1;
        }
        try {
            return sExtService.tct_setBtTestMode(mode);
        } catch (Exception ex) {
            Log.d(TAG, "Unhandled exception: " + ex);
        }
        return -1;
    }

    /*public int setBtChannel(int position) {
        if (sExtService == null) {
            return -1;
        }
        try {
            return sExtService.tct_setBtChannel(position);
        } catch (Exception ex) {
            Log.w(TAG, "Unhandled exception: " + ex);
        }
        return -1;
    }*/

    public int sendDutMode(int power) {
        if (sExtService == null) {
            return -1;
        }
        try {
            return sExtService.tct_sendDutMode(power);
        } catch (Exception ex) {
            Log.w(TAG, "Unhandled exception: " + ex);
        }
        return -1;
    }

    private static TctExtIBluetooth getTctExtIBluetoothInterface() {
        if (sService == null) {
            return null;
        }
        try {
            return sService.getTctExtIBluetoothInterface();
        } catch (RemoteException e) {
            return null;
        }
    }
}
