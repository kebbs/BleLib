package com.junkchen.blelib.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class BleScanActivity extends AppCompatActivity {

    //Layout view
    private Button btn_scanBle;
    private TextView txtv_connNum;
    private ListView lstv_devList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);
        initView();
        initAdapter();
    }

    private void initView() {
        btn_scanBle = (Button) findViewById(R.id.btn_connect);
        txtv_connNum = (TextView) findViewById(R.id.txtv_connNum);
        lstv_devList = (ListView) findViewById(R.id.lstv_devList);


    }

    private void initAdapter() {

    }
}
