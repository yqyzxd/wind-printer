package com.wind.printer_lib;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.wind.printer_lib.aidl.EscCommand;
import com.wind.printer_lib.aidl.IPrinter;
import com.wind.printer_lib.aidl.LabelCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by wind on 2018/6/9.
 */

public class PrinterService extends Service {
    public static final String ACTION_DEVICE_PERMISSION = "action_device_permission";
    public static final int TIME_OUT=500;
    private Map<String, UsbDevice> mUsbDeviceMap = new HashMap<>();
    private Map<String, UsbInterface> mUsbInterfaceMap = new HashMap<>();
    private Map<String, UsbDeviceConnection> mUsbDeviceConnectionMap = new HashMap<>();
    private Map<String, UsbEndpoint> mUsbEndpointInMap = new HashMap<>();
    private Map<String, UsbEndpoint> mUsbEndpointOutMap = new HashMap<>();
    private PendingIntent mRequestPermissionPendingIntent;
    private UsbManager mUsbManager;

    private ExecutorService mExecutorService;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mPrinterFrameworkLayerService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //申请USB使用的权限
        mRequestPermissionPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DEVICE_PERMISSION), 0);
        //注册接收申请权限结果的广播接收器
        IntentFilter permissionFilter = new IntentFilter(ACTION_DEVICE_PERMISSION);
        registerReceiver(mUsbPermissionReceiver, permissionFilter);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mExecutorService= Executors.newSingleThreadExecutor();
    }

    /**
     * bindService方式不会调用onStartCommand
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    private IPrinter.Stub mPrinterFrameworkLayerService = new IPrinter.Stub() {
        @Override
        public int openDevice(String deviceName, int connectMode) throws RemoteException {
            int err_code = ErrorCode.CODE_ERROR;
            if (mUsbDeviceMap.containsKey(deviceName)) {
                return ErrorCode.CODE_SUCCESS;
            }
            //1，获取UsbManager
            // UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
            //2,获取UsbDeviceList，并迭代
            HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
            Iterator<UsbDevice> iterator = deviceMap.values().iterator();
            while (iterator.hasNext()) {
                UsbDevice device = iterator.next();
                if (deviceName.equals(device.getDeviceName())) {
                    if (!mUsbDeviceMap.containsKey(deviceName)) {
                        err_code = ErrorCode.CODE_SUCCESS;
                        mUsbDeviceMap.put(deviceName, device);
                        break;
                    }
                }
            }
            //3，申请USB使用权限
            if (err_code == ErrorCode.CODE_SUCCESS) {
                UsbDevice device = mUsbDeviceMap.get(deviceName);
                if (isPrinterDevice(device)) {
                    if (mUsbManager.hasPermission(device)) {
                        usbDeviceInit(device);
                    } else {
                        //没有权限则申请权限
                        mUsbManager.requestPermission(device, mRequestPermissionPendingIntent);
                    }
                }
            }
            return err_code;
        }

        /**
         * 发送esc打印命令，用于打印票据
         * @param deviceName
         * @param command
         * @return
         * @throws RemoteException
         */
        @Override
        public int sendEscCommand(final String deviceName,final EscCommand command) throws RemoteException {
            Future<Integer> futureTask=mExecutorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int retCode=ErrorCode.CODE_ERROR;
                    UsbEndpoint endpointOut = mUsbEndpointOutMap.get(deviceName);
                    if (endpointOut != null) {
                        ArrayList<Byte> commandList = command.getCommandList();
                        ArrayList<Byte> partOfCommand = new ArrayList<>(commandList.size());

                        for (int i = 0; i < commandList.size(); i++) {
                             if (partOfCommand.size() >= 1024) {
                                 //Thread.sleep(300);可以解决某些机型在小票上打印两张图片失败的情况，但是会导致打印略显卡顿
                                //长度超过1024直接打印，避免一次性打印太大，打印机缓存存不下，导致数据丢失
                                retCode=bulk(deviceName, partOfCommand);
                                partOfCommand.clear();
                                System.out.println("bulk retval:"+retCode);
                                if (retCode!=ErrorCode.CODE_SUCCESS){
                                    return retCode;
                                }

                            }
                            partOfCommand.add(commandList.get(i));
                        }
                        retCode=bulk(deviceName, partOfCommand);
                    }


                    return retCode;
                }
            });
            int code=ErrorCode.CODE_ERROR;
            try {
                code=futureTask.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return code;
        }

        @Override
        public int sendLabelCommand(final String deviceName, final LabelCommand command) throws RemoteException {
            Future<Integer> futureTask=mExecutorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int retCode=ErrorCode.CODE_ERROR;
                    UsbEndpoint endpointOut = mUsbEndpointOutMap.get(deviceName);
                    if (endpointOut != null) {
                        ArrayList<Byte> commandList = command.getCommand();
                        ArrayList<Byte> partOfCommand = new ArrayList<>(commandList.size());

                        for (int i = 0; i < commandList.size(); i++) {
                            if (partOfCommand.size() >= 1024) {
                                //Thread.sleep(300);可以解决某些机型在小票上打印两张图片失败的情况，但是会导致打印略显卡顿
                                //长度超过1024直接打印，避免一次性打印太大，打印机缓存存不下，导致数据丢失
                                retCode=bulk(deviceName, partOfCommand);
                                partOfCommand.clear();
                                System.out.println("bulk retval:"+retCode);
                                if (retCode!=ErrorCode.CODE_SUCCESS){
                                    return retCode;
                                }

                            }
                            partOfCommand.add(commandList.get(i));
                        }
                        retCode=bulk(deviceName, partOfCommand);
                    }


                    return retCode;
                }
            });
            int code=ErrorCode.CODE_ERROR;
            try {
                code=futureTask.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return code;
        }

        @Override
        public void closeDevice(String deviceName) throws RemoteException {
            mUsbDeviceMap.remove(deviceName);
        }



    };

    private int bulk(String deviceName, ArrayList<Byte> command) {
        if (command!=null && !command.isEmpty()) {
            UsbEndpoint endpointOut = mUsbEndpointOutMap.get(deviceName);
            UsbDeviceConnection connection = mUsbDeviceConnectionMap.get(deviceName);
            if (endpointOut == null || connection == null) {
                return ErrorCode.CODE_ERROR;
            }
            byte[] data = new byte[command.size()];
            for (int i = 0; i < command.size(); i++) {
                data[i] = command.get(i).byteValue();
            }
            int ret = connection.bulkTransfer(endpointOut, data, data.length, TIME_OUT);
            if (ret >= 0) {
                return ErrorCode.CODE_SUCCESS;
            }

            return ErrorCode.CODE_ERROR;
        }else {
            return ErrorCode.CODE_SUCCESS;
        }

    }


    /**
     * 获取通信相关类UsbInterface，UsbEndpoint，UsbDeviceConnection
     */
    private void usbDeviceInit(UsbDevice device) {
        int interfaceCount = device.getInterfaceCount();
        UsbInterface usbInterface = null;
        for (int i = 0; i < interfaceCount; i++) {
            usbInterface = device.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                break;
            }
        }
        if (usbInterface != null) {
            mUsbInterfaceMap.put(device.getDeviceName(), usbInterface);
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null) {
                mUsbDeviceConnectionMap.put(device.getDeviceName(), connection);

                if (connection.claimInterface(usbInterface, true)) {
                    for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                        UsbEndpoint endpoint = usbInterface.getEndpoint(j);
                        //类型为大块传输
                        if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                mUsbEndpointOutMap.put(device.getDeviceName(), endpoint);
                            } else {
                                mUsbEndpointInMap.put(device.getDeviceName(), endpoint);
                            }
                        }
                    }
                }
            }


        }
    }

    private boolean isPrinterDevice(UsbDevice usbDevice) {
        return true;
    }

    private BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_DEVICE_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    //获得了usb使用权限
                    usbDeviceInit(device);
                }
            }
        }
    };
}
