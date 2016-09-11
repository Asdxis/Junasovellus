package com.example.asdxis.volleyjson;

import com.example.asdxis.volleyjson.R;
import com.example.asdxis.volleyjson.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    private Calendar calendar;
    private int year, month, day;
    private String formatUrlDate;
    private String formatTitleDate;

    private static String TAG = MainActivity.class.getSimpleName();

    private ProgressDialog pDialog;

    private TextView txtResponseTitle;
    private TextView txtResponse;
    private EditText editTrainNumber;
    private Button btnArrayRequest;

    // temporary string to show the parsed response
    private String jsonResponseTitle;
    private String jsonResponse;
    private String previousStation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Odota hetki...");
        pDialog.setCancelable(false);

        btnArrayRequest = (Button) findViewById(R.id.btnArrayRequest);
        txtResponseTitle = (TextView) findViewById(R.id.txtResponseTitle);
        txtResponse = (TextView) findViewById(R.id.txtResponse);
        editTrainNumber = (EditText) findViewById(R.id.addTrain);
        editTrainNumber.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    btnArrayRequest.performClick();         //Perform onClick if enter is pressed
                    return true;
                }
                return false;
            }
        });

        Stations stations = new Stations();
        stations.makeJsonArrayRequestStations(this); //Get all the station names

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        showDate(year, month+1, day);//Takes the date and formats it
    }
    public void k(View view)
    {
        Intent intent = new Intent(this, StationSchedule.class);
        startActivity(intent);
    }
    public void searchClick(View view) {
        Stations getStations = new Stations();
        getStations.makeJsonArrayRequestStations(this);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); //Hide the keyboard
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        makeJsonArrayRequest();
    }
    /**
     * Method to make json array request where response starts with [
     * */
    public void makeJsonArrayRequest() {

        previousStation = "";
        String trainNumber = editTrainNumber.getText().toString();
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("rata.digitraffic.fi")
                .appendPath("api")
                .appendPath("v1")
                .appendPath("live-trains")
                .appendPath(trainNumber)
                .appendQueryParameter("departure_date", formatUrlDate);
        String myUrl = builder.build().toString();

        showpDialog();

        JsonArrayRequest req = new JsonArrayRequest(myUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Log.d(TAG, response.toString());

                try {
                    // Parsing json array response
                    // loop through each json object
                    jsonResponseTitle = "";
                    jsonResponse = "";
                    Stations stations = new Stations();

                    JSONObject train = response.getJSONObject(0);
                    String trainNumber = train.getString("trainNumber");
                    jsonResponseTitle += "Junan numero: " + trainNumber + "\n";

                    jsonResponseTitle += "Lähtöpäivä: " + formatTitleDate;


                    JSONArray timeTableRows = train.getJSONArray("timeTableRows");
                    for (int i = 0; i <timeTableRows.length(); i++) {   //Goes through all the objects in the inner array, "timeTableRows"
                        JSONObject jsonObject = timeTableRows.getJSONObject(i);

                        String stationShortCode = jsonObject.getString("stationShortCode");
                        String type = jsonObject.getString("type");
                        String scheduledTime = jsonObject.getString("scheduledTime");
                        String trainStopping = jsonObject.getString("trainStopping");
                        if (trainStopping.equals("true")){                  //Only displays the stations where the train stops
                            if (type.equals("DEPARTURE")) {
                                type = "Lähtee: ";              //Extra space to align the time
                            }
                            if (type.equals("ARRIVAL")) {
                                type = "Saapuu:";
                            }

                            if (!previousStation.equals(stationShortCode))  //Don't print same station name twice when train is arriving and departing
                            {
                                jsonResponse += "\n\nAsema:   " + stations.formatStationName(stationShortCode) + "\n\n";
                            }

                            previousStation = stationShortCode;             // Set previous station
                            jsonResponse += type + " ";                     // Arriving or departing
                            jsonResponse += " " + formatScheduledTime(scheduledTime);  //Adds formatted scheduled time

                            if (jsonObject.has("differenceInMinutes")) {    //How many minutes train os late/early, only shows if train is on the move
                                String differenceInMinutes = jsonObject.getString("differenceInMinutes");
                                if(differenceInMinutes.contains("-")) {    //If train is early don't print "+"
                                    jsonResponse += "   " + differenceInMinutes + " min\n";
                                }
                                else
                                {
                                    jsonResponse += "   +" + differenceInMinutes + " min\n";
                                }

                            } else {
                                jsonResponse += "\n";
                            }
                        }

                    }
                    txtResponseTitle.setText(jsonResponseTitle);
                    txtResponse.setText(jsonResponse);

                } catch (JSONException e) {
                    e.printStackTrace();
                      Toast.makeText(getApplicationContext(),
                              "Junaa ei löytynyt" ,
                              Toast.LENGTH_LONG).show();
                            Toast.makeText(getApplicationContext(),
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                }

                hidepDialog();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_SHORT).show();
                hidepDialog();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(req);
    }

    public String formatScheduledTime(String scheduledTime) {
        try {   // Formats scheduled time from inputFormat to outputFormat and sets timezone
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm");
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = inputFormat.parse(scheduledTime);
            return outputFormat.format(d);
        }
        catch (java.text.ParseException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public void setDate(View view) {
        showDialog(999);
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 999) {
            long now = System.currentTimeMillis() - 1000;
            DatePickerDialog dialog = new DatePickerDialog(this, myDateListener, year, month, day);
            dialog.getDatePicker().setMaxDate(now+(1000*60*60*24*7)); // Max date is 1 week after today
            dialog.getDatePicker().setMinDate(now-(1000*60*60*24*21)); // Min date is 3 weeks before
            return dialog;
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
            showDate(arg1, arg2+1, arg3);
        }
    };

    private void showDate(int year, int month, int day) { //Builds date to two different format
        StringBuilder urldate = new StringBuilder();
        StringBuilder titledate = new StringBuilder();

        DecimalFormat mFormat = new DecimalFormat("00");
        urldate.append(year).append("-")
                .append(mFormat.format(Double.valueOf(month))).append("-").append(mFormat.format(Double.valueOf(day)));
        formatUrlDate = urldate.toString(); // yyyy-MM-dd

        titledate.append(day).append(".").append(month).append(".").append(year);
        formatTitleDate = titledate.toString(); // dd.MM.yyyy
    }

    private void showpDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hidepDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
 }