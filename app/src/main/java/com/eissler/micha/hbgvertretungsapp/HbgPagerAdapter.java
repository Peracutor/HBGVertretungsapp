package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.peracutor.hbgserverapi.DownloadException;
import com.peracutor.hbgserverapi.HbgAvailableWeeksDownload;
import com.peracutor.hbgserverapi.ResultCallback;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HbgPagerAdapter extends FragmentStatePagerAdapter {

    private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);
    private static final String FORMAT = "%s - %s";

    static ArrayList<Integer> availableWeeks;
    private static App.WaitFor<ArrayList<Integer>> waitFor;
    private static Date lastAvailablePagesLoad;

    private Fragment[] fragments;
    private String[] titles;
    private Context context;

    public HbgPagerAdapter(AppCompatActivity activity) {
        super(activity.getSupportFragmentManager());
        this.context = activity;
        titles = new String[getCount()];
        if (availableWeeks == null || new Date(new Date().getTime() - MainActivity.REFRESH_COUNTDOWN_MILLIS).after(lastAvailablePagesLoad)) {
            availableWeeks = null;
            fragments = new Fragment[]{new Fragment()}; //empty page

            System.out.println("MainActivity.loadAvailablePages");
            if (App.isConnected(context)) {
                new AvailableWeeksDownload().executeAsync();
            } else {
                EventBus.getDefault().post(new Event.Exception(new DownloadException(DownloadException.ErrorType.NO_CONNECTION)));
            }
        } else {
            fragments = new Fragment[getCount()];
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment;

        if (fragments[position] == null) {
            System.out.println("GENERATING, SAVING");
            fragment = new FragmentPage();
            Bundle args = new Bundle(1);
            args.putInt("position", position);
            args.putInt("weekNumber", availableWeeks.get(position));
            fragment.setArguments(args);
            fragments[position] = fragment;
        } else {
            System.out.println("RETURNING SAVED FRAGMENT PAGE");
            fragment = fragments[position];
        }
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
            return "Keine Woche auswählbar";
        }

        String title = titles[position];

        if (title == null) {
            //noinspection WrongConstant
            if (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) == availableWeeks.get(position)) {
                title = FragmentPage.THIS_WEEK;
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

    private class AvailableWeeksDownload extends HbgAvailableWeeksDownload {

        public void executeAsync() {
            super.executeAsync(new ResultCallback<ArrayList<Integer>>() {
                @Override
                public void onResult(ArrayList<Integer> availableWeeks) {
                    System.out.println("availableWeeks = " + availableWeeks);
                    HbgPagerAdapter.availableWeeks = availableWeeks;
                    lastAvailablePagesLoad = new Date();
                    fragments = new Fragment[getCount()];
                    titles = new String[getCount()];

                    if (waitFor != null) {
                        waitFor.onResult(availableWeeks);
                        waitFor = null;
                    }

                    notifyDataSetChanged();
                }

                @Override
                public void onError(Throwable t) {
                    EventBus.getDefault().post(new Event.Exception(new DownloadException(t)));
                }
            });
        }

        @Override
        public void asyncDownload(String urlString, Charset charset, final ResultCallback<String> callback) {
            Ion.with(context)
                    .load(urlString)
                    .asString(charset)
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            if (e != null) {
                                callback.onError(e);
                            } else {
                                callback.onResult(result);
                            }
                        }
                    })
            ;
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
