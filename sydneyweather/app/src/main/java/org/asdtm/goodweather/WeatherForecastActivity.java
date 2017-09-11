package org.asdtm.goodweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.asdtm.goodweather.adapter.WeatherForecastAdapter;
import org.asdtm.goodweather.model.WeatherForecast;
import org.asdtm.goodweather.utils.AppPreference;
import org.asdtm.goodweather.utils.Constants;
import org.asdtm.goodweather.utils.LanguageUtil;
import org.asdtm.goodweather.utils.PreferenceUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WeatherForecastActivity extends BaseActivity {

    private final String TAG = "WeatherForecastActivity";
    public static  String ACTION_MODE = "action_mode";

    private List<WeatherForecast> mWeatherForecastList;
    private ConnectionDetector mConnectionDetector;
    private RecyclerView mRecyclerView;
    private static Handler mHandler;
    private ProgressDialog mGetWeatherProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((GoodWeatherApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forecast);

        mConnectionDetector = new ConnectionDetector(this);
        mWeatherForecastList = new ArrayList<>();
        mGetWeatherProgress = getProgressDialog();

        mRecyclerView = (RecyclerView) findViewById(R.id.forecast_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case Constants.TASK_RESULT_ERROR:
                        Toast.makeText(WeatherForecastActivity.this,
                                       R.string.toast_parse_error,
                                       Toast.LENGTH_SHORT).show();
                        setVisibleUpdating(false);
                        break;
                    case Constants.PARSE_RESULT_ERROR:
                        Toast.makeText(WeatherForecastActivity.this,
                                       R.string.toast_parse_error,
                                       Toast.LENGTH_SHORT).show();
                        setVisibleUpdating(false);
                        break;
                    case Constants.PARSE_RESULT_SUCCESS:
                        setVisibleUpdating(false);
                        updateUI();
                        if (!mWeatherForecastList.isEmpty()) {
                            AppPreference.saveWeatherForecast(WeatherForecastActivity.this,
                                                              mWeatherForecastList);
                        }
                        break;
                }
            }
        };

    }

    private void updateUI() {
        ImageView android = (ImageView) findViewById(R.id.android);
        if (mWeatherForecastList.size() < 5) {
            mRecyclerView.setVisibility(View.INVISIBLE);
            android.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            android.setVisibility(View.GONE);
        }
        WeatherForecastAdapter adapter = new WeatherForecastAdapter(this,
                                                                    mWeatherForecastList,
                                                                    getSupportFragmentManager());
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWeatherForecastList.isEmpty()) {
            mWeatherForecastList = AppPreference.loadWeatherForecast(this);
        }
        getWeather();
        updateUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.weather_forecast_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_forecast_refresh:
                if (mConnectionDetector.isNetworkAvailableAndConnected()) {
                    getWeather();
                    setVisibleUpdating(true);
                } else {
                    Toast.makeText(WeatherForecastActivity.this,
                                   R.string.connection_not_found,
                                   Toast.LENGTH_SHORT).show();
                }
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setVisibleUpdating(boolean visible) {
        if (visible) {
            mGetWeatherProgress.show();
        } else {
            mGetWeatherProgress.cancel();
        }
    }

    private void getWeather() {
        if (!mWeatherForecastList.isEmpty()) {
            mWeatherForecastList.clear();
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if(getIntent().getIntExtra(ACTION_MODE,-1) == 1){
                    SharedPreferences pref = getSharedPreferences(Constants.APP_SETTINGS_NAME, 0);
                    String latitude = pref.getString(Constants.APP_SETTINGS_LATITUDE, "33.51");
                    String longitude = pref.getString(Constants.APP_SETTINGS_LONGITUDE, "151.12");
                    String locale = LanguageUtil.getLanguageName(PreferenceUtil.getLanguage(WeatherForecastActivity.this));
                    String units = AppPreference.getTemperatureUnit(WeatherForecastActivity.this);

                    String requestResult = "";
                    HttpURLConnection connection = null;
                    try {
                        String urle = Uri.parse("http://api.openweathermap.org/data/2.5/forecast?APPID=5ad5e7c716b9c1c77790deb8fc72c62e&q=Sydney").toString();
                        URL url = new URL(urle); //getWeatherForecastUrl(Constants.WEATHER_FORECAST_ENDPOINT, latitude, longitude, units, locale);
                        connection = (HttpURLConnection) url.openConnection();

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                            InputStream inputStream = connection.getInputStream();

                            int bytesRead;
                            byte[] buffer = new byte[1024];
                            while ((bytesRead = inputStream.read(buffer)) > 0) {
                                byteArray.write(buffer, 0, bytesRead);
                            }
                            byteArray.close();
                            requestResult = byteArray.toString();
                            AppPreference.saveLastUpdateTimeMillis(WeatherForecastActivity.this);
                        }

                    } catch (IOException e) {
                        mHandler.sendEmptyMessage(Constants.TASK_RESULT_ERROR);
                        Log.e(TAG, "IOException: " + requestResult);
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                    parseWeatherForecast(requestResult);
                }else if(getIntent().getIntExtra(ACTION_MODE,-1) == 0){

                    String requestResult = getJson("weather.json",WeatherForecastActivity.this);
                    AppPreference.saveLastUpdateTimeMillis(WeatherForecastActivity.this);
                    parseWeatherForecast(requestResult);
                }
            }
        });
        t.start();
    }

    public static String getJson(String fileName,Context context) {
        //将json数据变成字符串
        StringBuilder stringBuilder = new StringBuilder();
        try {
            //获取assets资源管理器
            AssetManager assetManager = context.getAssets();
            //通过管理器打开文件并读取
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private void parseWeatherForecast(String data) {
        try {
            if (!mWeatherForecastList.isEmpty()) {
                mWeatherForecastList.clear();
            }
            Date oldD = null;

            JSONObject jsonObject = new JSONObject(data);
            JSONArray listArray = jsonObject.getJSONArray("list");

            int listArrayCount = listArray.length();
            for (int i = 0; i < listArrayCount; i++) {
                WeatherForecast weatherForecast = new WeatherForecast();
                JSONObject listItem = listArray.getJSONObject(i);
                weatherForecast.setDateTime(listItem.getLong("dt"));

                String time = listItem.getString("dt_txt");
                SimpleDateFormat sdf  =   new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
                Date date = sdf.parse(time);
                if(isSameDate(date,oldD)){
                    oldD = date;
                    continue;
                }
                oldD = date;
                JSONObject mainObject = listItem.getJSONObject("main");
                weatherForecast.setPressure(mainObject.getString("pressure"));
                weatherForecast.setHumidity(mainObject.getString("humidity"));

                JSONObject windObject = listItem.getJSONObject("wind");
                weatherForecast.setWindSpeed(windObject.getString("speed"));
                weatherForecast.setWindDegree(windObject.getString("deg"));

                JSONObject cloudObject = listItem.getJSONObject("clouds");
                weatherForecast.setCloudiness(cloudObject.getString("all"));

                if (listItem.has("rain")) {
                    JSONObject rainObject = listItem.getJSONObject("rain");
                    weatherForecast.setRain(rainObject.getString("rain"));
                } else {
                    weatherForecast.setRain("0");
                }
                if (listItem.has("snow")) {
                    JSONObject snowObject = listItem.getJSONObject("snow");
                    weatherForecast.setSnow(snowObject.getString("snow"));
                } else {
                    weatherForecast.setSnow("0");
                }

                weatherForecast.setTemperatureMin(
                        Float.parseFloat(mainObject.getString("temp_min"))-273);
                weatherForecast.setTemperatureMax(
                        Float.parseFloat(mainObject.getString("temp_max"))-273);
              /*  weatherForecast.setTemperatureMorning(
                        Float.parseFloat(temperatureObject.getString("morn")));
                weatherForecast.setTemperatureDay(
                        Float.parseFloat(temperatureObject.getString("day")));
                weatherForecast.setTemperatureEvening(
                        Float.parseFloat(temperatureObject.getString("eve")));
                weatherForecast.setTemperatureNight(
                        Float.parseFloat(temperatureObject.getString("night")));*/

                JSONArray weatherArray = listItem.getJSONArray("weather");
                JSONObject weatherObject = weatherArray.getJSONObject(0);
                weatherForecast.setDescription(weatherObject.getString("description"));
                weatherForecast.setIcon(weatherObject.getString("icon"));

                mWeatherForecastList.add(weatherForecast);
            }
            mHandler.sendEmptyMessage(Constants.PARSE_RESULT_SUCCESS);
        } catch (JSONException e) {
            mHandler.sendEmptyMessage(Constants.TASK_RESULT_ERROR);
            e.printStackTrace();
        }catch (ParseException e2){
          e2.printStackTrace();
            mHandler.sendEmptyMessage(Constants.TASK_RESULT_ERROR);
        }
    }

    private static boolean isSameDate(Date date1, Date date2) {
        if(date1 == null || date2== null){
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        boolean isSameYear = cal1.get(Calendar.YEAR) == cal2
                .get(Calendar.YEAR);
        boolean isSameMonth = isSameYear
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
        boolean isSameDate = isSameMonth
                && cal1.get(Calendar.DAY_OF_MONTH) == cal2
                .get(Calendar.DAY_OF_MONTH);

        return isSameDate;
    }
}
