package edu.msoe.supermileagehud.RaspberryPi;

import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.msoe.supermileagehud.ConnectionActivity;
import edu.msoe.supermileagehud.R;

/**
 * Created by Connor on 10/16/2015.
 */
public class UIUpdater
{
    private ConnectionActivity activity;

    private final double MULTIPLIER = (1000.0 / 1.0) * (3600.0 / 1.0) * (2.0 * Math.PI * 10.0) * (1.0 / 12.0) * (1.0 / 5280.0);

    private TextView mSpeedView;
    private TextView mRpmView;
    private TextView mLatencyView;

    public UIUpdater(ConnectionActivity activity)
    {
        this.activity = activity;

        declareElements();
    }

    /**
     * Declares the elements for the layout
     */
    private void declareElements()
    {
        mSpeedView = (TextView) activity.findViewById(R.id.speedLabel);
        mRpmView = (TextView) activity.findViewById(R.id.rpmLabel);
        //mLatencyView = (TextView) activity.findViewById(R.id.latency);
    }

    public void updateUI(JSONArray array)
    {
        //Loop through the JSONArray
        for (int i = 0; i < array.length(); i++)
        {
            try
            {
                JSONObject obj = array.getJSONObject(i);

                final String data = obj.get("data").toString();
                final Object value = obj.get("value");

                //Do the following if the object's data value is equal to...
                if (data.equalsIgnoreCase("speed"))
                {
                    /*
                    mSpeedView.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mSpeedView.setText(value.toString());
                        }
                    });
                    */
                } else if (data.equalsIgnoreCase("rpm"))
                {
                    /*
                    mRpmView.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mRpmView.setText(value.toString());
                        }
                    });
                    */
                } else if (data.equalsIgnoreCase("time"))
                {
                    int timeDifference = Integer.parseInt(value.toString());

                    final double rpMs = (1.0 / timeDifference);

                    final double speed = rpMs * MULTIPLIER;

                    mSpeedView.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mSpeedView.setText(Math.round(speed) + "");
                        }
                    });

                    mRpmView.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mRpmView.setText(Math.round(rpMs * 60000.0) + "");
                        }
                    });

                    /*
                    final double latency = currentTime - System.currentTimeMillis() / 1000L;

                    mLatencyView.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mLatencyView.setText(latency + " ms");
                        }
                    });
                    */
                }
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }
}