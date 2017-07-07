package com.eissler.micha.hbgvertretungsapp;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.eissler.micha.hbgvertretungsapp.evaluation.DownloadHandler;
import com.eissler.micha.hbgvertretungsapp.fcm.Subscriptions;
import com.eissler.micha.hbgvertretungsapp.settings.AutoName;
import com.eissler.micha.hbgvertretungsapp.settings.Blacklist;
import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.settings.WhitelistSubjects;
import com.eissler.micha.hbgvertretungsapp.util.DownloadException;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.peracutor.hbgserverapi.HbgAsOfDateDownload;
import com.peracutor.hbgserverapi.HbgClassListDownload;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.ResultCallback;
import com.viewpagerindicator.TitlePageIndicator;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tr.xip.errorview.ErrorView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    //Object-filename
    private final static String CLASSES = "ClassSelectionArrayAdapter";

    public static final long REFRESH_COUNTDOWN_MILLIS = 5 * 60 * 1000;

    private ListView classesList;
    private DrawerLayout drawerLayout;

    Preferences prefs;
    public static boolean mainActivityPaused = true;

    private ActionBarDrawerToggle drawerToggle;
    private ViewPager pager;
    private TitlePageIndicator titleIndicator;
    private ErrorView errorView;

    public com.eissler.micha.hbgvertretungsapp.SwipeRefreshLayout swipeRefreshLayout;
    private App.WaitFor<SwipeRefreshLayout> waitForSwipeRefreshLayout;
    private Date lastReload;
    private Integer currentItem;

//    private Snackbar refreshSnack;

    private final CountDownTimer refreshTimer = new CountDownTimer(REFRESH_COUNTDOWN_MILLIS, REFRESH_COUNTDOWN_MILLIS) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            System.out.println("TIMER FINISHED");
            checkForNewData();
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        App.logCodeSection("ConfigChange");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false); // TODO: 23.03.2017 REMOVE
        prefs = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, this);

//        AccountManager accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
//        Account[] accounts = accountManager.getAccounts();
//        for (Account acc : accounts) {
//            System.out.println("acc.name = " + acc.name);
//        }


//        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        System.out.println("androidId = " + androidId); // TODO: 18.04.2016 maybe use this id

        App.logCodeSection("MainActivity.onCreate");


//        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) { // TODO: 01.03.2016 Create First-Time-Password-Validation
//            System.out.println("getIntent().getDataString() = " + getIntent().getDataString());
//        }

//        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
//            System.out.println("UNCAUGHT EXCEPTION OCCURRED");
//            App.exitWithError(e);
//        });

//        refreshSnack = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
//                .setAction(R.string.act_ma_refresh, refreshOnClick)
//                .setActionTextColor(Color.YELLOW)
//                .setDuration(5000);

        System.out.println("token = " + FirebaseInstanceId.getInstance().getToken());

        firstStartCheck();

        setupSwipeRefreshLayout();
        setupDrawer();
        setupClassSelection();
        setupPager();

        updateCheck();
        checkIntent(getIntent());

        showWhitelistPrompt();
    }

    private void firstStartCheck() {
        final int thisVersion;
        try {
            thisVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        final int previousVersion = prefs.getInt(Preferences.Key.VERSION, 1);
        final boolean firstStart = prefs.getBoolean(Preferences.Key.FIRST_START, true);

        if (!firstStart && previousVersion <= 6) {
            makeUpdateV7Work();
            appIntro(true);
        }

        if (firstStart) {
            Preferences.Editor editor = prefs.edit();
            editor.putBoolean(Preferences.Key.FIRST_START, false);
            editor.putInt(Preferences.Key.VERSION, thisVersion);
            editor.apply();

            Notifications.newInstance(this).enable();
            Subscriptions.newInstance(this).resetSubscriptions();
            appIntro(true);

            App.report(new Exception("App wurde das erste Mal gestartet"));
        } else if (thisVersion > previousVersion) {
            App.logCodeSection("Update was installed");
            prefs.edit().putInt(Preferences.Key.VERSION, thisVersion).apply();
            InstallApk lastSavedApk = InstallApk.getLastSavedApk(this);
            if (lastSavedApk.getVersion() <= thisVersion && lastSavedApk.exists()) {
                lastSavedApk.deleteWithToast(this);
            }

            Notifications.newInstance(this).enable();
            Subscriptions.newInstance(this).resetSubscriptions();


            App.report(new Exception("App-Update wurde installiert"));
        }
    }

    private void makeUpdateV7Work() {
        System.out.println("MainActivity.makeUpdateV7Work");
        try {
            Whitelist.isWhitelistModeActive(this);
        } catch (ClassCastException e) {
            Preferences defPrefs = Preferences.getDefaultPreferences(this);
            Whitelist.enableWhitelistMode(defPrefs.getBoolean(Preferences.Key.WHITELIST_SWITCH, false), this);
        }

        CustomNames customNames = CustomNames.get(this);
        Blacklist blacklist = Blacklist.get(this);

        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, String> entry : customNames.entrySet()) {
            if (entry.getValue().equals("Nicht anzeigen")) {
                keysToRemove.add(entry.getKey()); //else ConcurrentModificationException is thrown
                blacklist.add(entry.getKey());
            }
        }
        for (String key : keysToRemove) {
            customNames.remove(key);
        }
        blacklist.save();
        customNames.save();

        if (isOberstufe(App.getSelectedClass(this))) {
            Preferences.getDefaultPreferences(this).edit().putBoolean(Preferences.Key.AUTO_NAME, false).apply();
        }
    }

    private void appIntro(boolean checkIfClassSelected) {
        if (!checkIfClassSelected || prefs.getInt(Preferences.Key.SELECTED_CLASS, -1) != -1) {
            startActivity(new Intent(this, AppIntro.class));
        }
//        showPushInfo();
//        showHuaweiAlert();
    }

    private void setupDrawer() {
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
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //noinspection ConstantConditions
                getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
                //noinspection ConstantConditions
                if (getSupportActionBar().getSubtitle().equals(getString(R.string.act_ma_choose_class))) {
                    Toast.makeText(MainActivity.this, R.string.act_ma_choose_class, Toast.LENGTH_SHORT).show();
                    drawerLayout.openDrawer(classesList);
                }
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.act_ma_class_selection);
                }
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle.syncState();
    }

    private void setupPager() {
        pager = (ViewPager) findViewById(R.id.view_pager);
        titleIndicator = (TitlePageIndicator) findViewById(R.id.titles);
        onRefresh();
        titleIndicator.setViewPager(pager);


        ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
            private int position;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(final int position) {
                this.position = position;
                System.out.println("MainActivity.onPageSelected");
                System.out.println("new page: position = " + position);

                EventBus.getDefault().post(new Event.LoadPermission(position));
                EventBus.getDefault().post(new Event.FinishActionMode());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                System.out.println("MainActivity.onPageScrollStateChanged");
                boolean pullDownAllowed = state == ViewPager.SCROLL_STATE_IDLE;

                if (!pullDownAllowed) {
                    System.out.println("Preventing Pulldown");
                    EventBus.getDefault().post(new Event.CanScrollUp(true));
                } else {
                    EventBus.getDefault().post(new Event.CanScrollUpRequest(position));
                }
            }
        };

        titleIndicator.setOnPageChangeListener(onPageChangeListener);
    }

    private void setupClassSelection() {
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.act_ma_loading_classes));
        classesList = (ListView) findViewById(R.id.left_drawer);

        assert classesList != null;
        classesList.setOnItemClickListener(this);

        prepareClassSelection(false);
    }

    @SuppressLint("PrivateResource")
    private void setupSwipeRefreshLayout() {
        swipeRefreshLayout = (com.eissler.micha.hbgvertretungsapp.SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        assert swipeRefreshLayout != null;
        swipeRefreshLayout.setColorSchemeResources(R.color.accent_material_light);
        swipeRefreshLayout.setOnRefreshListener(this);

        if (waitForSwipeRefreshLayout != null) {
            waitForSwipeRefreshLayout.onResult(swipeRefreshLayout);
            waitForSwipeRefreshLayout = null;
        }
    }

    private void updateCheck() {
        String action = getIntent().getAction();
        if (action != null && action.equals(App.ACTION_UPDATE)) {
            return;
        }

        long lastUpdateMillis;
        try {
            lastUpdateMillis = prefs.getLong(Preferences.Key.LAST_UPDATE_CHECK, 0);
        } catch (ClassCastException e) {
            e.printStackTrace();
            lastUpdateMillis = 0;
        }
        App.logTrace("lastUpdateMillis = " + lastUpdateMillis);

        if (lastUpdateMillis == 0) {
            new UpdateCheck(this);
        } else {
            Calendar nextUpdateCal = Calendar.getInstance();
            nextUpdateCal.setTimeInMillis(lastUpdateMillis);
            nextUpdateCal.add(Calendar.DAY_OF_MONTH, 2);

            if (new Date().after(nextUpdateCal.getTime())) {
                new UpdateCheck(this);
            }
        }
    }

    private void checkForNewData() {
        new HbgAsOfDateDownload(new DownloadHandler(this)).executeAsync(new ResultCallback<Date>() {
            @Override
            public void onError(Throwable t) {
                Snackbar refreshSnack = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
                        .setAction(R.string.act_ma_refresh, v -> onRefresh())
                        .setActionTextColor(Color.YELLOW)
                        .setDuration(5000);
                refreshSnack.setText(getString(R.string.act_ma_last_reload) + new SimpleDateFormat("HH:mm", Locale.GERMANY).format(lastReload)).show();
            }

            @Override
            public void onResult(Date asOf) {
                System.out.print("As of date: ");
                System.out.println(SimpleDateFormat.getInstance().format(asOf));
                if (lastReload.before(asOf)) {
                    onRefresh();
                }
            }
        });
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

//        mTracker.setScreenName("MainActivity");
//        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        refreshTimer.start();

        resetPager();
        if (App.isMillisecondsLater(lastReload, REFRESH_COUNTDOWN_MILLIS)) {
            checkForNewData();
        }
    }

    @Override
    protected void onPause() {
        System.out.println("ONPAUSE MainActivity");
        super.onPause();
        mainActivityPaused = true;
        refreshTimer.cancel();
        EventBus.getDefault().post(new Event.FinishActionMode());
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
                HbgPagerAdapter.setAvailableWeeks(null);
                errorSolved();
                onRefresh();
                break;
            case R.id.action_settings:
                Log.d(HbgApplication.HBG_APP, "Settings - Button clicked");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.show_in_webbrowser:
                Log.d(HbgApplication.HBG_APP, "Show in browser - Button clicked");

                Toast.makeText(MainActivity.this, R.string.act_ma_open_in_browser, Toast.LENGTH_SHORT).show();

                HbgPagerAdapter.getAvailableWeeks(availableWeeks -> {
                    final String url;
                    if (availableWeeks.size() == 0) {
                        url = "http://vp.hbgym.de/";
                    } else {
                        int weekNumber = availableWeeks.get(pager.getCurrentItem());
                        url = HbgDataDownload.makeURL(weekNumber, getSelectedClass());
                    }

                    Intent dummyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
                    ResolveInfo resolveInfo = getPackageManager().resolveActivity(dummyIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    // This is the default browser's packageName
                    String packageName = resolveInfo.activityInfo.packageName;

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.setPackage(packageName);

                    try {
                        startActivity(browserIntent);
                    } catch (ActivityNotFoundException e) {
                        browserIntent.setPackage(null);
                        startActivity(Intent.createChooser(browserIntent, "Browser wählen"));
                    }
                });

                break;
            default:
                break;
        }
        return true;
    }

    public int getSelectedClass() {
        return prefs.getInt(Preferences.Key.SELECTED_CLASS, 0);
    }

    void resetPager() {
        EventBus.getDefault().post(new Event.ResetRequest());
    }

    @Subscribe
    public void onError(Event.Exception exceptionEvent) {
        showDefaultError(exceptionEvent.getException().getMessage());
    }

    private void showDefaultError(String message) {
        showError(message, getString(R.string.act_ma_refresh), v -> onRefresh());
    }

    private void showError(String errorMessage, String buttonText, final View.OnClickListener onClickListener) {
        System.out.println("MainActivity.showError");
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setCanScrollUp(new Event.CanScrollUp(false));

        refreshTimer.cancel();

        titleIndicator.setVisibility(View.GONE);
        if (errorView == null) {
            errorView = (ErrorView) findViewById(R.id.error_view);
        }
        pager.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);

        errorView.setSubtitle(errorMessage);
        errorView.setRetryButtonText(buttonText);
        errorView.setOnRetryListener(() -> {
            Toast.makeText(MainActivity.this, R.string.act_ma_refreshing, Toast.LENGTH_SHORT).show();
            errorSolved();
            onClickListener.onClick(null);
        });
    }

    private void errorSolved() {
        if (errorView == null) {
            return;
        }
        refreshTimer.start();
        runOnUiThread(() -> {
            errorView.setVisibility(View.GONE);
            titleIndicator.setVisibility(View.VISIBLE);
            pager.setVisibility(View.VISIBLE);
        });
    }

    @Subscribe
    public void onRefreshRequested(Event.RefreshRequest refreshRequest) {
        onRefresh();
    }

    @Override
    public void onRefresh() {
//        if (refreshSnack.isShown()) {
//            refreshSnack.dismiss();
//        }
        if (errorView != null && errorView.getVisibility() == View.VISIBLE) {
            Toast.makeText(MainActivity.this, R.string.act_ma_refreshing, Toast.LENGTH_SHORT).show();
            errorSolved();
        }

        EventBus.getDefault().post(new Event.FinishActionMode());

        swipeRefreshLayout.setRefreshing(true);

        currentItem = pager.getCurrentItem();
        HbgPagerAdapter pagerAdapter = new HbgPagerAdapter(this);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(10/*more than enough, just to make sure ^^ */);
        pager.setCurrentItem(currentItem);
        currentItem = null;

        lastReload = new Date();
        refreshTimer.cancel();
        refreshTimer.start();

        if (classesList.getAdapter().getCount() < 5) {
            prepareClassSelection(true);
        }
//        swipeRefreshLayout.setRefreshing(false);
    }

    @Subscribe
    public void onPermissionRequested(Event.LoadPermissionRequest permissionRequest) {
        System.out.println("Request received");
        int currentItem = this.currentItem != null ? this.currentItem : pager.getCurrentItem();
        if (permissionRequest.getSender() == currentItem) {
            EventBus.getDefault().post(new Event.LoadPermission(currentItem));
        }
    }

    private void prepareClassSelection(boolean refresh) {
        System.out.println("Prepare Class-Selection");

        ArrayList<String> classes;
        if (refresh || (classes = loadClassList(this)) == null) {
            setReloading();
            downloadAndSaveClassList(new ResultCallback<List<String>>() {
                @Override
                public void onError(Throwable t) {
                    classListError((DownloadException) t);
                }

                @Override
                public void onResult(List<String> result) {
                    setClasses(result);
                }
            }, this);
            return;
        }
        setClasses(classes);
    }

    public static ArrayList<String> loadClassList(Context context) {
        try {
            return App.retrieveObject(CLASSES, context);
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            App.report(e);
            return null;
        }
    }

    static void downloadAndSaveClassList(ResultCallback<List<String>> callback, Context context) {
        new HbgClassListDownload(new DownloadHandler(context)).executeAsync(new ResultCallback<List<String>>() {
            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }

            @Override
            public void onResult(List<String> classList) {
                classList = new ArrayList<>(classList);
                classList.set(0, context.getString(R.string.act_ma_choose_class));
                classList.add(context.getString(R.string.act_ma_refresh));

                try {
                    App.writeObject(classList, CLASSES, context);
                } catch (Exception e1) {
                    App.logError("Error writing object");
                    App.report(e1);
                    e1.printStackTrace();
                }
                callback.onResult(classList);
            }
        });
    }


    private void setClasses(List<String> classes) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                MainActivity.this,
                android.R.layout.simple_list_item_1,
                classes);

        classesList.setAdapter(adapter);

        int classSelection = getSelectedClass();
        if (classSelection == 0) {
            drawerLayout.openDrawer(classesList);
        }
        classesList.setSelection(classSelection);

        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(classesList.getItemAtPosition(classSelection).toString());
        ACRA.getErrorReporter().putCustomData("Selected class", String.valueOf(classSelection));
        FirebaseCrash.log("Selected class: " + String.valueOf(classSelection));
    }

    private void classListError(DownloadException e) {
        ArrayList<String> items = new ArrayList<>(2);
        items.add(e.getMessage());
        items.add("Aktualisieren");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                MainActivity.this,
                android.R.layout.simple_list_item_1,
                items);

        classesList.setAdapter(adapter);
    }

    private void setReloading() {
        classesList.setAdapter(new ArrayAdapter<>(
                MainActivity.this,
                android.R.layout.simple_list_item_1,
                new String []{getString(R.string.act_ma_refreshing)}));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        Log.d(HbgApplication.HBG_APP, "onItemSelected, position = " + position);
        if (classesList.getAdapter().getCount() == 2 && position == 0) {
            //error-string (first item) was selected - ignore
            return;
        }

        EventBus.getDefault().post(new Event.FinishActionMode());

        String className = classesList.getItemAtPosition(position).toString();
        final int previousPosition = getSelectedClass();
        final boolean posChanged = position != previousPosition;

        if (!className.equals(getString(R.string.act_ma_refresh))) {
            if (!className.equals("Wähle deine Klasse")) drawerLayout.closeDrawers();
            //noinspection ConstantConditions
            getSupportActionBar().setSubtitle(className);
        }

        if (className.equals("Aktualisieren")) {
            prepareClassSelection(true);
        } else if (posChanged && Whitelist.isWhitelistModeActive(this)) {
            App.dialog("Klassenauswahl ändern", "Achtung! Eventuell solltest du vorher die Liste deiner Fächer ändern.\n\nMöchtest du trotzdem eine andere Klasse auswählen?", this)
                    .setPositiveButton("Ja", (a, b) -> changeClassSelection(position, previousPosition))
                    .setNegativeButton("Nein", (a, b) -> classesList.performItemClick(null, previousPosition, previousPosition))
                    .setNeutralButton("Liste ändern", (a, b) -> {
//                        App.dialog("Fachfilter-Modus", "Möchtest du den Whitelist-Modus deaktivieren, oder die Fächer ändern?", MainActivity.this)
//                                .setPositiveButton("Ändern", (dialogInterface1, which1) -> startActivity(new Intent(MainActivity.this, WhitelistSubjects.class)))
//                                .setNegativeButton("Deaktivieren", (c, d) -> {
//                                    Whitelist.enable(false, MainActivity.this);
//                                    resetPager();
//                                })
//                                .show();
                        startActivity(new Intent(MainActivity.this, WhitelistSubjects.class));
                        changeClassSelection(position, previousPosition);
                    })
                    .setCancelable(false)
                    .show();
        } else if (posChanged) {
            changeClassSelection(position, previousPosition);
        }
    }

    public void changeClassSelection(int position, int previousPosition) {
        if (isOberstufe(position) != isOberstufe(previousPosition)) {
            if (AutoName.isAutoNamingEnabled(this)) {
                Preferences.Editor editor = Preferences.getDefaultPreferences(this).edit();
                editor.putString(Preferences.Key.AUTO_NAME_PATTERN, "*f"); //set to default value
                editor.putBoolean(Preferences.Key.AUTO_NAME, false);
                editor.apply();
                Toast.makeText(this, "Ausschreiben der Fachkürzel wurde deaktiviert", Toast.LENGTH_LONG).show();
            }
            showWhitelistPrompt();
        }

        previousPosition = prefs.getInt(Preferences.Key.SELECTED_CLASS, -1);

        prefs.edit().putInt(Preferences.Key.SELECTED_CLASS, position).apply();
        ACRA.getErrorReporter().putCustomData("Selected class", String.valueOf(position));
        FirebaseCrash.log("Selected class: " + String.valueOf(position));
        onRefresh();
        Subscriptions.newInstance(this).resetSubscriptions();

        if (previousPosition == -1) { //no value saved for Key.SELECTED_CLASS, so show AppIntro
            appIntro(false);
        }
    }

    private void showWhitelistPrompt() {
        if (isOberstufe(getSelectedClass()) && !Whitelist.isWhitelistModeActive(this) && !prefs.getBoolean(Preferences.Key.WHITELIST_DONT_PROMPT, false)) {
            new MaterialDialog.Builder(this)
                    .title("Oberstufenfächer einspeichern")
                    .content("Willst du einmalig alle deine Kurse einspeichern, sodass du ausschließlich Meldungen zu diesen erhältst?")
                    .checkBoxPrompt("Nicht mehr fragen", true, null)
                    .positiveText("Ja")
                    .negativeText("Nein")
                    .onPositive((dialog, which) -> {
                        Whitelist.enableWhitelistMode(true, MainActivity.this);

                        startActivity(new Intent(MainActivity.this, WhitelistSubjects.class));
                    })
                    .onAny((dialog, which) -> {
                        if (dialog.isPromptCheckBoxChecked()) prefs.edit().putBoolean(Preferences.Key.WHITELIST_DONT_PROMPT, true).apply();
                    })
                    .cancelable(false)
                    .show();
        }
    }

    private boolean isOberstufe(int classNumber) {
        return classNumber == 21 || classNumber == 22;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(Intent intent) {
        System.out.println("MainActivity.checkIntent");
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case App.ACTION_UPDATE:
                String type = intent.getExtras().getString("type");
                assert type != null;
                if (type.equals("actionButton")) {
                    System.out.println("ACTION_UPDATE");
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(intent.getIntExtra("id", -1));
//                    new UpdateTask(intent.getIntExtra("versionNumber", -1), intent.getStringExtra("apkUrl"), this);
                }
                new UpdateCheck(this);
                break;
            case Intent.ACTION_VIEW:
                System.out.println("ACTION_VIEW");
                try {
                    URL url = new URL(intent.getDataString());
                    System.out.println("url = " + url);
                    String path = url.getPath();
                    boolean matches = path.matches("/w/[0-5][0-9]/w[0-9]{5}.htm");
                    if (matches) {
                        path = path.substring(1, path.lastIndexOf('.')); //remove prefix "/" and appendix ".htm"
                        final int week = Integer.parseInt(path.substring(path.indexOf('/') + 1, path.lastIndexOf('/')));
                        HbgPagerAdapter.getAvailableWeeks(availablePages -> {
                            int index = availablePages.indexOf(week);
                            if (index == -1) {
                                Toast.makeText(MainActivity.this, "Woche nicht verfügbar", Toast.LENGTH_LONG).show();
                                return;
                            }
                            pager.setCurrentItem(index);
                        });

                        final int classNumber = Integer.parseInt(path.substring(path.length() - 3));
                        if (classNumber != getSelectedClass()) {
                            //noinspection unchecked
                            App.dialog("Link", "Der angeklickte Link ist für Klasse " + ((ArrayAdapter<String>) classesList.getAdapter()).getItem(classNumber) + ".\n\n" +
                                    "Diese Klasse jetzt auswählen?", this)
                                    .setPositiveButton("Ja", (dialogInterface, i) -> classesList.performItemClick(null, classNumber, classNumber))
                                    .show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Subscribe
    public void setRefreshing(Event.RefreshStatus status) {
        waitForSwipeRefreshLayout(refreshLayout -> refreshLayout.setRefreshing(status.getStatus()));
    }

    @Subscribe
    public void provideActivity(Event.WaitForMainActivity waitForMainActivity) {
        waitForMainActivity.getWaitFor().onResult(this);
    }

    public void waitForSwipeRefreshLayout(App.WaitFor<SwipeRefreshLayout> waitFor) {
        if (swipeRefreshLayout != null) {
            waitFor.onResult(swipeRefreshLayout);
        } else {
            waitForSwipeRefreshLayout = waitFor;
        }
    }
}

