package com.eissler.micha.hbgvertretungsapp.fcm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;

import com.eissler.micha.hbgvertretungsapp.AlarmReceiver;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.ConnectivityChangeReceiver;
import com.eissler.micha.hbgvertretungsapp.ExponentialBackoff;
import com.eissler.micha.hbgvertretungsapp.Preferences;
import com.eissler.micha.hbgvertretungsapp.ProcessorDistributor;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.peracutor.hbgbackend.registration.Registration;

import org.acra.ACRA;
import org.acra.util.Installation;

import java.io.IOException;

public class InstanceIdListenerService extends FirebaseInstanceIdService {

    private static Registration sRegistration;


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
        new RegistrationTask().execute(token, Installation.id(context));
    }

    private static void setRegisterCheckAlarm(Context context) {
        System.out.println("InstanceIdListenerService.setRegisterCheckAlarm");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("alarm.register_check");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 3, intent, 0);

        RegisterCheckBackoff backoff = new RegisterCheckBackoff(context);
        if (backoff.retry()) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + backoff.getValue(), pendingIntent);
        } else {
            ACRA.getErrorReporter().handleSilentException(new Exception("Failed to send registration token after 8 tries"));
        }

    }

    private static class RegistrationTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            if (sRegistration == null) {
                sRegistration = AppEngine.getApiInstance(new Registration.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null));
            }


            try {
                String token = strings[0];
                String acraID = strings[1];
                System.out.println("token = " + token);
                System.out.println("acraID = " + acraID);
                sRegistration.registerDevice(token, acraID).execute();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String errorMessage) {
//            Toast.makeText(InstanceIdListenerService.this, s, Toast.LENGTH_SHORT).show();
            if (errorMessage != null) {
                System.out.println("Registration-error: " + errorMessage); //will retry when alarm is launched
            } else {
                System.out.println("FCM token sent to server");
            }
        }
    }

    static class RegisterCheckBackoff extends ExponentialBackoff {
        public RegisterCheckBackoff(Context context) {
            super(context);
        }

        @Override
        protected Preferences.Key getPreferenceName() {
            return Preferences.Key.REGISTER_CHECK_BACKOFF;
        }

        @Override
        protected long getStartValue() {
            return 2 * 60 * 1000;
        }

        @Override
        protected int getMaxRetries() {
            return 8;
        }
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
