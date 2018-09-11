package com.wind.printer_lib.aidl;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.wind.printer_lib.Utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class LabelCommand implements Parcelable{
    private static final String DEBUG_TAG = "LabelCommand";
   // Vector<Byte> Command = null;
    private ArrayList<Byte> commandList=new ArrayList<>();
    public LabelCommand() {
       // this.Command = new Vector();
    }

    public LabelCommand(int width, int height, int gap) {
      //  this.Command = new Vector(4096, 1024);
        this.addSize(width, height);
        this.addGap(gap);
    }

    protected LabelCommand(Parcel in) {
        commandList=in.readArrayList(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(commandList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LabelCommand> CREATOR = new Creator<LabelCommand>() {
        @Override
        public LabelCommand createFromParcel(Parcel in) {
            return new LabelCommand(in);
        }

        @Override
        public LabelCommand[] newArray(int size) {
            return new LabelCommand[size];
        }
    };

    public void clrCommand() {
        this.commandList.clear();
    }

    private void addStrToCommand(String str) {
        byte[] bs = null;
        if(!str.equals("")) {
            try {
                bs = str.getBytes("GB2312");
            } catch (UnsupportedEncodingException var4) {
                var4.printStackTrace();
            }

            for(int i = 0; i < bs.length; ++i) {
                this.commandList.add(Byte.valueOf(bs[i]));
            }
        }

    }

    /*private void addStrToCommand(String str, LabelCommand.FONTTYPE font) {
        byte[] bs = null;
        if(!str.equals("")) {
            try {
                switch(null.$SwitchMap$com$gprinter$command$LabelCommand$FONTTYPE[font.ordinal()]) {
                case 1:
                    bs = str.getBytes("gb18030");
                    break;
                case 2:
                    bs = str.getBytes("big5");
                    break;
                case 3:
                    bs = str.getBytes("cp949");
                    break;
                default:
                    bs = str.getBytes("gb2312");
                }
            } catch (UnsupportedEncodingException var5) {
                var5.printStackTrace();
            }

            for(int i = 0; i < bs.length; ++i) {
                this.Command.add(Byte.valueOf(bs[i]));
            }
        }

    }*/

    public void addGap(int gap) {
        String str = "GAP " + gap + " mm," + 0 + " mm\r\n";
        this.addStrToCommand(str);
    }

    public void addSize(int width, int height) {
        String str = "SIZE " + width + " mm," + height + " mm\r\n";
        this.addStrToCommand(str);
    }

    public void addCashdrwer(LabelCommand.FOOT m, int t1, int t2) {
        String str = "CASHDRAWER " + m.getValue() + "," + t1 + "," + t2 + "\r\n";
        this.addStrToCommand(str);
    }

    public void addOffset(int offset) {
        String str = "OFFSET " + offset + " mm\r\n";
        this.addStrToCommand(str);
    }

    public void addSpeed(LabelCommand.SPEED speed) {
        String str = "SPEED " + speed.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addDensity(LabelCommand.DENSITY density) {
        String str = "DENSITY " + density.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addDirection(LabelCommand.DIRECTION direction, LabelCommand.MIRROR mirror) {
        String str = "DIRECTION " + direction.getValue() + ',' + mirror.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addReference(int x, int y) {
        String str = "REFERENCE " + x + "," + y + "\r\n";
        this.addStrToCommand(str);
    }

    public void addShif(int shift) {
        String str = "SHIFT " + shift + "\r\n";
        this.addStrToCommand(str);
    }

    public void addCls() {
        String str = "CLS\r\n";
        this.addStrToCommand(str);
    }

    public void addFeed(int dot) {
        String str = "FEED " + dot + "\r\n";
        this.addStrToCommand(str);
    }

    public void addBackFeed(int dot) {
        String str = "BACKFEED " + dot + "\r\n";
        this.addStrToCommand(str);
    }

    public void addFormFeed() {
        String str = "FORMFEED\r\n";
        this.addStrToCommand(str);
    }

    public void addHome() {
        String str = "HOME\r\n";
        this.addStrToCommand(str);
    }

    public void addPrint(int m, int n) {
        String str = "PRINT " + m + "," + n + "\r\n";
        this.addStrToCommand(str);
    }

    public void addPrint(int m) {
        String str = "PRINT " + m + "\r\n";
        this.addStrToCommand(str);
    }

    public void addCodePage(LabelCommand.CODEPAGE page) {
        String str = "CODEPAGE " + page.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addSound(int level, int interval) {
        String str = "SOUND " + level + "," + interval + "\r\n";
        this.addStrToCommand(str);
    }

    public void addLimitFeed(int n) {
        String str = "LIMITFEED " + n + "\r\n";
        this.addStrToCommand(str);
    }

    public void addSelfTest() {
        String str = "SELFTEST\r\n";
        this.addStrToCommand(str);
    }

    public void addBar(int x, int y, int width, int height) {
        String str = "BAR " + x + "," + y + "," + width + "," + height + "\r\n";
        this.addStrToCommand(str);
    }

   /* public void addText(int x, int y, LabelCommand.FONTTYPE font, LabelCommand.ROTATION rotation, LabelCommand.FONTMUL Xscal, LabelCommand.FONTMUL Yscal, String text) {
        String str = "TEXT " + x + "," + y + ",\"" + font.getValue() + "\"," + rotation.getValue() + "," + Xscal.getValue() + "," + Yscal.getValue() + ",\"" + text + "\"\r\n";
        this.addStrToCommand(str, font);
    }*/

    public void add1DBarcode(int x, int y, LabelCommand.BARCODETYPE type, int height, LabelCommand.READABEL readable, LabelCommand.ROTATION rotation, String content) {
        int narrow = 2;
        int width = 2;
        String str = "BARCODE " + x + "," + y + ",\"" + type.getValue() + "\"," + height + "," + readable.getValue() + "," + rotation.getValue() + "," + narrow + "," + width + ",\"" + content + "\"\r\n";
        this.addStrToCommand(str);
    }

    public void add1DBarcode(int x, int y, LabelCommand.BARCODETYPE type, int height, LabelCommand.READABEL readable, LabelCommand.ROTATION rotation, int narrow, int width, String content) {
        String str = "BARCODE " + x + "," + y + ",\"" + type.getValue() + "\"," + height + "," + readable.getValue() + "," + rotation.getValue() + "," + narrow + "," + width + ",\"" + content + "\"\r\n";
        this.addStrToCommand(str);
    }

    public void addBox(int x, int y, int xend, int yend, int thickness) {
        String str = "BOX " + x + "," + y + "," + xend + "," + yend + "," + thickness + "\r\n";
        this.addStrToCommand(str);
    }

    public void addBitmap(int x, int y, LabelCommand.BITMAP_MODE mode, int nWidth, Bitmap b) {
        if(b != null) {
            int width = (nWidth + 7) / 8 * 8;
            int height = b.getHeight() * width / b.getWidth();
            Log.d("BMP", "bmp.getWidth() " + b.getWidth());
            Bitmap grayBitmap = Utils.grayBitmap(b);
            Bitmap rszBitmap = Utils.resizeImage(grayBitmap, width, height);
            byte[] src = Utils.bitmapToBWPix(rszBitmap);
            height = src.length / width;
            width /= 8;
            String str = "BITMAP " + x + "," + y + "," + width + "," + height + "," + mode.getValue() + ",";
            this.addStrToCommand(str);
            byte[] codecontent = Utils.pixToLabelCmd(src);

            for(int k = 0; k < codecontent.length; ++k) {
                this.commandList.add(Byte.valueOf(codecontent[k]));
            }

            Log.d("LabelCommand", "codecontent" + codecontent);
        }

    }

    public void addBitmapByMethod(int x, int y, LabelCommand.BITMAP_MODE mode, int nWidth, Bitmap b) {
        if(b != null) {
            int width = (nWidth + 7) / 8 * 8;
            int height = b.getHeight() * width / b.getWidth();
            Log.d("BMP", "bmp.getWidth() " + b.getWidth());
            Bitmap rszBitmap = Utils.resizeImage(b, width, height);
            Bitmap grayBitmap = Utils.grayBitmap(rszBitmap);
            byte[] src = Utils.bitmapToBWPix(grayBitmap);
            height = src.length / width;
            width /= 8;
            String str = "BITMAP " + x + "," + y + "," + width + "," + height + "," + mode.getValue() + ",";
            this.addStrToCommand(str);
            byte[] codecontent = Utils.pixToLabelCmd(src);

            for(int k = 0; k < codecontent.length; ++k) {
                this.commandList.add(Byte.valueOf(codecontent[k]));
            }

            Log.d("LabelCommand", "codecontent" + codecontent);
        }

    }

    public void addBitmap(int x, int y, int nWidth, Bitmap bmp) {
        if(bmp != null) {
            int width = (nWidth + 7) / 8 * 8;
            int height = bmp.getHeight() * width / bmp.getWidth();
            Log.d("BMP", "bmp.getWidth() " + bmp.getWidth());
            Bitmap rszBitmap = Utils.resizeImage(bmp, width, height);
            byte[] bytes = Utils.printTscDraw(x, y, LabelCommand.BITMAP_MODE.OVERWRITE, rszBitmap);

            for(int i = 0; i < bytes.length; ++i) {
                this.commandList.add(Byte.valueOf(bytes[i]));
            }

            this.addStrToCommand("\r\n");
        }

    }

    public void addErase(int x, int y, int xwidth, int yheight) {
        String str = "ERASE " + x + "," + y + "," + xwidth + "," + yheight + "\r\n";
        this.addStrToCommand(str);
    }

    public void addReverse(int x, int y, int xwidth, int yheight) {
        String str = "REVERSE " + x + "," + y + "," + xwidth + "," + yheight + "\r\n";
        this.addStrToCommand(str);
    }

    public void addQRCode(int x, int y, LabelCommand.EEC level, int cellwidth, LabelCommand.ROTATION rotation, String data) {
        String str = "QRCODE " + x + "," + y + "," + level.getValue() + "," + cellwidth + "," + 'A' + "," + rotation.getValue() + ",\"" + data + "\"\r\n";
        this.addStrToCommand(str);
    }

    public ArrayList<Byte> getCommand() {
        return this.commandList;
    }

    public void addQueryPrinterType() {
        new String();
        String str = "~!T\r\n";
        this.addStrToCommand(str);
    }

    public void addQueryPrinterStatus() {
        this.commandList.add(Byte.valueOf((byte) 27));
        this.commandList.add(Byte.valueOf((byte)33));
        this.commandList.add(Byte.valueOf((byte)63));
    }

    public void addResetPrinter() {
        this.commandList.add(Byte.valueOf((byte)27));
        this.commandList.add(Byte.valueOf((byte)33));
        this.commandList.add(Byte.valueOf((byte)82));
    }

    public void addQueryPrinterLife() {
        String str = "~!@\r\n";
        this.addStrToCommand(str);
    }

    public void addQueryPrinterMemory() {
        String str = "~!A\r\n";
        this.addStrToCommand(str);
    }

    public void addQueryPrinterFile() {
        String str = "~!F\r\n";
        this.addStrToCommand(str);
    }

    public void addQueryPrinterCodePage() {
        String str = "~!I\r\n";
        this.addStrToCommand(str);
    }

    public void addPeel(ENABLE enable) {
        if(enable.getValue() == 0) {
            String str = "SET PEEL " + enable.getValue() + "\r\n";
            this.addStrToCommand(str);
        }

    }

    public void addTear(ENABLE enable) {
        String str = "SET TEAR " + enable.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addCutter(ENABLE enable) {
        String str = "SET CUTTER " + enable.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addCutterBatch() {
        String str = "SET CUTTER BATCH\r\n";
        this.addStrToCommand(str);
    }

    public void addCutterPieces(short number) {
        String str = "SET CUTTER " + number + "\r\n";
        this.addStrToCommand(str);
    }

    public void addReprint(ENABLE enable) {
        String str = "SET REPRINT " + enable.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addPrintKey(ENABLE enable) {
        String str = "SET PRINTKEY " + enable.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addPrintKey(int m) {
        String str = "SET PRINTKEY " + m + "\r\n";
        this.addStrToCommand(str);
    }

    public void addPartialCutter(ENABLE enable) {
        String str = "SET PARTIAL_CUTTER " + enable.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addQueryPrinterStatus(LabelCommand.RESPONSE_MODE mode) {
        String str = "SET RESPONSE " + mode.getValue() + "\r\n";
        this.addStrToCommand(str);
    }

    public void addUserCommand(String command) {
        this.addStrToCommand(command);
    }

    public static enum RESPONSE_MODE {
        ON("ON"),
        OFF("OFF"),
        BATCH("BATCH");

        private final String value;

        private RESPONSE_MODE(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public static enum BARCODETYPE {
        CODE128("128"),
        CODE128M("128M"),
        EAN128("EAN128"),
        ITF25("25"),
        ITF25C("25C"),
        CODE39("39"),
        CODE39C("39C"),
        CODE39S("39S"),
        CODE93("93"),
        EAN13("EAN13"),
        EAN13_2("EAN13+2"),
        EAN13_5("EAN13+5"),
        EAN8("EAN8"),
        EAN8_2("EAN8+2"),
        EAN8_5("EAN8+5"),
        CODABAR("CODA"),
        POST("POST"),
        UPCA("UPCA"),
        UPCA_2("UPCA+2"),
        UPCA_5("UPCA+5"),
        UPCE("UPCE13"),
        UPCE_2("UPCE13+2"),
        UPCE_5("UPCE13+5"),
        CPOST("CPOST"),
        MSI("MSI"),
        MSIC("MSIC"),
        PLESSEY("PLESSEY"),
        ITF14("ITF14"),
        EAN14("EAN14");

        private final String value;

        private BARCODETYPE(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public static enum EEC {
        LEVEL_L("L"),
        LEVEL_M("M"),
        LEVEL_Q("Q"),
        LEVEL_H("H");

        private final String value;

        private EEC(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public static enum ROTATION {
        ROTATION_0(0),
        ROTATION_90(90),
        ROTATION_180(180),
        ROTATION_270(270);

        private final int value;

        private ROTATION(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum FONTTYPE {
        FONT_1("1"),
        FONT_2("2"),
        FONT_3("3"),
        FONT_4("4"),
        FONT_5("5"),
        FONT_6("6"),
        FONT_7("7"),
        FONT_8("8"),
        FONT_9("9"),
        FONT_10("10"),
        SIMPLIFIED_CHINESE("TSS24.BF2"),
        TRADITIONAL_CHINESE("TST24.BF2"),
        KOREAN("K");

        private final String value;

        private FONTTYPE(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public static enum FONTMUL {
        MUL_1(1),
        MUL_2(2),
        MUL_3(3),
        MUL_4(4),
        MUL_5(5),
        MUL_6(6),
        MUL_7(7),
        MUL_8(8),
        MUL_9(9),
        MUL_10(10);

        private final int value;

        private FONTMUL(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum CODEPAGE {
        PC437(437),
        PC850(850),
        PC852(852),
        PC860(860),
        PC863(863),
        PC865(865),
        WPC1250(1250),
        WPC1252(1252),
        WPC1253(1253),
        WPC1254(1254);

        private final int value;

        private CODEPAGE(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum MIRROR {
        NORMAL(0),
        MIRROR(1);

        private final int value;

        private MIRROR(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum DIRECTION {
        FORWARD(0),
        BACKWARD(1);

        private final int value;

        private DIRECTION(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum DENSITY {
        DNESITY0(0),
        DNESITY1(1),
        DNESITY2(2),
        DNESITY3(3),
        DNESITY4(4),
        DNESITY5(5),
        DNESITY6(6),
        DNESITY7(7),
        DNESITY8(8),
        DNESITY9(9),
        DNESITY10(10),
        DNESITY11(11),
        DNESITY12(12),
        DNESITY13(13),
        DNESITY14(14),
        DNESITY15(15);

        private final int value;

        private DENSITY(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum BITMAP_MODE {
        OVERWRITE(0),
        OR(1),
        XOR(2);

        private final int value;

        private BITMAP_MODE(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum READABEL {
        DISABLE(0),
        EANBEL(1);

        private final int value;

        private READABEL(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum SPEED {
        SPEED1DIV5(1.5F),
        SPEED2(2.0F),
        SPEED3(3.0F),
        SPEED4(4.0F);

        private final float value;

        private SPEED(float value) {
            this.value = value;
        }

        public float getValue() {
            return this.value;
        }
    }

    public static enum FOOT {
        F2(0),
        F5(1);

        private final int value;

        private FOOT(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static enum ENABLE {
        OFF(0),
        ON(1);

        private final int value;

        private ENABLE(int value) {
            this.value = value;
        }

        public byte getValue() {
            return (byte)this.value;
        }
    }

    public static enum STATUS {
        PRINTER_STATUS(1),
        PRINTER_OFFLINE(2),
        PRINTER_ERROR(3),
        PRINTER_PAPER(4);

        private final int value;

        private STATUS(int value) {
            this.value = value;
        }

        public byte getValue() {
            return (byte)this.value;
        }
    }
}