package com.shellever.plugin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.shellever.plugin.common.Common;

public class MainActivity extends AppCompatActivity {

    private TextView mInfoTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInfoTv = findViewById(R.id.tv_dev_info);

        Common common = new Common(this);
        String devinfo = common.getDeviceInfo();
        mInfoTv.setText(devinfo);
    }
}
