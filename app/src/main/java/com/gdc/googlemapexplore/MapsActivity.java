package com.gdc.googlemapexplore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gdc.googlemapexplore.ver1.FetchURL;
import com.gdc.googlemapexplore.ver1.TaskLoadedCallback;
import com.gdc.googlemapexplore.ver2.HttpConnection;
import com.gdc.googlemapexplore.ver2.PathJSONParser;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, TaskLoadedCallback {
    private static final String TAG = "_MapsActivity";

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean mLocationPermissionGranted = false;
    private Location lastLocation;
    private Button btn_getRoute;
    private MarkerOptions marker = new MarkerOptions();
    Polyline currentPolyline;

    private static final LatLng HOME = new LatLng(-7.2374363, 112.6274348);
    private static final LatLng T_JUNCTION = new LatLng(-7.2411412, 112.6288518);
    private static final LatLng LIGHT_PARK = new LatLng(-7.2404317, 112.6251765);
    private static final LatLng GAS_STATION = new LatLng(-7.2369845, 112.61866);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gmap);
        initView();

        if (!checkGps()) {
            Toast.makeText(this, "GPS not activated", Toast.LENGTH_SHORT).show();
        } else {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

        initListener();

    }

    private void setPathGoogleMap() {
        MarkerOptions options = new MarkerOptions();
        options.position(HOME);
        options.position(T_JUNCTION);
        options.position(LIGHT_PARK);
        options.position(GAS_STATION);
        googleMap.addMarker(options);
        String url = getMapsApiDirectionsUrl3();
        Log.d(TAG, "setPathGoogleMap: URL: " + url);
        ReadTask downloadTask = new ReadTask();
        downloadTask.execute(url);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HOME, 13));
        addMarkers();
    }

    private void addMarkers() {
        if (googleMap != null) {
            googleMap.addMarker(new MarkerOptions().position(HOME)
                    .title("First Point"));
            googleMap.addMarker(new MarkerOptions().position(T_JUNCTION)
                    .title("Second Point"));
            googleMap.addMarker(new MarkerOptions().position(LIGHT_PARK)
                    .title("Third Point"));
            googleMap.addMarker(new MarkerOptions().position(GAS_STATION)
                    .title("Fourth Point"));
        }
    }

    private String getMapsApiDirectionsUrl() {
        String waypoints = "waypoints=optimize:true|"
                + HOME.latitude + "," + HOME.longitude
                + "|" + "|" + T_JUNCTION.latitude + ","
                + T_JUNCTION.longitude + "|" + GAS_STATION.latitude + ","
                + GAS_STATION.longitude;

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        return url;
    }

    private String getMapsApiDirectionsUrl2() {
        String waypoints = "waypoints=optimize:true|"
                + HOME.latitude + "," + HOME.longitude
                + "|" + "|" + T_JUNCTION.latitude + ","
                + T_JUNCTION.longitude + "|" + GAS_STATION.latitude + ","
                + GAS_STATION.longitude;
        String OriDest = "origin="+HOME.latitude+","+HOME.longitude+"&destination="+GAS_STATION.latitude+","+GAS_STATION.longitude;

        String sensor = "sensor=false";
        String params = OriDest+"&%20"+waypoints + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        return url;
    }

    private String getMapsApiDirectionsUrl3() {
        String waypoints = "waypoints=optimize:true|"
                + HOME.latitude + "," + HOME.longitude
                + "|" + "|" + T_JUNCTION.latitude + ","
                + T_JUNCTION.longitude + "|" + GAS_STATION.latitude + ","
                + GAS_STATION.longitude;

        String str_origin = "origin=" + HOME.latitude + "," + HOME.longitude;
        // Destination of route
        String str_dest = "destination=" + GAS_STATION.latitude + "," + GAS_STATION.longitude;

        // Sensor enabled
        String sensor = "sensor=true";
        String mode = "mode=driving";
        String key = "key="+getResources().getString(R.string.google_maps_key);
        // Building the parameters to the web service
        String parameters = str_origin + "&" + waypoints + "&" + str_dest + "&" + sensor + "&" + mode + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    private class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HttpConnection http = new HttpConnection();
                data = http.readUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(2);
                polyLineOptions.color(Color.BLUE);
            }

            googleMap.addPolyline(polyLineOptions);
        }
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    private String getUrl2(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        //waypoint
        String waypoints = "waypoints=optimize:true|-7.2411412, 112.6288518|";
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        // Mode
//        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + waypoints + "&" + str_dest + "&" + sensor;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

//    private String getMapsApiDirectionsUrl() {
//        String origin = "origin=" + LOWER_MANHATTAN.latitude + "," + LOWER_MANHATTAN.longitude;
//        String waypoints = "waypoints=optimize:true|" + BROOKLYN_BRIDGE.latitude + "," + BROOKLYN_BRIDGE.longitude + "|";
//        String destination = "destination=" + WALL_STREET.latitude + "," + WALL_STREET.longitude;
//
//        String sensor = "sensor=false";
//        String params = origin + "&" + waypoints + "&"  + destination + "&" + sensor;
//        String output = "json";
//        return "https://maps.googleapis.com/maps/api/directions/"
//                + output + "?" + params;
//    }

    private boolean checkGps() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;

                getDeviceLocation();
                Log.d(TAG, "onRequestPermissionsResult: Permission Running...");
            }
        }

        updateLocationUI();
    }

    private void updateLocationUI() {
        try {
            if (mLocationPermissionGranted) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setZoomControlsEnabled(true);
            } else {
                googleMap.setMyLocationEnabled(false);
                googleMap.getUiSettings().setZoomControlsEnabled(false);
                lastLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.d(TAG, "updateLocationUI: Error: " + e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = fusedLocationClient.getLastLocation();
                locationResult.addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            lastLocation = location;
                            Log.d(TAG, "onSuccess: " + lastLocation);

                            setPathGoogleMap();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: Error: " + e.getMessage());
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void initListener() {
        btn_getRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                btn_getRoute.setText("Lat: " + lastLocation.getLatitude() + ", Lng; " + lastLocation.getLongitude());
            }
        });
    }

    private void initView() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        btn_getRoute = findViewById(R.id.btn_getRoute);
    }

    private void getDirectionJson() {

    }

//    private void getGmapData() {
//        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
//        Call<GMapResponse> call = apiInterface.getGmapData("AIzaSyAPBJLBVxvOLO2LinOTDOc-iAKzrsqyzAE",
//                "Surabaya");
//        call.enqueue(new Callback<GMapResponse>() {
//            @Override
//            public void onResponse(Call<GMapResponse> call, Response<GMapResponse> response) {
//                if (response.isSuccessful()) {
//                    Log.d(TAG, "onResponse: " + response.body());
//                } else {
//                    Toast.makeText(MapsActivity.this, "" + response.message(), Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<GMapResponse> call, Throwable t) {
//                Toast.makeText(MapsActivity.this, "Error2: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        updateLocationUI();

        getDeviceLocation();
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null) {
            currentPolyline.remove();
        }
        currentPolyline = googleMap.addPolyline((PolylineOptions) values[0]);
    }
}
