package com.wind.printer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.na.printer_base.Goods;
import com.na.printer_base.Label;
import com.na.printer_base.Ticket;
import com.wind.printer_lib.ConnectMode;
import com.wind.printer_lib.DeviceState;
import com.wind.printer_lib.DeviceUtil;
import com.wind.printer_lib.ErrorCode;
import com.wind.printer_lib.PrinterException;
import com.wind.printer_lib.PrinterService;
import com.wind.printer_lib.aidl.EscCommand;
import com.wind.printer_lib.aidl.IPrinter;
import com.wind.printer_lib.aidl.LabelCommand;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by wind on 2018/6/9.
 */

public class PrinterHelper {
    private static PrinterHelper instance = null;
    private IPrinter mPrinter;
    private CopyOnWriteArrayList<String> mDeviceNameList = new CopyOnWriteArrayList<>();
    private Set<String> mConnectedDevice = new HashSet<>();
    private boolean mBinding;//是否正在bindService
    private UsbManager mUsbManager;
    private boolean mContinuePrint;

    public static PrinterHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (PrinterHelper.class) {
                if (instance == null) {
                    instance = new PrinterHelper(context);
                }
            }
        }
        return instance;
    }

    private PrinterHelper(Context context) {
        if (mPrinter == null) {
            if (!mBinding) {
                mBinding = true;
                Intent service = new Intent(context, PrinterService.class);
                context.bindService(service, new PrinterConnection(), Context.BIND_AUTO_CREATE);

                mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            }
            //IntentFilter filter=new IntentFilter("action.device.receipt.response");
            //context.registerReceiver(new PrinterFinishReceiver(),filter);
        }
    }



    /*private class PrinterFinishReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mContinuePrint){
                String action = intent.getAction();
                if ("action.device.receipt.response".equals(action)) {
                    //收到打印结束消息
                    System.out.println("打印结束，开始新的打印");
                    mContinuePrint=false;
                    if (TextUtils.isEmpty(mPrintingDeviceName) || mPrintingTicket==null) {
                      return;
                    }else {
                        printTicket(mPrintingDeviceName, mPrintingTicket);
                    }
                }
            }

        }
    }*/

    /**
     * 连接指定设备
     *
     * @param deviceName
     */
    public synchronized void connectDevice(String deviceName) {
        mDeviceNameList.add(deviceName);
        if (mPrinter == null) {
            try {
                instance.wait();
                if (mPrinter != null) {
                    openDevice();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            openDevice();
        }

    }

    private synchronized void openDevice() {
        for (String deviceName : mDeviceNameList) {
            try {
                int code = mPrinter.openDevice(deviceName, ConnectMode.MODE_USB);
                if (code == ErrorCode.CODE_SUCCESS) {
                    mConnectedDevice.add(deviceName);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                mDeviceNameList.clear();
            }
        }
        mDeviceNameList.clear();
    }

    public int getDeviceState(String deviceName) {
        try {
            return mPrinter.getDeviceState(deviceName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return DeviceState.STATE_OFFLINE;
    }

    public void printLabelTest(String deviceName, Label label) {
        Bitmap b = label.getLabelBitmap();
        LabelCommand tsc = new LabelCommand();
        tsc.addSize(60, 40); // 设置标签尺寸，按照实际尺寸设置
        tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
        tsc.addReference(0, 0);// 设置原点坐标
        //tsc.addTear(ENABLE.ON); // 撕纸模式开启
        tsc.addDensity(LabelCommand.DENSITY.DNESITY1);
        tsc.addCls();// 清除打印缓冲区

        tsc.addBitmap(0, 0, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);

        tsc.addPrint(1, 1); // 打印标签

        int ret;
        try {
            ret = mPrinter.sendLabelCommand(deviceName, tsc);
            System.out.println("=======printTicket return:rs" + ret);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("=======printTicket error");
        }
    }

    public void printTicketTest(String deviceName) {
        EscCommand esc = new EscCommand();
        esc.reset();
        esc.feedLines((byte) 1);
        esc.setGravity(EscCommand.Gravity.CENTER);
        esc.printlnString("这是小票模式");
        esc.feedLines(5);
        int rs;
        try {
            rs = mPrinter.sendEscCommand(deviceName, esc);

            if (rs != ErrorCode.CODE_SUCCESS) {
                throw new PrinterException("打印机异常，请重新连接");
            }
            System.out.println("=======printTicket return:rs" + rs);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("=======printTicket error");
        }
    }

    class PrinterConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (instance) {
                mPrinter = IPrinter.Stub.asInterface(service);
                mBinding = false;
                //打开设备
                //openDevice(mCurDeviveName);
                instance.notify();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPrinter = null;
            mBinding = false;
            instance.notify();
        }
    }

    public void printLabel(String deviceName, Label lable) {
        Bitmap b = lable.getLabelBitmap();

        LabelCommand tsc = new LabelCommand();
        tsc.addSize(60, 40); // 设置标签尺寸，按照实际尺寸设置
        tsc.addGap(2); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
        tsc.addReference(0, 0);// 设置原点坐标
        //tsc.addTear(ENABLE.ON); // 撕纸模式开启
        tsc.addDensity(LabelCommand.DENSITY.DNESITY1);
        tsc.addCls();// 清除打印缓冲区

       /* tsc.addText(20, 30, FONTTYPE.KOREAN, ROTATION.ROTATION_0, FONTMUL.MUL_1, FONTMUL.MUL_1,
                "조선말");*/
      /*  tsc.addText(100, 30, FONTTYPE.SIMPLIFIED_CHINESE, ROTATION.ROTATION_0, FONTMUL.MUL_1, FONTMUL.MUL_1,
                "简体字");
        tsc.addText(180, 30, FONTTYPE.TRADITIONAL_CHINESE, ROTATION.ROTATION_0, FONTMUL.MUL_1, FONTMUL.MUL_1,
                "繁體字");*/

        // 绘制图片
        //b = BitmapFactory.decodeResource(getResources(), R.drawable.qrcode);
        //tsc.addText(0, 0, FONTTYPE.SIMPLIFIED_CHINESE, ROTATION.ROTATION_0, FONTMUL.MUL_2, FONTMUL.MUL_2,"波霸奶茶");
        tsc.addBitmap(0, 0, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);

        //绘制二维码
        //tsc.addQRCode(105, 75, EEC.LEVEL_L, 5, ROTATION.ROTATION_0, " www.smarnet.cc");
        // 绘制一维条码
        //tsc.add1DBarcode(50, 350, BARCODETYPE.CODE128, 100, READABEL.EANBEL, ROTATION.ROTATION_0, "SMARNET");
        tsc.addPrint(1, 1); // 打印标签
        //tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
        //tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        int ret;
        try {
            ret = mPrinter.sendLabelCommand(deviceName, tsc);
            System.out.println("=======printTicket return:rs" + ret);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("=======printTicket error");
        }

    }

    private String mPrintingDeviceName;
    private Ticket mPrintingTicket;

    public void printLabel(String deviceName, Label lable, int printCount) {
        for (int i = 0; i < printCount; i++) {
            printLabel(deviceName, lable);
        }
    }

    public void printTicket(String deviceName, Ticket ticket, int printCount) {
        mContinuePrint = true;//继续打印
        mPrintingDeviceName = deviceName;
        mPrintingTicket = ticket;
      /*  boolean needMoreTime = false;
        boolean enablePaperSensor = false;
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            if (deviceName.equals(device.getDeviceName())) {
                enablePaperSensor = DeviceUtil.enablePaperSensor(device);
                needMoreTime = DeviceUtil.needMoreSleepTime(device);
                break;
            }
        }*/

        for (int i = 0; i < printCount; i++) {
            printTicket(deviceName, ticket);
        }

       /* if (enablePaperSensor){
            printTicket(deviceName, ticket);
        }else {
            for (int i = 0; i < printCount; i++) {
                printTicket(deviceName, ticket);
            }
        }*/
    }

    public void queryPrinterStates(String deviceName) {
        EscCommand esc = new EscCommand();
        esc.addQueryPrinterStatus();

      /*  Vector<Byte> datas = esc.getCommand();
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);*/
        // System.out.println("=======printTicket string:" + sss);
        int rs;
        try {
            rs = mPrinter.sendEscCommand(deviceName, esc);
            if (rs != ErrorCode.CODE_SUCCESS) {
                throw new PrinterException("打印机异常，请重新连接");
            }
            System.out.println("=======printTicket return:rs" + rs);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("=======printTicket error");
        }
    }

    /**
     * 打印小票
     */
    public void printTicket(String deviceName, Ticket ticket) throws PrinterException {
        if (!mConnectedDevice.contains(deviceName)) {
            return;
        }
        System.out.println("=======printTicket start");
        EscCommand esc = new EscCommand();
        esc.reset();
        esc.feedLines((byte) 1);
        printBrand(esc, ticket);
        //esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 设置为倍高倍宽

        printSplitLine(esc);

        printNumberAndCashier(esc, ticket);
        printSplitLine(esc);

        printGoods(esc, ticket.getGoodsList());
        printSplitLine(esc);
        printMoney(esc, ticket);

        printSplitLine(esc);

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        esc.printlnString("日期：" + dateFormat.format(date));
        if (!TextUtils.isEmpty(ticket.getRemark())) {
            printSplitLine(esc);
            esc.printlnString("备注：" + ticket.getRemark());
        }

        esc.setGravity(EscCommand.Gravity.CENTER);

        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            if (deviceName.equals(device.getDeviceName())) {
                if (!DeviceUtil.enableQrcode(device)) {
                    esc.printBitmap(ticket.getQrcodeBitmap(), 200, EscCommand.BitmapMode.NORMAL);
                } else {
                    //支持二维码打印
                    String data = "http://weixin.qq.com/r/-S4JEVzEagEVrRhy93vv";
                    esc.printQrcode(data, 6, 49);
                }
                break;
            }

        }
        esc.feedLines(1);
        esc.printlnString(ticket.getWelcome());

        esc.feedLines((byte) 5);
        if (ticket.isNeedOpenCashBox()) {
            //开钱箱
            esc.openCashBox();
        }

        //打印机打印结束通知
        esc.addQueryPrinterStatus();

      /*  Vector<Byte> datas = esc.getCommand();
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);*/
        // System.out.println("=======printTicket string:" + sss);
        int rs;
        try {
            rs = mPrinter.sendEscCommand(deviceName, esc);

            if (rs != ErrorCode.CODE_SUCCESS) {
                throw new PrinterException("打印机异常，请重新连接");
            }
            System.out.println("=======printTicket return:rs" + rs);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("=======printTicket error");
        }
    }


    private void printSplitLine(EscCommand esc) {
        esc.printlnString("------------------------------");
    }

    private void printNumberAndCashier(EscCommand esc, Ticket ticket) {
        esc.setGravity(EscCommand.Gravity.LEFT);
        esc.printlnString("单号：" + ticket.getOrderNo());
        esc.printlnString("收银员：" + ticket.getCashier());

    }

    private void printBrand(EscCommand esc, Ticket ticket) {
        esc.setGravity(EscCommand.Gravity.CENTER);
        esc.printBitmap(ticket.getMerchantLogo(), 240, EscCommand.BitmapMode.NORMAL);
        esc.printlnString(ticket.getMerchantName());
    }

    private void printGoods(EscCommand esc, List<Goods> goodsList) {
        String titld = "　　　　　　　　　　　　　　　　";
        String title = "商品名称　　单价　　数量　　金额";

        String goodsNameTitle = String.format("%-8s", "商品名称");
        String priceTitle = String.format("%-4s", "单价");
        String numTitle = String.format("%-4s", "数量");
        String moneyTitle = String.format("%-4s", "金额");
        title = goodsNameTitle + priceTitle + numTitle + moneyTitle;
        esc.printlnString(title);
        for (int i = 0; i < goodsList.size(); i++) {
            Goods goods = goodsList.get(i);


            esc.printlnString((i + 1) + "．" + goods.getName());

            String serialNumber = goods.getSerialNumber();
            serialNumber = String.format("%-12s", serialNumber);

            String price = goods.getPrice() + "";
            price = String.format("%-6s", price);

            String num = goods.getNum() + "";
            num = String.format("%-6s", num);

            String totalPrice = goods.getTotalPrice() + "";
            totalPrice = String.format("%-8s", totalPrice);

            String line = serialNumber + price + num + totalPrice;


            esc.printlnString(line);


        }

    }

    private void printMoney(EscCommand esc, Ticket ticket) {
        //技巧，使用String.format("%-12s",count);  使打印出的每列对齐
        String count = "数量：" + ticket.getCount();
        String total = "总计：" + ticket.getTotalPrice();
        count = String.format("%-12s", count);
        String countAndTotal = count + total;
        esc.setGravity(EscCommand.Gravity.LEFT);
        esc.printlnString(countAndTotal);
        String discountAmount = "优惠：" + ticket.getDiscountAmount();
        String needPayPrice = "应付：" + ticket.getNeedPayPrice();

        discountAmount = String.format("%-12s", discountAmount);
        String discountAmountAndNeedPayAmount = discountAmount + needPayPrice;
        esc.printlnString(discountAmountAndNeedPayAmount);

        String cash = "现金：" + ticket.getCash();
        String change = "找零：" + ticket.getChange();

        cash = String.format("%-12s", cash);
        String cashAndChange = cash + change;
        esc.printlnString(cashAndChange);

        if (ticket.isShowBalance()) {
            String balance = "余额：" + ticket.getBalance();
            balance = String.format("%-12s", balance);
            esc.printlnString(balance);
        }

    }

    /**
     * 仅打开钱箱
     */
    public void openCashBox(String deviceName) {
        EscCommand esc = new EscCommand();
        esc.openCashBox();

        int rs;
        try {
            rs = mPrinter.sendEscCommand(deviceName, esc);

            if (rs != ErrorCode.CODE_SUCCESS) {
                throw new PrinterException("打印机异常，请重新连接");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
