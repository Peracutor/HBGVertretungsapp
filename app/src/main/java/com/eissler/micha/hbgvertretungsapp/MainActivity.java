package com.eissler.micha.hbgvertretungsapp;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

import com.eissler.micha.hbgvertretungsapp.settings.SettingsActivity;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.settings.WhitelistSubjects;
import com.google.firebase.iid.FirebaseInstanceId;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.peracutor.hbgserverapi.DownloadException;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.viewpagerindicator.TitlePageIndicator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import hugo.weaving.DebugLog;
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

    private View.OnClickListener refreshOnClick;

    public com.eissler.micha.hbgvertretungsapp.SwipeRefreshLayout swipeRefreshLayout;
    private App.WaitFor<SwipeRefreshLayout> waitForSwipeRefreshLayout;
    private Date lastReload;
    private Integer currentItem;

    private Snackbar refreshSnack;

    private final CountDownTimer refreshTimer = new CountDownTimer(REFRESH_COUNTDOWN_MILLIS, REFRESH_COUNTDOWN_MILLIS) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            System.out.println("TIMER FINISHED");
            showRefreshSnack();
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

        prefs = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, this);

//        AccountManager accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
//        Account[] accounts = accountManager.getAccounts();
//        for (Account acc : accounts) {
//            System.out.println("acc.name = " + acc.name);
//        }


//        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        System.out.println("androidId = " + androidId); // TODO: 18.04.2016 maybe use this id


//        Log.d(HbgApplication.HBG_APP, "onCreate MainActivity");
        App.logCodeSection("MainActivity.onCreate");


//        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) { // TODO: 01.03.2016 Create First-Time-Password-Validation
//            System.out.println("getIntent().getDataString() = " + getIntent().getDataString());
//        }

        refreshOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        };

        refreshSnack = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
                .setAction(R.string.act_ma_refresh, refreshOnClick)
                .setActionTextColor(Color.YELLOW)
                .setDuration(5000);

        App.logCodeSection("ViewAssignment");

        System.out.println("token = " + FirebaseInstanceId.getInstance().getToken());

        firstStartCheck();

        setupSwipeRefreshLayout();
        setupDrawer();
        setupClassSelection();
        setupPager();

        updateCheck();
        checkIntent(getIntent());
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

    private void firstStartCheck() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            final int thisVersion = packageInfo.versionCode;
//            final String thisVersionName = packageInfo.versionName;
            final boolean firstStart = prefs.getBoolean(Preferences.Key.FIRST_START, true);

            if (firstStart) {
                Preferences.Editor editor = prefs.edit();
                editor.putBoolean(Preferences.Key.FIRST_START, false);
                editor.putInt(Preferences.Key.VERSION, thisVersion);
                editor.apply();

                Notifications.newInstance(this).enable();


//                ACRA.getErrorReporter().handleSilentException(new Exception("App wurde das erste Mal gestartet")); // TODO: 18.04.2016 App wurde erstes mal gestartet - ACRA Meldung deaktiviert
            } else if (thisVersion > prefs.getInt(Preferences.Key.VERSION, 1)) {
                App.logCodeSection("Update was installed");
                prefs.edit().putInt(Preferences.Key.VERSION, thisVersion).apply();
                InstallApk lastSavedApk = InstallApk.getLastSavedApk(this);
                if (lastSavedApk.getVersion()== thisVersion && lastSavedApk.exists()) {
                    lastSavedApk.deleteWithToast(this);
                }
//                ACRA.getErrorReporter().handleSilentException(new Exception("App-Update wurde installiert"));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
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

    private void showRefreshSnack() {
        refreshSnack.setText(getString(R.string.act_ma_last_reload) + new SimpleDateFormat("HH:mm", Locale.GERMANY).format(lastReload)).show();
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
            showRefreshSnack();
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
                onRefresh();
                break;
            case R.id.action_settings:
                Log.d(HbgApplication.HBG_APP, "Settings - Button clicked");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.show_in_webbrowser:
                Log.d(HbgApplication.HBG_APP, "Show in browser - Button clicked");

                if (HbgPagerAdapter.availableWeeks != null) {
                    int weekNumber = HbgPagerAdapter.availableWeeks.get(pager.getCurrentItem());
                    int classNum = prefs.getInt(Preferences.Key.SELECTED_CLASS, 0);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(HbgDataDownload.makeURL(weekNumber, classNum)));
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(MainActivity.this, R.string.act_ma_no_week_selected, Toast.LENGTH_LONG).show();
                }

                break;
            default:
                break;
        }
        return true;
    }

    void resetPager() {
        EventBus.getDefault().post(new Event.ResetRequest());
    }

    @Subscribe
    public void onError(Event.Exception exceptionEvent) {
        showDefaultError(exceptionEvent.getException().getMessage());
    }

    private void showDefaultError(String message) {
        showError(message, getString(R.string.act_ma_refresh), refreshOnClick);
    }

    private void showError(String errorMessage, String buttonText, final View.OnClickListener onClickListener) {
        System.out.println("MainActivity.showError");
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setEnabled(false);
        refreshTimer.cancel();

        titleIndicator.setVisibility(View.GONE);
        if (errorView == null) {
            errorView = (ErrorView) findViewById(R.id.error_view);
        }
        errorView.setVisibility(View.VISIBLE);
        pager.setVisibility(View.GONE);

        errorView.setSubtitle(errorMessage);
        errorView.setRetryButtonText(buttonText);
        errorView.setOnRetryListener(new ErrorView.RetryListener() {
            @Override
            public void onRetry() {
                Toast.makeText(MainActivity.this, R.string.act_ma_refreshing, Toast.LENGTH_SHORT).show();
                errorSolved();
                onClickListener.onClick(null);
            }
        });
    }

    private void errorSolved() {
        refreshTimer.start();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setEnabled(true);
                titleIndicator.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                pager.setVisibility(View.VISIBLE);
            }
        });
    }

    @Subscribe
    public void onRefreshRequested(Event.RefreshRequest refreshRequest) {
        onRefresh();
    }

    @DebugLog
    @Override
    public void onRefresh() {
        if (refreshSnack.isShown()) {
            refreshSnack.dismiss();
        }
        EventBus.getDefault().post(new Event.FinishActionMode());

        if (!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);

        currentItem = pager.getCurrentItem();
        HbgPagerAdapter pagerAdapter = new HbgPagerAdapter(this);
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(currentItem);
        currentItem = null;

        lastReload = new Date();
        refreshTimer.cancel();
        refreshTimer.start();

        if (classesList.getAdapter().getCount() < 10) {
            prepareClassSelection(true);
        }
//        swipeRefreshLayout.setRefreshing(false);
    }

    @Subscribe
    public void onPermissionRequested(Event.LoadPermissionRequest permissionRequest) {
        System.out.println("Request received");
        EventBus.getDefault().post(new Event.LoadPermission(currentItem != null ? currentItem : pager.getCurrentItem()));
    }


    private void prepareClassSelection(boolean refresh) {
        System.out.println("Prepare Class-Selection");
        ArrayList<String> classes = null;
        boolean error = false;
        if (!refresh) try {
            classes = App.retrieveObject(CLASSES, MainActivity.this);
        } catch (IOException e) {
            error = true;
        } catch (ClassNotFoundException e) {
            App.exitWithError(e);
            return;
        }

        if (classes == null) {
            error = true;
        }

        if (!refresh && !error) {
            setClasses(classes);
            return;
        }




        //refreshing:
        setReloading();

        if (!App.isConnected(this)) {
            classListError(new DownloadException(DownloadException.ErrorType.NO_CONNECTION));
            return;
        }

        Ion.with(this)
                .load("http://vp.hbgym.de/frames/navbar.htm")
                .noCache()
                .asString(Charset.forName("ISO-8859-15"))
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String html) {
                        if (e != null) {
                            classListError(DownloadException.getCorrespondingExceptionFor(e));
                            return;
                        }

                        int occ = html.indexOf("\"05a");
                        String[] seq = html.substring(occ, occ + 350).split(";", 2);
                        String[] classes = seq[0].split(",");

                        for (int i = 0; !classes[i].contains("]") && i < classes.length - 1; i++) {
                            classes[i] = classes[i].substring(1, classes[i].length() - 1);
                        }
                        classes[classes.length - 1] = classes[classes.length - 1].substring(1, classes[classes.length - 1].length() - 2);

                        ArrayList<String> downloadedClassList = new ArrayList<>(Arrays.asList(classes));
                        downloadedClassList.add(0, getString(R.string.act_ma_choose_class));
                        downloadedClassList.add(getString(R.string.act_ma_refresh));

                        try {
                            App.writeObject(downloadedClassList, CLASSES, getBaseContext());
                        } catch (Exception e1) {
                            App.logError("Error writing object");
                            App.reportUnexpectedException(e1);
                            e1.printStackTrace();
                        }
                        setClasses(downloadedClassList);
                    }
                });
    }

    private void setClasses(ArrayList<String> classes) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                MainActivity.this,
                android.R.layout.simple_list_item_1,
                classes);

        classesList.setAdapter(adapter);

        int classSelection = prefs.getInt(Preferences.Key.SELECTED_CLASS, 0);
        if (classSelection == 0) {
            drawerLayout.openDrawer(classesList);
        }
        classesList.setSelection(classSelection);

        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(classesList.getItemAtPosition(classSelection).toString());
    }

    private void classListError(DownloadException e) {
        ArrayList<String> downloadedClassList = new ArrayList<>(2);
        downloadedClassList.add(e.getMessage());
        downloadedClassList.add("Aktualisieren");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                MainActivity.this,
                android.R.layout.simple_list_item_1,
                downloadedClassList);

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
        final int previousSelection = prefs.getInt(Preferences.Key.SELECTED_CLASS, 0);
        final boolean posChanged = position != previousSelection;

        if (!className.equals(getString(R.string.act_ma_refresh))) {
            if (!className.equals("Wähle deine Klasse")) drawerLayout.closeDrawers();
            //noinspection ConstantConditions
            getSupportActionBar().setSubtitle(className);
        }

        if (className.equals("Aktualisieren")) {
            prepareClassSelection(true);
        } else if (posChanged && Whitelist.isWhitelistModeActive(this)) {
            final DialogInterface.OnClickListener listener;
            App.dialog("Klassenauswahl", "Der Whitelist-Modus ist aktiv - eventuell solltest du die Fächer deiner Whitelist ändern!\n\nMöchtest du wirklich eine andere Klasse auswählen?", this)
                    .setPositiveButton("Ja", listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                classesList.performItemClick(null, previousSelection, previousSelection);
                                return;
                            } else if (which == DialogInterface.BUTTON_NEUTRAL) {
                                DialogInterface.OnClickListener listener2;
                                App.dialog("Whitelist-Modus", "Möchtest du den Whitelist-Modus deaktivieren, oder die Fächer ändern?", MainActivity.this)
                                        .setPositiveButton("Ändern", listener2 = new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int which) {
                                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                                    startActivity(new Intent(MainActivity.this, WhitelistSubjects.class));
                                                } else {
                                                    Whitelist.enable(false, MainActivity.this);
                                                    resetPager();
                                                }
                                            }
                                        })
                                        .setNegativeButton("Deaktivieren", listener2)
                                        .show();
                            }

                            prefs.edit().putInt(Preferences.Key.SELECTED_CLASS, position).apply();
                            onRefresh();
                        }
                    })
                    .setNegativeButton("Nein", listener)
                    .setNeutralButton("Whitelist ändern", listener)
                    .show();
        } else if (posChanged) {
            prefs.edit().putInt(Preferences.Key.SELECTED_CLASS, position).apply();
            onRefresh();
        }
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
                System.out.println("ACTION_UPDATE");
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(intent.getIntExtra("id", -1));
                new UpdateTask(intent.getIntExtra("versionNumber", -1), intent.getStringExtra("apkUrl") , this);
                break;
            case Intent.ACTION_VIEW:
                System.out.println("ACTION_VIEW");
                try {
                    URL url = new URL(intent.getDataString());
                    System.out.println("url = " + url);
                    String path = url.getPath();
                    boolean matches = path.matches("\\/w\\/[0-5][0-9]\\/w[0-9]{5}.htm");
                    if (matches) {
                        path = path.substring(1, path.lastIndexOf('.')); //remove prefix "/" and appendix ".htm"
                        final int week = Integer.parseInt(path.substring(path.indexOf('/') + 1, path.lastIndexOf('/')));
                        HbgPagerAdapter.getAvailableWeeks(new App.WaitFor<ArrayList<Integer>>() {
                            @Override
                            public void onResult(ArrayList<Integer> availablePages) {
                                int index = availablePages.indexOf(week);
                                if (index == -1) {
                                    Toast.makeText(MainActivity.this, "Woche nicht verfügbar", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                pager.setCurrentItem(index);
                            }
                        });

                        final int classNumber = Integer.parseInt(path.substring(path.length() - 3));
                        if (classNumber != prefs.getInt(Preferences.Key.SELECTED_CLASS, 0)) {
                            //noinspection unchecked
                            App.dialog("Link", "Der angeklickte Link ist für Klasse " + ((ArrayAdapter<String>) classesList.getAdapter()).getItem(classNumber) + ".\n\n" +
                                    "Diese Klasse jetzt auswählen?", this)
                                    .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            classesList.performItemClick(null, classNumber, classNumber);
                                        }
                                    })
                                    .show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }



    public void waitForSwipeRefreshLayout(App.WaitFor<SwipeRefreshLayout> waitFor) {
        if (swipeRefreshLayout != null) {
            waitFor.onResult(swipeRefreshLayout);
        } else {
            waitForSwipeRefreshLayout = waitFor;
        }
    }
}

