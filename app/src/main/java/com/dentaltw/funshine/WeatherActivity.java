package com.dentaltw.funshine;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dentaltw.funshine.models.DailyWeatherReport;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener{
    private static final int PERMISSION_FINE_LOCATION = 1112;
    final String URL_BASE = "http://samples.openweathermap.org/data/2.5/forecast";
    final String URL_COORD = "?lat=";
    final String URL_API_KEY = "&appid=b1b15e88fa797225412429c1c50c122a1";
    final private int PERMISSION_LOCATION = 111;

    final ArrayList<DailyWeatherReport> weatherReportList = new ArrayList();

    private ImageView weatherIcon;
    private ImageView weatherIconMini;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDescription;

    WeatherAdapter adapter;
    GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherIcon = (ImageView) findViewById(R.id.weatherIcon);
        weatherIconMini = (ImageView) findViewById(R.id.weatherIcon_mini);
        weatherDate = (TextView) findViewById(R.id.weatherDate);
        currentTemp = (TextView) findViewById(R.id.currentTemp);
        lowTemp = (TextView) findViewById(R.id.lowTemp);
        cityCountry = (TextView) findViewById(R.id.cityCountry);
        weatherDescription = (TextView) findViewById(R.id.weatherDescription);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.content_weather_reports);

        adapter = new WeatherAdapter(weatherReportList);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        googleApiClient = new GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .enableAutoManage(this, this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(AppIndex.API).build();
    }

    public void downloadWeatherData(Location location){
        final String fullCoords = URL_COORD + location.getLatitude()+ "&lon="+location.getLongitude();
        final String url = URL_BASE + fullCoords + URL_API_KEY;
        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    JSONObject city = response.getJSONObject("city");
                    String cityName = city.getString("name");
                    String country = city.getString("country");

                    JSONArray list = response.getJSONArray("list");

                    for(int x = 0;x<5;x++){
                        JSONObject obj = list.getJSONObject(x);
                        JSONObject main = obj.getJSONObject("main");
                        Double currentTemp = main.getDouble("temp");
                        Double maxTemp = main.getDouble("temp_max");
                        Double minTemp = main.getDouble("temp_min");

                        JSONArray weatherArr = obj.getJSONArray("weather");
                        JSONObject weather = weatherArr.getJSONObject(0);
                        String weatherType = weather.getString("main");

                        String rawDate = obj.getString("dt_txt");

                        DailyWeatherReport report = new DailyWeatherReport(cityName,country,currentTemp.intValue(),maxTemp.intValue(),minTemp.intValue(), weatherType, rawDate);
                        weatherReportList.add(report);
                    }

                    Log.v("JSON", cityName+country);
                }
                catch(JSONException e){
                    Log.v("JSON", "EXEC:"+e.getLocalizedMessage());
                }
                updateUI();
                adapter.notifyDataSetChanged();
                
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("FUN", "Err: "+error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
        }
        else{
            startLocationService();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        downloadWeatherData(location);
    }

    public void startLocationService(){
        try{
            com.google.android.gms.location.LocationListener l = null;
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, req, this);
        }
        catch(SecurityException  ex){

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode)
        {
            case PERMISSION_FINE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    startLocationService();
                }
                else{
                    Toast.makeText(this,"I can't run your location you denied permission", Toast.LENGTH_LONG);
                }
                break;
        }
    }

    public void updateUI(){
        if(weatherReportList.size()>0){
            DailyWeatherReport report = weatherReportList.get(0);

            switch (report.getWeather()){
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
            }
            weatherDate.setText("Today, May 1");
            currentTemp.setText(String.valueOf(report.getCurrentTemp()));
            lowTemp.setText(String.valueOf(report.getMinTemp()));
            cityCountry.setText(report.getCityName()+" "+report.getCountry());
            weatherDescription.setText(report.getWeather());
        }
    }

    public class WeatherAdapter extends RecyclerView.Adapter<WeatherReportViewHolder>{

        private ArrayList<DailyWeatherReport> list;

        public WeatherAdapter(ArrayList<DailyWeatherReport> list) {
            this.list = list;
        }

        @Override
        public WeatherReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_weather, parent, false);
            return new WeatherReportViewHolder(card);
        }

        @Override
        public void onBindViewHolder(WeatherReportViewHolder holder, int position) {
            DailyWeatherReport report = list.get(position);
            holder.updateUI(report);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    public class WeatherReportViewHolder extends RecyclerView.ViewHolder{
        private ImageView weatherIcon;
        private TextView weatherDay;
        private TextView weatherDescription;
        private TextView tempHigh;
        private TextView tempLow;

        public WeatherReportViewHolder(View itemView) {
            super(itemView);
            weatherIcon = (ImageView)itemView.findViewById(R.id.card_weather_icon);
            weatherDay = (TextView) itemView.findViewById(R.id.card_weather_day);
            weatherDescription = (TextView) itemView.findViewById(R.id.card_weather_description);
            tempHigh = (TextView) itemView.findViewById(R.id.card_weather_temp_high);
            tempLow = (TextView) itemView.findViewById(R.id.card_weather_temp_low);
        }
        public void updateUI(DailyWeatherReport report){
            weatherDay.setText(report.getFormattedDate());
            weatherDescription.setText(report.getWeather());
            tempHigh.setText(String.valueOf(report.getMaxTemp()));
            tempLow.setText(String.valueOf(report.getMinTemp()));

            switch (report.getWeather()){
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
            }
        }
    }
}