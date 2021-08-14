package aerofree.gg.com.gdtech;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
implements OnMapReadyCallback, LocationListener, NavigationView.OnNavigationItemSelectedListener {
    int MY_PERMISSION_REQUEST_ACCESS_LOCATION;
    GoogleMap mMap;//Google Map Object
    //MapView mapaView;//MapView Object
    //TextView lala;
    TextView textView2;//A textView in the List of Devices frame
    TextView Textnotif;//Textview in the Notification page.
    TextView text_ip;
    TextView text_port;
    LatLng currentPos;

    boolean alertDialog=false;

    LocationManager locationManager;
    Location location;
    String mprovider;
    int viewmode = 0;//viewmode Checker
    int initialnotif = 2;//Checkers before the First Initial Notification should pop up. (Not necessary)
    double levelvalue1 = 0;//???
    String fala;
    SQLiteDatabase db;
    Timer timer = new Timer();
    boolean hidden = false;
    AlertDialog.Builder builder, builder2, alertDi;
    int started = 0;
    String locode[] = {};
    String ip, sms_no, websocket_ip;
//    int delay=1;
    int isRegistered=0;
    BroadcastReceiver receiver;
    IntentFilter filter;
    boolean alert = false;
    boolean aware = false;


    DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("Broadcast","Broadcast Received!");
                String device_name = intent.getStringExtra("device_name");
                String device_id = intent.getStringExtra("device_id");
                String lat = intent.getStringExtra("lat");
                String lng = intent.getStringExtra("lng");
                String ppm_lpg = intent.getStringExtra("ppm_lpg");
                String status = intent.getStringExtra("status");
                if(intent.hasExtra("registered")){
                    isRegistered = 1;
                }
                Log.d("TriggerChcek:","");
//                Toast.makeText(context,"Received:"+device_name+ "  "+device_id+"|"+lat+"|"+lng+"|"+ppm_lpg+"--"+status+"", Toast.LENGTH_SHORT).show();
                update(device_name, device_id, lat, lng, ppm_lpg, status);
            }
        };

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("This application does not work without GPS! Please Allow permission.")
                        .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }

                        });
                builder.create();
                builder.show();
            }else{
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_ACCESS_LOCATION);
            }
        }

        filter = new IntentFilter();
        // specify the action to which receiver will listen
        filter.addAction("aerofree.gg.com.gdtech.reactBack");
        Log.e("IntentReceiver", receiver.toString());
        Log.e("IntentReceiver", filter.toString());
        registerReceiver(receiver,filter);

        super.onCreate(savedInstanceState);
        createNotificationChannel();

        setContentView(R.layout.activity_selector);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Log.d("Database","Checking Database.");
        checkDatabase();

    }

    @Override
    public void onStart(){
        super.onStart();
        if(started==0) {
            viewmode = 2;
            Fragment frag = new map_view();
            translateView(frag);
            started=1;
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AeroFree";
            String description = "AeroFree Notification Channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("CHANNEL1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void update(String name, String id, String lat, String lng, String ppm_lpg,  String status){
        boolean notify = false;
        int condition = 0;
        Log.d("Database Updater","Updaaaaate");
        Log.d("Wtfs!",
                "ID:" + id + "\n" +
                "Name:" + name + "\n" +
                        "Lat:" + lat + "\n"+
                        "Lng:" + lng + "\n"+
                        "ppm_lpg:" + ppm_lpg + "\n"+
                "Status:" + status + "\n");

            Toast.makeText(this, "Device Hasn't registered yet.", Toast.LENGTH_LONG);
            Cursor c = db.rawQuery("select * from devices where id="+id+";", null);
            c.moveToFirst();
            String file = DatabaseUtils.dumpCursorToString(c);
            Log.d("Wtfs", file);
            if(c.getCount()!=0) {
                Double db_ppm_lpg = Double.parseDouble(ppm_lpg);
                String db_status = (c.getString(c.getColumnIndex("status")));
                Log.d("ftws", "Triggered1:" + status + "Triggered2:" + (c.getString(c.getColumnIndex("status"))));

                try {
                    if (Integer.parseInt(c.getString(c.getColumnIndex("condition"))) != condition) {
                        notify = true;
                        Log.d("CDN1", "Condition 1 Changed!");
                    }
                    Log.d("", "Triggered1:" + status + "Triggered2:" + (c.getString(c.getColumnIndex("status"))));
                    String tt1 = (db_status);
                    String tt2 = (status);
                    if (tt2 != tt1) {
                        notify = true;
                        Log.d("CDNCHECKER", "Trigger1:" + db_status + "Trigger2+" + status);
                        Log.d("CDN2", "Condition 2 Changed!");
                    }
                } catch (Exception e) {
                    Log.d("DBError", "Prolly is first time.");
                    Log.e("DBError", e.toString());
                    e.printStackTrace();
                }
            }

            if(c.isAfterLast()){
                db.execSQL("INSERT INTO devices(id, name, lat, lng, ppm_lpg, condition, status) VALUES " +
                        "('"+id+"', " +
                        "'"+name+"', " +
                        "'"+lat+"', " +
                        "'"+lng+"', " +
                        "'"+ppm_lpg+"', " +
                        "'"+condition+"', " +
                        "'"+status+"');");
                Log.d("Database Update","Inserted New Record!");
            }else{
                db.execSQL("UPDATE devices set " +
                        "name='"+name+"',"+
                        "lat='"+lat+"'," +
                        "lng='"+lng+"'," +
                        " status='"+status+"'," +
                        " ppm_lpg='"+ppm_lpg+"'," +
                        " condition='"+condition+"'" +
                        " where id="+id);
                Log.d("Database Update","Updated A Record!");
            }
            populateList();
            Log.d("update_map?", notify + "eh");

            if(notify==true) {
//                notifyChanges(condition, name, ppm_lpg);
                populateMap();
                startTracking();
            }
    }

    public void checkDatabase(){
        try{
            Log.d("mobileDB","Parsing Database...");
            db = this.openOrCreateDatabase("gdtech",MODE_PRIVATE,null);
            Log.d("wtf","Passed1");
            Cursor c = db.rawQuery("select * from settings", null);
            Log.d("wtf","Loaded Cursor");

            Log.d("wtf","Moved Cursor");
            c.moveToFirst();
                ip = c.getString(c.getColumnIndex("ip"));
                websocket_ip = c.getString(c.getColumnIndex("websocket_ip"));
                sms_no = c.getString(c.getColumnIndex("sms_no"));
                isRegistered = c.getInt(c.getColumnIndex("is_registered"));

                Log.d("TEST","isRegistered:"+c.getInt(c.getColumnIndex("is_registered")));
            if(isRegistered==0){
                initialSettings();
            }else{
                startReact();
            }

        }catch (Exception e){
            Log.d("Database", "No database found. Creating.");
            createDatabase();
        }
    }

    public void initialSettings(){
        Log.d("Initial Settings Dialog", "Yo.");
        builder2 = null;
        builder2 = new AlertDialog.Builder(this);
        final EditText input_ip = new EditText(this);
        input_ip.setInputType(InputType.TYPE_CLASS_TEXT);
        input_ip.setHint("e.g. 192.168.43.1 or api.aerofree.com");
        builder2.setCancelable(false);
        builder2.setView(input_ip);
        builder2.setTitle("Enter Initial Settings")
                .setMessage("Before we begin, You'll need to enter the IP Address of the running server for AeroFree. Please consult the developers / Maintainer for help on this guide. ")

                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        db.execSQL("UPDATE settings set ip = '"+input_ip.getText().toString()+"' where id=1");
                        ip = input_ip.getText().toString();
                        startReact();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder2.show();
            }
        });
    }

    public void createDatabase(){
        try{
            db =this.openOrCreateDatabase("gdtech", MODE_PRIVATE, null);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
                            db.execSQL("CREATE TABLE IF NOT EXISTS  settings (" +
                                    "id INT PRIMARY KEY,"+
                                    "ip TEXT," +
                                    "websocket_ip TEXT," +
                                    "is_registered INT) ");

                            db.execSQL("CREATE TABLE IF NOT EXISTS devices (" +
                                    "id INT PRIMARY KEY," +
                                    "lat FLOAT," +
                                    "lng FLOAT," +
                                    "status TEXT," +
                                    "name TEXT," +
                                    "ppm_lpg FLOAT,"+
                                    "updated DATETIME,"+
                                    "condition int,"+
                                    "misc TEXT) ");
                            db.execSQL("INSERT INTO settings (id, ip) VALUES (1, '192.168.1.1')");
                            initialSettings();

        } catch(Exception error){
            Log.e("Database Error",""+error);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        hidden = false;

    }
    public void onPause(){
        super.onPause();
        hidden = true;

    }
    public void notifyChanges() {

                Log.d("Minimized","Sending Notification");

        Intent fullScreenIntent = new Intent(this, MainActivity.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + this.getPackageName() + "/" + R.raw.alert);  //Here is FILE_NAME is the name of file that you want to play

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL1")
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle("AeroFree")
                        .setContentText("Caution, You're inside the vicinity of an active gas leak.")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Caution, You're inside the vicinity of an active gas leak."))
                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
                        .setSound(sound)
                        .setFullScreenIntent(fullScreenPendingIntent, true);


                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(1, builder.build());

    }

    public void populateList(){
        if(viewmode==1) {
            textView2.setText("");
            Cursor c = db.rawQuery("Select * from devices", null);
            try {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    String id = c.getString(c.getColumnIndex("id"));
                    String lat = c.getString(c.getColumnIndex("lat"));
                    String lng= c.getString(c.getColumnIndex("lng"));
                    String name = c.getString(c.getColumnIndex("name"));
                    Double ppm_lpg = Double.parseDouble(c.getString(c.getColumnIndex("ppm_lpg")));
                    String status = c.getString(c.getColumnIndex("status"));
                    String message = "";
                    if(ppm_lpg!=0){
                        message= "Gas Detected!";
                    }else{
                        message = "None";
                    }
                    textView2.append(
                            "Device Name:" + name +
                            "\nDevice ID:" + id +
                                    "\nLPG:" + ppm_lpg + "ppm" +
                                    "\nDetected:" + message + "" +
                                    "\n------------------------------\n\n"
                    );
                }
            } catch (Exception e) {

            }
        }
    }

    public void satteliteview1(View v){
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }
    public void mapview1(View v){
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public void populateMap(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMap.clear();
            }
        });

        Log.d("MapPopulate","Querying Database...");
        try{
            Cursor c = db.rawQuery("Select * from devices",null);

            for(c.moveToFirst();!c.isAfterLast();c.moveToNext()){
                String debug = DatabaseUtils.dumpCursorToString(c);
                Log.d("FTW", debug);
                String id = c.getString(c.getColumnIndex("id"));
                String lat = c.getString(c.getColumnIndex("lat"));
                String lng= c.getString(c.getColumnIndex("lng"));
                String name = c.getString(c.getColumnIndex("name"));
                Double ppm_lpg = Double.parseDouble(c.getString(c.getColumnIndex("ppm_lpg")));

                String status = (c.getString(c.getColumnIndex("status")));
                Log.d("PARSE DATABASE",
                        "ID: " + id +
                "Device Name:" + name +
                        "LPG:" + ppm_lpg +
                        "Status:" + status);

                if(ppm_lpg>100){
                    final CircleOptions circle1 = new CircleOptions().strokeWidth(1);
                    circle1.center(new LatLng(Float.parseFloat(lat), Float.parseFloat(lng)));
                    circle1.radius(100);
                    circle1.fillColor(0x55FF0000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMap.addCircle(circle1);
                        }
                    });
//                    Toast.makeText(this, "circle...", Toast.LENGTH_SHORT).show();
                }

                LatLng pos = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                final MarkerOptions deviceMarker = new MarkerOptions()
                        .position(pos)
                        .title(name)
                        .snippet("LPG:"+ppm_lpg+"ppm");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMap.addMarker(deviceMarker);
                    }
                });

            }
        } catch (Exception e){
            Log.e("Ftw","Probably No Database Yet. Ignoring");
            Log.e("mapPopulate",e.toString());
        }

    }

    public void populateNotifs(){
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    long temptime;

    // Navigation Item Selector Cases
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment frag = null;

        if (id == R.id.button_devices) {
            viewmode = 1;
            frag = new device_list();
            translateView(frag);
        } else if (id == R.id.button_map) {
            viewmode = 2;
            frag = new map_view();
            translateView(frag);
        } else if(id == R.id.button_notifications){
            viewmode = 3;
            frag = new view_notifi();
            translateView(frag);
        } else if(id == R.id.button_settings){
            viewmode = 4;
            frag = new settings();
            translateView(frag);


        } else if(id == R.id.button_start_react){
           startReact();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void startReact(){
        Intent i = new Intent(this, reactBack.class); // <-- Production Mode
//        Intent i = new Intent(getApplicationContext(), reactBackend.class); // <-- Dev Mode
        Log.d("DEBUG",ip+"");
        i.putExtra("ip", ip.toString());
        startService(i); // <-- Production Mode
//        startActivity(i); // <-- Dev Mode
    }

    public void translateView(Fragment frag){
        if (frag != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame, frag); // replace a Fragment with Frame Layout
            transaction.commitNow(); // commit the changes

            if(viewmode==1){ // Device List View
                textView2 = findViewById(R.id.textView2);
               populateList();
            }else if(viewmode==2){ // Map View
                SupportMapFragment falala = (SupportMapFragment) frag.getChildFragmentManager().findFragmentById(R.id.mapaaa);
                falala.getMapAsync(this);
                Log.d("References",falala.toString());
            }else if(viewmode==3){ //Notifications View
                Textnotif = findViewById(R.id.notifTextView);
                populateNotifs();
            }else if(viewmode==4){ //
                text_ip = findViewById(R.id.textFieldIP);
                text_ip.setText(ip);
            }
        }
    }

    public void changeIp(View v){
        ip = text_ip.getText().toString();
        db.execSQL("UPDATE settings set ip='"+ip+"' where id = 1");
        Intent intent =  new Intent(this, reactBack.class);
        stopService(intent);
        startReact();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(receiver!=null)
        {
            unregisterReceiver(receiver);
        }
    }

    public void queryLocation(){
        Cursor c = db.rawQuery("Select * from devices", null);
        try {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                String id = c.getString(c.getColumnIndex("id"));
                String lat = c.getString(c.getColumnIndex("lat"));
                String lng= c.getString(c.getColumnIndex("lng"));
                String name = c.getString(c.getColumnIndex("name"));
                Double ppm_lpg = Double.parseDouble(c.getString(c.getColumnIndex("ppm_lpg")));
                String status = c.getString(c.getColumnIndex("status"));
                String message = "";
                if(ppm_lpg!=0){
                    message= "Gas Detected!";
                }else{
                    message = "None";
                }

                textView2.append(
                        "Device Name:" + name +
                                "\nDevice ID:" + id +
                                "\nLPG:" + ppm_lpg + "ppm" +
                                "\nDetected:" + message + "" +
                                "\n------------------------------\n\n"
                );
            }
        } catch (Exception e) {

        }
    }



    @Override
    public void onLocationChanged(Location locationa) {
        Log.e("GPS", locationa.getProvider());
        Log.e("GPS", locationa.toString());
        currentPos = new LatLng (locationa.getLatitude(),locationa.getLongitude());
        Location current = new Location("currentPosition");
        current.setLatitude(currentPos.latitude);
        current.setLongitude(currentPos.longitude);
        Location compare = new Location("cmp");
        try{
            Cursor c = db.rawQuery("Select * from devices",null);
            boolean cleanAlert = true;
            for(c.moveToFirst();!c.isAfterLast();c.moveToNext()) {
//                String debug = DatabaseUtils.dumpCursorToString(c);
//                Log.d("FTW", debug.toString());
                String id = c.getString(c.getColumnIndex("id"));
                String lat = c.getString(c.getColumnIndex("lat"));
                String lng= c.getString(c.getColumnIndex("lng"));
                String name = c.getString(c.getColumnIndex("name"));
                Double ppm_lpg = Double.parseDouble(c.getString(c.getColumnIndex("ppm_lpg")));
                String status = (c.getString(c.getColumnIndex("status")));
                compare.setLatitude(Float.parseFloat(lat));
                compare.setLongitude(Float.parseFloat(lng));

                Log.d("TEST", ""+ppm_lpg);
                if(ppm_lpg>=100){

                    double distance = current.distanceTo(compare);
//                    Log.d("CMPR", distance + "m");
//                    Toast.makeText(this, "DISTANCE:"+distance, Toast.LENGTH_SHORT);
                    if(distance<100){
                        if(cleanAlert){
                            cleanAlert = false;
                        }
                    }else if(distance>100){
                        //?????
                    }
                }
            }
            if(cleanAlert && alert){
                Toast.makeText(this,"User outside Scope", Toast.LENGTH_SHORT).show();
                alert=false;
                //Clear Alerts
            }
            if(!cleanAlert && !alert){
                Log.e("aaa","BB1");
                Toast.makeText(this,"User inside Scope", Toast.LENGTH_SHORT).show();
                Log.e("aaa","BB2");
                alert=true;
                if(hidden){
                   notifyChanges();
                }
                if(!alertDialog){
                Log.e("aaa","BB3");
                    Toast.makeText(this, "Should be a message box!", Toast.LENGTH_SHORT);
                    alertDi = new AlertDialog.Builder(this);
                Log.e("aaa","BB4");

                    Log.wtf("Notification system", "Building MessageBox");
                Log.e("aaa","BB5");
                    alertDi.setTitle("Alert!")
                            .setMessage("Caution, You're inside the vicinity of an active gas leak.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    alertDialog=false;
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert);
                    Log.wtf("Notification system", "SHOWING MESSAGE BOX");
                Log.e("aaa","BB6");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("aaa","BB7");
                            alertDialog=true;
                            alertDi.show();
                        }
                    });
                }
                //Ring ring ring maderfader.
            }
        } catch (Exception e){
            Log.e("WTF error", e.toString());
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // And From your main() method or any other method

    public void startTracking(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        mprovider = locationManager.getBestProvider(criteria, false);

        if (mprovider != null && !mprovider.equals("")) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            location = locationManager.getLastKnownLocation(mprovider);
            locationManager.requestLocationUpdates(mprovider,100,1,this);
            Log.d("Debug",mprovider);
            if (location != null) {
                onLocationChanged(location);
            }
            else {
                Toast.makeText(getBaseContext(), "Either GPS isn't locked, or GPS is Unavailable at this time", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng pos = new LatLng(7.086381,125.615616);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(16));
        try{
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException e){
            Toast.makeText(getBaseContext(), "Please allow Location access for Proximity Warnings.", Toast.LENGTH_LONG).show();
        }
        populateMap();
        startTracking();
    }
}








