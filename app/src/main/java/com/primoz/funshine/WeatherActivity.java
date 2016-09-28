package com.primoz.funshine;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.primoz.funshine.model.DailyWeatherReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast";
    final String URL_COORD = "/?lat="; //"/?lat=9.96878&lon=76.299";
    final String URL_UNIT = "&units=metric";
    final String URL_API_KEY = "&APPID=0c2132f0264d180a43926bebb297389a";

    private final int PERMISSION_LOCATION = 111;
    private GoogleApiClient googleApiClient;

    private ArrayList<DailyWeatherReport> dailyWeatherReports = new ArrayList<>();


    private ImageView weatherIconMini;
    private ImageView weatherIcon;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDescription;


    WeatherAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherIcon = (ImageView) findViewById(R.id.ivWeather);
        weatherIconMini = (ImageView) findViewById(R.id.ivWeatherMini);
        weatherDate = (TextView) findViewById(R.id.tvDate);
        currentTemp = (TextView) findViewById(R.id.tvCurrent);
        lowTemp = (TextView) findViewById(R.id.tvSecond);
        cityCountry = (TextView) findViewById(R.id.tvCityCountry);
        weatherDescription = (TextView) findViewById(R.id.tvWeatherDescription);


        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.content_weather_reports);

        adapter = new WeatherAdapter(dailyWeatherReports);
        recyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(layoutManager);


        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    public void downloadWeatherData(Location location) {
        final String fullCords = URL_COORD + location.getLatitude() + "&lon=" + location.getLongitude();
        final String url = URL_BASE + fullCords + URL_UNIT + URL_API_KEY;

        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    JSONObject city = response.getJSONObject("city");
                    String cityName = city.getString("name");
                    String country = city.getString("country");

                    JSONArray list = response.getJSONArray("list");

                    for (int x = 0; x < 5; x++) {
                        JSONObject object = list.getJSONObject(x);
                        JSONObject main = object.getJSONObject("main");
                        Double currentTemp = main.getDouble("temp");
                        Double maxTemp = main.getDouble("temp_max");
                        Double minTemp = main.getDouble("temp_min");


                        JSONArray weatherArr = object.getJSONArray("weather");
                        JSONObject weather = weatherArr.getJSONObject(0);
                        String weatherType = weather.getString("main");

                        String rawDate = object.getString("dt_txt");

                        DailyWeatherReport report = new DailyWeatherReport(cityName, country, currentTemp.intValue(), maxTemp.intValue(), minTemp.intValue(), weatherType, rawDate);

                        dailyWeatherReports.add(report);

                        Log.v("JSON", "Testing if it works " + report.getWeather());

                    }
                    Log.v("JSON", "Name: " + cityName + " - " + "Country: " + country);


                } catch (JSONException e) {
                    Log.v("JSON", "EXC: " + e.getLocalizedMessage());
                }

                updateUI();

                adapter.notifyDataSetChanged();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("FUN", "Err:" + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
            Log.v("MYSERVICES", "Requesting Permission");
        } else {
            Log.v("MYSERVICES", "Starting Location Services from onConnected");
            startLocationServices();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationServices();
                    Log.v("MYSERVICES", "PERMISSION GRANTED");
                } else {
                    //show dialog, DENIED PERMISSION
                    Log.v("MYSERVICES", "PERMISSION NOT GRANTED");
                }
            }
        }

    }

    public void startLocationServices() {
        Log.v("MYSERVICES", "Starting Location Services Called");
        try {
            LocationRequest request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, this);
        } catch (SecurityException ex) {
            //Show dialog, can't get location unless you give permission
            Log.v("MYSERVICES", ex.toString());
            Toast.makeText(this, "Ne morem zagnati lokacije. Zavrnjeno dovoljenje.", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateUI() {
        if (dailyWeatherReports.size() > 0) {
            DailyWeatherReport report = dailyWeatherReports.get(0);

            Log.v("WEATHER", report.getWeather());

            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.cloudy, null));
                    weatherIconMini.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.cloudy_mini, null));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.rainy, null));
                    weatherIconMini.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.rainy_mini, null));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.snow, null));
                    weatherIconMini.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.snow_mini, null));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_STORM:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.thunder_lightning, null));
                    weatherIconMini.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.thunder_lightning_mini, null));
                    break;
                default:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.sunny, null));
                    weatherIconMini.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.sunny_mini, null));
            }

            weatherDate.setText(report.getRawDate());
            currentTemp.setText(Integer.toString(report.getCurrentTemp()) + "°");
            lowTemp.setText(Integer.toString(report.getMinTemp()) + "°");
            cityCountry.setText(report.getCityName() + ", " + report.getCountry());
            weatherDescription.setText(report.getWeather());
        }
    }


    public class WeatherAdapter extends RecyclerView.Adapter<WeatherReportViewHolder> {
        private ArrayList<DailyWeatherReport> dailyWeatherReports;

        public WeatherAdapter(ArrayList<DailyWeatherReport> dailyWeatherReports) {
            this.dailyWeatherReports = dailyWeatherReports;
        }

        @Override
        public WeatherReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_weather, parent, false);
            return new WeatherReportViewHolder(card);
        }

        @Override
        public void onBindViewHolder(WeatherReportViewHolder holder, int position) {
            DailyWeatherReport report = dailyWeatherReports.get(position);
            holder.updateUI(report);
        }

        @Override
        public int getItemCount() {
            return dailyWeatherReports.size();
        }
    }


    public class WeatherReportViewHolder extends RecyclerView.ViewHolder {

        private ImageView weatherIcon;
        private TextView weatherDate;
        private TextView tempHigh;
        private TextView lowTemp;
        private TextView weatherDescription;

        public WeatherReportViewHolder(View itemView) {
            super(itemView);

            weatherIcon = (ImageView) itemView.findViewById(R.id.ivWeatherImage);
            weatherDate = (TextView) itemView.findViewById(R.id.tvWeatherDate);
            lowTemp = (TextView) itemView.findViewById(R.id.tvTempLow);
            tempHigh = (TextView) itemView.findViewById(R.id.tvTempHigh);
            weatherDescription = (TextView) itemView.findViewById(R.id.tvWeatherDescriptione);
        }

        public void updateUI(DailyWeatherReport report) {

            weatherDate.setText(report.getRawDate());
            weatherDescription.setText(report.getWeather());
            tempHigh.setText(Integer.toString(report.getMaxTemp()) + "°");
            lowTemp.setText(Integer.toString(report.getMinTemp()) + "°");


            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.cloudy_mini, null));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.rainy_mini, null));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.snow_mini, null));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_STORM:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.thunder_lightning_mini, null));
                    break;
                default:
                    weatherIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.sunny_mini, null));
            }
        }
    }
}
