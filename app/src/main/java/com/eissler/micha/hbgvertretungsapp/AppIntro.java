package com.eissler.micha.hbgvertretungsapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.eissler.micha.hbgvertretungsapp.settings.AutoName;
import com.github.paolorotolo.appintro.AppIntro2Fragment;
import com.github.paolorotolo.appintro.AppIntroFragment;

import java.util.List;

/**
 * Created by Micha.
 * 22.02.2017
 */
public class AppIntro extends com.github.paolorotolo.appintro.AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int primaryColorDark = Color.parseColor("#84000D");
        showSkipButton(false);
        setColorDoneText(primaryColorDark);
        setIndicatorColor(primaryColorDark, Color.LTGRAY);
        addSlide(AppIntro2Fragment.newInstance("Anzeigenamen der Fächer", "Tippe auf Fächer, um sie umzubenennen. " +
                (AutoName.isAutoNamingEnabled(this) ? "Fachkürzel werden autmatisch ausgeschrieben." : "Fachkürzel können in den Eintstellungen automatisch ausgeschrieben werden."), R.drawable.img_intro_1, Color.WHITE, primaryColorDark, Color.BLACK));
        addSlide(AppIntro2Fragment.newInstance("Echtzeit-Benachrichtigungen", "Du erhältst automatisch Benachrichtigungen bei neuen Vertretungsmeldungen (Beta).", R.drawable.img_intro_4, Color.WHITE, primaryColorDark, Color.BLACK));
        addSlide(AppIntro2Fragment.newInstance("Meldungen auswählen", "Meldungen für Mehrfachauswahl gedrückt halten. Verstecken oder Teilen von Meldungen möglich.",
                R.drawable.img_intro_2, Color.WHITE, primaryColorDark, Color.BLACK));


        Intent intent = new Intent();
        intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            AppIntroFragment fragment = AppIntro2Fragment.newInstance("Huawei \"Geschützte Apps\"", "Die HBG-Vertretungsapp muss beim Huawei Akkumanager als \"geschützt\" eingestellt werden, damit Benachrichtigungen erhalten werden können",
                    R.drawable.img_intro_3, Color.WHITE, primaryColorDark, Color.BLACK);
            Bundle arguments = fragment.getArguments();
            arguments.putBoolean("isHuaweiFragment", true);
            fragment.setArguments(arguments);
            addSlide(fragment);
        }

    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        if (currentFragment != null && currentFragment.getArguments().containsKey("isHuaweiFragment")) {
            Intent intent = new Intent();
            intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
            finish();
            startActivity(intent);
            return;
        }
        finish();
//        startActivity(new Intent(this, MainActivity.class));
    }

//    private void showHuaweiAlert() {
//        Intent intent = new Intent();
//        intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
//        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
//                PackageManager.MATCH_DEFAULT_ONLY);
//        if (list.size() > 0) {
//            new MaterialDialog.Builder(this)
//                    .title("Huawei \"Geschützte Apps\"")
//                    .content("Die HBG-Vertretungsapp muss beim Huawei Akkumanager als \"geschützt\" eingestellt werden, damit Benachrichtigungen erhalten werden können")
//                    .positiveText("Geschützte Apps")
//                    .onPositive((dialog, which) -> startActivity(intent))
//                    .negativeText("Abbrechen")
//                    .cancelable(false)
//                    .show();
//        }
//    }
//
//    private void showPushInfo() {
//        new MaterialDialog.Builder(this)
//                .title("Echtzeit-Benachrichtigungen")
//                .content("Ab sofort erhältst du bei neuen Vertretungsmeldungen (zur aktuellen Woche) eine Benachrichtigung.")
//                .positiveText("Ok")
//                .show();
//    }


}
