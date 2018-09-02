package com.shahzaib.freereecharge;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import Database.DatabaseHelper;
import Database.DbContract;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static  int RANDOM_NUMBER_LENGTH = 14;
    public static String COMPLETE_CODE_STARTING_CODE = "*123*";
    public static  String ADD_MOB_APP_ID;
    public static  String INTERSTITIAL_AD_UNIT_ID;
    MyAdListener adListener = new MyAdListener();


    TextView generatedNumberTV, completeCodeTV;
    Button generateNumberBtn, tryItBtn;
    String generatedNumber = null;
    DatabaseHelper databaseHelper;
    Toolbar toolbar;
    SharedPreferences sharedPreferences;
    private AdView bannerAdView;
    private InterstitialAd interstitialAd;



    boolean isZongSim = false;
    boolean isNetworkConnected = false;
    int numberTriesCount = 0;
    int numberGeneratedCount = 0;
    int randomNumberToTrickUser; // that number is checked from the server when device is connected
    int randomNumberToGenerateInterstitialAd;


//    int tempCount = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ADD_MOB_APP_ID = getString(R.string.admob_app_id);
        INTERSTITIAL_AD_UNIT_ID = getString(R.string.interstitial_ad_unit_id);

        if(!isDefaultSimSelected()) promptUserToSelectDefaultSim();
        generatedNumberTV = findViewById(R.id.generatedNumberTV);
        completeCodeTV = findViewById(R.id.completeCodeTV);
        generateNumberBtn = findViewById(R.id.generateNumberBtn);
        tryItBtn = findViewById(R.id.tryItBtn);
        toolbar = findViewById(R.id.toolbar);
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        bannerAdView = findViewById(R.id.bannerAdView);
        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(INTERSTITIAL_AD_UNIT_ID);









        // initial Setup
        setupToolbar();
        setButtonEnabled(tryItBtn,false);
        checkNetworkState();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        generateNumberBtn.setOnClickListener(this);
        tryItBtn.setOnClickListener(this);
        if(isNetworkConnected) MobileAds.initialize(this, ADD_MOB_APP_ID);
        bannerAdView.setAdListener(adListener);
        interstitialAd.setAdListener(adListener);







        // tricky part
        setupTrickToPretendNumberAlreadyTried();
        setupWhenToShowInterstitialAd();

    }




    @Override
    protected void onResume() {
        super.onResume();
        setupDefaultValues(sharedPreferences.getString(getString(R.string.default_sim),null));
        if(isNetworkConnected) {
            requestInterstitialAd(interstitialAd);
            requestAndLoadBannerAd(bannerAdView);
        }

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu,menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setupDefaultValues(key);
        generatedNumber = generateRandomNumber(RANDOM_NUMBER_LENGTH);
        generatedNumberTV.setText(generatedNumber);
        completeCodeTV.setVisibility(View.INVISIBLE);
        setButtonEnabled(tryItBtn,false);

    }






    /**
     * Click Listeners
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.menu_item_rate_this_app:
                rateThisApp();
                return true;

            case R.id.menu_item_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
        }
        return false;
    }



    @Override
    public void onClick(View v) {

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_raise_animation));
        }

        switch (v.getId())
        {
            case R.id.generateNumberBtn:
//                tempCount++;

                incrementNumberGeneratedCount();
                SHOW_LOG("Number generated count: "+numberGeneratedCount);
                if(isNetworkConnected && numberGeneratedCount == randomNumberToGenerateInterstitialAd)
                {
                    SHOW_LOG("SHow interstitial Ad");
                    showInterstitialAd();
                    setupWhenToShowInterstitialAd();
                }
                generatedNumber = generateRandomNumber(RANDOM_NUMBER_LENGTH);
//                if(tempCount==10) generatedNumber = "13602530420702";
                animateTextView(generatedNumberTV,generatedNumber);
                completeCodeTV.setText("Complete Code:  "+COMPLETE_CODE_STARTING_CODE+generatedNumber+"#");
                completeCodeTV.setVisibility(View.INVISIBLE);
                setButtonEnabled(tryItBtn,true);
                break;

            case R.id.tryItBtn:
                if(isUserHasPermission(Manifest.permission.CALL_PHONE)) {
                    if(!isNumberAlreadyChecked(Long.parseLong(generatedNumber))) {
                            tryNumberForRecharge(generatedNumber);
                    }
                    else {
                        Toast.makeText(this, "Number Is Already Tried", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE},0);
                }
                break;
        }
    }




























    /**
     * Helper Methods
     * */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null) getSupportActionBar().setTitle(getString(R.string.app_name));
    }
    private void setupDefaultValues(String defaultSelectedSim){
        if(defaultSelectedSim!=null)
        {
            if(defaultSelectedSim.equals(getString(R.string.value_jazz) )){
                RANDOM_NUMBER_LENGTH = 14;
                COMPLETE_CODE_STARTING_CODE = "*123*";
            }else if(defaultSelectedSim.equals(getString(R.string.value_warid)))
            {
                RANDOM_NUMBER_LENGTH = 14;
                COMPLETE_CODE_STARTING_CODE = "*123*";
            }
            else if(defaultSelectedSim.equals(getString(R.string.value_telenor))){
                RANDOM_NUMBER_LENGTH = 14;
                COMPLETE_CODE_STARTING_CODE = "*555*";
            }
            else if(defaultSelectedSim.equals(getString(R.string.value_zong)))
            {
                isZongSim = true;
                RANDOM_NUMBER_LENGTH = 15;
                COMPLETE_CODE_STARTING_CODE = "*101*";
            }else if(defaultSelectedSim.equals(getString(R.string.value_ufone)))
            {
                RANDOM_NUMBER_LENGTH = 14;
                COMPLETE_CODE_STARTING_CODE = "*123*";
            }
            return;
        }
        SHOW_LOG("Failed to set Default Values");
    }
    private void animateTextView(final TextView  textview, final String text) {

        int initialValue = 100000000;
        int finalValue = 999999999;

        ValueAnimator valueAnimator = ValueAnimator.ofInt(initialValue, finalValue);
        valueAnimator.setDuration(500);

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                StringBuilder stringBuilder = new StringBuilder();
                String animationString = valueAnimator.getAnimatedValue().toString();
                stringBuilder.append(animationString.charAt(2));
                stringBuilder.append(animationString.charAt(3));
                stringBuilder.append(animationString.charAt(4));
                stringBuilder.append(animationString.charAt(5));
                stringBuilder.append(animationString.charAt(2));
                stringBuilder.append(animationString.charAt(3));
                stringBuilder.append(animationString.charAt(4));
                stringBuilder.append(animationString.charAt(5));
                stringBuilder.append(animationString.charAt(5));
                stringBuilder.append(animationString.charAt(4));
                stringBuilder.append(animationString.charAt(5));
                stringBuilder.append(animationString.charAt(2));
                stringBuilder.append(animationString.charAt(3));
                stringBuilder.append(animationString.charAt(4));
                if(isZongSim){
                    stringBuilder.append(animationString.charAt(2));
                }


                textview.setText(stringBuilder.toString());

            }



        });

        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                textview.setText(text);
                completeCodeTV.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();

    }
    private void setButtonEnabled(View button, boolean enabled) {
        button.setEnabled(enabled);
        if(enabled)
        {
            ObjectAnimator.ofFloat(button,"alpha",1f).start();
        }
        else{
            ObjectAnimator.ofFloat(button,"alpha",0.5f).start();
        }
    }
    public boolean isDefaultSimSelected() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.default_sim), null) != null;
    }
    private boolean isNumberAlreadyChecked(long number) {
        SQLiteDatabase sqLiteDatabase = databaseHelper.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(DbContract.CHECKED_NUMBERS.TABLE_NAME, null,
                DbContract.CHECKED_NUMBERS.COLUMN_NUMBER + "=" + number, null, null, null, null);

        return cursor != null && cursor.moveToFirst();
    }
    @SuppressLint("MissingPermission")
    private void tryNumberForRecharge(String generatedNumber) {
        incrementTriesCount();
        SHOW_LOG("Current Try of the user: "+numberTriesCount);

        if(isNetworkConnected)
        {
            if(numberTriesCount == randomNumberToTrickUser)
            {
                Toast.makeText(this, "Number is Already Tried", Toast.LENGTH_SHORT).show();
                setButtonEnabled(tryItBtn,false);
                setupTrickToPretendNumberAlreadyTried();
                return;
            }
        }

        setButtonEnabled(tryItBtn,false);
        putNumberIntoDatabase(Long.parseLong(generatedNumber));
        String completeCode = makeCompleteDialableCode(generatedNumber);
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + completeCode));
        startActivity(intent);
    }
    private void putNumberIntoDatabase(long checkedNumber) {
        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.CHECKED_NUMBERS.COLUMN_NUMBER,checkedNumber);
        sqLiteDatabase.insert(DbContract.CHECKED_NUMBERS.TABLE_NAME,null,values);

    }
    private String makeCompleteDialableCode(String generatedNumber) {
        return COMPLETE_CODE_STARTING_CODE+generatedNumber+Uri.encode("#");
    }
    private String generateRandomNumber(long length) {
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i<length; i++)
        {
            stringBuilder.append(generateOneRandomDigit());
        }
        return stringBuilder.toString();
    }
    private int generateRandomNumber(int min, int max) {
        return (int) ((Math.random() * ((max - min) + 1)) + min);
    }
    private int generateOneRandomDigit()
    {
        return (int) ((Math.random() * ((9) + 1)));
    }
    private void SHOW_LOG(String message)
    {
        Log.i("123456",message);
    }
    private boolean isUserHasPermission(String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    private void promptUserToSelectDefaultSim() {
        startActivity(new Intent(this,SelectSim.class));
    }
    private void rateThisApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }else {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }

        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }
    private void checkNetworkState() {
        ConnectivityManager conMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(conMgr==null) return;

        if ( conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED
                || conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED ) {
            isNetworkConnected = true;
            SHOW_LOG("Network is connected");
        }
        else if ( conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.DISCONNECTED
                || conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.DISCONNECTED) {
            isNetworkConnected = false;
            SHOW_LOG("Network is OFF");
        }
    }
    private void setupTrickToPretendNumberAlreadyTried() {
        if(isNetworkConnected)
        {
            randomNumberToTrickUser = generateRandomNumber(4,8);
            SHOW_LOG("random number generated to trick user: "+randomNumberToTrickUser);
        }
    }
    private void setupWhenToShowInterstitialAd() {
        if(isNetworkConnected)
        {
            randomNumberToGenerateInterstitialAd = generateRandomNumber(4,8);
            SHOW_LOG("Interstitial add showing number generated: "+randomNumberToGenerateInterstitialAd);
        }
    }
    private void incrementTriesCount() {
        numberTriesCount++;
        if(numberTriesCount==9) numberTriesCount=0;
    }
    private void incrementNumberGeneratedCount() {
        numberGeneratedCount++;
        if(numberGeneratedCount==9) numberGeneratedCount=0;
    }

    private void requestAndLoadBannerAd(AdView bannerAdView) {
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("6C11C58267C4DD8B942D2272850C1298").addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
//        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAdView.loadAd(adRequest);
    }

    private void requestInterstitialAd(InterstitialAd interstitialAd) {
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("6C11C58267C4DD8B942D2272850C1298").addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
//        AdRequest adRequest = new AdRequest.Builder().build();
        interstitialAd.loadAd(adRequest);
    }
    private void showInterstitialAd() {
        if(interstitialAd.isLoaded())
        {
            interstitialAd.show();
        }
        else
        {
            SHOW_LOG("interstitail add is not loaded");
        }
    }












    public class MyAdListener extends AdListener
    {
        public MyAdListener() {
            super();
        }



        @Override
        public void onAdFailedToLoad(int i) {
            super.onAdFailedToLoad(i);
            SHOW_LOG("Ad Failed to load");
            switch (i)
            {
                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                    SHOW_LOG("Error code internal error");
                    break;

                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                    SHOW_LOG("Error code invalid request");
                    break;

                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                    SHOW_LOG("Error code network error");
                    break;

                case AdRequest.ERROR_CODE_NO_FILL:
                    SHOW_LOG("Error code no fill");
                    break;
            }
        }


        @Override
        public void onAdLoaded() {
            SHOW_LOG("Ad Successfully loaded");
            super.onAdLoaded();
        }


    }










}
