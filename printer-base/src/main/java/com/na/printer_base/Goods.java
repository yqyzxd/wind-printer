package com.na.printer_base;

/**
 * Created by wind on 2018/5/3.
 */

public class Goods {
    private String name;
    private int num;
    private String serialNumber;//商品编号

    private float price;//单价
    private float totalPrice;//price*num

    private boolean isFreeOne;//免单一杯
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public float getTotalPrice() {
        return price*num;
    }

    public void setTotalPrice(float totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
