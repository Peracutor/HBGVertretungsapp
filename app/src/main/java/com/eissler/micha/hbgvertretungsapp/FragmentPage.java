package com.eissler.micha.hbgvertretungsapp;

import android.app.Activity;
import android.content.Context;
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
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.HeaderMessage;
import com.peracutor.hbgserverapi.ReplacedCoverMessage;
import com.peracutor.hbgserverapi.ResultCallback;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tr.xip.errorview.ErrorView;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class FragmentPage extends Fragment {

    static final String THIS_WEEK = "Diese Woche";

    private RecyclerView recyclerView;
    private ViewGroup view;
    private SortedCoverMessages mSortedCoverMessages;
//    private boolean noData = false;
    private Integer position;
    private int weekNumber;
    private ProgressBar progressBar;
//    private int initialized = 0;
//    private Event.LoadPermission waitingForInit;
    private ActionModeHelper actionModeHelper;
    private FastItemAdapter<IItem> fastAdapter;
    private ErrorView errorView;
    private boolean loadingOrLoaded = false;
//    private Menu actionMenu;
    private App.WaitFor<Activity> onAttach;


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
            fastAdapter.withSelectable(true);
            fastAdapter.withSelectOnLongClick(true);
            fastAdapter.withMultiSelect(true);
//            fastAdapter.withPositionBasedStateManagement(true);

            actionModeHelper = new ActionModeHelper(fastAdapter, R.menu.menu_cab, new ActionBarCallback());

            //Called prior to selection, if returning true, click is consumed (not
            fastAdapter.withOnPreClickListener((v, adapter, item, position) -> {
                if (item instanceof DateHeaderItem) {
                    return true;
                }
                App.logUserInput("List-Item at position " + position + " was clicked");

                System.out.println("item.isSelected() = " + item.isSelected());

                if (getActionMode() != null) {
                    Boolean onClick = actionModeHelper.onClick(item);

                    if (onClick != null && !onClick) {
                        println("finishing ActionMode");
                        return true;
                    }
//                    boolean enabled = getActionMenu().getItem(1).isEnabled();
//                    if (cannotApplyHideOptionOn((CoverMessageItem) item)) {
//                        return true;
//                    }
                }
                return false;
            });
//            fastAdapter.withSelectWithItemUpdate(true);

            fastAdapter.withOnClickListener((v, adapter, item, position) -> {
                if (getActionMode() != null) {
                    updateCab();
                    return true;
                }
                CoverMessageItem coverMessageItem = (CoverMessageItem) item;

                ReplacedCoverMessage coverMessage = coverMessageItem.getCoverMessage();


                String originalSubject = coverMessage.getOriginal(CoverMessage.SUBJECT);
                App.logTrace("subject = " + originalSubject);

                String originalNewSubject = coverMessage.getOriginal(CoverMessage.NEW_SUBJECT);
                App.logTrace("newSubject = " + originalNewSubject);

                if (originalSubject.equals("")) {
                    originalSubject = originalNewSubject;
                }

                if (!originalSubject.equals("") && !originalNewSubject.equals("") && !originalSubject.equals(originalNewSubject)) {
                    App.logTrace("Building OptionDialog");
                    final CharSequence[] subjects = new CharSequence[]{coverMessage.get(CoverMessage.SUBJECT), coverMessage.get(CoverMessage.NEW_SUBJECT)};
                    final CharSequence[] origSubjects = new CharSequence[]{originalSubject, originalNewSubject};

                    new OptionDialogBuilder(getActivity(), subjects, origSubjects).getOptionDialog().show();
                } else {
                    try {
                        new FilterDialog(originalSubject, coverMessage.get(CoverMessage.SUBJECT), getActivity(), this::resetPage).show(); // FIXME: 25.03.2016 causes BadTokenException sometimes
                    } catch (Exception e) {
                        App.logError("FilterDialog konnte nicht angezeigt werden");
                        App.reportUnexpectedException(e);
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Ein unerwarteter Fehler ist aufgetreten. Aktualisiere und versuche es erneut", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            });

            fastAdapter.withOnPreLongClickListener((v, adapter, item, position) -> actionModeHelper.onLongClick((AppCompatActivity) getActivity(), position) == null);

            fastAdapter.withOnLongClickListener((v, adapter, item, position1) -> {
                App.logUserInput("List-Item at position " + position1 + " was long-clicked");
                updateCab();
                return true;
            });
        }
    }

    private void updateCab() {
        getActionMode().setTitle(String.valueOf(fastAdapter.getSelectedItems().size()));

//        MenuItem hideOption = getActionMenu().getItem(1);
//        if (item.isSelected() && cannotApplyHideOptionOn((CoverMessageItem) item)) {
//            hideOption.setEnabled(false);
//        } else if (!hideOption.isEnabled() && !item.isSelected() && cannotApplyHideOptionOn((CoverMessageItem) item)) {
//            boolean containsInapplicableItems = false;
//            for (IItem item1 : fastAdapter.getSelectedItems()) {
//                println("item.getIdentifier() = " + item1.getIdentifier());
//                if (cannotApplyHideOptionOn(((CoverMessageItem) item1))) {
//                    containsInapplicableItems = true;
//                    break;
//                }
//            }
//            hideOption.setEnabled(!containsInapplicableItems);
//        }
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
        if (getActivity() == null) {
            onAttach = activity -> EventBus.getDefault().post(new Event.LoadPermissionRequest(position));
        } else {
            onAttach = null;
            EventBus.getDefault().post(new Event.LoadPermissionRequest(position));
        }

    }

    @Override
    public void onAttach(Context context) {
        System.out.println("FragmentPage.onAttach");
        super.onAttach(context);
        if (onAttach != null) {
            onAttach.onResult((Activity) context);
            onAttach = null;
        }
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

//        if (!isInitializedAndAttached()) { // TODO: 16.12.2016 check if working as intended
//            println("Not initialized");
//            waitingForInit = loadPermission;
//            return;
//        }

        if (loadingOrLoaded) {
            println("ALREADY LOADING OR ALREADY LOADED");
            return;
        }
//        if (!noData) {
            println("Starting download");
//            noData = false;
            fastAdapter.set(loadingHeaderAsList());
            loadingOrLoaded = true;
            new HbgDataDownload(App.getSelectedClass(getContext()), weekNumber, new DownloadHandler(getContext(), progressBar)).executeAsync(new ResultCallback<SortedCoverMessages>() {
                @Override
                public void onResult(SortedCoverMessages sortedCoverMessages) {
                    setRefreshing(false); // TODO: 18.12.2016 activity is still null sometimes
                    if (sortedCoverMessages == null) {
                        fastAdapter.set(noDataHeaderAsList());
//                        noData = true;
                    } else {
                        resetRecyclerView(sortedCoverMessages);
                        mSortedCoverMessages = sortedCoverMessages;
                    }
                }

                @Override
                public void onError(Throwable t) {
//                if (t instanceof FileNotFoundException) {
//                      //no data for this week
//                }
                    setRefreshing(false);
                    println("Error: " + t.getMessage());
                    showError(t);
                }
            });
//        }

    }

//    private boolean isInitializedAndAttached() {
//        return initialized == 2;
//    }

    private void showError(Throwable t) {
        progressBar.setProgress(100);
        errorView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        errorView.setSubtitle(t.getMessage());
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
//        println("FragmentPage.resetPage, position : " + position);
//        int index = recyclerView.getFirstVisiblePosition();
//        View v = recyclerView.getChildAt(0);
//        int top = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());

        resetRecyclerView(mSortedCoverMessages);


//        recyclerView.setSelectionFromTop(index, top);

    }

    public void resetRecyclerView(SortedCoverMessages sortedCoverMessages) {
        fastAdapter.set(toRecyclerViewItems(sortedCoverMessages));
    }

    private List<IItem> toRecyclerViewItems(SortedCoverMessages sortedCoverMessages) {
        ArrayList<HBGMessage> messages = sortedCoverMessages.getListItems(App.getCoverMessageFilter(getContext()), App.getReplacer(getContext()));
        if (messages.size() == 0) {
            messages.add(new HeaderMessage("Keine Vertretungsdaten"));
        }

        List<IItem> items = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            HBGMessage message = messages.get(i);
//            System.out.print("i = " + i);
            if (message instanceof CoverMessage) {
//                println(" ,  CoverMessage");
                ReplacedCoverMessage coverMessage = (ReplacedCoverMessage) message;
                items.add(new CoverMessageItem(coverMessage).withIdentifier(i));
            } else if (message instanceof HeaderMessage) {
//                println(" ,  HeaderMessage");
                items.add(new DateHeaderItem((HeaderMessage) message).withIdentifier(i));
            }
        }

        return items;
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
        getActivity().runOnUiThread(() -> ((MainActivity) getActivity()).waitForSwipeRefreshLayout(swipeRefreshLayout -> swipeRefreshLayout.setRefreshing(refreshing)));
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
        return actionModeHelper != null ? actionModeHelper.getActionMode() : null;
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
//            setActionMenu(menu);
            menu.getItem(1).setVisible(!Whitelist.isWhitelistModeActive(FragmentPage.this.getContext()));
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_delete:
//                    final boolean whitelistModeActive = Whitelist.isWhitelistModeActive(getActivity());
                    App.dialog("Bestätigen", "Sollen Meldungen zu den ausgewählten Fächern nicht mehr angezeigt werden?"/* + (whitelistModeActive ? "\n\nAchtung, der Whitelist-Modus ist aktiv!" : "")*/, getActivity())
                            .setPositiveButton("Ja", (dialog, which) -> {
                                println("Not showing following subjects anymore:");

//                                final Whitelist whiteList;
//                                whiteList = Whitelist.get(getContext());
                                final Blacklist blacklist = Blacklist.get(getContext());

                                for (IItem item1 : fastAdapter.getSelectedItems()) {
                                    ReplacedCoverMessage coverMessage = ((CoverMessageItem) item1).getCoverMessage();

                                    String subject = coverMessage.getOriginal(CoverMessage.SUBJECT);
                                    println("subject = " + subject);

                                    String newSubject = coverMessage.getOriginal(CoverMessage.NEW_SUBJECT);
                                    println("newSubject = " + newSubject);



                                    if (subject.equals("") && newSubject.equals("")) {
                                        App.dialog("Kein Fach enthalten", "In \"" + coverMessage.toString() + "\" ist kein Fach enthalten, das nicht mehr angezeigt werden könnte.", getActivity()).show();
                                        continue;
                                    } else if (!subject.equals("") && !newSubject.equals("") && !subject.equals(newSubject) &&
                                            !(blacklist.contains(coverMessage.get(CoverMessage.SUBJECT)) || blacklist.contains(coverMessage.get(CoverMessage.NEW_SUBJECT)))) {

                                        App.dialog("Mehrere Fächer enthalten", "In \"" + coverMessage.toString() + "\" sind zwei Fächer enthalten.\n\nMeldungen zu beiden nicht mehr anzeigen?\nWähle \"nein\", um nur eins auszuwählen.", getActivity())
                                                .setPositiveButton("Ja", (dialogInterface, i) -> {
                                                    blacklist.add(subject);
                                                    blacklist.add(newSubject);
                                                    blacklist.save();
                                                    resetPage();
                                                })
                                                .setNeutralButton("Überspringen", null)
                                                .setNegativeButton("Nein", (dialogInterface, i) -> {
                                                    AlertDialog optionDialog = new OptionDialogBuilder(getActivity(), new CharSequence[]{coverMessage.get(CoverMessage.SUBJECT), coverMessage.get(CoverMessage.NEW_SUBJECT)},
                                                            new CharSequence[]{subject, newSubject}).getOptionDialog();
                                                    optionDialog.setTitle("Welches nicht mehr anzeigen?");
                                                    optionDialog.show();
                                                })
                                                .show();

                                        continue;
                                    }



//                                    if (whitelistModeActive) {
//                                        for (int j = 0; j < whiteList.size(); j++) {
//                                            String whitelistedSubject = whiteList.get(j);
//                                            println("whitelistedSubject = " + whitelistedSubject);
//                                            if (whitelistedSubject.equals(originalSubject)) {
//                                                whiteList.remove(j);
//                                                break;
//                                            }
//                                        }
//                                    } else {
//                                    }
                                    blacklist.add(!subject.equals("") ? subject : newSubject);
                                }

//                                if (whitelistModeActive) {
//                                    whiteList.save();
//                                } else {
//                                }
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
                        CoverMessage coverMessage = ((CoverMessageItem) item2).getCoverMessage();
                        sortedCoverMessages.insert(coverMessage);
                    }
                    ArrayList<HBGMessage> listItems = sortedCoverMessages.getListItems(null, null);
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
//            setActionMenu(null);
        }

    }

}
