package com.wind.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.na.printer_base.Goods;
import com.na.printer_base.Lable;
import com.na.printer_base.Ticket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btn;
    Spinner spinner;
    Bitmap logoBitmap, qrcodeBitmap;
    ImageView iv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv=findViewById(R.id.iv);
        spinner = findViewById(R.id.spinner);
        btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deviceName = spinner.getSelectedItem().toString();
                //PrinterHelper.getInstance(MainActivity.this).printTicket(deviceName,buildTicket());
                Lable lable=new Lable();
                lable.setLabelBitmap(getLabelBitmap());
                PrinterHelper.getInstance(MainActivity.this).printLable(deviceName,lable);
            }
        });
        InputStream is = null;
        InputStream q_is = null;
        try {
            is = getAssets().open("ticket_logo_.jpg");
            q_is = getAssets().open("qrcode_200.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logoBitmap = BitmapFactory.decodeStream(is);
        qrcodeBitmap = compressImage(BitmapFactory.decodeStream(q_is));
        logoBitmap = compressImage(BitmapFactory.decodeStream(is));
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Collection<UsbDevice> collectionDevs = manager.getDeviceList().values();
        final List<UsbDevice> devs = new ArrayList<>(collectionDevs);
        final String[] strDev = new String[devs.size()];
        for (int i = 0; i < strDev.length; i++) {
            UsbDevice d = devs.get(i);
            strDev[i] = d.getDeviceName();
            System.out.println("vid:" + d.getVendorId() + "--pid:" + d.getProductId());
            // strDev[i] = String.format("USB[%04X:%04X]: id=%d, %s", d.getVendorId(), d.getProductId(), d.getDeviceId(), d.getDeviceName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, strDev);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("onItemSelected:" + position);
                String deviceName = strDev[(position)];
                PrinterHelper.getInstance(MainActivity.this).connectDevice(deviceName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("onNothingSelected");
            }
        });


        int i = 1 & 255;//255->1111 1111
        System.out.println("1&255=" + i);

    }

    private Ticket buildTicket() {
        List<Goods> goodsList = new ArrayList<>();
        Goods latte = new Goods();
        latte.setName("拿铁");
        latte.setNum(16);
        latte.setPrice(15);
        latte.setSerialNumber("533434");

        Goods cappuccino = new Goods();
        cappuccino.setName("卡布奇诺");
        cappuccino.setNum(1);
        cappuccino.setSerialNumber("533434");
        cappuccino.setPrice(115);

        Goods white = new Goods();
        white.setName("白咖");
        white.setNum(1);
        white.setSerialNumber("533434");
        white.setPrice(15);

        goodsList.add(latte);
        goodsList.add(cappuccino);
        goodsList.add(white);


        //ZqPrinterHelper.getInstance().printTicket(goodsList);
        Ticket ticket = new Ticket();
        ticket.setGoodsList(goodsList);
        ticket.setMerchantName("MarryU 爱情咖啡馆");
        ticket.setDiscountAmount("15.5");
        ticket.setQrcodeBitmap(qrcodeBitmap);
        ticket.setMerchantLogo(logoBitmap);
        ticket.setTel("Tel:110");
        ticket.setOrderNo("97597394789753");
        ticket.setCustomer("顾客：wind");
        ticket.setCashier("wind");
        ticket.setWelcome("谢谢惠顾！欢迎下次再来");
        return ticket;
    }


    private static Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        boolean flag = true;
        while (baos.toByteArray().length / 1024 > 10 && flag) {  //循环判断如果压缩后图片是否大于500kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
            if (options < 20) {
                flag = false;
            }
        }
        System.out.println("baos.toByteArray().length:" + baos.toByteArray().length);
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    private Bitmap getLabelBitmap() {


        int width = 720;
        int height = (int) (width * 0.67f);
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int density = b.getDensity();
        System.out.println("density:" + density);
        //b.setDensity(440);
        Canvas canvas = new Canvas();
        canvas.setBitmap(b);
        View labelView = getLayoutInflater().inflate(R.layout.lable_layout, null, false);


        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int hieghtMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        labelView.measure(widthMeasureSpec, hieghtMeasureSpec);
        int measuredWidth = labelView.getMeasuredWidth();
        int measuredlHeight = labelView.getMeasuredHeight();
        labelView.layout(0, 0, measuredWidth, measuredlHeight);
        labelView.draw(canvas);
        width = 720;
        height = (int) (width * 0.67f);
        b = Bitmap.createScaledBitmap(b, width, height, true);
        iv.setImageBitmap(b);
       //Bitmap b=BitmapFactory.decodeResource(getResources(),R.drawable.code);
        return b;
    }
}
