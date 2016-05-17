package com.eissler.micha.hbgvertretungsapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HbgPagerAdapter extends FragmentStatePagerAdapter {

    private static final SimpleDateFormat SHORT_SDF = new SimpleDateFormat("dd.MM.", Locale.GERMANY);
    private static final String FORMAT = "%s - %s";

    private FragmentPage[] fragmentPages;
//    private FragmentPage.FragmentPageListener fragmentPageListener;
    private String[] titles;

    public HbgPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
//        this.fragmentPageListener = fragmentPageListener;
        fragmentPages = new FragmentPage[5];
        titles = new String[5];
    }

    @Override
    public Fragment getItem(int position) {
        FragmentPage fragmentPage;

        if (fragmentPages[position] == null) {
            System.out.println("GENERATING, SAVING");
            fragmentPage = new FragmentPage();
            Bundle args = new Bundle(1);
            args.putInt("position", position);
            fragmentPage.setArguments(args);
            fragmentPages[position] = fragmentPage;
        } else {
            System.out.println("RETURNING SAVED FRAGMENT PAGE");
            fragmentPage = fragmentPages[position];
        }
        return fragmentPage;
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (titles[position] == null) {
            int dayInWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

            int weeksToAdd = position;
            boolean isWeekEnd = dayInWeek == Calendar.SATURDAY || dayInWeek == Calendar.SUNDAY;
            if (isWeekEnd) {
                weeksToAdd++;
            }

            if (weeksToAdd == 0) {
                titles[position] = FragmentPage.THIS_WEEK;

            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.WEEK_OF_YEAR, weeksToAdd);
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

                final String startDate = SHORT_SDF.format(calendar.getTime());
                calendar.add(Calendar.DAY_OF_WEEK, 6);

                final String endDate = SHORT_SDF.format(calendar.getTime());
                titles[position] = String.format(FORMAT, startDate, endDate);
            }
        }
        return titles[position];
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public ListView getListView(int position) {
        return fragmentPages[position] != null ? fragmentPages[position].getListView() : null;
    }
}
