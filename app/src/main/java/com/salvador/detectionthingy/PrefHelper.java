package com.salvador.detectionthingy;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PrefHelper {
    private SharedPreferences sharedPreferences;
    public PrefHelper(Context ct){
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ct);
    }

    public void saveQuery(Long minSave){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();

        SharedPreferences.Editor ee = this.sharedPreferences.edit();
        ee.putLong("qstr",minSave);
        ee.putString("ddd",formatter.format(date).toString());

        ee.commit();
    }

    public Long getLastQuery(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();

        String currentdate = formatter.format(date).toString();
        String lastdaterecorded = this.sharedPreferences.getString("ddd","none");

        if (currentdate.equals(lastdaterecorded)){
            return this.sharedPreferences.getLong("qstr",0l);
        }
        else{
            saveQuery(0l);
            return this.sharedPreferences.getLong("qstr",0l);
        }
    }
}
