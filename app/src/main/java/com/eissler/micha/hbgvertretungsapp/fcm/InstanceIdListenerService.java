package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.eissler.micha.hbgvertretungsapp.AlarmReceiver;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.ConnectivityChangeReceiver;
import com.eissler.micha.hbgvertretungsapp.util.PreferenceExponentialBackoff;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.eissler.micha.hbgvertretungsapp.util.ProcessorDistributor;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.peracutor.hbgbackend.registration.Registration;

import org.acra.ACRA;
import org.acra.util.Installation;

import java.io.IOException;

import static com.eissler.micha.hbgvertretungsapp.util.Preferences.Key.REGISTER_CHECK_BACKOFF;

public class InstanceIdListenerService extends FirebaseInstanceIdService {


    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        System.out.println("InstanceIdListenerService.onTokenRefresh");
        Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, this).edit().putBoolean(Preferences.Key.TOKEN_SENT, false).apply();
        FirebaseMessaging.getInstance().subscribeToTopic("global");

        sendRegistrationToServer(this);
    }

    public static void sendRegistrationToServer(Context context) {
        System.out.println("InstanceIdListenerService.sendRegistrationToServer");
        if (!App.isConnected(context)) {
            ConnectivityChangeReceiver.enable(true, context);
            return;
        }

        setRegisterCheckAlarm(context);
        String token = FirebaseInstanceId.getInstance().getToken();
        new AppEngine.Task<Registration>(new Registration.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null), registration -> {
            try {
                registration.registerDevice(token, Installation.id(context)).execute();
                System.out.println("FCM token sent to server");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Registration-error: " + e.getMessage()); //will retry when alarm is launched
            }
        }).execute();
    }

    private static void setRegisterCheckAlarm(Context context) {
        System.out.println("InstanceIdListenerService.setRegisterCheckAlarm");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("alarm.register_check");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 3, intent, 0);

        PreferenceExponentialBackoff backoff = getRegisterCheckBackoff(context);
        if (backoff.retry()) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + backoff.getValue(), pendingIntent);
        } else {
            ACRA.getErrorReporter().handleSilentException(new Exception("Failed to send registration token after 8 tries"));
        }

    }

    @NonNull
    static PreferenceExponentialBackoff getRegisterCheckBackoff(Context context) {
        return new PreferenceExponentialBackoff(2 * 60 * 1000, 2, 8, REGISTER_CHECK_BACKOFF.getKey(), Preferences.getDefaultPreferences(context));
    }

    public static class RegisterCheckProcessor extends ProcessorDistributor.Processor<Intent> {
        @Override
        public String getAction() {
            return "alarm.register_check";
        }

        @Override
        public void process(Intent object) {
            System.out.println("Checking if registered...");
            boolean tokenSent = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, getContext()).getBoolean(Preferences.Key.TOKEN_SENT, false);
            System.out.println("tokenSent = " + tokenSent);
            if (!tokenSent) {
                InstanceIdListenerService.sendRegistrationToServer(getContext());
            }
        }
    }
}
