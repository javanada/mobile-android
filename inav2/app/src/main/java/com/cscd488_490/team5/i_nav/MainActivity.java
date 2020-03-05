package com.cscd488_490.team5.i_nav;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import i_nav.Edge;
import i_nav.INavClient;
import i_nav_model.Location;
import i_nav_model.LocationObject;
import i_nav.LocationObjectVertex;
import i_nav.Search;

public class MainActivity extends AppCompatActivity implements Observer, AdapterView.OnItemSelectedListener {

    private LocationMap locationMap;
    private List<Integer> maps;
    private int currentMapIdIndex = 0;

    List<StringWithTag> locationOptions;
    List<StringWithTag> fromOptions;
    List<StringWithTag> toOptions;
    ArrayAdapter<StringWithTag> dataAdapter;
    ArrayAdapter<StringWithTag> dataAdapterFrom;
    ArrayAdapter<StringWithTag> dataAdapterTo;
    String currentLocation = "1";
    private ProgressBar progressBar;
    Integer count = 1;

    private LocationObject fromObject;
    private LocationObject toObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationMap = findViewById(R.id.locationMap);
        maps = new ArrayList<>();

        locationMap.getMyObservable().addObserver(this);

        progressBar = findViewById(R.id.progressBar);
        progressBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
        progressBar.setMax(10);

        // Location Selector
        Spinner spinner = findViewById(R.id.locationSpinner);
        // From and To Selectors
        Spinner spinnerFrom = findViewById(R.id.from_spinner);
        Spinner spinnerTo = findViewById(R.id.to_spinner);

        // Spinner click listener
        spinner.setOnItemSelectedListener(this);
        spinnerFrom.setOnItemSelectedListener(this);
        spinnerTo.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        locationOptions = new ArrayList<>();
        fromOptions = new ArrayList<>();
        toOptions = new ArrayList<>();
        // Creating adapter for spinner
        dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationOptions);
        dataAdapterFrom = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fromOptions);
        dataAdapterTo = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, toOptions);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataAdapterFrom.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataAdapterTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
        spinnerFrom.setAdapter(dataAdapterFrom);
        spinnerTo.setAdapter(dataAdapterTo);

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);



        new AsyncTaskGetLocations().execute("1");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        try {
            Object tag = ((StringWithTag) parent.getItemAtPosition(position)).tag;

            if (parent.getId() == R.id.locationSpinner) {

                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);

                currentLocation = tag.toString();
                new AsyncTaskGetLocationObjectsAndEdges().execute(tag.toString());

            }
        } catch (Exception e) {
            Log.e("onItemSelected", "Message: " + e.getMessage());
            e.printStackTrace();
        }

    }
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            Toast.makeText(this, "CSCD488/490 Senior Project: INav", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable o, Object arg) {
        String locationDetails = "";

        if (locationMap == null || locationMap.locationCache == null || locationMap.locationCache.get("" + currentLocation) == null) {
            locationDetails = "No location loaded...";
            ((TextView) findViewById(R.id.textView)).setText(locationDetails);
            return;
        }
        if ((locationMap.locationCache.containsKey("" + currentLocation) &&
                locationMap.locationCache.get("" + currentLocation) != null &&
                Objects.requireNonNull(locationMap.locationCache.get("" + currentLocation)).location != null)
        ) {
            Location l = Objects.requireNonNull(locationMap.locationCache.get("" + currentLocation)).location;
            locationDetails += l.getLong_name() + "#" + l.getLocation_id();
            locationDetails += "\n" + l.getDescription();
        }
        ((TextView) findViewById(R.id.textView)).setText(locationDetails);
    }

    public void onSetToClick(View view) {
        TextView directionsTextView = findViewById(R.id.directionsTextStatus);

        // set toObject to value of spinner
        Spinner spinnerTo = findViewById(R.id.to_spinner);
        String from = ((StringWithTag)spinnerTo.getSelectedItem()).tag.toString();
        for (LocationObject o : locationMap.objects) {
            if (o.getObject_id() == Integer.parseInt(from)) {
                toObject = o;
            }
        }

        String str = "";
        if (this.fromObject != null) {
            str += "From " + fromObject.getLocation_id() + "#" + fromObject.getObject_id() + " " + fromObject.getShort_name();
        }
        if (this.toObject != null) {
            str += " to " + toObject.getLocation_id() + "#" + toObject.getObject_id() +" " + toObject.getShort_name();
        }
        directionsTextView.setText(str);
    }

    public void onSetFromClick(View view) {
        TextView directionsTextView = findViewById(R.id.directionsTextStatus);

        // set toObject to value of spinner
        Spinner spinnerFrom = findViewById(R.id.from_spinner);
        String from = ((StringWithTag)spinnerFrom.getSelectedItem()).tag.toString();
        for (LocationObject o : locationMap.objects) {
            if (o.getObject_id() == Integer.parseInt(from)) {
                fromObject = o;
            }
        }

        String str = "";
        if (this.fromObject != null) {
            str += "From " + fromObject.getLocation_id() + "#" + fromObject.getObject_id() + " " + fromObject.getShort_name();
        }
        if (this.toObject != null) {
            str += " to " + toObject.getLocation_id() + "#" + toObject.getObject_id() +" " + toObject.getShort_name();
        }
        directionsTextView.setText(str);
    }

    public void onDirectionsMapClick(View view) {

        String from = null;
        if (fromObject != null) {
            from = "" + fromObject.getObject_id();
        }
        String to = null;
        if (toObject != null) {
            to = "" + toObject.getObject_id();
        }
        Log.i("onDirectionsMapClick", "Attempting to get shortest path for objects: from: " + from + " to: " + to);

        if (fromObject != null && toObject != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            new AsyncTaskGetPath().execute(from, to);
        }
    }

    public void onDirectionsListClick(View view) {
        Intent intent = new Intent(this, DirectionsActivity.class);

        StringBuilder stringBuilder = new StringBuilder();
        for (Edge e : locationMap.shortestPath) {
            String directionsStep = e.getStep();
            stringBuilder.append(directionsStep);
            stringBuilder.append("\n\n");
        }
        String directions = stringBuilder.toString();

        intent.putExtra("directions", directions);
        startActivityForResult(intent,  1);
    }

    public void printCache(View view) {
        for (String s : locationMap.locationCache.keySet()) {
            LocationMapCacheItem item = locationMap.locationCache.get(s);
            if (item != null) {
                Log.i("printCache", "key: " + s + " item: " + item.toString());
            }
        }
    }

    public void onBackClick(View view) {

        if (currentMapIdIndex > 0) {
            currentMapIdIndex--;
            Log.i("onBackClick", "Attempting to get previous location in directions");
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            new AsyncTaskGetLocationObjectsAndEdges().execute("" + maps.get(currentMapIdIndex));
        }
    }

    public void onForwardClick(View view) {
        if (currentMapIdIndex < maps.size() - 1) {
            currentMapIdIndex++;
            Log.i("onForwardClick", "Attempting to get next location in directions");
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            new AsyncTaskGetLocationObjectsAndEdges().execute("" + maps.get(currentMapIdIndex));
        }
    }

    public class AsyncTaskGetLocations extends AsyncTask<String, Integer, String> {

        //Constructor
        private AsyncTaskGetLocations() {
        }

        @Override
        protected String doInBackground(String... strings) {

            locationOptions.clear();
            publishProgress(count++);

            JSONArray arrLocations = INavClient.get("locations");

            for (Object obj : arrLocations) {
                JSONObject jsonObject = (JSONObject) obj;
                Location location = new Location(jsonObject);
                StringWithTag stringWithTag = new StringWithTag(location.getShort_name(), location.getLocation_id());
                locationOptions.add(stringWithTag);

                if (locationMap.locationCache.containsKey("" + location.getLocation_id())) {
                    Objects.requireNonNull(locationMap.locationCache.get("" + location.getLocation_id())).location = location;
                } else {
                    locationMap.locationCache.put("" + location.getLocation_id(), new LocationMapCacheItem());
                    Objects.requireNonNull(locationMap.locationCache.get("" + location.getLocation_id())).location = location;
                }
            }
            publishProgress(count++);

            return "success";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.i("!!!", "Running..."+ values[0]);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {

            progressBar.setVisibility(View.GONE);
            count = 1;
            if (result == null) {
                return;
            }

            super.onPostExecute(result);

            if (dataAdapter != null) {
                dataAdapter.notifyDataSetChanged();
            }

        }
    }



    public class AsyncTaskGetLocationObjectsAndEdges extends AsyncTask<String, Integer, String> {

        //Constructor
        private AsyncTaskGetLocationObjectsAndEdges() {
        }

        @Override
        protected String doInBackground(String... strings) {

            Log.i("AsyncTaskGetLocationObjectsAndEdges", "Calling AsyncTaskGetLocationObjectsAndEdges strings[0]: " + strings[0]);

            locationMap.objects.clear();
            locationMap.edges.clear();
            //locationMap.shortestPath.clear();
            fromOptions.clear();
            toOptions.clear();

            LocationMapCacheItem item = new LocationMapCacheItem();
            if (locationMap.locationCache == null) {
                locationMap.locationCache = new HashMap<>();
            } else {
                if (locationMap.locationCache.containsKey(strings[0]) && Objects.requireNonNull(locationMap.locationCache.get(strings[0])).objects != null) {
                    LocationMapCacheItem cacheItem = locationMap.locationCache.get(strings[0]);

                    if (cacheItem != null) {
                        for (LocationObject locationObject : cacheItem.objects) {
                            locationMap.objects.add(locationObject);
                            StringWithTag stringWithTag = new StringWithTag(locationObject.getShort_name(), locationObject.getObject_id());
                            fromOptions.add(stringWithTag);
                            toOptions.add(stringWithTag);
                        }
                    }

                    if (cacheItem != null) {
                        locationMap.edges.addAll(cacheItem.edges);
                        locationMap.canvas_image = cacheItem.canvas_image;
                    }
                    return "success";
                }
            }
            if (!locationMap.locationCache.containsKey(strings[0])) {
                locationMap.locationCache.put(strings[0], item);
            }

            JSONArray arr = INavClient.get("objects/location/" + strings[0]);

            if (arr.size() == 0) {
                return null;
            }

            for (Object obj : arr) {
                JSONObject jsonObject = (JSONObject) obj;
                if (jsonObject.get("object_type") != null) {
                    JSONObject jsonObjectType = ((JSONObject)jsonObject.get("object_type"));
                    if (jsonObjectType != null && jsonObjectType.get("image") != null && !locationMap.objectTypes.containsKey(Objects.requireNonNull(jsonObjectType.get("object_type_id")).toString())) {
                        String image = Objects.requireNonNull(jsonObjectType.get("image")).toString();
                        Log.i("!!!", "CREATING BITMAP...........");
                        try {
                            URL url = new URL(image);
                            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                            Bitmap bmp2 = ThumbnailUtils.extractThumbnail(bmp, 25, 25);
                            locationMap.objectTypes.put(Objects.requireNonNull(jsonObjectType.get("object_type_id")).toString(), bmp2);
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                    }
                }
                LocationObject o = new LocationObject(jsonObject);

                locationMap.objects.add(o);
                StringWithTag stringWithTag = new StringWithTag(o.getShort_name(), o.getObject_id());
                fromOptions.add(stringWithTag);
                toOptions.add(stringWithTag);

                if (jsonObject.get("location") != null) {
                    JSONObject j2 = (JSONObject) jsonObject.get("location");

                    if (j2 != null) {
                        locationMap.canvas_image = j2.get("canvas_image") != null ? Objects.requireNonNull(j2.get("canvas_image")).toString() : "";
                    }
                    item.canvas_image = locationMap.canvas_image;
                }
            }
            publishProgress(count++);

            item.objects = new ArrayList<>();
            item.objects.addAll(locationMap.objects);

            JSONArray arr2 = INavClient.get("edges/location/" + strings[0]);

            for (Object obj : arr2) {
                JSONObject jsonObject = (JSONObject) obj;
                if (jsonObject.get("v1") != null) {
                    LocationObjectVertex v1 = new LocationObjectVertex((JSONObject) Objects.requireNonNull(jsonObject.get("v1")));
                    LocationObjectVertex v2 = new LocationObjectVertex((JSONObject) Objects.requireNonNull(jsonObject.get("v2")));
                    int weight = Integer.parseInt(Objects.requireNonNull(jsonObject.get("weight")).toString());
                    Edge e = new Edge(v1, v2, weight);
                    locationMap.edges.add(e);
                }
            }
            publishProgress(count++);
            item.edges = new ArrayList<>();
            item.edges.addAll(locationMap.edges);

            return "success";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.i("!!!", "Running..."+ values[0]);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {

            progressBar.setVisibility(View.GONE);
            count = 1;
            if (result == null) {
                return;
            }

            super.onPostExecute(result);

            if (dataAdapterFrom != null) {
                dataAdapterFrom.notifyDataSetChanged();
            }
            if (dataAdapterTo != null) {
                dataAdapterTo.notifyDataSetChanged();
            }

            if (fromOptions.size() > 0 && toOptions.size() > 0) {
                Button btn = findViewById(R.id.buttonDirectionsMap);
                btn.setEnabled(true);
            }

//            locationMap.invalidate();
            new AsyncTaskDownloadImage(locationMap).execute(locationMap.canvas_image);
            // Do things like hide the progress bar or change a TextView

            Log.i("!!!", "currentMapIdIndex: " + currentMapIdIndex);
            for (int i = 0; i < maps.size(); i++) {
                Log.i("!!!", "maps: " + i + ": " + maps.get(i));
            }
        }
    }

    private class AsyncTaskGetPath extends AsyncTask<String, Integer, String> {

        AsyncTaskGetPath() {
        }

        protected String doInBackground(String... strings) {

            JSONArray arr2 = INavClient.get("path/shortest-source-dest/?source_object_id=" + strings[0] + "&dest_object_id=" + strings[1]);
            System.out.println("!!! arr2: " + arr2);

            HashMap<String, LocationObject> allObjects = new HashMap<>();
            for (LocationObject locationObject : locationMap.objects) {
                allObjects.put("" + locationObject.getObject_id(), locationObject);
            }

            locationMap.shortestPath = new ArrayList<>();


            for (int i = 0; i < arr2.size(); i++) {
                Object obj = arr2.get(i);
                JSONObject jsonObject = (JSONObject) obj;
                if (jsonObject.get("v1") != null) {
                    LocationObjectVertex v1 = new LocationObjectVertex((JSONObject) Objects.requireNonNull(jsonObject.get("v1")));
                    LocationObjectVertex v2 = new LocationObjectVertex((JSONObject) Objects.requireNonNull(jsonObject.get("v2")));
                    int weight = Integer.parseInt(Objects.requireNonNull(jsonObject.get("weight")).toString());
//                    String step = jsonObject.get("directions") != null ? Objects.requireNonNull(jsonObject.get("directions")).toString() : "";
                    Edge e = new Edge(v1, v2, weight);

                    locationMap.shortestPath.add(e);

                    if (!maps.contains(v1.getLocation_id())) {
                        maps.add(v1.getLocation_id());
                    }
                    if (!maps.contains(v2.getLocation_id())) {
                        maps.add(v2.getLocation_id());
                    }

                }
            }

            for (int i = 0; i < maps.size(); i++) {
                Log.i("!!!", "maps: " + i + ": " + maps.get(i));
                if (locationMap.objects.get(locationMap.objects.size() - 1).getLocation_id() == maps.get(i)) {
                    currentMapIdIndex = i;
                }
            }
            Log.i("!!!", "currentMapIdIndex: " + currentMapIdIndex);

//            locationMap.shortestPath = Search.getDirections(locationMap.shortestPath, allObjects);
            Search.getDirections(locationMap.shortestPath, allObjects);



            if (locationMap.shortestPath.size() > 0) {
                return "success";
            } else {
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.i("!!!", "Running..."+ values[0]);
            progressBar.setProgress(values[0]);
        }

        protected void onPostExecute(String result) {

            progressBar.setVisibility(View.GONE);
            count = 1;
            Button btn = (Button) findViewById(R.id.buttonDirectionsList);

            if (result == null) {
                btn.setEnabled(false);
                return;
            }

            if (locationMap.shortestPath.size() > 0) {
                btn.setEnabled(true);
            }

        }
    }

    private class AsyncTaskDownloadImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        AsyncTaskDownloadImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap bitmap;

            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                bitmap = null;
                Log.e("AsyncTaskDownloadImage", e.getMessage() + ", " +  urlDisplay);
                e.printStackTrace();
            }
            return bitmap;
        }
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                return;
            }
            locationMap.imageWidth = result.getWidth();
            locationMap.setImageBitmap(result);

//            locationMap.invalidate();
            locationMap.mTimer.start();
        }

    }

    public class StringWithTag {
        String string;
        Object tag;

        public StringWithTag(String stringPart, Object tagPart) {
            string = stringPart;
            tag = tagPart;
        }

        @Override
        public String toString() {
            return string + "#" + tag;
        }
    }
}
