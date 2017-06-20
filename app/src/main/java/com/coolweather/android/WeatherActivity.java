package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.db.SelectedCounty;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;

    private Button navButton;

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sprotText;

    private ImageView bingPicImg;

    public SwipeRefreshLayout swipeRefresh;

    private String mWeatherId;

    private ListView listView;

    private Button addCounty;

    private ArrayAdapter<String> adapter;

    private List<String> currentCounties = new ArrayList<>() ;

    private List<SelectedCounty> selectedCountyList ;

    private boolean isRefresh = false;

    private Spinner spinner;

    private List<String> updateTime = new ArrayList<>();

    private ArrayAdapter<String> updateTimeAdapter;

    private int updateHour;

    private String picAddress ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("cool","进入WeatherActivity");
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);



        //初始化控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sprotText = (TextView) findViewById(R.id.sport_text);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        addCounty = (Button) findViewById(R.id.add_county);
        listView = (ListView) findViewById(R.id.current_county);
        spinner = (Spinner) findViewById(R.id.update_time);

        updateTime.add("不自动更新");
        updateTime.add("1小时");
        updateTime.add("2小时");
        updateTime.add("4小时");
        updateTime.add("8小时");
        updateTime.add("16小时");

        updateTimeAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,updateTime);
        updateTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(updateTimeAdapter);





        selectedCountyList = DataSupport.findAll(SelectedCounty.class);
        for(SelectedCounty selectedCounty:selectedCountyList){
            currentCounties.add(selectedCounty.getCityName());
        }
        adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,currentCounties);
        listView.setAdapter(adapter);
        

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);

        spinner.setSelection(prefs.getInt("updateTime",0));







        if(weatherString!=null){

            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            //无缓存时去服务器查询天气
            Log.d("coolweather","从服务器请求数据");
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);

        }


        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){

            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
                loadWeatherPic(picAddress);
            }
        });
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        updateHour = 0 ;
                        break;
                    case 1:
                        updateHour = 1 ;
                        break;
                    case 2:
                        updateHour = 2 ;
                        break;
                    case 3:
                        updateHour = 4;
                        break;
                    case 4:
                        updateHour = 8 ;
                        break;
                    case 5:
                        updateHour = 16;
                        break;
                }


                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putInt("updateTime",position);
                editor.apply();
                startUpdateService(updateHour);
                Log.d("cool","updateHour的值为:"+updateHour+"position:"+position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        addCounty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.remove("weather");
                editor.apply();

                Intent intent = new Intent(WeatherActivity.this,MainActivity.class);
                startActivityForResult(intent,1);



            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mWeatherId = selectedCountyList.get(position).getWeatherId();
                requestWeather(mWeatherId);
                drawerLayout.closeDrawers();
            }
        });

        listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("确认删除?");
                menu.add(0, 0, 0, "删除");
            }


        });


    }


    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        //info.id得到listview中选择的条目绑定的id
        final int id = info.position;
        switch (item.getItemId()) {
            case 0:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("cool","这是菜单项"+id);
                        DataSupport.deleteAll(SelectedCounty.class,"cityName=?",currentCounties.get(id));
                        selectedCountyList.remove(id);
                        currentCounties.remove(id);
                        requestWeather(selectedCountyList.get(0).getWeatherId());
                        adapter.notifyDataSetChanged();
                    }
                });


                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    private void loadWeatherPic(final String picAddress){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(WeatherActivity.this).load(picAddress).into(bingPicImg);
            }
        });
    }



    /**
     * 根据天气ID请求城市天气信息
     * @param weatherId
     */
    public void requestWeather(final String weatherId) {

        Log.d("cool","执行方法:requestWeather");

        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=90749affd26b49d3a5905e0a3b0d0328";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }

                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);


               for(String s:currentCounties){
                   if(s.equals(weather.basic.cityName)){
                       isRefresh = true;
                       Log.d("cool","存在该字段");
                   }
               }

                if(!isRefresh) {
                    final SelectedCounty selectedCounty2 = new SelectedCounty();
                    selectedCounty2.setCityName(weather.basic.cityName);
                    selectedCounty2.setWeatherId(weather.basic.weatherId);
                    selectedCounty2.save();
                }







                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather !=null && "ok".equals(weather.status)){

                            adapter.notifyDataSetChanged();
                            listView.setSelection(0);

                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            selectedCountyList = DataSupport.findAll(SelectedCounty.class);
                            if(selectedCountyList.size()>0){
                                currentCounties.clear();
                                for(SelectedCounty selectedCounty1:selectedCountyList){
                                    currentCounties.add(selectedCounty1.getCityName());
                                    Log.d("cool","currentCounties里有"+currentCounties.size()+"个对象.");
                                }
                            }else {
                                currentCounties.add("无");
                            }

                            showWeatherInfo(weather);

                        }else {
                            Log.d("cool",weather.status);
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    /**
     * 处理并展示Weather实体类中的数据
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        final String newPicAddress;
        Log.d("cool","执行方法:showWeatherInfo");
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature+"℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        if(weatherInfo.indexOf("晴")>=0){
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/sun");

        }else if(weatherInfo.indexOf("雨")>=0) {
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/rain");
        }else if(weatherInfo.indexOf("风")>=0){
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/wind");

        }else if(weatherInfo.indexOf("雾")>=0){
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/fog");

        }else if(weatherInfo.indexOf("霾")>=0){
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/haze");

        }else if(weatherInfo.indexOf("雪")>=0){
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/snow");

        }else if(weatherInfo.indexOf("阴")>=0){
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/overcast");
        }else if(weatherInfo.indexOf("云")>=0){
            newPicAddress = new String("android.resource://com.coolweather.android/drawable/cloud");
        }else {
            newPicAddress = picAddress;
        }

        loadWeatherPic(newPicAddress);




        for(Forecast forecast:weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);

        }

        if(weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        if(weather !=null && "ok".equals(weather.status)){

            startUpdateService(updateHour);

        }else {
            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
        }


        String comfort = "舒适度:"+weather.suggestion.comfort.info;
        String carWash = "洗车指数:"+weather.suggestion.carWash.info;
        String sport = "运动建议:"+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sprotText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                if (resultCode == RESULT_OK){
                    finish();
                }
                break;
            default:
                break;
        }
    }

    protected void startUpdateService(int updateHour){
        Intent intent = new Intent(this,AutoUpdateService.class);
        intent.putExtra("updateTime",updateHour);

        startService(intent);
    }
}
