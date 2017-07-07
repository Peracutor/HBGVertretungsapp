package com.eissler.micha.hbgvertretungsapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import com.eissler.micha.hbgvertretungsapp.evaluation.DownloadHandler;
import com.eissler.micha.hbgvertretungsapp.util.DownloadException;
import com.peracutor.hbgserverapi.HbgAvailableWeeksDownload;
import com.peracutor.hbgserverapi.ResultCallback;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HbgPagerAdapter extends FragmentStatePagerAdapter {

    private static final String THIS_WEEK = "Diese Woche";
    private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);
    private static final String FORMAT = "%s - %s";

    private static ArrayList<Integer> availableWeeks;
    private static App.WaitFor<ArrayList<Integer>> waitFor;
    private static Date lastAvailablePagesLoad;

    private String[] titles;

    public HbgPagerAdapter(AppCompatActivity activity) {
        super(activity.getSupportFragmentManager());
        titles = new String[getCount()];
        if (availableWeeks == null || new Date(new Date().getTime() - MainActivity.REFRESH_COUNTDOWN_MILLIS).after(lastAvailablePagesLoad)) {
            availableWeeks = null;

            new HbgAvailableWeeksDownload(new DownloadHandler(activity)).executeAsync(new ResultCallback<ArrayList<Integer>>() {
                @Override
                public void onResult(ArrayList<Integer> availableWeeks) {
                    System.out.println("availableWeeks = " + availableWeeks);
//                    if (true)
//                    {
//                        Calendar cal = Calendar.getInstance();
//                        cal.set(Calendar.WEEK_OF_YEAR, availableWeeks.get(availableWeeks.size() - 1));
//                        for (int i = 1; i < 4; i++) {
//                            cal.add(Calendar.WEEK_OF_YEAR, 1);
//                            int weekNumber = cal.get(Calendar.WEEK_OF_YEAR);
//                            System.out.println("weekNumber = " + weekNumber);
//                            availableWeeks.add(weekNumber);
//                        }
//                    }
                    HbgPagerAdapter.availableWeeks = availableWeeks;
                    lastAvailablePagesLoad = new Date();
                    titles = new String[getCount()];

                    if (waitFor != null) {
                        waitFor.onResult(availableWeeks);
                        waitFor = null;
                    }

                    notifyDataSetChanged();
                }

                @Override
                public void onError(Throwable t) {
                    Exception e;
                    if (!t.getMessage().equals("Fehler beim Parsen")) {
                        e = new DownloadException(t);
                    } else {
                        e = new Exception(t);
                    }
                    EventBus.getDefault().post(new Event.Exception(e));
                }
            });
        }
    }

    public static void setAvailableWeeks(ArrayList<Integer> availableWeeks) {
        HbgPagerAdapter.availableWeeks = availableWeeks;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        if (availableWeeks == null || availableWeeks.size() == 0) {
            return new Fragment();
        }
        Fragment fragment = new FragmentPage();
        Bundle args = new Bundle(1);
        args.putInt("position", position);
        args.putInt("weekNumber", availableWeeks.get(position));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        if (availableWeeks != null && availableWeeks.size() != 0) {
            return availableWeeks.size();
        }
        return 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        if (availableWeeks == null) {
            return "Lade Wochenauswahl...";
        } else if (availableWeeks.size() == 0) {
            EventBus.getDefault().post(new Event.RefreshStatus(false));
            return "Keine Woche verfügbar";
        }

        String title = titles[position];

        if (title == null) {
            //noinspection WrongConstant
            if (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) == availableWeeks.get(position)) {
                title = THIS_WEEK;
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.WEEK_OF_YEAR, availableWeeks.get(position));
                System.out.println("calendar.get(Calendar.WEEK_OF_YEAR) = " + calendar.get(Calendar.WEEK_OF_YEAR));//<-- don't remove, doesn't work without printing (probably a bug)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

                final String startDate = SHORT_SDF.format(calendar.getTime());
                calendar.add(Calendar.DAY_OF_WEEK, 6);

                final String endDate = SHORT_SDF.format(calendar.getTime());
                title = String.format(FORMAT, startDate, endDate);
            }

            titles[position] = title;
        }

        return title;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public static void getAvailableWeeks(App.WaitFor<ArrayList<Integer>> waitFor) {
        if (availableWeeks != null) {
            waitFor.onResult(availableWeeks);
        } else {
            HbgPagerAdapter.waitFor = waitFor;
        }
    }


//    public static class ErrorFragment extends Fragment {
//        private ViewGroup view;
//        private ErrorView errorView;
//
//        @Nullable
//        @Override
//        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//            if (view == null) {
//                view = (ViewGroup) inflater.inflate(
//                        R.layout.text_fragment, container, false);
//
//                errorView = (ErrorView) view.findViewById(R.id.error_view);
//            }
//            return view;
//        }
//
//        public void setError(String error) {
//            Bundle args = new Bundle(1);
//            args.putString("error", error);
//            setArguments(args);
//            updateView();
//        }
//
//        private void updateView() {
//            errorView.setSubtitle(getArguments().getString("error"));
//        }
//    }
}
