package com.example.asdxis.volleyjson;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StationSchedule extends AppCompatActivity {

    private static String TAG = StationSchedule.class.getSimpleName();

    AutoCompleteTextView editStationName;

    private String jsonResponseA;
    private String jsonResponseD;

    private TextView txtArrivals;
    private TextView txtDepartures;
    private Button getStationInfo;

    String[] stationsList;
    String[] stationShortCodeList;
    private String inputStation;
    private String inputStationFull;

    private ProgressDialog progressDialogStationSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_schedule);

        txtArrivals = (TextView) findViewById(R.id.txtArrivals);
        txtDepartures = (TextView) findViewById(R.id.txtDepartures);
        getStationInfo = (Button) findViewById(R.id.getStationInfo);
        editStationName = (AutoCompleteTextView)findViewById(R.id.stationName);

        Stations stations = new Stations();
        stationsList = stations.getStationNames();
        stationShortCodeList = stations.getStationShortCodes();

        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,stationsList);
        editStationName.setAdapter(adapter);    //Takes station names and suggests them on the AutoCompleteTextView
        editStationName.setThreshold(1);        //Suggest when 1 letter is typed

        editStationName.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    getStationInfo.performClick();          //Perform onClick if enter is pressed
                    return true;
                }
                return false;
            }
        });
    }
    public void getStationInfo(View view)
    {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); //Hide the keyboard
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        makeJsonArrayRequest();
    }
    public void makeJsonArrayRequest() {
        progressDialogStationSchedule = new ProgressDialog(this);
        progressDialogStationSchedule.setTitle("Odota hetki...");
        progressDialogStationSchedule.setMessage("Haetaan aikatauluja");

        Stations stations = new Stations();
        String[] stationList = stations.getStationNames();

        stationShortCodeList = stations.getStationShortCodes();
        stationsList = stations.getStationNames();

        inputStationFull = editStationName.getText().toString();
        for(int i = 0; i < stationShortCodeList.length; i++)
        {
            if (inputStationFull.equals(stationList[i]))
            {
                inputStation = stationShortCodeList[i];
            }
        }

        //http://rata.digitraffic.fi/api/v1/live-trains?station=HKI
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("rata.digitraffic.fi")
                .appendPath("api")
                .appendPath("v1")
                .appendPath("live-trains")
                .appendQueryParameter("station", inputStation)
                .appendQueryParameter("departed_trains", "0")
                .appendQueryParameter("arrived_trains", "0")
                .appendQueryParameter("arriving_trains", "20")
                .appendQueryParameter("departing_trains", "20");

        String myUrl = builder.build().toString();
        Log.d("myUrl", myUrl);

        progressDialogStationSchedule.show();

        JsonArrayRequest req = new JsonArrayRequest(myUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                //Log.d(TAG, response.toString());

                try {
                    // Parsing json array response
                    // loop through each json object
                    jsonResponseA = "";
                    jsonResponseD = "";

                    jsonResponseA += "Saapuvat:\n\n";
                    jsonResponseD += "Lähtevät:\n\n";

                    for (int i = 0; i < response.length(); i++) {
                        //Loops through every object, where one object contains information for one train
                        JSONObject train = response.getJSONObject(i);
                        String trainCategory = train.getString("trainCategory");
                        if (!trainCategory.equals("Cargo")) {       //Don't print cargo trains
                            String trainNumber = train.getString("trainNumber");

                            JSONArray timeTableRows = train.getJSONArray("timeTableRows");

                            for (int j = 0; j < timeTableRows.length(); j++) {

                                JSONObject station = timeTableRows.getJSONObject(j);
                                String stationShortCode = station.getString("stationShortCode");
                                String scheduledTime = station.getString("scheduledTime");
                                String type = station.getString("type");    //Departing or arriving

                                MainActivity mainActivity = new MainActivity();
                                Stations stations = new Stations();

                                JSONObject startStation = timeTableRows.getJSONObject(0);   //First station in trains schedule
                                String startStationString = startStation.getString("stationShortCode");

                                JSONObject endStation = timeTableRows.getJSONObject(timeTableRows.length() - 1);    //Destination
                                String endStationString = endStation.getString("stationShortCode");

                                if (!station.has("actualTime")) { //Filter out if the train has actual time when it was at the station

                                    if (inputStation.equals(stationShortCode) && type.equals("ARRIVAL")) {  //Gets the info for right station and if it's arrival
                                        jsonResponseA += "Juna: " + trainNumber + "\n";

                                        jsonResponseA += stations.formatStationName(startStationString) + " - " + stations.formatStationName(endStationString) + "\n";
                                        if (station.has("differenceInMinutes")) {
                                            String differenceInMinutes = station.getString("differenceInMinutes");

                                            if(differenceInMinutes.contains("-")) {    //If train is early don't print "+"
                                                jsonResponseA += mainActivity.formatScheduledTime(scheduledTime) + "  " + differenceInMinutes + " min\n\n";
                                            }
                                            else
                                            {
                                                jsonResponseA += mainActivity.formatScheduledTime(scheduledTime) + "  +" + differenceInMinutes + " min\n\n";
                                            }

                                        } else {
                                            jsonResponseA += mainActivity.formatScheduledTime(scheduledTime) + "\n\n";
                                        }
                                    }
                                    if (inputStation.equals(stationShortCode) && type.equals("DEPARTURE")) {
                                        jsonResponseD += "Juna: " + trainNumber + "\n";

                                        jsonResponseD += stations.formatStationName(startStationString) + " - " + stations.formatStationName(endStationString) + "\n";
                                        if (station.has("differenceInMinutes")) {
                                            String differenceInMinutes = station.getString("differenceInMinutes");
                                            if(differenceInMinutes.contains("-")) {    //If train is early don't print "+"
                                                jsonResponseD += mainActivity.formatScheduledTime(scheduledTime) + "  " + differenceInMinutes + " min\n\n";
                                            }
                                            else
                                            {
                                                jsonResponseD += mainActivity.formatScheduledTime(scheduledTime) + "  +" + differenceInMinutes + " min\n\n";
                                            }

                                             } else {
                                            jsonResponseD += mainActivity.formatScheduledTime(scheduledTime) + "\n\n";
                                        }
                                    }
                                }

                            }
                        }
                    }
                    txtDepartures.setText(jsonResponseA);
                    txtArrivals.setText(jsonResponseD);
                } catch (JSONException e) {
                    e.printStackTrace();
                   Toast.makeText(getApplicationContext(),
                            "" ,
                            Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }

                progressDialogStationSchedule.hide();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialogStationSchedule.hide();

            }
        });
        AppController.getInstance().addToRequestQueue(req);

    }
}