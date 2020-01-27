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
import java.util.Map;
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

    List<StringWithTag> locationOptions;
    List<StringWithTag> fromOptions;
    List<StringWithTag> toOptions;
    ArrayAdapter<StringWithTag> dataAdapter;
    ArrayAdapter<StringWithTag> dataAdapterFrom;
    ArrayAdapter<StringWithTag> dataAdapterTo;

    String currentLocation = "1";

    private ProgressBar progressBar;
    Integer count = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationMap = (LocationMap) findViewById(R.id.locationMap);

        locationMap.getMyObservable().addObserver(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getProgressDrawable().setColorFilter(
                Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
        progressBar.setMax(10);

        // Spinner element
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        Spinner spinnerFrom = (Spinner) findViewById(R.id.from_spinner);
        Spinner spinnerTo = (Spinner) findViewById(R.id.to_spinner);
        // Spinner click listener
        spinner.setOnItemSelectedListener(this);
        spinnerFrom.setOnItemSelectedListener(this);
        spinnerTo.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        locationOptions = new ArrayList<StringWithTag>();
        fromOptions = new ArrayList<StringWithTag>();
        toOptions = new ArrayList<StringWithTag>();
        // Creating adapter for spinner
        dataAdapter = new ArrayAdapter<StringWithTag>(this, android.R.layout.simple_spinner_item, locationOptions);
        dataAdapterFrom = new ArrayAdapter<StringWithTag>(this, android.R.layout.simple_spinner_item, fromOptions);
        dataAdapterTo = new ArrayAdapter<StringWithTag>(this, android.R.layout.simple_spinner_item, toOptions);
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



        new MyTaskLocations().execute("1");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        try {
            // On selecting a spinner item
            String item = parent.getItemAtPosition(position).toString();
            Object tag = ((StringWithTag) parent.getItemAtPosition(position)).tag;

            // Showing selected spinner item
//            Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show();

            //        Log.i("!!!", "" + view.getId() + " " + (parent.getId() == R.id.spinner) + " parent: " + parent.getId());
            if (parent.getId() == R.id.spinner) {
                Log.i("!!!", "GET NEW LOCATION");

                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);

                currentLocation = tag.toString();
                new MyTask().execute(tag.toString());

            }
        } catch (Exception e) {
            Log.e("!!!", "Error: " + e.getMessage());
            e.printStackTrace();
        }

    }
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here
        int id = item.getItemId();
        if (id == R.id.action_about) {
            Toast.makeText(this, "CSCD 488/490 Senior Project: INav", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable o, Object arg) {
        String details = "";
        if ((locationMap.locationCache.containsKey("" + currentLocation) && locationMap.locationCache.get("" + currentLocation).location != null)) {
            Location l = locationMap.locationCache.get("" + currentLocation).location;
            details += l.getLong_name();
            details += l.getJSONString();
        }
//        ((TextView) findViewById(R.id.textView)).setText(arg.toString());
        ((TextView) findViewById(R.id.textView)).setText(details);
    }


    public void onDirectionsMapClick(View view) {
        Spinner spinnerFrom = (Spinner)findViewById(R.id.from_spinner);
        String from = ((StringWithTag)spinnerFrom.getSelectedItem()).tag.toString();

        Spinner spinnerTo = (Spinner)findViewById(R.id.to_spinner);
        String to = ((StringWithTag)spinnerTo.getSelectedItem()).tag.toString();

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        new GetPathTask().execute(from, to);
    }

    public void onDirectionsListClick(View view) {
        Intent intent = new Intent(this, DirectionsActivity.class);

        String directions = "";
        for (Edge e : locationMap.shortestPath) {
            String directionsStep = e.getStep();
            directions += directionsStep + "\n\n";
        }

        intent.putExtra("directions", directions);
        startActivityForResult(intent,  1);
    }

    public void printCache(View view) {
        for (String s : locationMap.locationCache.keySet()) {
            LocationMapCacheItem item = locationMap.locationCache.get(s);
            Log.i("&&&", "key: " + s + " item: " + item.toString());
        }

    }

    public class MyTaskLocations extends AsyncTask<String, Integer, String> {

        //Constructor
        private MyTaskLocations() {
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
                    locationMap.locationCache.get("" + location.getLocation_id()).location = location;
                } else {
                    locationMap.locationCache.put("" + location.getLocation_id(), new LocationMapCacheItem());
                    locationMap.locationCache.get("" + location.getLocation_id()).location = location;
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



    public class MyTask extends AsyncTask<String, Integer, String> {

        //Constructor
        private MyTask() {
        }

        @Override
        protected String doInBackground(String... strings) {

            locationMap.objects.clear();
            locationMap.edges.clear();
            locationMap.shortestPath.clear();
            fromOptions.clear();
            toOptions.clear();

            LocationMapCacheItem item = new LocationMapCacheItem();
            if (locationMap.locationCache == null) {
                locationMap.locationCache = new HashMap<>();
            } else {
                if (locationMap.locationCache.containsKey(strings[0]) && locationMap.locationCache.get(strings[0]).objects != null) {
                    LocationMapCacheItem cacheItem = locationMap.locationCache.get(strings[0]);

                    for (LocationObject locationObject : cacheItem.objects) {
                        locationMap.objects.add(locationObject);
                        StringWithTag stringWithTag = new StringWithTag(locationObject.getShort_name(), locationObject.getObject_id());
                        fromOptions.add(stringWithTag);
                        toOptions.add(stringWithTag);
                    }

                    for (Edge edge : cacheItem.edges) {
                        locationMap.edges.add(edge);
                    }
                    locationMap.canvas_image = cacheItem.canvas_image;
                    return "success";
                }
            }
            if (!locationMap.locationCache.containsKey(strings[0])) {
                locationMap.locationCache.put(strings[0], item);
            }


            Log.i("!!!", "strings[0]: " + strings[0]);

            JSONArray arr = INavClient.get("objects/location/" + strings[0]);
            System.out.println("!!! arr: " + arr);

            if (arr.size() == 0) {
                return null;
            }

            for (Object obj : arr) {
                JSONObject jsonObject = (JSONObject) obj;
                if (jsonObject.get("object_type") != null) {
                    JSONObject jsonObjectType = ((JSONObject)jsonObject.get("object_type"));
                    if (jsonObjectType.get("image") != null && !locationMap.objectTypes.containsKey(jsonObjectType.get("object_type_id").toString())) {
                        String image = jsonObjectType.get("image").toString();
                        Log.i("!!!", "CREATING BITMAP...........");
                        try {
                            URL url = new URL(image);
                            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                            Bitmap bmp2 = ThumbnailUtils.extractThumbnail(bmp, 25, 25);
                            locationMap.objectTypes.put(jsonObjectType.get("object_type_id").toString(), bmp2);
                        } catch(IOException e) {
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
                    Log.i("!!! j2", j2.toJSONString());
                    locationMap.canvas_image = j2.get("canvas_image").toString();
                    item.canvas_image = locationMap.canvas_image;
                }
            }
            publishProgress(count++);

            item.objects = new ArrayList<>();
            for (LocationObject locationObject : locationMap.objects) {
                item.objects.add(locationObject);
            }



            JSONArray arr2 = INavClient.get("edges/location/" + strings[0]);
            System.out.println("!!! arr2: " + arr2);

            for (Object obj : arr2) {
                JSONObject jsonObject = (JSONObject) obj;
                if (jsonObject.get("v1") != null) {
                    LocationObjectVertex v1 = new LocationObjectVertex((JSONObject)jsonObject.get("v1"));
                    LocationObjectVertex v2 = new LocationObjectVertex((JSONObject)jsonObject.get("v2"));
                    int weight = Integer.parseInt(jsonObject.get("weight").toString());
                    Edge e = new Edge(v1, v2, weight);
                    locationMap.edges.add(e);
                }
            }
            publishProgress(count++);
            item.edges = new ArrayList<>();
            for (Edge edge : locationMap.edges) {
                item.edges.add(edge);
            }

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
                Button btn = (Button) findViewById(R.id.buttonDirectionsMap);
                btn.setEnabled(true);
            }

//            locationMap.invalidate();
            new DownloadImageTask(locationMap).execute(locationMap.canvas_image);
            // Do things like hide the progress bar or change a TextView
        }
    }

    private class GetPathTask extends AsyncTask<String, Integer, String> {

        public GetPathTask() {
        }

        protected String doInBackground(String... strings) {

            JSONArray arr2 = INavClient.get("path/shortest-source-dest/?source_object_id=" + strings[0] + "&dest_object_id=" + strings[1]);
            System.out.println("!!! arr2: " + arr2);

            HashMap<String, LocationObject> allObjects = new HashMap<String, LocationObject>();
            for (LocationObject locationObject : locationMap.objects) {
                allObjects.put("" + locationObject.getObject_id(), locationObject);
            }

            locationMap.shortestPath = new ArrayList<Edge>();


            for (int i = 0; i < arr2.size(); i++) {// (Object obj : arr2) {
                Object obj = arr2.get(i);
                JSONObject jsonObject = (JSONObject) obj;
                if (jsonObject.get("v1") != null) {
                    LocationObjectVertex v1 = new LocationObjectVertex((JSONObject) jsonObject.get("v1"));
                    LocationObjectVertex v2 = new LocationObjectVertex((JSONObject) jsonObject.get("v2"));
                    int weight = Integer.parseInt(jsonObject.get("weight").toString());
                    String step = jsonObject.get("directions") != null ? jsonObject.get("directions").toString() : "";
                    Edge e = new Edge(v1, v2, weight);
                    locationMap.shortestPath.add(e);
                }
            }

            locationMap.shortestPath = Search.getDirections(locationMap.shortestPath, allObjects);
            


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

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage() + ", " +  urldisplay);
                e.printStackTrace();
            }
            return bmp;
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
        public String string;
        public Object tag;

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
