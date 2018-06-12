package com.na.printer_base;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Created by wind on 2018/5/3.
 */

public class Ticket {

    private String merchantName;
    private Bitmap merchantLogo;
    private Bitmap qrcodeBitmap;
    private String tel;

    private String orderNo;
    private String table;

    private String customer;
    private String cashier;//收银员
    private String cardNo;

    private String dateTime;

    private String splitLine;

    private String totalPrice;//商品总价
    private String needPayPrice;//应付商品总价
    private String cash;//用户给的现金
    private String change;//找零
    private String discountAmount;//优惠金额

    private String welcome;

    private int count;

    private List<Goods> goodsList;

    private boolean needOpenCashBox;//是否需要打开钱箱
    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getTel() {
        return tel+"\n";
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getCustomer() {
        return customer+"\n";
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSplitLine() {
        return splitLine;
    }

    public void setSplitLine(String splitLine) {
        this.splitLine = splitLine;
    }


    public String getWelcome() {
        return welcome;
    }

    public void setWelcome(String welcome) {
        this.welcome = welcome;
    }

    public List<Goods> getGoodsList() {
        return goodsList;
    }

    public void setGoodsList(List<Goods> goodsList) {
        this.goodsList = goodsList;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(String totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCash() {
        return cash;
    }

    public void setCash(String cash) {
        this.cash = cash;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public Bitmap getMerchantLogo() {
        return merchantLogo;
    }

    public void setMerchantLogo(Bitmap merchantLogo) {
        this.merchantLogo = merchantLogo;
    }

    public String getCashier() {
        return cashier;
    }

    public void setCashier(String cashier) {
        this.cashier = cashier;
    }

    public int getCount() {
        int num=0;
        for (Goods goods:goodsList){
            num+=goods.getNum();
        }
        return num;
    }

    public String getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(String discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getNeedPayPrice() {
        return needPayPrice;
    }

    public void setNeedPayPrice(String needPayPrice) {
        this.needPayPrice = needPayPrice;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Bitmap getQrcodeBitmap() {
        return qrcodeBitmap;
    }

    public void setQrcodeBitmap(Bitmap qrcodeBitmap) {
        this.qrcodeBitmap = qrcodeBitmap;
    }


    public boolean isNeedOpenCashBox() {
        return needOpenCashBox;
    }

    public void setNeedOpenCashBox(boolean needOpenCashBox) {
        this.needOpenCashBox = needOpenCashBox;
    }




}
