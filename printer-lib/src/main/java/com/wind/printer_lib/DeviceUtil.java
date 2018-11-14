package com.wind.printer_lib;

import android.hardware.usb.UsbDevice;

/**
 * Created by wind on 2018/6/12.
 */

public class DeviceUtil {
    public static boolean enableQrcode(UsbDevice device){
        int vid=device.getVendorId();
        int pid=device.getProductId();
       // vid:26728--pid:1280
        if (vid==26728 && pid==1280){
            return true;
        }

        return false;

    }

    public static boolean needMoreSleepTime(UsbDevice device){
        int vid=device.getVendorId();
        int pid=device.getProductId();
        if (pid==85 && vid==1137){//佳博gp5890xiii
            return true;
        }

        return false;

    }

    /**
     * 是否支持纸传感器
     * @param device
     * @return
     */
    public static boolean enablePaperSensor(UsbDevice device) {
        int vid=device.getVendorId();
        int pid=device.getProductId();
        //vid:26728--pid:1280
        if (vid==26728 && pid==1280){
            return true;
        }
        return false;

    }
}
