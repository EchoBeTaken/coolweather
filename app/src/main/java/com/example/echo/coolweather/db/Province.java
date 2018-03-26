package com.example.echo.coolweather.db;

import org.litepal.crud.DataSupport;

/**
 * Created by Echo on 2018/3/26.
 */

public class Province extends DataSupport {

    private int id;    //省id

    private String provinceName;  //省的名字

    private int provinceCode;     //省的代码

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}
