package com.example.ajinkya.chatappwithfirebase;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    final String BASE_URL = "https://test-helloworld-f1323.firebaseio.com/";

    final String URL_APPEND = ".json";

    Spinner spinner;
    String theURL = null;
    URL url = null;
    HttpURLConnection urlConnection = null;
    BufferedReader bufferedReader = null;

    HashMap<String, String> roomHash;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //https://developer.android.com/guide/topics/ui/controls/spinner.html#Populate
        // |cursor adapter for dynamic spinner item generation from DB
        // Could also get room names by querying room_names/$id/title
        spinner = (Spinner) findViewById(R.id.room_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.room_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        roomHash = new HashMap<>();
        roomHash.put("Room Charlie","one");
        roomHash.put("Room Brown","two");

        TextView textView = (TextView) findViewById(R.id.chatText);
        textView.setText("");
    }

    protected void onStart()
    {
        super.onStart(); // necessary call to the super class onStart method

        TextView textView = (TextView) findViewById(R.id.chatText);
        textView.setText("");

        //Log.e("Spinner value ==",spinner.getSelectedItem().toString());
        new fetchData().execute(spinner.getSelectedItem().toString());
    }

    private class fetchData extends AsyncTask<String,Void,LinkedHashMap<String,String>>
    {
        @Override
        protected LinkedHashMap<String,String> doInBackground(String... param)
        {
            //Log.e("Spinner in AyncTask =",param[0]);
            // GET data from DB if present
            LinkedHashMap<String,String> msgMap = null;

            try
            {
                //Log.e("Spinner element ====",spinner.getSelectedItem().toString());
                theURL = new String(BASE_URL+"messages/"+roomHash.get(param[0])+URL_APPEND);//retrieve the value displayed
                //Log.e("Constructed URL == ",theURL);


                // GET request for messages
                url = new URL (theURL);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                String line;
                StringBuffer strBuffer = new StringBuffer();
                String messageJsonStr = null;

                while ((line = bufferedReader.readLine()) != null)
                {
                    strBuffer.append(line+ "\n");
                }

                if (strBuffer.length() == 0)
                {
                    messageJsonStr = null;
                }
                // else { }
                messageJsonStr = strBuffer.toString();
                //Log.i("Message JSON str = ",messageJsonStr);

                // Parse returned JSON and populate textview

                JSONObject jsonObject = new JSONObject(messageJsonStr);

                Iterator<String> itr = jsonObject.keys(); // iterate over returned JSON keys, no matter their value. Here its m1,m2,FBCREATEDSTR,etc

                msgMap = new LinkedHashMap<>();

                while (itr.hasNext())
                {
                    String key = itr.next();
                    try{
                        JSONObject msgBody = jsonObject.getJSONObject(key); // get the message body for the related key
                        // make an entry in DB called timePosted, and use that int to sort msgs here
                        // msgMap.sortDescending(timePosted) after all values are in
                        // or maybe LinkedHasMap is the answer
                        msgMap.put(msgBody.getString("sender"), msgBody.getString("content"));

                    }
                    catch (Exception ex) {ex.printStackTrace();}
                }

                bufferedReader.close();

            }
            catch(Exception ex) { ex.printStackTrace(); }
            finally { urlConnection.disconnect(); }

            return msgMap; // gets passed to onPostExecute, hence the return
        }

        @Override
        protected void onPostExecute(LinkedHashMap<String,String> result)
        {
            // Gets called on the UI thread so textview append works here
            TextView chatView = (TextView) findViewById(R.id.chatText);
            chatView.setText("");

            for (Map.Entry<String,String> entry : result.entrySet())
            {
                chatView.append(entry.getKey()+": "+entry.getValue());
                chatView.append("\n");
            }
        }
    }

    private class postData extends AsyncTask<String,Void,Void>
    {
        @Override
        protected Void doInBackground(String... params)
        {
            try
            {
                //Log.e("Spinner element ====",spinner.getSelectedItem().toString());
                theURL = new String(BASE_URL+"messages/"+roomHash.get(params[1])+URL_APPEND);
                Log.e("POST URL == ",theURL);

                url = new URL (theURL);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);// for sending payloads
                urlConnection.setRequestMethod("POST");

                OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                out.write(params[0]);
                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                Log.i("Message ID=",in.readLine()); // POST Success
                in.close();
            }
            catch(Exception ex) { ex.printStackTrace(); }
            finally { urlConnection.disconnect(); }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            // Gets called on the UI thread so textview append works here
            // Done with POSTing, clear...
            TextView chatView = (TextView) findViewById(R.id.chatText);
            chatView.setText("");

            // ... and refresh view
            new fetchData().execute(spinner.getSelectedItem().toString());
        }
    }

    // Spinner item selected code block
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id)
    {
        // An item was selected. You can retrieve the selected item using
        //Log.e("SPINNER=",(String)parent.getItemAtPosition(pos));
        Toast.makeText(this,(String)parent.getItemAtPosition(pos),Toast.LENGTH_SHORT).show();

        new fetchData().execute(spinner.getSelectedItem().toString());
    }

    public void onNothingSelected(AdapterView<?> parent)
    {
        // Another interface callback
    }
    /////////////////////////////////////

    // Send button click handler
    public void processMessage(View view)
    {
        //Log.e("Process=","Inside");
        // inputs are not blank
        EditText user_text = (EditText) findViewById(R.id.user_message);
        EditText user_name = (EditText) findViewById(R.id.username);
        //Log.e("User_name=",user_name.getText().toString());

        if (user_name.getText().toString().equals("") || user_name.getText().toString().equals(" "))
        {
            Toast.makeText(this,"User name is required",Toast.LENGTH_SHORT).show();
            user_name.setText("");
            user_name.requestFocus();
        }
        else
        {
        // get message from edit texts and transmit POST - FB.push

            String msgToPost = "{\"content\":\""+user_text.getText()+"\",\"sender\":\""+user_name.getText()+"\"}";

            new postData().execute(msgToPost,spinner.getSelectedItem().toString());
        }

        user_text.setText("");
        user_name.setText("");
        user_name.requestFocus();
        // GET new data and re populate textview
        // done in PostExecute of postData AsyncTask
    }
    ////////////////////////////////////
}
