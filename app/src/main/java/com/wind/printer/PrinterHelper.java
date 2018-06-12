package com.wind.printer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.RemoteException;

import com.na.printer_base.Goods;
import com.na.printer_base.Ticket;
import com.wind.printer_lib.ConnectMode;
import com.wind.printer_lib.DeviceUtil;
import com.wind.printer_lib.ErrorCode;
import com.wind.printer_lib.PrinterService;
import com.wind.printer_lib.aidl.EscCommand;
import com.wind.printer_lib.aidl.IPrinter;

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
    private static  PrinterHelper instance=null;
    private IPrinter mPrinter;
    private CopyOnWriteArrayList<String> mDeviceNameList=new CopyOnWriteArrayList<>();
    private Set<String> mConnectedDevice=new HashSet<>();
    private boolean mBinding;//是否正在bindService
    private UsbManager mUsbManager;
    public static PrinterHelper getInstance(Context context){
        if (instance ==null){
            synchronized (PrinterHelper.class){
                if (instance==null){
                    instance=new PrinterHelper(context);
                }
            }
        }
        return instance;
    }

    private PrinterHelper(Context context){
        if (mPrinter==null) {
            if (!mBinding) {
                mBinding = true;
                Intent service = new Intent(context, PrinterService.class);
                context.bindService(service, new PrinterConnection(), Context.BIND_AUTO_CREATE);

                mUsbManager= (UsbManager) context.getSystemService(Context.USB_SERVICE);
            }
        }
    }

    /**
     * 连接指定设备
     * @param deviceName
     */
    public synchronized void connectDevice(String deviceName){
        mDeviceNameList.add(deviceName);
        if (mPrinter==null){
            try {
                instance.wait();
                if (mPrinter!=null){
                    openDevice();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            openDevice();
        }

    }

    private synchronized void openDevice() {
        for (String deviceName:mDeviceNameList){
            try {
                int code=mPrinter.openDevice(deviceName, ConnectMode.MODE_USB);
                if (code== ErrorCode.CODE_SUCCESS){
                    mConnectedDevice.add(deviceName);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                mDeviceNameList.clear();
            }
        }
        mDeviceNameList.clear();
    }

    class PrinterConnection implements ServiceConnection{
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
            mPrinter=null;
            mBinding=false;
            instance.notify();
        }
    }



    /**
     * 打印小票
     */
    public void printTicket(String deviceName,Ticket ticket) {
        if (!mConnectedDevice.contains(deviceName)){
            return;
        }
        System.out.println("=======printTicket start");
        EscCommand esc = new EscCommand();
        esc.reset();
        esc.feedLines((byte) 1);
        printBrand(esc, ticket);
        //esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 设置为倍高倍宽

        printSplitLine(esc);

        printNumberAndCashier(esc,ticket);
        printSplitLine(esc);

        printGoods(esc,ticket.getGoodsList());
        printSplitLine(esc);
        printMoney(esc,ticket);

        printSplitLine(esc);

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        esc.printlnString("日期：" + dateFormat.format(date));
        esc.setGravity(EscCommand.Gravity.CENTER);

        for (UsbDevice device:mUsbManager.getDeviceList().values()){
            if (deviceName.equals(device.getDeviceName())){
                if (!DeviceUtil.enableQrcode(device)) {
                    esc.printBitmap(ticket.getQrcodeBitmap(), 200, EscCommand.BitmapMode.NORMAL);
                }else {
                    //支持二维码打印
                    String data="http://weixin.qq.com/r/-S4JEVzEagEVrRhy93vv";
                    esc.printQrcode(data,6,49);
                }
                break;
            }

        }

        esc.feedLines(1);
        esc.printlnString( ticket.getWelcome());

        esc.feedLines((byte)5);
        if (ticket.isNeedOpenCashBox()){
            //开钱箱
            esc.openCashBox();
        }


      /*  Vector<Byte> datas = esc.getCommand();
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);*/
        // System.out.println("=======printTicket string:" + sss);
        int rs;
        try {
            rs = mPrinter.sendEscCommand(deviceName, esc);


            System.out.println("=======printTicket return:rs"+rs);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("=======printTicket error");
        }
    }


    public void readFromPrinter(String deviceName){
        EscCommand esc=new EscCommand();
        esc.addQueryPrinterStatus();
        int rs;
        try {
            rs = mPrinter.sendEscCommand(deviceName, esc);


            System.out.println("=======printTicket return:rs"+rs);
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
        esc.printlnString("单号：" + ticket.getOrderNo() );
        esc.printlnString("收银员：" + ticket.getCashier());

    }
    private void printBrand(EscCommand esc, Ticket ticket) {
        esc.setGravity(EscCommand.Gravity.CENTER);
        esc.printBitmap(ticket.getMerchantLogo(),240, EscCommand.BitmapMode.NORMAL);
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


            esc.printlnString(line );


        }

    }

    private void printMoney(EscCommand esc, Ticket ticket) {
        //技巧，使用String.format("%-12s",count);  使打印出的每列对齐
        String count = "数量：" + ticket.getCount();
        String total = "总计：" + ticket.getTotalPrice();
        count = String.format("%-12s", count);
        String countAndTotal = count + total ;
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

    }
}
