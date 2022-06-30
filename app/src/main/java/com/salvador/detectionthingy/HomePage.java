package com.salvador.detectionthingy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.salvador.detectionthingy.data.DetectionData;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;




public class HomePage extends AppCompatActivity {


    private PieChart chart;
    private String SERVER_URL_TV = "http://192.168.1.3/ska/data_fetch.php?itemkey=tv";
    private String SERVER_URL_LAPTOP = "http://192.168.1.3/ska/data_fetch.php?itemkey=laptop";
    private String SERVER_URL_CELLPHONE = "http://192.168.1.3/ska/data_fetch.php?itemkey=cell";

    private final OkHttpClient htc = new OkHttpClient();

    ArrayList<PieEntry> entries = new ArrayList<>();

    private ProgressBar prgLoad;
    private TextView tvStat;

    private Handler h;

    public TextView greetings;
    private TextView big_number;

    private double sideload_out;

    Long secCounter = 0l;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_home_page);


        //Button to Graph
        ImageButton btn = (ImageButton) findViewById(R.id.home_button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomePage.this, MonitoringData.class));
                finish();
            }
        });

        //Greeting Write Function?
        greetings = (TextView)findViewById(R.id.textView2); //this is the text for the greeting thing
        greetings.setText(Greeting());

        //The Big Number
        big_number = (TextView)findViewById(R.id.tvMinuteCount); //this is the text for the number
        big_number.setText(compute_sum());


        h = new Handler(this.getMainLooper());

        prgLoad = (ProgressBar)findViewById(R.id.prgLoadingData);
        tvStat = (TextView)findViewById(R.id.tvLoadingStat);


        entries.clear();

        fetchData(SERVER_URL_TV,"TV");
        fetchData(SERVER_URL_LAPTOP,"Laptop");
        fetchData(SERVER_URL_CELLPHONE,"Cellphone");

        startTimer();

    }


    private String compute_sum() {

        int sum = Math.toIntExact(Math.round(sideload_out));
        String output = String.valueOf(sum);
        Log.d("This is the sum", output);
        return output;
    }



    private void fetchData(String url,String searchKey){

        Request rq = new Request.Builder()
                .url(url)
                .build();

        prgLoad.setVisibility(View.VISIBLE);
        tvStat.setVisibility(TextView.VISIBLE);
        tvStat.setText("Loading data...");

        htc.newCall(rq).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("FAIL","Data fetch fail: " + e.getMessage());

                Snackbar.make(chart,"Fetch from server failed.",Snackbar.LENGTH_LONG).show();

                h.post(new Runnable() {
                    @Override
                    public void run() {
                        prgLoad.setVisibility(View.GONE);
                        tvStat.setVisibility(TextView.GONE);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Gson gg = new Gson();
                String respData = response.body().string();

                Log.d("RAWDATA" + searchKey,respData);
//        ResultDataSet rds = gg.fromJson(SampleData.getDataString(this), ResultDataSet.class);
                DetectionData[] ddata = gg.fromJson(respData, DetectionData[].class);

                List<Long> listDiff = new ArrayList<>();

                for(int i=0;i<ddata.length;i++){
//                    Log.d("DATA" +searchKey,ddata[i].object_enum + "|" + ddata[i].confidence + "|" + ddata[i].img_timestamp);

                    try {
                        if (i+1 < ddata.length){

                            Date d1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ddata[i].img_timestamp);
                            Date d2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ddata[i+1].img_timestamp);

                            Long cDiff = Long.valueOf(d2.getTime()-d1.getTime()) / 1000;

                            if (cDiff <= 600){
                                listDiff.add(cDiff);
//                                Log.d("DIFF " + searchKey,"Difference: " + cDiff.toString());
                            }
                            else{
                                Log.d("THRES_LIMIT " + searchKey,"Disregarded due to long time gap:" + cDiff.toString());
                            }
                        }
                        else{
                            break;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
//                entries.add(new PieEntry((float) (getTotalDifference(listDiff).floatValue()),
//                        searchKey,
//                        getResources().getDrawable(R.drawable.ic_launcher_foreground)));

                entries.add(new PieEntry((getTotalDifference(listDiff).floatValue()),
                        searchKey));


//                data.setValueFormatter(new PercentFormatter());



                Log.d("Total difference " + searchKey,"DIFF:"+getTotalDifference(listDiff).toString());


                h.post(new Runnable() {
                    @Override
                    public void run() {
                        prgLoad.setVisibility(View.GONE);
                        tvStat.setVisibility(TextView.GONE);
                    }
                });
            }
        });

    }

    private void startTimer(){

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("DATA",secCounter.toString());
                secCounter++;
                if(secCounter % 60 == 0){
                    big_number.setText(Long.valueOf(secCounter / 60).toString());
                    Snackbar.make(tvStat,"60 seconds has passed",Snackbar.LENGTH_LONG).show();
                }

                h.postDelayed(this,1000);
            }
        },1000);
    }



    private Double getTotalDifference(List<Long> diffs){

        Long totDiff = 0l;

        for(int i=0;i<diffs.size();i++){
            totDiff += diffs.get(i);
        }

        return Double.valueOf(totDiff.doubleValue() / 3600);
    }



    private SpannableString generateCenterSpannableText() {

        SpannableString s = new SpannableString("Activity in hrs");
        s.setSpan(new RelativeSizeSpan(1.7f), 0, s.length(), 0);
//        s.setSpan(new StyleSpan(Typeface.NORMAL), 18, s.length() - 15, 0);

//        s.setSpan(new ForegroundColorSpan(Color.GRAY), 19, s.length() - 15, 0);
//        s.setSpan(new RelativeSizeSpan(.8f), 19, s.length() - 15, 0);
//        s.setSpan(new StyleSpan(Typeface.ITALIC), s.length() - 15, s.length(), 0);
//        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), s.length() - 15, s.length(), 0);
        return s;
    }

    private String Greeting() {
        //Get the time of day
        Calendar c = Calendar.getInstance();
        int datetime = c.get(Calendar.HOUR_OF_DAY);

        String greeting = null;
        if(datetime>=6 && datetime<12){
            greeting = "Good Morning!";
        } else if(datetime>= 12 && datetime < 17){
            greeting = "Good Afternoon!";
        } else if(datetime >= 17 && datetime < 24){
            greeting = "Good Evening!";
        }

        return greeting;
    }


}