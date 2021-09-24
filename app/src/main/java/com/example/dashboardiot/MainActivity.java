package com.example.dashboardiot;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.ybq.android.spinkit.SpinKitView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

class MyTimer {
    static int NumberOfFeeds = 2;
    static int MaxRepeat = 3;

    public int counter = 0;
    public boolean againflag = false;
    public boolean stop = true;
    public int repetition = 0;

    public Queue<String> buffer = new LinkedList<>();

    public void init() {
        this.counter = 2;
        this.againflag = false;
        this.stop = false;
        this.repetition = 0;
    }
    public void update() {
        if(this.counter > 0) {
            this.counter--;
            if(this.counter == 0) {
                this.againflag = true;
                if(this.repetition < this.MaxRepeat) {
                    this.repetition++;
                }
            }
        }
    }
    public void resetNewRepetition() {
        this.counter = 2;
        this.againflag = false;
    }

};

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    public static ToggleButton btnLED;
    public static ToggleButton btnPUMP;
    TextView txtHumid, txtTemp;
    SpinKitView spin;

    String []feeds = {"iot-led", "iot-pump"}; // feeds which are received message from mobile
    Hashtable<String, MyTimer> dictFeed = new Hashtable<String, MyTimer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupScheduler();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtTemp = findViewById(R.id.txtTemperature );
        txtHumid = findViewById(R.id.txtHumidity);

        btnLED = findViewById(R.id.btnLED);
        btnPUMP = findViewById(R.id.btnPUMP);

        spin = findViewById(R.id.spin_kit);

        spin.setVisibility(View.INVISIBLE);

        txtTemp.setText("30°C");
        txtHumid.setText("79%");

        MyTimer.NumberOfFeeds = feeds.length;
        for(int i = 0; i < MyTimer.NumberOfFeeds; i++) {
            dictFeed.put(feeds[i], new MyTimer());
        }


        Http testAPI = new Http();
        testAPI.execute();

        btnLED.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    Log.d("Mqtt", "Button is ON");
                }
                else{
                    Log.d("Mqtt", "Button is OFF");
                }
            }
        });

        btnLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spin.setVisibility(View.VISIBLE);
                MyTimer tempTimer = dictFeed.get("iot-led");

                tempTimer.init();
                Log.d("Mqtt", "Button's state is being sent");
                if(((ToggleButton)view).isChecked()) {
                    sendDataMQTT("phamdinhtrung/feeds/iot-led", "1");
                    tempTimer.buffer.add("1");
                }
                else {
                    sendDataMQTT("phamdinhtrung/feeds/iot-led", "0");
                    tempTimer.buffer.add("0");
                }
            }
        });




        btnPUMP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    Log.d("Mqtt", "Button is ON");
                }
                else{
                    Log.d("Mqtt", "Button is OFF");
                }
            }
        });

        btnPUMP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spin.setVisibility(View.VISIBLE);
                MyTimer tempTimer = dictFeed.get("iot-pump");

                tempTimer.init();
                Log.d("Mqtt", "Button's state is being sent");
                if(((ToggleButton)view).isChecked()) {
                    sendDataMQTT("phamdinhtrung/feeds/iot-pump", "1");
                    tempTimer.buffer.add("1");
                }
                else {
                    sendDataMQTT("phamdinhtrung/feeds/iot-pump", "0");
                    tempTimer.buffer.add("0");
                }
            }
        });

        startMQTT();
    }

    private void setupScheduler() { // setup scheduler
        Timer aTimer = new Timer();
        TimerTask scheduler = new TimerTask() {
            @Override
            public void run() {

                for(int i = 0; i < MyTimer.NumberOfFeeds; i++) {
                    MyTimer tempTimer =  dictFeed.get(feeds[i]);
                    if(tempTimer.stop) continue;

                    tempTimer.update();
                    if(tempTimer.againflag) {
                        if(tempTimer.repetition < MyTimer.MaxRepeat) {
                            tempTimer.resetNewRepetition();
                            Log.d("Mqtt", "Button's state is sent again");
                            sendDataMQTT("phamdinhtrung/feeds/" + feeds[i], tempTimer.buffer.peek());
                        }
                        else {
                            tempTimer.stop = true;
                            tempTimer.buffer.remove();
                            spin.setVisibility(View.INVISIBLE);

                            if(tempTimer.buffer.isEmpty() == false) { // send remain data in queue
                                tempTimer.init();
                            }
                        }
                    }
                }


            }
        };
        aTimer.schedule(scheduler, 1000, 1000);
    }

    private void startMQTT() {
        mqttHelper = new MQTTHelper(getApplicationContext(), "0792010");
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("Mqtt", "Connection is successfull");
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("Mqtt", "Received: " +  message.toString() + " from " + topic);
                if(topic.contains("iot-temp")) {
                    txtTemp.setText(message.toString()+"°C");
                }
                else if(topic.contains("iot-humid")) {
                    txtHumid.setText(message.toString()+"%");
                }
                else if(topic.contains("iot-led")) {
                    if(message.toString().equals(dictFeed.get("iot-led").buffer.peek())) { // validate a response by check with queue's front
                        Log.d("Mqtt", "Button's state is sent successfully");
                        dictFeed.get("iot-led").buffer.remove();
                        dictFeed.get("iot-led").stop = true;

                        if(dictFeed.get("iot-led").buffer.isEmpty() == false) { // send remain data in queue
                            dictFeed.get("iot-led").init();
                        }

                        spin.setVisibility(View.INVISIBLE);
                    }
                    if(message.toString().equals("1") ) { // update the button on app
                        btnLED.setChecked(true);
                    }
                    else {
                        btnLED.setChecked(false);
                    }
                }
                else if(topic.contains("iot-pump")) {
                    if(message.toString().equals(dictFeed.get("iot-pump").buffer.peek())) { // validate a response by check with queue's front
                        Log.d("Mqtt", "Button's state is sent successfully");
                        dictFeed.get("iot-pump").buffer.remove();
                        dictFeed.get("iot-pump").stop = true;

                        if(dictFeed.get("iot-pump").buffer.isEmpty() == false) { // send remain data in queue
                            dictFeed.get("iot-pump").init();
                        }

                        spin.setVisibility(View.INVISIBLE);
                    }
                    if(message.toString().equals("1") ) { // update the button on app
                        btnPUMP.setChecked(true);
                    }
                    else {
                        btnPUMP.setChecked(false);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    private void sendDataMQTT(String topic, String value) {
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);


        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            spin.setVisibility(View.VISIBLE);
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        }catch (Exception e){}

    }
}