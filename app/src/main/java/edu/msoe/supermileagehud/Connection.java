package edu.msoe.supermileagehud;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Connection extends AppCompatActivity
{
    private Socket socket;

    private BufferedReader socketIn;
    private PrintStream socketOut;

    private TextView speedView;
    private TextView rpmView;
    private TextView latencyView;

    private long startTime;
    private BufferedWriter logWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        speedView = (TextView) findViewById(R.id.speedLabel);
        rpmView = (TextView) findViewById(R.id.rpmLabel);
        latencyView = (TextView) findViewById(R.id.latency);

        //Create new log file
        createLogFile();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ServerSocket serverSocket = null;
                        try
                        {
                            serverSocket = new ServerSocket(5001);

                            while (true)
                            {
                                Socket clientSocket = serverSocket.accept();
                                //Waiting for raspberry pi to connect to android device
                                Log.d("Connecting", "Waiting for connection.....");
                                try
                                {
                                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                                    String line = in.readLine();

                                    out.println("out: " + line);

                                    in.close();

                                    out.flush();
                                    out.close();
                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                }

                            }
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }

                        Log.d("epics", "Connection successful");
                        Log.d("epics", "Waiting for input.....");
                    }
                };
                new Thread(r).start();

                //new ConnectionTask().execute();
            }
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        try
        {
            logWriter.flush();
            logWriter.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //Create new log file
        createLogFile();
    }

    private class ConnectionTask extends AsyncTask<Void, JSONArray, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                System.out.println("Starting");

                socket = new Socket("localhost", 5052);

                socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                socketOut = new PrintStream(socket.getOutputStream());

                String str = "{\"method\":\"speed\", \"value\":\"20\"}";

                socketOut.write(ByteBuffer.allocate(4).putInt(str.length()).array());
                socketOut.print(str);

                String line;

                while ((line = socketIn.readLine()) != null)
                {
                    /*
                    try
                    {
                        publishProgress(new JSONArray(line));
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                    */
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(JSONArray... values)
        {
            String logData = (System.currentTimeMillis() - startTime) + "";

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

                        logData += "," + value.toString();
                    } else if (data.equalsIgnoreCase("rpm"))
                    {
                        rpmView.setText(value.toString());

                        logData += "," + value.toString();
                    } else if (data.equalsIgnoreCase("time"))
                    {
                        long latency = System.currentTimeMillis() / 1000L - Long.parseLong(value.toString());

                        System.out.println(System.currentTimeMillis() + ", " + Long.parseLong(value.toString()));
                        System.out.println("Latency: " + latency);

                        latencyView.setText(latency + " ms");

                        logData += "," + latency;
                    }
                } catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }

            try
            {
                logWriter.write(logData + "\n");
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            //Snackbar.make(Connection.this.getWindow().getDecorView().getRootView(), values[0], Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    private void createLogFile()
    {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SuperMileage-Logs");

            dir.mkdirs();

            DateFormat df = new SimpleDateFormat("MM-dd-yy_hh:mm:ss");

            File logFile = new File(dir, "Log_" + df.format(System.currentTimeMillis()) + ".csv");

            try
            {
                logFile.createNewFile();

                FileWriter fw = new FileWriter(logFile.getAbsoluteFile());
                logWriter = new BufferedWriter(fw);

                logWriter.write("Time,Speed,RPM,Latency\n");
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        } else
        {
            System.out.println("Log file directory is not writable");
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
