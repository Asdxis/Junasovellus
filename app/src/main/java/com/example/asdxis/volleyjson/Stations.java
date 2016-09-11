package com.example.asdxis.volleyjson;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class Stations extends MainActivity {

    private String stationShortCodeS;
    public String stationName;

    private Activity activityContext;

    private static String[] nameList = new String[1000];
    public static String[] shortCodeList = new String[1000];

    private static String TAG = Stations.class.getSimpleName();
    private ProgressDialog progressDialogStations;

    private String[] setupArray(String[] arrr)
    {
        for (int i = 0; i<arrr.length; i++)
        {
            arrr[i] = "";
        }
        return arrr;
    }

    public void makeJsonArrayRequestStations(Activity activityContext) {

        this.activityContext = activityContext;

        progressDialogStations = new ProgressDialog(activityContext);
        progressDialogStations.setTitle("Odota hetki...");
        progressDialogStations.setMessage("Haetaan asemia");
        String stationsUrl = "http://rata.digitraffic.fi/api/v1/metadata/stations";

        progressDialogStations.show();

        JsonArrayRequest req = new JsonArrayRequest(stationsUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Log.d(TAG, response.toString());
                try {
                    // Parsing json array response
                    // loop through each json object
                    setupArray(nameList);
                    setupArray(shortCodeList);
                    for (int i = 0; i < response.length(); i++) {

                        JSONObject station = response.getJSONObject(i);
                        stationName = station.getString("stationName");
                        stationShortCodeS = station.getString("stationShortCode");
                        if(!stationName.toLowerCase().contains("tavara".toLowerCase())) //Don't get cargo stations
                        {
                            nameList[i] = stationName;
                            shortCodeList[i] = stationShortCodeS;
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }

                progressDialogStations.hide();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                       error.getMessage(), Toast.LENGTH_SHORT).show();
                 progressDialogStations.hide();
            }
        });
        AppController.getInstance().addToRequestQueue(req);
        // Adding request to request queue

    }

    public String formatStationName(String stationShortCode)    // Changes the given stationshortcode to full name
    {
        for (int j = 0; j <nameList.length; j++)
        {
            if (shortCodeList[j].equals(stationShortCode))
            {
                return nameList[j];
            }
        }
        return null;
    }

    public String[] getStationNames()
    {
        return nameList;
    }
    public String[] getStationShortCodes()
    {
        return shortCodeList;
    }

}