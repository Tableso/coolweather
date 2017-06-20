package com.coolweather.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.coolweather.android.gson.Weather;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/6/8 0008.
 */

public class CurrentCounty extends Fragment {

    private Button addCounty;

    private ListView currentCountyList;

    private ArrayAdapter<String> adapter;

    private List<String> dateList = new ArrayList<>();

    private Weather currentWeather;







    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.current_county,container,false);
        addCounty = (Button) view.findViewById(R.id.add_county);
        currentCountyList = (ListView) view.findViewById(R.id.current_county);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dateList);
        currentCountyList.setAdapter(adapter);
        return view;

    }


    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        addCounty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        dateList.add("成都");
    }



}
