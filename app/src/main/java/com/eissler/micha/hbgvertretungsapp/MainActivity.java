package com.eissler.micha.hbgvertretungsapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eissler.micha.hbgvertretungsapp.evaluation.CoverMessage;
import com.eissler.micha.hbgvertretungsapp.evaluation.DataEvaluation;
import com.eissler.micha.hbgvertretungsapp.evaluation.HBGMessage;
import com.eissler.micha.hbgvertretungsapp.evaluation.HeaderMessage;
import com.eissler.micha.hbgvertretungsapp.gcm.RegistrationIntentService;
import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.eissler.micha.hbgvertretungsapp.settings.WhitelistSubjects;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.viewpagerindicator.TitlePageIndicator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import hugo.weaving.DebugLog;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ActionMode.Callback, SwipeRefreshLayout.OnRefreshListener {

    //Object-filename
    private final static String CLASSES = "ClassSelectionArrayAdapter";


    //Preference-names
    private static final String PREFERENCE = "MainPreference";

    //Preference-Keys
    private final static String SELECTED_CLASS = "classSelection";
    private final static String FIRST_START = "FirstStart";
    private static final String VERSION = "version";
    final static String LAST_UPDATE_CHECK = "LastUpdateCheck";

    //private static final String REFRESH_DISPLAY_LIST = "RefreshDisplayList";

    //RefreshSpinnerTask-Mode
    private final static int GET_SAVE_STATE = 0;
    private final static int REFRESH = 1;

    private static final long REFRESH_COUNTDOWN_MILLIS = 60 * 1000;

    private final CountDownTimer refreshTimer = new CountDownTimer(REFRESH_COUNTDOWN_MILLIS, REFRESH_COUNTDOWN_MILLIS) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            System.out.println("TIMER FINISHED");
            swipeRefreshLayout.setRefreshing(true);
            MainActivity.this.onRefresh();
        }
    };

    //    private TextView selectedClassText;
    private ListView classesList;
    private DrawerLayout drawerLayout;

    private ActionMode mActionMode;

    private static boolean firstCreation = true; // TODO: 11.04.2016 firstcreation boolean

    static SharedPreferences prefs;
    public static SharedPreferences defaultPrefs;

//    public static ProgressBar progressBar;

    private ActionBarDrawerToggle drawerToggle;
    private ViewPager pager;
    private HbgPagerAdapter pagerAdapter;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private TitlePageIndicator titleIndicator;
    private CardView errorCard;
    private TextView errorText;
    private Tracker mTracker;

    public com.eissler.micha.hbgvertretungsapp.SwipeRefreshLayout swipeRefreshLayout;
    public static boolean mainActivityPaused = true;
    private Date lastReload;
    private Integer currentItem;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        App.logCodeSection("ConfigChange");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);


        // Obtain the shared Tracker instance.
        HbgApplication application = (HbgApplication) getApplication();
        mTracker = application.getDefaultTracker();

        if (firstCreation) {
            App.logTrace("First onCreate call");

            prefs = getSharedPreferences(PREFERENCE, MODE_PRIVATE);
            defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        }

//        runOnUiThread(new Runnable() { // TODO: 10.04.2016 showcaseview
//            @Override
//            public void run() {
//                new ShowcaseView.Builder(MainActivity.this)
//                        .setTarget(new ViewTarget(listView))
//                        .setContentText("This is the listview")
//                        .setContentTitle("ListView")
//                        .setStyle(R.style.HbgTheme)
//                        .hideOnTouchOutside()
//                        .setShowcaseEventListener(new OnShowcaseEventListener() {
//                            @Override
//                            public void onShowcaseViewHide(ShowcaseView showcaseView) {
//
//                            }
//
//                            @Override
//                            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
//
//                            }
//
//                            @Override
//                            public void onShowcaseViewShow(ShowcaseView showcaseView) {
//
//                            }
//
//                            @Override
//                            public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
//
//                            }
//                        }).build();
//            }
//        });

        try {
            final int thisVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            final boolean firstStart = prefs.getBoolean(FIRST_START, true);
            if (firstStart) {


                mRegistrationBroadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        System.out.println("RECEIVED!<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                        SharedPreferences sharedPreferences =
                                PreferenceManager.getDefaultSharedPreferences(context);
                        boolean sentToken = sharedPreferences
                                .getBoolean(Preferences.SENT_TOKEN_TO_SERVER, false);
                        if (sentToken) {
                            Toast.makeText(MainActivity.this, "TOKEN WAS SENT", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "WAS NOT SENT", Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                // Registering BroadcastReceiver
                registerReceiver();

                Intent intent = new Intent(this, RegistrationIntentService.class);
                intent.putStringArrayListExtra("topics", new ArrayList<>(Arrays.asList(new String[]{"21-GLaN3"})));
                startService(intent); // TODO: 09.04.2016  do not register every time

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(FIRST_START, false);
                editor.putInt(VERSION, thisVersion);
                editor.apply();

                setAlarm(7, 0, 0, this);
                setAlarm(19, 0, 1, this);



//                App.dialog("Testphase", "Diese App ist in der Testphase und es können noch Fehler auftreten. Um diese zu beheben, wird, wenn ein Fehler auftritt, ein Fehlerbericht an mich gesendet. " +
//                        "In den Einstellungen kannst du deine E-Mail-Adresse speichern, die allen Fehlerberichten angehängt wird, sodass ich dich erreichen kann, falls ich den Fehler, der bei dir aufgetreten ist, nicht durch den Fehlerbericht beheben kann.", MainActivity.this)
//                        .setPositiveButton("Einstellungen", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
//                                startActivity(settingsIntent);
//                            }
//                        })
//                        .show();

//                ACRA.getErrorReporter().handleSilentException(new Exception("App wurde das erste Mal gestartet")); // TODO: 18.04.2016 App wurde erstes mal gestartet - ACRA Meldung deaktiviert
            } else if (thisVersion > prefs.getInt(VERSION, 1)) {
                App.logCodeSection("Update was installed");
                prefs.edit().putInt(VERSION, thisVersion).apply();
//                ACRA.getErrorReporter().handleSilentException(new Exception("App-Update wurde installiert"));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

//        AccountManager accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
//        Account[] accounts = accountManager.getAccounts();
//        for (Account acc : accounts) {
//            System.out.println("acc.name = " + acc.name);
//        }
//        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        System.out.println("androidId = " + androidId); // TODO: 18.04.2016 maybe use this id

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            App.logError("GOOGLE PLAY SERVICES NOT AVAILABLE");
        }


//        Log.d(HbgApplication.HBG_APP, "onCreate MainActivity");
        App.logCodeSection("MainActivity.onCreate");


//        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) { // TODO: 01.03.2016 Create First-Time-Password-Validation
//            System.out.println("getIntent().getDataString() = " + getIntent().getDataString());
//        }

//        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiMgr.getConnectionInfo(); // requires permission
//        System.out.println("wifiInfo.getSSID() = " + wifiInfo.getSSID());
//        System.out.println("wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED = " + wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED));

        new RefreshSpinnerTask().execute(MainActivity.this, GET_SAVE_STATE);

//        ArrayList<String> classesList; // TODO: 26.03.2016 implement this
//        try {
//            //noinspection unchecked
//            classesList = (ArrayList<String>) App.retrieveObject(CLASSES, ArrayList.class, getApplicationContext());
//            selectedClassText.setText(classesList.get(prefs.getInt(SELECTED_CLASS, 0)));
//        } catch (IOException | ClassNotFoundException e) {
//            App.logError("Could not retrieve saved classesList.");
//            App.reportUnexpectedException(e);
//            e.printStackTrace();
//        }


        App.logCodeSection("ViewAssignment");

//        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        swipeRefreshLayout = (com.eissler.micha.hbgvertretungsapp.SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        assert swipeRefreshLayout != null;
        swipeRefreshLayout.setColorSchemeResources(R.color.accent_material_light);
        swipeRefreshLayout.setOnRefreshListener(this);

        errorCard = (CardView) findViewById(R.id.error_card);
        assert errorCard != null;
        errorCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeRefreshLayout.setRefreshing(true);
                onRefresh();
            }
        });
        errorText = (TextView) errorCard.findViewById(R.id.error_text);

        classesList = (ListView) findViewById(R.id.left_drawer);

        assert classesList != null;
        classesList.setOnItemSelectedListener(this);
        classesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                MainActivity.this.classesList.requestFocusFromTouch();
                MainActivity.this.classesList.setSelection(position);

                String className = MainActivity.this.classesList.getItemAtPosition(position).toString();
                if (!className.equals("Aktualisieren")) {
                    drawerLayout.closeDrawers();
                }

                //noinspection ConstantConditions
                getSupportActionBar().setSubtitle(className);

            }
        });

        pager = (ViewPager) findViewById(R.id.view_pager);
        onRefresh();
        titleIndicator = (TitlePageIndicator) findViewById(R.id.titles);
        assert titleIndicator != null;
        titleIndicator.setViewPager(pager);


        ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(final int position) {
                App.logCodeSection("MainActivity.onPageSelected");
                System.out.println("position = " + position);

                EventBus.getDefault().post(new Event.LoadPermission(position));

                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
//                swipeRefreshLayout.setCanScrollUp(state != ViewPager.SCROLL_STATE_IDLE);
                boolean pullDownAllowed = state == ViewPager.SCROLL_STATE_IDLE;

                if (!pullDownAllowed) {
                    System.out.println("Preventing Pulldown");
                    EventBus.getDefault().post(new Event.CanScrollUp(true));
                } else {
                    EventBus.getDefault().post(new Event.CanScrollUpRequest());
                }
            }
        };

        titleIndicator.setOnPageChangeListener(onPageChangeListener);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                null,  /* nav drawer icon to replace 'Up' caret */
                R.string.open,  /* "open drawer" description */
                R.string.close  /* "close drawer" description */
        ) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
                }
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Klassenauswahl");
                }
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle.syncState();

//        selectedClassText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                drawerLayout.openDrawer(GravityCompat.START);
//            }
//        });

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        App.logCodeSection("LastUpdateCheck");

        String lastUpdateCheckString = prefs.getString(LAST_UPDATE_CHECK, null);
        App.logTrace("lastUpdateCheckString = " + lastUpdateCheckString);

        if (lastUpdateCheckString == null) {
            new CheckForUpdate(MainActivity.this);
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);
            Date lastUpdate;

            try {
                lastUpdate = dateFormat.parse(lastUpdateCheckString);
            } catch (ParseException e) {
                App.reportUnexpectedException(e);
                e.printStackTrace();
                return;
            }

            Calendar nextUpdateCal = Calendar.getInstance();
            nextUpdateCal.setTime(lastUpdate);
            nextUpdateCal.add(Calendar.DAY_OF_MONTH, 1);

            if (new Date().after(nextUpdateCal.getTime())) {
                new CheckForUpdate(MainActivity.this);
            }
        }
    }

    private void registerReceiver() {
//        if(!isReceiverRegistered) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Preferences.REGISTRATION_COMPLETE));
//            isReceiverRegistered = true;
//        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        System.out.println("ON RESUME");
        super.onResume();
        mainActivityPaused = false;

        mTracker.setScreenName("MainActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        refreshTimer.start();

//        if (WhitelistSubjects.isWhitelistModeActive()) {
//            ArrayList<String> whiteListArray = WhitelistSubjects.getWhiteListArray(this);
////            if (whiteListArray.size() == 0) {// TODO: 19.04.2016 finish here
////
////            }
//        }


        if (App.isMillisecondsLater(lastReload, REFRESH_COUNTDOWN_MILLIS)) {
            onRefresh();
        } else {
            resetPager();
        }
    }

    @Override
    protected void onPause() {
        System.out.println("ONPAUSE MainActivity");
        super.onPause();
        mainActivityPaused = true;
        refreshTimer.cancel();
    }

    public static void setAlarm(int hour, int minute, int requestCode, Context c) {

        Intent alarmIntent = new Intent(c, AlarmReceiver.class);

        AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(c, requestCode, alarmIntent, 0);


        Calendar calendar = Calendar.getInstance();

        if (calendar.get(Calendar.HOUR_OF_DAY) > hour || (calendar.get(Calendar.HOUR_OF_DAY) == hour && calendar.get(Calendar.MINUTE) > minute)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);


        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY
                , pendingAlarmIntent);

        System.out.println("Alarm " + requestCode + " was set: " + hour + ":" + minute);
    }

    public static void cancelAlarm(int requestCode, Context c) {
        AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(c, AlarmReceiver.class);
        PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(c, requestCode, alarmIntent, 0);
        alarmManager.cancel(pendingAlarmIntent);

        System.out.println("Alarm " + requestCode + " cancelled.");
        boolean alarmSet = PendingIntent.getBroadcast(c, requestCode, alarmIntent, PendingIntent.FLAG_NO_CREATE) != null;
        System.out.println("alarm is set = " + alarmSet);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (drawerToggle.onOptionsItemSelected(menuItem)) {
            return true;
        }

        switch (menuItem.getItemId()) {
            case R.id.action_refresh:
                swipeRefreshLayout.setRefreshing(true);
                onRefresh();
//
////                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
////                ImageView imageView = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);
////                Animation rotation = AnimationUtils.loadAnimation(this, R.anim.anim_refresh);
////                rotation.setRepeatCount(Animation.INFINITE);
////                imageView.startAnimation(rotation);
////                menuItem.setActionView(imageView);
//
//                final ListView listView = pagerAdapter.getListView(pager.getCurrentItem());
//
//                listView.setAdapter(getLoadingAdapter(this));
//
//                if (classesList.getAdapter() == null) {
//                    new RefreshSpinnerTask().execute(this, REFRESH);
//                }
//
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        dataEvaluation = new DataEvaluation(MainActivity.this, true, pager.getCurrentItem());
//                        final HbgListAdapter listAdapter = dataEvaluation.getFormattedListAdapter();
//                        listView.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                listView.setAdapter(listAdapter);
//                            }
//                        });
//
////                        runOnUiThread(new Runnable() {
////                            @Override
////                            public void run() {
////                                if (menuItem.getActionView() != null) {
////                                    menuItem.getActionView().clearAnimation();
////                                    menuItem.setActionView(null);
////                                }
////                            }
////                        });
//                    }
//                }).start();


                break;
            case R.id.action_settings:
                Log.d(HbgApplication.HBG_APP, "Settings - Button clicked");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.show_in_webbrowser:
                Log.d(HbgApplication.HBG_APP, "Show in browser - Button clicked");

                int weekNumber = DataEvaluation.getWeek(pager.getCurrentItem(), DataEvaluation.getCalendar(pager.getCurrentItem()));
                int classNum = prefs.getInt(SELECTED_CLASS, 0);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DataEvaluation.makeURL(weekNumber, classNum)));
                startActivity(browserIntent);

                break;
            default:
                break;
        }
        return true;
    }

    AdapterView.OnItemClickListener tempOnItemClick;
    ListView tempListView;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_cab, menu);
        tempListView = pagerAdapter.getListView(pager.getCurrentItem());
        tempOnItemClick = tempListView.getOnItemClickListener();


        tempListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App.logUserInput("List-Item at position " + position + " was clicked");
                App.logCodeSection("Selection-OnItemClickListener");

                HbgListAdapter listAdapter = (HbgListAdapter) tempListView.getAdapter();

                String subject = listAdapter.getFieldOfList(position, CoverMessage.SUBJECT);
                App.logTrace("subject = " + subject);
                String newSubject = listAdapter.getFieldOfList(position, CoverMessage.NEW_SUBJECT);
                App.logTrace("newSubject = " + newSubject);

                if (!subject.equals("") && !newSubject.equals("") && !subject.equals(newSubject)) {
                    return;
                }

                if (!listAdapter.isSelected(position)) {
                    listAdapter.select(position, true);
                    mActionMode.setTitle(String.valueOf(listAdapter.getNumberOfSelectedItems()));
                    System.out.println("Selected item " + position);
                } else {
                    listAdapter.deselect(position, true);
                    mActionMode.setTitle(String.valueOf(listAdapter.getNumberOfSelectedItems()));
                    System.out.println("Unselected item " + position);

                }

                if (!listAdapter.itemsSelected()) {
                    System.out.println("No items selected");
                    mActionMode.finish();
                }
            }
        });
        return true;

    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_delete:
                final boolean whitelist_switch = defaultPrefs.getBoolean("whitelist_switch", false);
                App.dialog("Bestätigen", "Sollen Meldungen zu den ausgewählten Fächern nicht mehr angezeigt werden?" + (whitelist_switch ? "\n\nAchtung, der Whitelist-Modus ist aktiv!" : ""), this)
                        .setPositiveButton("Ja", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final CustomNames customNames;

                                final ListView listView = pagerAdapter.getListView(pager.getCurrentItem());
                                final HbgListAdapter listAdapter;
                                if (listView != null) {
                                    listAdapter = (HbgListAdapter) listView.getAdapter();
                                } else {
                                    return;
                                }


                                try {
                                    customNames = new CustomNames(getApplicationContext(), listAdapter.getNumberOfSelectedItems());
                                } catch (Exception e) {
                                    System.err.println("Error retrieving custom Names.");
                                    App.reportUnexpectedException(e);
                                    e.printStackTrace();
                                    return;
                                }

                                System.out.println("Not showing following subjects anymore:");

                                final ArrayList<String> whiteList = WhitelistSubjects.getWhiteListArray(MainActivity.this);

                                for (int position : listAdapter.getSelectedItems()) {
                                    String subject = listAdapter.getFieldOfList(position, CoverMessage.SUBJECT);
                                    if (subject.equals("")) {
                                        subject = listAdapter.getFieldOfList(position, CoverMessage.NEW_SUBJECT);
                                    }

                                    String originalSubject = null;

                                    for (Map.Entry<String, String> entry : customNames.entrySet()) {
                                        if (entry.getValue().equals(subject)) {
                                            originalSubject = entry.getKey();
                                        }
                                    }
                                    if (originalSubject == null) {
                                        originalSubject = subject;
                                    }

                                    System.out.println(originalSubject);

                                    if (whitelist_switch) {
                                        assert whiteList != null;
                                        for (int j = 0; j < whiteList.size(); j++) {
                                            String whitelistedSubject = whiteList.get(j);
                                            System.out.println("whitelistedSubject = " + whitelistedSubject);
                                            if (whitelistedSubject.equals(originalSubject)) {
                                                whiteList.remove(j);
                                                break;
                                            }
                                        }
                                    } else {
                                        customNames.put(originalSubject, "Nicht anzeigen");
                                    }
                                }

                                if (!whitelist_switch) {
                                    customNames.save();
                                } else {
                                    assert whiteList != null;
                                    WhitelistSubjects.saveWhiteListArray(whiteList, MainActivity.this);
                                }

                                resetPager();
                                mode.finish();
                            }
                        })
                        .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mode.finish();
                            }
                        })
                        .show();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (tempListView != null) {
            System.out.println("TEMPONCLICK");
            tempListView.setOnItemClickListener(tempOnItemClick);
            ((HbgListAdapter) tempListView.getAdapter()).clearSelection(true);
        }
        tempOnItemClick = null;
        tempListView = null;
        mActionMode = null;
    }

    void resetPager() {
        System.out.println("RESET PAGER");
//        pagerAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(new Event.ResetRequest());
    }

    @Subscribe
    public void onListViewChange(final Event.ListViewChange listViewChange) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                final ListView listView = listViewChange.getListView();
                final HbgListAdapter listAdapter = (HbgListAdapter) listView.getAdapter();

                AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        App.logUserInput("List-Item at position " + position + " was clicked");
                        App.logCodeSection("Default-OnItemClickListener");

                        String subject = listAdapter.getFieldOfList(position, CoverMessage.SUBJECT);
                        App.logTrace("subject = " + subject);

                        String newSubject = listAdapter.getFieldOfList(position, CoverMessage.NEW_SUBJECT);
                        App.logTrace("newSubject = " + newSubject);

                        if (subject.equals("")) {
                            subject = newSubject;
                        }

                        if (!subject.equals("") && !newSubject.equals("") && !subject.equals(newSubject)) {
                            App.logTrace("Building OptionDialog");
                            final CharSequence[] subjects = new CharSequence[]{subject, newSubject};
                            new OptionDialogBuilder(MainActivity.this, subjects).getOptionDialog().show();
                        } else {
                            try {
                                new FilterDialog(subject,
                                        new FilterDialog.PostExecuteInterface() {
                                            @Override
                                            public void onPostExecute() {
                                                MainActivity.this.resetPager();
                                            }
                                        },
                                        MainActivity.this).show(); // FIXME: 25.03.2016 causes BadTokenException sometimes

                            } catch (Exception e) {
                                App.logError("FilterDialog konnte nicht angezeigt werden");
                                App.reportUnexpectedException(e);
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "Ein unerwarteter Fehler ist aufgetreten. Drücke den Aktualisieren-Button und versuche es erneut", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                };
                listView.setOnItemClickListener(onItemClickListener);

                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        App.logUserInput("List-Item at position " + position + " was long-clicked");
                        App.logCodeSection("OnItemLongClickListener");

                        if (mActionMode != null) {
                            //noinspection ConstantConditions
                            listView.getOnItemClickListener().onItemClick(parent, view, position, id);
                            return true;
                        }

                        String subject = listAdapter.getFieldOfList(position, CoverMessage.SUBJECT);
                        App.logTrace("subject = " + subject);
                        String newSubject = listAdapter.getFieldOfList(position, CoverMessage.NEW_SUBJECT);
                        App.logTrace("newSubject = " + newSubject);

                        if (!subject.equals("") && !newSubject.equals("") && !subject.equals(newSubject)) {
                            return false; // TODO: 28.03.2016 do not return but show OptionDialog
                        }

                        mActionMode = MainActivity.this.startActionMode(MainActivity.this);
                        mActionMode.setTitle(String.valueOf(0));
                        //noinspection ConstantConditions
                        listView.getOnItemClickListener().onItemClick(parent, view, position, id);
                        return true;
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    @Subscribe
    public  void onEvaluationError(DataEvaluation.DataEvaluationException e) {
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setEnabled(false);

        titleIndicator.setVisibility(View.GONE);
        pager.setVisibility(View.GONE);

        errorCard.setVisibility(View.VISIBLE);
        errorText.setText(String.format("%s\n\nKlicke hier zum Aktualisieren", e.getMessage()));
    }

    private void hideErrorCard() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setEnabled(true);
                titleIndicator.setVisibility(View.VISIBLE);
                pager.setVisibility(View.VISIBLE);
                errorCard.setVisibility(View.GONE);
            }
        });
    }

    @DebugLog
    @Override
    public void onRefresh() {
        if (errorCard.getVisibility() == View.VISIBLE) {
            hideErrorCard();
            Toast.makeText(MainActivity.this, "Aktualisiere...", Toast.LENGTH_SHORT).show();
        }

        if (mActionMode != null) {
            mActionMode.finish();
        }

        if (!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);

        currentItem = pager.getCurrentItem();
        pagerAdapter = new HbgPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        System.out.println("pager.getCurrentItem() = " + pager.getCurrentItem());
        pager.setCurrentItem(currentItem);
        currentItem = null;

        lastReload = new Date();
        refreshTimer.cancel();
        refreshTimer.start();

//        DataEvaluation.resetTempSaves();

        if (classesList.getAdapter() == null) {
            new RefreshSpinnerTask().execute(this, REFRESH);
        }
//        swipeRefreshLayout.setRefreshing(false);
    }

    @Subscribe
    public void onPermissionRequested(Event.LoadPermissionRequest permissionRequest) {
        System.out.println("Request received");
        EventBus.getDefault().post(new Event.LoadPermission(currentItem != null ? currentItem : pager.getCurrentItem()));
    }


    //--------------RefreshSpinnerTask--------------//
    private class RefreshSpinnerTask extends AsyncTask<Object, Void, ArrayAdapter> {

        private boolean badInternetConnection;
        private boolean error;
        private boolean noInternetConnection;

        @Override
        protected ArrayAdapter doInBackground(Object... params) {
            App.logCodeSection("RefreshSpinnerTask");

            ArrayList<String> classesList = null;
            boolean couldNotRetrieve = false;
            if (params[1] == GET_SAVE_STATE) {
                App.logTrace("Getting save-state");
                try {
                    classesList = App.retrieveObject(CLASSES, getApplicationContext());
                } catch (Exception e) {
                    couldNotRetrieve = true;
                    e.printStackTrace();
                }
                App.logTrace("couldNotRetrieve = " + couldNotRetrieve);
            }

            if (params[1] == REFRESH || couldNotRetrieve) {

                classesList = downloadClassSelection();

                App.logCodeSection("Create arrayAdapter");

                if (classesList == null) {
                    return null;
                }

                App.logInfo("Downloaded and saved class-selection.");
            }

            //noinspection ConstantConditions
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    (Context) params[0],
                    android.R.layout.simple_list_item_1,
                    classesList);


            arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);


            return arrayAdapter;
        }

        protected void onPostExecute(ArrayAdapter aa) {
            App.logCodeSection("Post RefreshSpinnerTask");

            if (aa != null) {
                classesList.setAdapter(aa);
            } else {
                App.logError("ArrayAdapter was null.");
                if (noInternetConnection) {
                    Toast.makeText(MainActivity.this, "Es besteht keine Internetverbindung", Toast.LENGTH_SHORT).show();
                    return;
                } else if (error) {
                    Toast.makeText(MainActivity.this, "Fehler beim Herunterladen der Klassenauswahl", Toast.LENGTH_SHORT).show();
                    return;
                } else if (badInternetConnection) {
                    Toast.makeText(MainActivity.this, "Internetverbindung konnte nicht hergestellt werden", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            int classSelection = prefs.getInt(SELECTED_CLASS, 0);
            classesList.setSelection(classSelection);
//            selectedClassText.setText(classesList.getItemAtPosition(classSelection).toString());

            //noinspection ConstantConditions
            getSupportActionBar().setSubtitle(classesList.getItemAtPosition(classSelection).toString());

            App.logInfo("Spinner selection was set");
        }

        private ArrayList<String> downloadClassSelection() {
            App.logCodeSection("DownloadClassSelection");

            String file;
            try {
                file = new Download(1024, MainActivity.this).connect("http://vp.hbgym.de/frames/navbar.htm", "downloading class-selection-list", 15000).dataToString().trim();
            } catch (SocketTimeoutException | SocketException | UnknownHostException e) {
                e.printStackTrace();
                badInternetConnection = true;
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                error = true;
                return null;
            } catch (InterruptedException e) {
                //never thrown
                return null;
            } catch (Download.NoInternetConnectionException e) {
                System.err.println(e.getMessage());
                noInternetConnection = true;
                return null;
            }

            App.logCodeSection("Filter classes");

            int occ = file.indexOf("\"05a");
            String[] seq = file.substring(occ, occ + 350).split(";", 2);
            String[] classes = seq[0].split(",");

            for (int i = 0; !classes[i].contains("]") && i < classes.length - 1; i++) {
                classes[i] = classes[i].substring(1, classes[i].length() - 1);
            }
            classes[classes.length - 1] = classes[classes.length - 1].substring(1, classes[classes.length - 1].length() - 2);

            App.logInfo("Classes filtered");

            ArrayList<String> arrayListClasses = new ArrayList<>(Arrays.asList(classes));
            arrayListClasses.add(0, "Wähle deine Klasse");
            arrayListClasses.add("Aktualisieren");

            try {
                App.writeObject(arrayListClasses, CLASSES, getBaseContext());
            } catch (Exception e) {
                App.logError("Error writing object");
                App.reportUnexpectedException(e);
                e.printStackTrace();
            }
            return arrayListClasses;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }


    public void onItemSelected(AdapterView<?> parent, View view, final int pos, long id) {
        Log.d(HbgApplication.HBG_APP, "onItemSelected, position = " + pos);

        if (mActionMode != null) {
            mActionMode.finish();
        }

        String className = classesList.getItemAtPosition(pos).toString();

        if (className.equals("Aktualisieren")) {
            System.out.println("Spinnertask3");
            new RefreshSpinnerTask().execute(this, REFRESH);
            return;
        }

        if (pos == 0) {
            ArrayList<HBGMessage> arrayList = new ArrayList<>(1);
            arrayList.add(new HeaderMessage("Keine Klasse ausgewählt"));
            HbgListAdapter arrayAdapter = new HbgListAdapter(this, arrayList);
            final ListView listView = pagerAdapter.getListView(pager.getCurrentItem());
            if (listView == null) {
                return;
            }
            listView.setAdapter(arrayAdapter);
            return;
        }

        final boolean posChanged = pos != prefs.getInt(SELECTED_CLASS, 0);

        if (posChanged) {
//            try {
//                App.writeObject(null, DataEvaluation.SAVED_FILE, MainActivity.this);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            prefs.edit().putInt(SELECTED_CLASS, pos).apply();
            onRefresh();
        }
    }

//    private void changeWeek(int week) {
//        //listView.setAdapter(getLoadingAdapter());
//
//        if (dataEvaluation != null) {
//            dataEvaluation.saveTempListAdapter(listAdapter);
//            dataEvaluation.setWeek(week);
//        }


//        HbgListAdapter savedListAdapter;
//        if (dataEvaluation != null && (savedListAdapter = dataEvaluation.getSavedListAdapter()) != null) {
//            listAdapter = savedListAdapter;
//            listView.setAdapter(listAdapter);
//        } else {
//            final Context context = this;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    dataEvaluation = new DataEvaluation(context, DataEvaluation.REFRESH);
//                    listAdapter = dataEvaluation.getFormattedListAdapter();
//                    listView.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            listView.setAdapter(listAdapter);
//                        }
//                    });
//                }
//            }).start();
//        }
//    }
}

