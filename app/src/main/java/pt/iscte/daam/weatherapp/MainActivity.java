package pt.iscte.daam.weatherapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    protected EditText etLocation;
    protected final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?q=";
    protected final String APPID = "c3397f645600ceea70a97de590fe8147";

    protected GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        etLocation = (EditText) findViewById(R.id.etLocation);
        etLocation.clearFocus();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng lisboa = new LatLng(38.748547784895, -9.155314987341285);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(lisboa, 10));

        googleMap = map;
    }

    public void checkWeatherBtn(View v) {
        if(!etLocation.getText().toString().isEmpty()) {
            String slocation = etLocation.getText().toString();
            etLocation.setText("");
            etLocation.clearFocus();

            // usado para fechar o teclado
            View view = this.getCurrentFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            new getWeather().execute(slocation);
        } else {
            Toast.makeText(this, "You have to fill the location. Thank you!", Toast.LENGTH_LONG).show();
        }
    }


    private class getWeather extends AsyncTask<String, Void, String> {
        private ProgressDialog pDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            pDialog.setMessage("Getting weather from service...");
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String response = "";

            try {
                // Para usar este código em API > 21 é preciso adicionar o seguinte no ficheiro build.gradle, pois foram deprecated!!!
                /*
                android {
                    useLibrary 'org.apache.http.legacy'
                }*/

                HttpClient httpclient = new DefaultHttpClient();

                URI uri = new URI(BASE_URL + URLEncoder.encode(params[0], "UTF-8") + "&units=metric&APPID="+APPID);

                Log.i("WeatherApp", "Asking  = " + BASE_URL + URLEncoder.encode(params[0], "UTF-8") + "&units=metric&APPID="+APPID);

                HttpResponse httpResponse = httpclient.execute(new HttpGet(uri));

                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
                String temp = "";
                while((temp=reader.readLine())!=null) {
                    response += temp;
                }

                Log.i("WeatherApp", "Response = " + response);

            } catch(Exception e) {
                Log.i("WeatherApp", "An exception has occured in the connection - " + e.toString());
                return "-ERR";
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            pDialog.dismiss();


            try {
                // now, we have to handle all the necessary results and add them to the map
                JSONObject jobj = new JSONObject(result);

                if(jobj.getString("cod").equals("404")) {
                    Toast.makeText(getBaseContext(), "Location not found!", Toast.LENGTH_LONG).show();
                } else {

                    final String city = jobj.getString("name");

                    Double lat = Double.parseDouble(jobj.getJSONObject("coord").getString("lat"));
                    Double lon = Double.parseDouble(jobj.getJSONObject("coord").getString("lon"));

                    final String main = jobj.getJSONArray("weather").getJSONObject(0).getString("main");
                    String icon = jobj.getJSONArray("weather").getJSONObject(0).getString("icon");
                    final String description = jobj.getJSONArray("weather").getJSONObject(0).getString("description");
                    final String temp = jobj.getJSONObject("main").getString("temp");
                    final String temp_max = jobj.getJSONObject("main").getString("temp_max");
                    final String temp_min = jobj.getJSONObject("main").getString("temp_min");


                    Log.i("WeatherApp", "City => " + city);
                    Log.i("WeatherApp", "Lat => " + lat);
                    Log.i("WeatherApp", "Long => " + lon);
                    Log.i("WeatherApp", "Weather => " + main + ", " + description + " - Temp: " + temp + "º (Max:"+temp_max+"º/Min:"+temp_min+"º)");

                    final LatLng location = new LatLng(lat, lon);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));

                    // usado para carregar um icone que corresponde ao estado do tempo!
                    ImageLoader imageLoader = ImageLoader.getInstance();
                    imageLoader.loadImage("http://openweathermap.org/img/w/"+icon+".png", new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            // Do whatever you want with Bitmap
                            googleMap.addMarker(new MarkerOptions()
                                    .title(city)
                                    .snippet(main + ", " + description + " - Temp: " + temp + "º (Max:"+temp_max+"º/Min:"+temp_min+"º)")
                                    .position(location)
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.fromBitmap(loadedImage))).showInfoWindow();
                        }
                    });
                }

            } catch (Exception e) {
                Log.i("SiUinde", "An exception has occured in the connection - " + e.toString());
            }
        }
    }
}
