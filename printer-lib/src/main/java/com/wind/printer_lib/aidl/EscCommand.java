package com.wind.printer_lib.aidl;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.wind.printer_lib.Utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
/**
 * Created by wind on 2018/6/9.
 */

public class EscCommand implements Parcelable{
    private ArrayList<Byte> commandList=new ArrayList<>();
    public void reset(){
        commandList.clear();
    }

    public ArrayList<Byte> getCommandList() {
        return commandList;
    }

    /**
     * 初始化打印机命令
     * 清除打印缓冲区中的数据，但是接收缓冲区的数据并不被清除
     * 十进制码 27  64
     */
    public void initPrinter(){
        byte command[]=new byte[]{27,64};
        addCommand(command);
    }

    /**
     * 设置打印对齐方式
     * 十进制码 27 97  n
     *
     *   n
     *   0，48    左对齐
     *   1，49    中间对齐
     *   2，50    右对齐
     *
     * @param gravity
     */
    public void setGravity(Gravity gravity){
        byte [] command=new byte[3];
        command[0]=27;
        command[1]=97;
        command[2]=gravity.getValue();

        addCommand(command);
    }

    /**
     * 打印字符串
     */
    public void printString(String str){
        if (str!=null && !"".equals(str)) {
            byte[] command = str.getBytes(Charset.forName("gb2312"));
            addCommand(command);
        }
    }
    /**
     * 打印字符串
     */
    public void printlnString(String str){
        if (str!=null && !"".equals(str)) {
            str=str+"\n";
            printString(str);
        }
    }
    /**
     * 打印模式设置
     * 十进制码   27  33  n
     * 根据n的值设置字符打印模式
     */
    public void enableBold(boolean enable){
        byte [] command=new byte[3];
        command[0]=27;
        command[1]=33;
        byte val=0;
        if (enable) {
            val=8;
        }
        command[2] = val;

        addCommand(command);
    }

    /**
     * 倍高倍宽
     * @param enableDoubleW
     * @param enableDoubleH
     */
    public void enableDoubleWH(boolean enableDoubleW,boolean enableDoubleH){
        byte [] command=new byte[3];
        command[0]=27;
        command[1]=33;
        byte val=0;
        if (enableDoubleW) {
            val=32;
        }
        if (enableDoubleH) {
            val |=16;
        }
        command[2] = val;
        addCommand(command);
    }

    /**
     * 走纸命令
     *
     * 十进制码   27 100 n
     * @param lines
     */
    public void feedLines(int lines){
        if (lines>255){
            lines=255;
        }
        if (lines<0){
            lines=0;
        }
        byte [] command=new byte[]{27,100,(byte) lines};
        addCommand(command);
    }


    public void printBitmap(Bitmap bitmap,int width){
        printBitmap(bitmap,width,BitmapMode.NORMAL);
    }

    /**
     * 打印光栅位图
     * 十进制码   29 118 48 m  xL xH yL yH d1...dk
     * 0<=m<=3
     * 0<=xL<=255
     * 0<=xH<=255
     * 0<=yL<=255
     * 0<=yH<=255
     *
     * 0<=d<=255
     * k=(xL+xH*256)*(yL+yH*256)
     * @param bitmap
     * @param requestWidth
     * @param mode
     */
    public void printBitmap(Bitmap bitmap,int requestWidth,BitmapMode mode){
       if (bitmap!=null){
           //使宽度必定为8的整数倍
           int newWidth=(requestWidth+7)/8 *8;
           int newHeight=bitmap.getHeight()*newWidth/bitmap.getWidth();

           Bitmap grayBitmap = Utils.grayBitmap(bitmap);
           Bitmap rszBitmap = Utils.resizeImage(grayBitmap, newWidth, newHeight);
           byte[] src = Utils.bitmapToBWPix(rszBitmap);
           byte[] command = new byte[8];
           newHeight = src.length / newWidth;
           command[0] = 29;
           command[1] = 118;
           command[2] = 48;
           command[3] = mode.getMode();
           command[4] = (byte)(newWidth / 8 % 256);
           command[5] = (byte)(newWidth / 8 / 256);
           command[6] = (byte)(newHeight % 256);
           command[7] = (byte)(newHeight / 256);
           this.addCommand(command);
           byte[] bitmapBytes = Utils.pixToEscRastBitImageCmd(src);

           addCommand(bitmapBytes);


       }

    }

    /**
     * 查询打印机状态命令
     */
    public void addQueryPrinterStatus() {
        byte[] command = new byte[]{29, 114, 49};
        this.addCommand(command);
    }

    /**
     * 开启钱箱
     * 十进制码 27 112 m t1 t2
     */
    public void openCashBox(){
        byte [] command=new byte[]{27,112,1,(byte) 255,(byte)255};
        addCommand(command);
    }


    private void addCommand(byte[] command) {
        for (int i=0;i<command.length;i++) {
            commandList.add(Byte.valueOf(command[i]));
        }
    }



    public EscCommand(){

    }
    protected EscCommand(Parcel in) {
        commandList=in.readArrayList(getClass().getClassLoader());
    }

    public static final Creator<EscCommand> CREATOR = new Creator<EscCommand>() {
        @Override
        public EscCommand createFromParcel(Parcel in) {
            return new EscCommand(in);
        }

        @Override
        public EscCommand[] newArray(int size) {
            return new EscCommand[size];
        }
    };




    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(commandList);
    }

    public void printQrcode(String data, int size, int errorLevel) {
        //纠错等级
        byte[] error_command = new byte[]{29, 40, 107, 3, 0, 49, 69,(byte)errorLevel};
        addCommand(error_command);
        //qrcode大小
        if (size<=1){
            size=1;
        }else if (size>15){
            size=15;
        }
        byte[] size_command = new byte[]{29, 40, 107, 3, 0, 49, 67, (byte) size};
        addCommand(size_command);


        addQrcodeData(data);

        byte[] print_command = new byte[]{29, 40, 107, 3, 0, 49, 81, 48};
        addCommand(print_command);
    }

    private void addQrcodeData(String data) {
        byte[] command = new byte[]{29, 40, 107, (byte)((data.getBytes().length + 3) % 256), (byte)((data.getBytes().length + 3) / 256), 49, 80, 48};
        this.addCommand(command);
        byte[] bs = null;
        if(!data.equals("")) {
            try {
                bs = data.getBytes("utf-8");
            } catch (UnsupportedEncodingException var5) {
                var5.printStackTrace();
            }

            addCommand(bs);
        }
    }


    public static enum Gravity{
        LEFT(0),CENTER(1),RIGHT(2);
        private int value;
        Gravity(int value) {
          this.value=value;
        }

        public byte getValue() {
            return (byte) value;
        }
    }
    public static enum BitmapMode{
        NORMAL(0),DW(1),DH(2),DWH(3);
        int mode;
        BitmapMode(int mode){
            this.mode=mode;
        }

        public byte getMode() {
            return (byte)mode;
        }
    }
}

