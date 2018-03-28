package com.example.echo.coolweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.echo.coolweather.db.City;
import com.example.echo.coolweather.db.County;
import com.example.echo.coolweather.db.Province;
import com.example.echo.coolweather.util.HttpUtil;
import com.example.echo.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Echo on 2018/3/26.
 */

public class ChooseAreaFragment extends Fragment {

    private static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /*
    省列表
     */
    private List<Province> provinceList;

    /*
    市列表
     */
    private List<City> cityList;

    /*
    县列表
     */
    private List<County> countyList;

    /*
    选中的省
     */
    private Province selectedProvince;

    /*
    选中的市
     */
    private City selectedCity;

    /*
    当前选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);  //选择当前选择的省或市
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvince();
                }
            }
        });
        queryProvince();

    }

    /*
    查询全国所有的省，优先从数据库查询，如果没有再到服务器上去查询
     */
    private void queryProvince() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
                //Log.d(TAG, "queryProvince: provinceGetDataFromDatabase.");
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
            //Log.d(TAG, "queryProvince: getDataFromService.");
        }
    }

    /*
    查询省内所有的市，优先从数据库查询，如果没有再到服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        Log.d(TAG, "修改title为省名");
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId = ?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        Log.d(TAG, "queryCities: 获取城市列表");
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            Log.d(TAG, "更新城市数据");
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            Log.d(TAG, "将省转换到市。");
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /*
    查询市内所有的县，优先到数据库查询，如果没有再到服务器上查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",
                String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" +cityCode;
            queryFromServer(address, "county");
        }

    }

    /*
    根据传入地址和类型到服务器上查询市县数据
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                    Log.d(TAG, "进度条: 获取省级数据。");

                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, 
                            selectedProvince.getId());
                    Log.d(TAG, "onResponse: 从服务器获取市级数据");
                } else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText, 
                            selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            Log.d(TAG, "进度条: 关闭进度条");
                            if ("province".equals(type)) {
                                queryProvince();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    /*
    显示进度条
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /*
    隐藏进度条
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
