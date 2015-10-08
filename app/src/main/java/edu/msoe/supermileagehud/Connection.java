package edu.msoe.supermileagehud;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Connection extends AppCompatActivity
{
    private Socket socket;

    private BufferedReader socketIn;
    private PrintStream socketOut;

    private TextView speedView;
    private TextView rpmView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        speedView = (TextView) findViewById(R.id.speedLabel);
        rpmView = (TextView) findViewById(R.id.rpmLabel);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new ConnectionTask().execute();
            }
        });
    }

    private class ConnectionTask extends AsyncTask<Void, JSONArray, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                System.out.println("Starting");

                DatagramSocket serverSocket = new DatagramSocket(2004);

                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                System.out.println("Created");

                while (true)
                {
                    serverSocket.receive(receivePacket);

                    try
                    {
                        String line = new String(receivePacket.getData(), 0, receivePacket.getLength());

                        publishProgress(new JSONArray(line));
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }

                /*
                socket = new Socket("192.168.1.3", 50000);

                socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                socketOut = new PrintStream(socket.getOutputStream());

                String str = "{\"method\":\"speed\", \"value\":\"20\"}";

                socketOut.write(ByteBuffer.allocate(4).putInt(str.length()).array());
                socketOut.print(str);

                String line;

                while ((line = socketIn.readLine()) != null)
                {
                    try
                    {
                        publishProgress(new JSONArray(line));
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
                */
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(JSONArray... values)
        {
            for (int i = 0; i < values[0].length(); i++)
            {
                try
                {
                    JSONObject obj = values[0].getJSONObject(i);

                    String data = obj.get("data").toString();
                    Object value = obj.get("value");

                    if (data.equalsIgnoreCase("speed"))
                    {
                        speedView.setText(value.toString());
                    } else if (data.equalsIgnoreCase("rpm"))
                    {
                        rpmView.setText(value.toString());
                    }
                } catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }

            //Snackbar.make(Connection.this.getWindow().getDecorView().getRootView(), values[0], Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
