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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.wind.printer_lib.aidl.EscCommand;
import com.wind.printer_lib.aidl.IPrinter;
import com.wind.printer_lib.aidl.LabelCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
    public static final int TIME_OUT = 5000;
    private Map<String, UsbDevice> mUsbDeviceMap = new HashMap<>();
    private Map<String, UsbInterface> mUsbInterfaceMap = new HashMap<>();
    private Map<String, UsbDeviceConnection> mUsbDeviceConnectionMap = new HashMap<>();
    private Map<String, UsbEndpoint> mUsbEndpointInMap = new HashMap<>();
    private Map<String, UsbEndpoint> mUsbEndpointOutMap = new HashMap<>();
    private PendingIntent mRequestPermissionPendingIntent;
    private UsbManager mUsbManager;

    private ExecutorService mExecutorService;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    String deviceName = msg.getData().getString("printer.id");
                    int cnt = msg.getData().getInt("device.readcnt");
                    byte[] readBuf = msg.getData().getByteArray("device.read");
                    List<Byte> data = new ArrayList();

                    int result;
                    for (result = 0; result < cnt; ++result) {
                        if (readBuf[result] != 19 && readBuf[result] != 17) {
                            data.add(Byte.valueOf(readBuf[result]));
                        }
                    }

                    UsbDevice device = mUsbDeviceMap.get(deviceName);

                    Log.i("GpPrintService", "readMessage cnt" + cnt);
                    if (device != null) {
                        if (cnt <= 1) {
                            result =judgeResponseType(readBuf[0]);
                            if (result == 0) {
                                if (readBuf[0] == 0) {
                                  sendFinishBroadcastToFront(deviceName);
                                }
                            }
                        }
                    }

                    break;
            }
        }
    };
    private int judgeResponseType(byte r) {
        byte result = (byte)((r & 16) >> 4);
        return result;
    }
    private void sendFinishBroadcastToFront(String deviceName) {
        Intent statusBroadcast = new Intent("action.device.receipt.response");
        statusBroadcast.putExtra("printer.id", deviceName);
        this.sendBroadcast(statusBroadcast);
    }

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

        mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * bindService方式不会调用onStartCommand
     *
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
        public int sendEscCommand(final String deviceName, final EscCommand command) throws RemoteException {
            Future<Integer> futureTask = mExecutorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int retCode = ErrorCode.CODE_ERROR;
                    UsbEndpoint endpointOut = mUsbEndpointOutMap.get(deviceName);
                    if (endpointOut != null) {
                        ArrayList<Byte> commandList = command.getCommandList();
                        ArrayList<Byte> partOfCommand = new ArrayList<>(commandList.size());

                        for (int i = 0; i < commandList.size(); i++) {
                            if (partOfCommand.size() >= 1024) {
                                //Thread.sleep(300);可以解决某些机型在小票上打印两张图片失败的情况，但是会导致打印略显卡顿
                                //长度超过1024直接打印，避免一次性打印太大，打印机缓存存不下，导致数据丢失

                               /* if (i > 6000) {
                                    Thread.sleep(550);
                                } else if (i > 10000) {
                                    Thread.sleep(700);
                                }*/
                                retCode = bulk(deviceName, partOfCommand);
                                partOfCommand.clear();
                                System.out.println("bulk retval:" + retCode);
                                if (retCode != ErrorCode.CODE_SUCCESS) {
                                    return retCode;
                                }

                            }
                            partOfCommand.add(commandList.get(i));
                        }
                        retCode = bulk(deviceName, partOfCommand);
                    }


                    return retCode;
                }
            });
            int code = ErrorCode.CODE_ERROR;
            try {
                code = futureTask.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return code;
        }

        @Override
        public int sendLabelCommand(final String deviceName,final LabelCommand command) throws RemoteException {
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
                                //System.out.println("bulk retval:"+retCode);
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
        public int getDeviceState(String deviceName) throws RemoteException {
            UsbDevice device = mUsbDeviceMap.get(deviceName);
            if (device != null) {
                UsbInterface usbInterface = mUsbInterfaceMap.get(deviceName);
                if (usbInterface != null) {
                    UsbEndpoint endpointOut = mUsbEndpointOutMap.get(deviceName);
                    if (endpointOut != null) {
                        return DeviceState.STATE_ONLINE;
                    }
                } else if (!mUsbManager.hasPermission(device)) {
                    mUsbManager.requestPermission(device, mRequestPermissionPendingIntent);
                    return DeviceState.STATE_NO_PERMISSION;
                }
            }
            return DeviceState.STATE_OFFLINE;
        }

        @Override
        public void closeDevice(String deviceName) throws RemoteException {
            mUsbDeviceMap.remove(deviceName);
        }


    };

    private boolean needSleep(String deviceName) {
        UsbDevice device = mUsbDeviceMap.get(deviceName);
        if (device != null) {
            int pid = device.getProductId();
            int vid = device.getVendorId();
            //佳博gp5890xiii    中崎ab-58gk
            if (pid == 85 && vid == 1137 || pid == 22336 && vid == 1411) {
                return true;
            }


        }
        return false;

    }

    private int bulk(String deviceName, ArrayList<Byte> command) {
        if (command != null && !command.isEmpty()) {
            UsbEndpoint endpointOut = mUsbEndpointOutMap.get(deviceName);
            UsbDeviceConnection connection = mUsbDeviceConnectionMap.get(deviceName);
            if (endpointOut == null || connection == null) {
                return ErrorCode.CODE_ERROR;
            }
            byte[] data = new byte[command.size()];
            for (int i = 0; i < command.size(); i++) {
                data[i] = command.get(i).byteValue();
            }
            //超时时间需要设置的长一点，不然很可能打印卡住，返回-1。
            int ret = connection.bulkTransfer(endpointOut, data, data.length, TIME_OUT);
            if (ret >= 0) {
                return ErrorCode.CODE_SUCCESS;
            }

            return ErrorCode.CODE_ERROR;
        } else {
            return ErrorCode.CODE_SUCCESS;
        }

    }

    private StateQueryThread mStateQueryThread;
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
                //开启查询打印机状态的线程
                if (mStateQueryThread!=null){
                    mStateQueryThread.stopRun();
                }
                mStateQueryThread=new StateQueryThread(device.getDeviceName());
                mStateQueryThread.start();
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

    private class StateQueryThread extends Thread {
        private String deviceName;
        UsbEndpoint mmEndIn;
        UsbEndpoint mmEndOut;
        UsbDeviceConnection mmConnection;
        private boolean mRun=true;
        public StateQueryThread(String deviceName) {
            this.deviceName = deviceName;
            mmEndOut = mUsbEndpointOutMap.get(deviceName);
            mmEndIn = mUsbEndpointInMap.get(deviceName);
            mmConnection = mUsbDeviceConnectionMap.get(deviceName);
        }
        private void stopRun(){
            mRun=false;
        }

        @Override
        public void run() {

            if (mmEndOut != null && mmEndIn != null) {

                while (mRun) {
                    try {
                        byte[] ReceiveData = new byte[100];
                        int bytes = this.mmConnection.bulkTransfer(mmEndIn, ReceiveData, ReceiveData.length, 200);
                        //System.out.println("mmEndIn:"+bytes);
                        if (bytes > 0) {
                            Message msg = mHandler.obtainMessage(2);
                            Bundle bundle = new Bundle();
                            bundle.putString("printer.id", deviceName);
                            bundle.putInt("device.readcnt", bytes);
                            bundle.putByteArray("device.read", ReceiveData);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }

                        Thread.sleep(30L);
                    } catch (InterruptedException var5) {
                        // UsbPort.this.connectionLost();

                        break;
                    }
                }

                Log.d("UsbPortService", "Closing Usb work");
            } else {
              /*  UsbPort.this.stop();
                UsbPort.this.connectionLost();*/

            }
        }
    }
}
