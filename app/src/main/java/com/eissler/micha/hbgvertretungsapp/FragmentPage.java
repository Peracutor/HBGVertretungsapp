package com.eissler.micha.hbgvertretungsapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.eissler.micha.hbgvertretungsapp.evaluation.CoverMessageItem;
import com.eissler.micha.hbgvertretungsapp.evaluation.DateHeaderItem;
import com.eissler.micha.hbgvertretungsapp.evaluation.DownloadHandler;
import com.eissler.micha.hbgvertretungsapp.settings.Blacklist;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.eissler.micha.hbgvertretungsapp.util.MultiSelectHelper;
import com.google.firebase.crash.FirebaseCrash;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.HeaderMessage;
import com.peracutor.hbgserverapi.ReplacedCoverMessage;
import com.peracutor.hbgserverapi.ResultCallback;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tr.xip.errorview.ErrorView;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class FragmentPage extends Fragment {

    private RecyclerView recyclerView;
    private ViewGroup view;
    private SortedCoverMessages mSortedCoverMessages;
    private Integer position;
    private int weekNumber;
    private ProgressBar progressBar;
//    private int initialized = 0;
//    private Event.LoadPermission waitingForInit;
    private FastItemAdapter<IItem> fastAdapter;
    private ErrorView errorView;
    private boolean loadingOrLoaded = false;
    private MultiSelectHelper<IItem, FastItemAdapter<IItem>> multiSelectHelper;
    private Throwable pendingError;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(FragmentPage.this);
        position = getArguments().getInt("position");
        weekNumber = getArguments().getInt("weekNumber");
        println("FragmentPage.onCreate");

        if (fastAdapter == null) {
            fastAdapter = new FastItemAdapter<>();
            fastAdapter.setHasStableIds(true);
//            fastAdapter.withPositionBasedStateManagement(true);

            multiSelectHelper = new MultiSelectHelper<>(fastAdapter);
            fastAdapter = multiSelectHelper.setupMultiSelectAdapter((AppCompatActivity) getActivity(), R.menu.menu_cab, new ActionBarCallback(), null, (v, adapter, item, position) -> {
                if (item instanceof DateHeaderItem) {
                    return true;
                }
                CoverMessageItem coverMessageItem = (CoverMessageItem) item;

                ReplacedCoverMessage coverMessage = (ReplacedCoverMessage) coverMessageItem.getCoverMessage();

                String originalSubject = coverMessage.getOriginal(CoverMessage.SUBJECT);
                App.logTrace("subject = " + originalSubject);

                String originalNewSubject = coverMessage.getOriginal(CoverMessage.NEW_SUBJECT);
                App.logTrace("newSubject = " + originalNewSubject);

                if (!originalSubject.equals("") && !originalNewSubject.equals("") && !originalSubject.equals(originalNewSubject)) {
                    App.logTrace("Building OptionDialog");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle("Welches Fach umbenennen?");
                    final CharSequence[] subjects = new CharSequence[]{coverMessage.get(CoverMessage.SUBJECT), coverMessage.get(CoverMessage.NEW_SUBJECT)};
                    builder.setSingleChoiceItems(subjects, -1, (dialog, which) -> {
                        new FilterDialog(which == 0 ? originalSubject : originalNewSubject, subjects[which].toString(), getActivity(), this::resetPage).show();
                        dialog.dismiss();
                    });
                    builder.show();
                } else {
                    final String concernedSubject;

                    if (!originalSubject.equals("")) {
                        concernedSubject = originalSubject;
                    } else {
                        if (originalNewSubject.equals("")) return false;
                        concernedSubject = originalNewSubject;
                    }
                    try {
                        new FilterDialog(concernedSubject, coverMessage.get(CoverMessage.SUBJECT), getActivity(), this::resetPage).show(); // FIXME: 25.03.2016 causes BadTokenException sometimes
                    } catch (Exception e) {
                        App.logError("FilterDialog konnte nicht angezeigt werden");
                        App.report(e);
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Ein unerwarteter Fehler ist aufgetreten. Aktualisiere und versuche es erneut", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }, (MultiSelectHelper.OnClickView[]) null);

        }
    }

    private void getMainActivity(App.WaitFor<MainActivity> waitFor) {
        EventBus.getDefault().post(new Event.WaitForMainActivity(waitFor));
    }


    @Override
    public void onDestroy() {
        println("FragmentPage.onDestroy");
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        println("FragmentPage.onCreateView");


        if (view == null) {
            view = (ViewGroup) inflater.inflate(
                    R.layout.fragment_page_layout, container, false);

            errorView = (ErrorView) view.findViewById(R.id.error_view);
            errorView.setOnRetryListener(() -> EventBus.getDefault().post(new Event.RefreshRequest()));

            progressBar = (ProgressBar) view.findViewById(R.id.progress);
            ViewCompat.setElevation(progressBar, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getContext().getResources().getDisplayMetrics()));

            recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == SCROLL_STATE_IDLE) {
                        onCanScrollUpRequested(null);
                    }
                }
            });
            recyclerView.setAdapter(fastAdapter);
//            initialized++;

            if (pendingError != null) {
                showError(pendingError);
                pendingError = null;
            }
        }

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        println("FragmentPage.onActivityCreated");
        super.onActivityCreated(savedInstanceState);
//        initialized++;
//        if (isInitializedAndAttached() && waitingForInit != null) {
//            EventBus.getDefault().post(waitingForInit);
//            waitingForInit = null;
//        }

//        if (noData) {
//            println("NO DATA");
//            fastAdapter.clear();
//            fastAdapter.set(noDataHeaderAsList());
//        } else if (mSortedCoverMessages != null) {
//            println("ALREADY DATA");
//            resetPage();
//        }
//        else if (isInitializedAndAttached() && waitingForInit != null) {
//            EventBus.getDefault().post(waitingForInit);
//            waitingForInit = null;
//        } else if (isInitializedAndAttached()){
//            fastAdapter.clear();
//            println("Request Permission");
//            EventBus.getDefault().post(new Event.LoadPermissionRequest(position));
//        }
        EventBus.getDefault().post(new Event.LoadPermissionRequest(position));
    }

    private void println(String s) {
        System.out.println("Page " + position + ": " + s);
    }


    @Subscribe
    public void onLoadPermitted(Event.LoadPermission loadPermission) {
        if (!loadPermission.isPermitted(position)) {
            return;
        }
        println("FragmentPage.onLoadPermitted");

        if (loadingOrLoaded) {
            println("ALREADY LOADING OR ALREADY LOADED");
            return;
        }

        println("Starting download");
        fastAdapter.set(loadingHeaderAsList());
        loadingOrLoaded = true;
        ACRA.getErrorReporter().putCustomData("Week", String.valueOf(weekNumber));
        FirebaseCrash.log("Loading week " + weekNumber);
        new HbgDataDownload(App.getSelectedClass(getContext()), weekNumber, new DownloadHandler(getContext(), progressBar)).executeAsync(new ResultCallback<SortedCoverMessages>() {
            @Override
            public void onResult(SortedCoverMessages sortedCoverMessages) {
                try {
                    setRefreshing(false);
                    if (sortedCoverMessages == null) {
                        fastAdapter.set(noDataHeaderAsList());
                    } else {
                        mSortedCoverMessages = sortedCoverMessages;
                        resetPage();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    App.report(e);
                    loadingOrLoaded = false;
                    onLoadPermitted(loadPermission);
                }
            }

            @Override
            public void onError(Throwable t) {
//                if (t instanceof FileNotFoundException) {
//                      //no data for this week
//                }
                try {
                    setRefreshing(false);
                    println("Error: " + t.getMessage());
                    showError(t);
                } catch (Exception e) {
                    e.printStackTrace();
                    App.report(e);
                    loadingOrLoaded = false;
                    showError(new Exception("Unbekannter Fehler", e));
                }
            }
        });
    }

    private void showError(Throwable t) {
        if (view != null) {
            progressBar.setProgress(100);
            errorView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            errorView.setSubtitle(t.getMessage());
        } else {
            pendingError = t;
        }
    }


    @Subscribe
    public void onCanScrollUpRequested(Event.CanScrollUpRequest request) {
        if (request != null && request.forPosition() != position) {
            return;
        }
        boolean canScrollUp = true;

        recyclerView.canScrollVertically(1);
        if (recyclerView != null) {
            canScrollUp = canScrollUp(recyclerView);
        }

        EventBus.getDefault().post(new Event.CanScrollUp(canScrollUp));
    }


    @Subscribe
    public void onResetRequested(Event.ResetRequest resetRequest) {
        resetPage();
    }

    public void resetPage() {
        if (mSortedCoverMessages == null) {
            return;
        }

        getMainActivity(mainActivity -> {
            SortedCoverMessages sortedCoverMessages = new SortedCoverMessages(mSortedCoverMessages, App.getCoverMessageFilter(mainActivity), App.getReplacer(mainActivity));
            ArrayList<HBGMessage> messages = sortedCoverMessages.getListItems();

            if (messages.size() == 0) {
                messages.add(new HeaderMessage("Keine Vertretungsdaten"));
            }

            List<IItem> items = new ArrayList<>(messages.size());
            for (int i = 0; i < messages.size(); i++) {
                HBGMessage message = messages.get(i);
                if (message instanceof CoverMessage) {
                    items.add(new CoverMessageItem((CoverMessage) message).withIdentifier(i));
                } else if (message instanceof HeaderMessage) {
                    items.add(new DateHeaderItem((HeaderMessage) message).withIdentifier(i));
                }
            }

            fastAdapter.set(items);
        });
    }

    private static boolean canScrollUp(RecyclerView recyclerView) {
        return recyclerView.canScrollVertically(-1);
    }

    @Subscribe
    public void finishActionMode(Event.FinishActionMode finishActionMode) {
        if (getActionMode() != null) {
            getActionMode().finish();
        }
    }

    private void setRefreshing(final boolean refreshing) {
        EventBus.getDefault().post(new Event.RefreshStatus(refreshing));
    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof FragmentActivity) {
//            println("Fragment was attached to Activity");
//            initialized++;
//        } else {
//            println("Attached context is not a Activity");
//        }
//
//        if (isInitializedAndAttached() && waitingForInit != null) {
//            EventBus.getDefault().post(waitingForInit);
//            waitingForInit = null;
//        }
//    }

    private List<IItem> noDataHeaderAsList() {
        return Collections.singletonList(new DateHeaderItem(new HeaderMessage("Keine Vertretungsdaten")));
    }

    private List<IItem> loadingHeaderAsList() {
        return Collections.singletonList(new DateHeaderItem(new HeaderMessage("Lade Vertretungsdaten...")));
    }

    public ActionMode getActionMode() {
        return multiSelectHelper != null ? multiSelectHelper.getActionMode() : null;
    }

//    public void setActionMenu(Menu actionMenu) {
//        this.actionMenu = actionMenu;
//    }

//    public Menu getActionMenu() {
//        return actionMenu;
//    }

    //    static void takeScreenshot(View view, Context context) {
////        Date now = new Date();
////        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
//
//        try {
////            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";
//
////            view = view.getRootView();
//            // create bitmap screen capture
//            view.setDrawingCacheEnabled(true);
//            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
//            view.setDrawingCacheEnabled(false);
//
//
//
//            //noinspection deprecation
//            File imageFile = new File(context.getFilesDir(), "image.jpg");
//            println("imageFile.delete() = " + imageFile.delete());
//
//            //noinspection deprecation
//            FileOutputStream outputStream = context.openFileOutput("image.jpg", Context.MODE_WORLD_READABLE);//new FileOutputStream(imageFile);//
//            int quality = 100;
//            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
//            outputStream.flush();
//            outputStream.close();
//
//            println("imageFile exists? : " + imageFile.exists());
//            for (File file :
//                    context.getFilesDir().listFiles()) {
//                println("file = " + file);
//            }
//
//            openScreenshot(imageFile, context);
//        } catch (Throwable e) {
//            // Several error may come out with file handling or OOM
//            e.printStackTrace();
//        }
//    }
//
//    private static void openScreenshot(File imageFile, Context context) {
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_VIEW);
//        Uri uri = Uri.fromFile(imageFile);
//        intent.setDataAndType(uri, "image/*");
//        context.startActivity(Intent.createChooser(intent, "Wähle eine App aus"));
//    }



    private class ActionBarCallback implements android.support.v7.view.ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.getItem(1).setVisible(!Whitelist.isWhitelistModeActive(FragmentPage.this.getContext()));
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_delete:
                    App.dialog("Bestätigen", "Sollen Meldungen zu den ausgewählten Fächern nicht mehr angezeigt werden?", getActivity())
                            .setPositiveButton("Ja", (dialog1, which1) -> {

                                final Blacklist blacklist = Blacklist.get(getContext());

                                for (IItem item1 : fastAdapter.getSelectedItems()) {
                                    ReplacedCoverMessage coverMessage = (ReplacedCoverMessage) ((CoverMessageItem) item1).getCoverMessage();

                                    String subject = coverMessage.getOriginal(CoverMessage.SUBJECT);
                                    String newSubject = coverMessage.getOriginal(CoverMessage.NEW_SUBJECT);

                                    if (subject.equals("") && newSubject.equals("")) {
                                        App.dialog("Kein Fach enthalten", "In \"" + coverMessage.toString() + "\" ist kein Fach enthalten, das nicht mehr angezeigt werden könnte.", getActivity()).show();
                                    } else if (!subject.equals("") && !newSubject.equals("") && !subject.equals(newSubject) &&
                                            !(blacklist.contains(subject) || blacklist.contains(newSubject))) {

                                        App.dialog("Mehrere Fächer enthalten", "In \"" + coverMessage.toString() + "\" sind zwei Fächer enthalten.\n\n" +
                                                "Zu beiden keine Meldungen mehr anzeigen?\nWähle \"nein\", um nur ein Fach auszuwählen.", getActivity())
                                                .setPositiveButton("Ja", (dialogInterface, i) -> {
                                                    blacklist.add(subject);
                                                    blacklist.add(newSubject);
                                                    blacklist.save();
                                                    resetPage();
                                                })
                                                .setNegativeButton("Nein", (dialogInterface, i) -> {
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle("Welches nicht mehr anzeigen?");

                                                    builder.setSingleChoiceItems(new CharSequence[]{coverMessage.get(CoverMessage.SUBJECT), coverMessage.get(CoverMessage.NEW_SUBJECT)}, -1, (dialog, which) -> {
                                                        blacklist.add(which == 0 ? subject : newSubject);
                                                        blacklist.save();
                                                        resetPage();
                                                        dialog.dismiss();
                                                    });
                                                    builder.show();
                                                })
                                                .setNeutralButton("Überspringen", null)
                                                .show();
                                    } else {
                                        blacklist.add(!subject.equals("") ? subject : newSubject);
                                    }
                                }

                                blacklist.save();

                                resetPage();
                                mode.finish();
                            })
                            .setNegativeButton("Abbrechen", (dialog, which) -> mode.finish())
                            .show();
                    return true;
                case R.id.item_share:
                    StringBuilder sb = new StringBuilder();
                    SortedCoverMessages sortedCoverMessages = new SortedCoverMessages(fastAdapter.getSelectedItems().size());
                    for (IItem item2 : fastAdapter.getSelectedItems()) {
                        ReplacedCoverMessage coverMessage = (ReplacedCoverMessage) ((CoverMessageItem) item2).getCoverMessage();
                        coverMessage.setReplaced(CoverMessage.SUBJECT, null);
                        coverMessage.setReplaced(CoverMessage.NEW_SUBJECT, null);
                        sortedCoverMessages.insert(coverMessage);
                    }
                    ArrayList<HBGMessage> listItems = sortedCoverMessages.getListItems();
                    String bullet = " \u2022 ";
                    for (HBGMessage message : listItems) {
                        if (message instanceof CoverMessage) {
                            sb.append(bullet);
                        }
                        sb.append(message.toString());
                        if (message instanceof HeaderMessage) {
                            sb.append(":");
                        }
                        sb.append('\n');
                    }


                    sb.append("\n")
                            .append(HbgDataDownload.makeURL(weekNumber, App.getSelectedClass(getContext())));

                    String shareMessage = sb.toString();
                    Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "HBG-Vertretungsmeldungen");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);

                    startActivity(Intent.createChooser(shareIntent, "Teilen"));
                    return true;
                default:
                    return false;
            }
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

    }

}
