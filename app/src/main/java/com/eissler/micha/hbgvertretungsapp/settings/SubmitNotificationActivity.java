package com.eissler.micha.hbgvertretungsapp.settings;

import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eissler.micha.cloudmessaginglibrary.InfoNotification;
import com.eissler.micha.hbgvertretungsapp.App;
import com.eissler.micha.hbgvertretungsapp.MainActivity;
import com.eissler.micha.hbgvertretungsapp.R;
import com.eissler.micha.hbgvertretungsapp.RequestCodes;
import com.eissler.micha.hbgvertretungsapp.fcm.AppEngine;
import com.eissler.micha.hbgvertretungsapp.util.InputValidator;
import com.eissler.micha.hbgvertretungsapp.util.Preferences;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.firebase.iid.FirebaseInstanceId;
import com.peracutor.hbgbackend.messaging.Messaging;
import com.peracutor.hbgbackend.messaging.model.JsonMap;
import com.peracutor.hbgbackend.messaging.model.Recipients;
import com.peracutor.hbgbackend.messaging.model.SubmittedNotification;
import com.peracutor.hbgbackend.messaging.model.TimeToLive;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SubmitNotificationActivity extends AppCompatActivity {


    @BindView(R.id.activity_submit_notification)
    LinearLayout layout;

    @BindView(R.id.text_view_receiver)
    TextView receiverText;

    @BindView(R.id.img_view_edit)
    ImageView editImage;

    @BindView(R.id.card_view_receiver)
    CardView receiverCard;

    @BindView(R.id.edit_text_title)
    EditText title;

    @BindView(R.id.edit_text_content)
    EditText content;

    @BindView(R.id.edit_text_img_url)
    EditText imageUrl;

    @BindView(R.id.button_preview)
    Button previewButton;

    @BindView(R.id.text_view_end_date)
    TextView endDateText;

    @BindView(R.id.img_clear)
    ImageView clearImage;

    @BindView(R.id.card_view_end_date)
    CardView endDateCard;

    @BindView(R.id.button_submit)
    Button submitButton;

    private ArrayList<Integer> receivers = new ArrayList<>();
    private Calendar endDateCalendar;
    private ArrayList<String> classList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_notification);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            if (savedInstanceState.get("classList") != null) {
                classList = savedInstanceState.getStringArrayList("classList");
            }
            if (savedInstanceState.get("receivers") != null) {
                receivers = savedInstanceState.getIntegerArrayList("receivers");
                updateReceiverText();
            }
            if (savedInstanceState.get("endDate") != null) {
                endDateText.setText(savedInstanceState.getCharSequence("endDate"));
            }
        } else {
            title.setError("");
            content.setError("");
        }

        if (classList == null) {
            classList = MainActivity.loadClassList(SubmitNotificationActivity.this);
            if (classList != null) {
                classList.set(0, "Alle auswählen");
                classList.remove(classList.size() - 1);
            }
        }

        InputValidator.NotEmptyValidator titleValidator = new InputValidator.NotEmptyValidator(title);
        InputValidator.NotEmptyValidator contentValidator = new InputValidator.NotEmptyValidator(content);
        InputValidator.EditTextValidator urlValidator = new InputValidator.EditTextValidator(imageUrl) {
            private static final String ERROR_MESSAGE = "Ungültige URL";

            @Override
            public CharSequence validate(CharSequence text) {
                String input = text.toString().trim();
                if (input.equals("")) return null;
                return Patterns.WEB_URL.matcher(input).matches() ? null : ERROR_MESSAGE;
            }
        };

        InputValidator.DisablerValidatorGroup previewValidatorGroup = new InputValidator.DisablerValidatorGroup(previewButton);
        previewValidatorGroup.add(titleValidator);
        previewValidatorGroup.add(contentValidator);
        previewValidatorGroup.add(urlValidator);


        final InputValidator.EditTextValidator receiverValidator = new InputValidator.EditTextValidator(receiverText) {
            private static final String ERROR_MESSAGE = "Keine Empfänger ausgewählt";

            @Override
            public CharSequence validate(CharSequence text) {
                return receivers.size() == 0 ? ERROR_MESSAGE : null;
            }
        };
        receiverValidator.afterTextChanged(null);

        InputValidator.ValidatorGroup validatorGroup = new InputValidator.DisablerValidatorGroup(submitButton);
        validatorGroup.add(previewValidatorGroup);
        validatorGroup.add(receiverValidator);

        title.addTextChangedListener(titleValidator);
        content.addTextChangedListener(contentValidator);
        imageUrl.addTextChangedListener(urlValidator);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            private boolean allCheckboxSelected = false;

            @Override
            public void onClick(View view) {
                if (classList /*still*/ == null) {
                    App.showErrorDialog("Die Liste der Klassen konnte nicht geladen werden.", SubmitNotificationActivity.this);
                    return;
                }

                new MaterialDialog.Builder(SubmitNotificationActivity.this)
                        .title("Wähle Klassen als Empfänger")
                        .items(classList)
                        .itemsCallbackMultiChoice(receivers.toArray(new Integer[0]), (dialog, which, text) -> {
                            if (which.length != 0) {
                                if (which[0] == 0 && !allCheckboxSelected) {
                                    allCheckboxSelected = true;
                                    dialog.selectAllIndices(false);
                                } else if (which[0] == 0) { //&& allCheckboxSelected
                                    allCheckboxSelected = false;
                                    Integer[] indices = new Integer[which.length - 1];
                                    System.arraycopy(which, 1, indices, 0, which.length - 1);
                                    dialog.setSelectedIndices(indices);
                                } else if (allCheckboxSelected) { //&& which[0] != 0
                                    allCheckboxSelected = false;
                                    dialog.clearSelectedIndices(false);
                                } else if (which.length == classList.size() - 1) { //&& which[0] != 0
                                    dialog.selectAllIndices(false);
                                }
                            }
                            return true;
                        })
                        .negativeText("Abbrechen")
                        .positiveText("Speichern")
                        .onPositive((dialog, which) -> {
                            //noinspection ConstantConditions
                            receivers = new ArrayList<>(Arrays.asList(dialog.getSelectedIndices()));

                            updateReceiverText();
                            receiverValidator.afterTextChanged(null);

                        })
                        .alwaysCallMultiChoiceCallback()
                        .show();
            }
        };
        receiverCard.setOnClickListener(onClickListener);
        editImage.setOnClickListener(onClickListener);

        endDateCalendar = Calendar.getInstance(Locale.GERMANY);
        endDateCalendar.add(Calendar.WEEK_OF_YEAR, 4);

        final TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY);

            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                endDateCalendar.set(Calendar.HOUR_OF_DAY, hour);
                endDateCalendar.set(Calendar.MINUTE, minute);
                endDateText.setText(dateFormat.format(endDateCalendar.getTime()));
            }
        };

        final TimePickerDialog timePicker = new TimePickerDialog(SubmitNotificationActivity.this, onTimeSetListener, endDateCalendar.get(Calendar.HOUR_OF_DAY), endDateCalendar.get(Calendar.MINUTE), true);
        final DatePickerDialog.OnDateSetListener onDateSetListener = (datePicker1, year, month, day) -> {
            endDateCalendar.set(Calendar.YEAR, year);
            endDateCalendar.set(Calendar.MONTH, month);
            endDateCalendar.set(Calendar.DAY_OF_MONTH, day);
            timePicker.show();
        };

        final DatePickerDialog datePicker = new DatePickerDialog(SubmitNotificationActivity.this, onDateSetListener, endDateCalendar.get(Calendar.YEAR), endDateCalendar.get(Calendar.MONTH), endDateCalendar.get(Calendar.DAY_OF_MONTH));

        datePicker.getDatePicker().setMinDate(new Date().getTime() + 24 * 60 * 60 * 1000); //+ 1 day

        endDateCard.setOnClickListener(view -> datePicker.show());
        clearImage.setOnClickListener(view -> endDateText.setText("---"));
    }

    public void preview(View view) {
        InfoNotification notification = new InfoNotification.Builder()
                .setTitle(title.getText().toString())
                .setContent(content.getText().toString())
                .setImageUrl(imageUrl.getText().toString().trim())
                .build();

        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object[] params) {
                App.showNotification((InfoNotification) params[0], ((Context) params[1]));
                return null;
            }
        }.execute(notification, this);
    }

    public void submit(View view) {
        if (!App.isConnected(this)) {
            new MaterialDialog.Builder(this)
                    .title("Keine Internetverbindung")
                    .content("Es besteht keine Internetverbindung. Stelle eine Internetverbindung her und versuche es erneut.")
                    .positiveText("Ok")
                    .show();
            return;
        }
        new MaterialDialog.Builder(this)
                .title("Einreichen")
                .content("Die Benachrichtigung wird eingereicht und dann gesendet, sobald sie bestätigt wurde.\n\n(Eine Bestätigungsemail wird gesendet und von einem Lehrer oder Micha (Appentwickler) bestätigt.)")
                .negativeText("Abbrechen")
                .positiveText("Einreichen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    MaterialDialog progressDialog;
                    boolean background = false;

                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        new AppEngine.Task<Messaging>(new Messaging.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null), client -> {
                            com.eissler.micha.cloudmessaginglibrary.SubmittedNotification.Builder dataBuilder = new com.eissler.micha.cloudmessaginglibrary.SubmittedNotification.Builder();

                            dataBuilder.setTitle(title.getText().toString().trim())
                                    .setContent(content.getText().toString().trim());
                            String imgUrl = imageUrl.getText().toString().trim();
                            if (!imgUrl.equals("")) dataBuilder.setImageUrl(imgUrl);

                            com.eissler.micha.cloudmessaginglibrary.Recipients recipients = new com.eissler.micha.cloudmessaginglibrary.Recipients();
                            if (receivers.get(0) == 0 || SubmitNotificationActivity.this.allClassesSelected()) {
                                recipients.topic("global");
                            } else {
                                for (Integer classNumber : receivers) {
                                    recipients.topic(String.valueOf(classNumber));
                                }
                            }

                            SubmittedNotification notification = new SubmittedNotification();
                            JsonMap data = new JsonMap();
                            data.putAll(dataBuilder.build().getData());
                            notification.setData(data);
                            notification.setRecipients(new Recipients().setRecipients(recipients.getRecipients()));

                            if (!endDateText.getText().toString().equals("---")) try {
                                notification.setTimeToLive(new TimeToLive().setEndDate(new DateTime(new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY).parse(endDateText.getText().toString()))));
                            } catch (ParseException ignored) {
                            }

                            notification.setSenderToken(FirebaseInstanceId.getInstance().getToken());

                            MaterialDialog.Builder progressBuilder = new MaterialDialog.Builder(SubmitNotificationActivity.this)
                                    .title("Nachricht einreichen")
                                    .content("Nachricht wird gesendet...")
                                    .progress(true, 0)
                                    .cancelable(false)
                                    .negativeText("Hintergrund")
                                    .onNegative((dialog12, which12) -> {
                                        background = true;
                                        SubmitNotificationActivity.this.finish();
                                    });

                            CountDownLatch latch = new CountDownLatch(1);
                            SubmitNotificationActivity.this.runOnUiThread(() -> {
                                progressDialog = progressBuilder.show();
                                latch.countDown();
                            });

                            MaterialDialog.Builder builder = new MaterialDialog.Builder(SubmitNotificationActivity.this)
                                    .positiveText("Ok");

                            NotificationCompat.Builder notificationBuilder = App.getNotificationBuilder(SubmitNotificationActivity.this);

                            try {
                                client.messagingEndpoint().submitNotification(notification).execute();
                                String title = "Erfolgreich";
                                String content = "Die Nachricht wurde erfolgreich eingereicht.";
                                builder.title(title)
                                        .content(content)
                                        .onPositive((dialog1, which1) -> SubmitNotificationActivity.this.finish())
                                        .cancelListener(dialogInterface -> SubmitNotificationActivity.this.finish());

                                notificationBuilder.setContentTitle(title)
                                        .setContentText(content)
                                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content));
                            } catch (IOException e) {
                                String title = "Fehler";
                                String content;
                                if (e instanceof GoogleJsonResponseException && e.getMessage().contains("OverQuotaException")) {
                                    content = "Das tägliche Limit an Nachrichten, die eingereicht werden können, wurde erreicht.";
                                } else {
                                    content = "Die Nachricht konnte aufgrund eines Fehlers nicht gesendet werden. \n(\"" + e.getMessage() + "\")";
                                }

                                builder.title(title)
                                        .content(content);

                                notificationBuilder.setContentTitle(title)
                                        .setContentText(content)
                                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content));
                                App.report(e);
                            }

                            if (!background) {
                                runOnUiThread(() -> {
                                    builder.show();
                                    try {
                                        latch.await();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        App.report(e);
                                    }
                                    progressDialog.dismiss();
                                });
                            } else {
                                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                int id = App.getNextPushId(RequestCodes.NOTIFICATION_PUSH, Preferences.Key.LAST_NOTIFICATION_ID, SubmitNotificationActivity.this);
                                notificationManager.notify(id, notificationBuilder.build());
                            }
                        }).execute();
                    }
                })
                .show();
    }

    private boolean allClassesSelected() {
        for (int i = 1; i <= 22; i++) {
            if (!receivers.contains(i)) {
                return false;
            }
        }
        return true;
    }

    public void updateReceiverText() {
        int receiverCount = receivers.size();
        if (receiverCount == classList.size()) {
            receiverCount--;
        }
        receiverText.setText(receiverCount + " Klasse" + (receiverCount != 1 ? "n" : "") + " gewählt  ");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("classList", classList);
        outState.putIntegerArrayList("receivers", receivers);
        outState.putCharSequence("endDate", endDateText.getText());
    }
}
