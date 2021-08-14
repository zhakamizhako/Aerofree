package aerofree.gg.com.gdtech;

import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class reactCallBack extends ReactContextBaseJavaModule {

    private static Boolean isOn = false;
    SQLiteDatabase db;
    ContextWrapper context = new ContextWrapper(getReactApplicationContext());

    public reactCallBack(ReactApplicationContext reactContext) {
        super(reactContext);
    }


    @ReactMethod
    public void getStatus(Callback successCallback) {
        successCallback.invoke(null, isOn);
    }

    @ReactMethod
    public void turnOn() {
        isOn = true;
        System.out.println("Bulb is turn ON");
    }

    @ReactMethod
    public void getSettings(Callback successCallback){
        db = SQLiteDatabase.openOrCreateDatabase(context.getApplicationInfo().dataDir + "/databases/gdtech", null);
        Cursor c = db.rawQuery("select * from settings", null);
        c.moveToFirst();
        Log.d("Database Debug", DatabaseUtils.dumpCursorToString(c)+"");
        String ip = c.getString(c.getColumnIndex("ip"));
//        String name = c.getString(c.getColumnIndex("name"));
//        String sms = c.getString(c.getColumnIndex("sms_no"));
        Log.d("Database Access", ""+ip);
        successCallback.invoke(ip);
    }

    @ReactMethod
    public void alert(String id, String device_name, String lat, String lng, String ppm_lpg, String status, String updated) {
        Log.e("BROADCASTDETAILS", id);
        Log.e("BROADCASTDETAILS", device_name);
        Log.e("BROADCASTDETAILS", lat);
        Log.e("BROADCASTDETAILS", lng);
        Log.e("BROADCASTDETAILS", ppm_lpg);
        Log.e("BROADCASTDETAILS", status);
        Log.e("BROADCASTDETAILS", updated);

        Intent intent=new Intent("aerofree.gg.com.gdtech.reactBack");
        intent.putExtra("device_id",id);
        intent.putExtra("device_name",device_name);
        intent.putExtra("lat",lat);
        intent.putExtra("lng",lng);
        intent.putExtra("ppm_lpg", ppm_lpg);
        intent.putExtra("status", status);
        intent.putExtra("updated", updated);
        Log.e("Intent", intent.toString());
        Log.e("NOTIFY","KABLAAA");

        context.sendBroadcast(intent);
    }

    @ReactMethod
    public void confirmRegister(){
        db = SQLiteDatabase.openOrCreateDatabase(context.getApplicationInfo().dataDir + "/databases/gdtech", null);
        db.execSQL("UPDATE settings set is_registered = 1 where id=1");
        Intent intent=new Intent("gg.com.gdtech");
        intent.putExtra("registered", "true");
    }

    @ReactMethod
    public void event(String message){
        Toast.makeText(getReactApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public String getName() {
        return "reactCallBack";
    }
}
