package ch.hevs.fbonvin.disasterassistance;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toolbar;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import ch.hevs.fbonvin.disasterassistance.models.Message;
import ch.hevs.fbonvin.disasterassistance.utils.CommunicationManagement;
import ch.hevs.fbonvin.disasterassistance.utils.MandatoryPermissionsHandling;
import ch.hevs.fbonvin.disasterassistance.utils.MessagesManagement;
import ch.hevs.fbonvin.disasterassistance.utils.NearbyManagement;
import ch.hevs.fbonvin.disasterassistance.utils.PreferencesManagement;
import ch.hevs.fbonvin.disasterassistance.utils.interfaces.INearbyActivity;
import ch.hevs.fbonvin.disasterassistance.views.fragments.FragMap;
import ch.hevs.fbonvin.disasterassistance.views.fragments.FragMessages;
import ch.hevs.fbonvin.disasterassistance.views.onBoards.ActivityOnBoardTutorial;
import ch.hevs.fbonvin.disasterassistance.views.settings.ActivityPreferences;

import static ch.hevs.fbonvin.disasterassistance.Constant.CODE_MANDATORY_PERMISSIONS;
import static ch.hevs.fbonvin.disasterassistance.Constant.CURRENT_DEVICE_LOCATION;
import static ch.hevs.fbonvin.disasterassistance.Constant.ESTABLISHED_ENDPOINTS;
import static ch.hevs.fbonvin.disasterassistance.Constant.FIRST_INSTALL;
import static ch.hevs.fbonvin.disasterassistance.Constant.FRAG_MESSAGES_SENT;
import static ch.hevs.fbonvin.disasterassistance.Constant.FRAG_MESSAGE_LIST;
import static ch.hevs.fbonvin.disasterassistance.Constant.FUSED_LOCATION_PROVIDER;
import static ch.hevs.fbonvin.disasterassistance.Constant.MANDATORY_PERMISSION;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGES_DEPRECATED;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGES_DISPLAYED;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGES_RECEIVED;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_QUEUE;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_QUEUE_DELETED;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_QUEUE_LOCATION;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_SENT;
import static ch.hevs.fbonvin.disasterassistance.Constant.MIN_REFRESH_RATE_GPS;
import static ch.hevs.fbonvin.disasterassistance.Constant.NEARBY_MANAGEMENT;
import static ch.hevs.fbonvin.disasterassistance.Constant.REFRESH_RATE_GPS;
import static ch.hevs.fbonvin.disasterassistance.Constant.TAG;
import static ch.hevs.fbonvin.disasterassistance.Constant.VALUE_PREF_APPID;

public class MainActivity extends AppCompatActivity implements INearbyActivity{


    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private static Context appContext;

    /**
     * Bottom navigation fragment switching management
     */
    private final BottomNavigationView.OnNavigationItemSelectedListener mNavListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    switch (item.getItemId()) {
                        case R.id.nav_messages:
                            getSupportActionBar().setElevation(0);
                            selectedFragment = new FragMessages();
                            break;
                        case R.id.nav_map:
                            selectedFragment = new FragMap();
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                    return true;
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final String SHOWCASE_ID = "1";

        Toolbar toolbar;
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferencesManagement.initPreferences(this);
        initButtons();
        initConstants();
        appContext = getApplicationContext();

        showcaseDialogTutorial();

    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferencesManagement.initPreferences(this);

        if(!FIRST_INSTALL){
            initNearby();

            startLocationUpdates();
            checkHighAccuracy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferencesManagement.saveMessages(this);
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PreferencesManagement.saveMessages(this);
    }

    public static Context getAppContext(){
        return appContext;
    }

    /**
     * Initialize the important constants
     */
    private void initConstants() {
        FUSED_LOCATION_PROVIDER = LocationServices.getFusedLocationProviderClient(this);
        configureLocation();

        MESSAGES_RECEIVED = new ArrayList<>();
        MESSAGE_SENT = new ArrayList<>();
        MESSAGE_QUEUE = new ArrayList<>();
        MESSAGE_QUEUE_DELETED = new ArrayList<>();
        MESSAGES_DISPLAYED = new ArrayList<>();
        MESSAGE_QUEUE_LOCATION = new ArrayList<>();
        MESSAGES_DEPRECATED = new ArrayList<>();

        PreferencesManagement.retrieveMessages(this);
    }


    private void initButtons() {
        //Initial configuration
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(mNavListener);

        getSupportActionBar().setElevation(0);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new FragMessages()).commit();
    }

    private void initNearby() {
        //Configuration of Nearby
        ConnectionsClient connectionsClient = Nearby.getConnectionsClient(this);
        NEARBY_MANAGEMENT = new NearbyManagement(connectionsClient, VALUE_PREF_APPID, getPackageName());

        if(!NEARBY_MANAGEMENT.ismIsAdvertising() || !NEARBY_MANAGEMENT.ismIsDiscovering()){
            NEARBY_MANAGEMENT.startNearby(this);
        }

    }

    /**
     * Check that high accuracy is activated on the device
     */
    private void checkHighAccuracy() {
        try {
            if (Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {

                new AlertDialog.Builder(this)
                        .setTitle("You have to enable high accuracy")
                        .setMessage("Go to settings")
                        .setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        })
                        .setCancelable(false)
                        .create().show();
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configure the location request
     */
    private void configureLocation(){

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(REFRESH_RATE_GPS);
        mLocationRequest.setFastestInterval(MIN_REFRESH_RATE_GPS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i(TAG, "Main activity onSuccess: location settings satisfied");
            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    CURRENT_DEVICE_LOCATION = location;
                    MessagesManagement.updateDisplayedMessagesList();
                    FRAG_MESSAGES_SENT.recalculateDistance();

                    //Send message that where queued because of no location stored
                    if(MESSAGE_QUEUE_LOCATION.size() > 0 && ESTABLISHED_ENDPOINTS.size() > 0){
                        Log.i(TAG, "MainActivity onLocationResult: send messages that did not had location " + MESSAGE_QUEUE_LOCATION.size());
                        for(Message m : MESSAGE_QUEUE_LOCATION){
                            m.setMessageLatitude(location.getLatitude());
                            m.setMessageLongitude(location.getLongitude());
                            m.updateExpirationDate();

                            CommunicationManagement.sendMessageListRecipient(new ArrayList<>(ESTABLISHED_ENDPOINTS.keySet()), m);
                        }
                    }

                }
            }
        };
    }

    /**
     * Start location update at application start
     */
    public void startLocationUpdates(){
        MandatoryPermissionsHandling.checkPermission(this, CODE_MANDATORY_PERMISSIONS, MANDATORY_PERMISSION);
        FUSED_LOCATION_PROVIDER.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }
    private void stopLocationUpdates() {
        FUSED_LOCATION_PROVIDER.removeLocationUpdates(mLocationCallback);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.top_action_settings:
                Intent intentSettings = new Intent(MainActivity.this, ActivityPreferences.class);
                startActivity(intentSettings);
                return true;
            case R.id.top_action_tutorial:
                Intent intentTutorial = new Intent(MainActivity.this, ActivityOnBoardTutorial.class);
                startActivity(intentTutorial);
                return true;
            case R.id.top_action_filter_date:
                MessagesManagement.OrderByDate(MESSAGES_DISPLAYED);
                FRAG_MESSAGE_LIST.updateDisplay();
                return true;
            case R.id.top_action_filter_title:
                MessagesManagement.OrderByTitle(MESSAGES_DISPLAYED);
                FRAG_MESSAGE_LIST.updateDisplay();
                return true;
            case R.id.top_action_filter_distance:
                MessagesManagement.OrderByDistance(MESSAGES_DISPLAYED);
                FRAG_MESSAGE_LIST.updateDisplay();
            case R.id.top_action_filter_category:
                MessagesManagement.OrderByCategory(MESSAGES_DISPLAYED);
                FRAG_MESSAGE_LIST.updateDisplay();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_main_fragment, menu);

        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public void nearbyOk() {
        Snackbar.make(findViewById(R.id.snack_place), R.string.main_activity_google_nearby_launched, Snackbar.LENGTH_LONG).show();
    }

    private void showcaseDialogTutorial(){

        final ShowcaseView ShowcaseView;

        final SharedPreferences tutorialShowcases = getSharedPreferences("showcaseTutorial", MODE_PRIVATE);

        boolean run;

        run = tutorialShowcases.getBoolean("run?", true);

        if(run){//If the buyer already went through the showcases it won't do it again.
            final ViewTarget step1 = new ViewTarget(R.id.nav_messages , this);//Variable holds the item that the showcase will focus on.
            final ViewTarget step2 = new ViewTarget(R.id.nav_map, this);
            final ViewTarget step3 = new ViewTarget(R.id.fab_add_message_list , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);
//            final ViewTarget  = new ViewTarget(R.id. , this);


            //This code creates a new layout parameter so the button in the showcase can move to a new spot.
            final RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            // This aligns button to the bottom left side of screen
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_LEFT);
            // Set margins to the button, we add 16dp margins here
            int margin = ((Number) (getResources().getDisplayMetrics().density * 16)).intValue();
            lps.setMargins(margin, margin, margin, margin);


            //This creates the first showcase.
            ShowcaseView = new ShowcaseView.Builder(this)
                    .withMaterialShowcase()
                    .setContentTitle("Welcome on the tutorial")
                    .setContentText("This tutorial will show you the main part of the application")
                    .setStyle(2)
                    .build();
            ShowcaseView.setButtonText("next");


            //When the button is clicked then the switch statement will check the counter and make the new showcase.
            ShowcaseView.overrideButtonClick(new View.OnClickListener() {
                int count1 = 0;

                @Override
                public void onClick(View v) {
                    count1++;
                    switch (count1) {
                        case 1:
                            ShowcaseView.setTarget(step1);
                            ShowcaseView.setContentTitle("Titre du step 1");
                            ShowcaseView.setContentText("Texte du step 1");
                            ShowcaseView.setButtonText("next");
                            break;

                        case 2:
                            ShowcaseView.setTarget(step2);
                            ShowcaseView.setContentTitle("Titre du step 2");
                            ShowcaseView.setContentText("Text du step 2");
                            ShowcaseView.setButtonText("next");
                            ShowcaseView.setButtonPosition(lps);
                            break;

//                        case 3:
//                            ShowcaseView.setTarget(step3);
//                            ShowcaseView.setContentTitle("New message");
//                            ShowcaseView.setContentText("This button allow to create a new message");
//                            ShowcaseView.setButtonText("next");
//                            break;

                        case 3:
                            SharedPreferences.Editor tutorialShowcasesEdit = tutorialShowcases.edit();
                            tutorialShowcasesEdit.putBoolean("run?", false);
                            tutorialShowcasesEdit.apply();

                            ShowcaseView.hide();
                            break;
                    }
                }
            });
        }
    }
}
