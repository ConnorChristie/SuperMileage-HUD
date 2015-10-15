package edu.msoe.supermileagehud;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
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
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionActivity extends AppCompatActivity
{
    //The port that is forwarded by ADB, the pi connects to us through this port
    private static final int PORT = 5001;

    private ServerSocket serverSocket;

    private TextView mSpeedView;
    private TextView mRpmView;
    private TextView mLatencyView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        declareElements();

        try
        {
            //Start the socket server
            startServerSocket();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        //Example of a floating button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

            }
        });
    }

    /**
     * Declares the elements for the layout
     */
    private void declareElements()
    {
        mSpeedView = (TextView) findViewById(R.id.speedLabel);
        mRpmView = (TextView) findViewById(R.id.rpmLabel);
        mLatencyView = (TextView) findViewById(R.id.latency);
    }

    /**
     * Starts the server socket to begin listening for the raspberry pi
     *
     * @throws IOException
     */
    private void startServerSocket() throws IOException
    {
        serverSocket = new ServerSocket(PORT);

        new Thread(new ConnectorThread()).start();
    }

    /**
     * The thread that waits for the connection from the raspberry pi
     */
    private class ConnectorThread implements Runnable
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    Socket clientSocket = serverSocket.accept();

                    //Start new thread for the communication
                    new Thread(new ConnectionThread(clientSocket)).start();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * The thread that communicates to the raspberry pi
     * Receive and send data to and from the pi
     */
    private class ConnectionThread implements Runnable
    {
        private Socket socket;

        public ConnectionThread(Socket socket)
        {
            this.socket = socket;
        }

        @Override
        public void run()
        {
            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                String line;

                while ((line = in.readLine()) != null)
                {
                    try
                    {
                        final JSONArray value = new JSONArray(line);

                        ConnectionActivity.this.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //Loop through the JSONArray
                                for (int i = 0; i < value.length(); i++)
                                {
                                    try
                                    {
                                        JSONObject obj = value.getJSONObject(i);

                                        String data = obj.get("data").toString();
                                        Object value = obj.get("value");

                                        //Do the following if the object's data value is equal to...
                                        if (data.equalsIgnoreCase("speed"))
                                        {
                                            mSpeedView.setText(value.toString());
                                        } else if (data.equalsIgnoreCase("rpm"))
                                        {
                                            mRpmView.setText(value.toString());
                                        } else if (data.equalsIgnoreCase("time"))
                                        {
                                            long latency = Long.parseLong(value.toString()) - System.currentTimeMillis() / 1000L;

                                            mLatencyView.setText(latency + " ms");
                                        }
                                    } catch (JSONException e)
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }

                in.close();
                out.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
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
