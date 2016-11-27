package com.eissler.micha.hbgvertretungsapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
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
import com.eissler.micha.hbgvertretungsapp.evaluation.CustomNameReplacer;
import com.eissler.micha.hbgvertretungsapp.evaluation.DateHeaderItem;
import com.eissler.micha.hbgvertretungsapp.evaluation.HbgDownload;
import com.eissler.micha.hbgvertretungsapp.settings.CustomNames;
import com.eissler.micha.hbgvertretungsapp.settings.Whitelist;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;
import com.peracutor.hbgserverapi.CoverMessage;
import com.peracutor.hbgserverapi.HBGMessage;
import com.peracutor.hbgserverapi.HbgDataDownload;
import com.peracutor.hbgserverapi.HeaderMessage;
import com.peracutor.hbgserverapi.ResultCallback;
import com.peracutor.hbgserverapi.SortedCoverMessages;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import tr.xip.errorview.ErrorView;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class FragmentPage extends Fragment {

    static final String THIS_WEEK = "Diese Woche";

    private RecyclerView recyclerView;
    private ViewGroup view;
    private SortedCoverMessages mSortedCoverMessages;
    private boolean noData = false;
    private Integer position;
    private int weekNumber;
    private ProgressBar progressBar;
    private boolean initialized;
    private Event.LoadPermission waitingForInit;
    private ActionModeHelper actionModeHelper;
    private FastItemAdapter<IItem> fastAdapter;
    private ErrorView errorView;
    private boolean alreadyLoading = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(FragmentPage.this);
        position = getArguments().getInt("position");
        weekNumber = getArguments().getInt("weekNumber");
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        System.out.println("FragmentPage.onCreateView, position " + position);


        if (view == null) {
            view = (ViewGroup) inflater.inflate(
                    R.layout.fragment_page_layout, container, false);

            errorView = (ErrorView) view.findViewById(R.id.error_view);
            errorView.setOnRetryListener(new ErrorView.RetryListener() {
                @Override
                public void onRetry() {
                    EventBus.getDefault().post(new Event.RefreshRequest());
                }
            });

            progressBar = (ProgressBar) view.findViewById(R.id.progress);
            ViewCompat.setElevation(progressBar, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getContext().getResources().getDisplayMetrics()));

            fastAdapter = new FastItemAdapter<>();
            fastAdapter.setHasStableIds(true);
            fastAdapter.withSelectable(true);
            fastAdapter.withSelectOnLongClick(true);
            fastAdapter.withMultiSelect(true);
//            fastAdapter.withPositionBasedStateManagement(true);

            actionModeHelper = new ActionModeHelper(fastAdapter, R.menu.menu_cab, new ActionBarCallback());

            fastAdapter.withOnPreClickListener(new FastAdapter.OnClickListener<IItem>() {
                @Override
                public boolean onClick(View v, IAdapter<IItem> adapter, IItem item, int position) {
                    if (item instanceof DateHeaderItem) {
                        System.out.println("Wrong item");
                        return true;
                    }

                    Boolean onClick = actionModeHelper.onClick(item);
                    if (onClick != null && !onClick) {
                        System.out.println("finishing ActionMode");
                        return true;
                    }

                    if (getActionMode() != null) {
                        if (tooManySubjects((CoverMessageItem) item)) {
                            return true;
                        }
                    }
                    return false;
                }
            });

            fastAdapter.withOnClickListener(new FastAdapter.OnClickListener<IItem>() {
                @Override
                public boolean onClick(View v, IAdapter<IItem> adapter, IItem item, int position) {
                    App.logUserInput("List-Item at position " + position + " was clicked");
                    if (getActionMode() != null) {
                        updateCabTitle(getActionMode());
                        return true;
                    }
                    CoverMessage coverMessage = ((CoverMessageItem) item).getCoverMessage();

                    String subject = coverMessage.get(CoverMessage.SUBJECT);
                    App.logTrace("subject = " + subject);

                    String newSubject = coverMessage.get(CoverMessage.NEW_SUBJECT);
                    App.logTrace("newSubject = " + newSubject);

                    if (subject.equals("")) {
                        subject = newSubject;
                    }

                    if (!subject.equals("") && !newSubject.equals("") && !subject.equals(newSubject)) {
                        App.logTrace("Building OptionDialog");
                        final CharSequence[] subjects = new CharSequence[]{subject, newSubject};
                        new OptionDialogBuilder(getActivity(), subjects).getOptionDialog().show();
                    } else {
                        try {
                            new FilterDialog(subject,
                                    new FilterDialog.PostExecuteInterface() {
                                        @Override
                                        public void onPostExecute() {
                                            EventBus.getDefault().post(new Event.ResetRequest());
                                        }
                                    },
                                    getActivity()).show(); // FIXME: 25.03.2016 causes BadTokenException sometimes

                        } catch (Exception e) {
                            App.logError("FilterDialog konnte nicht angezeigt werden");
                            App.reportUnexpectedException(e);
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "Ein unerwarteter Fehler ist aufgetreten. Aktualisiere und versuche es erneut", Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true;
                }
            });

            fastAdapter.withOnPreLongClickListener(new FastAdapter.OnLongClickListener<IItem>() {
                @Override
                public boolean onLongClick(View v, IAdapter<IItem> adapter, IItem item, int position) {
                    if (tooManySubjects((CoverMessageItem) item)) {
                        return true;
                    }

                    actionModeHelper.onLongClick((AppCompatActivity) getActivity(), position);
                    return false;
                }
            });

            fastAdapter.withOnLongClickListener(new FastAdapter.OnLongClickListener<IItem>() {
                @Override
                public boolean onLongClick(View v, IAdapter<IItem> adapter, IItem item, int position) {
                    App.logUserInput("List-Item at position " + position + " was long-clicked");
                    System.out.println("item.isSelected() = " + item.isSelected());
                    if (getActionMode() != null) updateCabTitle(getActionMode());
                    return true;
                }
            });

//            headers = new HeaderAdapter<>();
//            items = new ItemAdapter<>();


//            fastAdapter.withOnPreLongClickListener(new FastAdapter.OnLongClickListener<IItem>() {
//                //                            @SuppressWarnings("ConstantConditions")
//                @Override
//                public boolean onLongClick(View v, IAdapter<IItem> adapter, IItem item, int position) {
//                    android.support.v7.view.ActionMode actionMode = actionModeHelper.onLongClick((AppCompatActivity) getActivity(), position);
//
//
////                                if (actionMode != null) {
////                                    // Set CAB background color
////                                    findViewById(R.id.action_mode_bar).setBackgroundColor(Color.GRAY);
////                                }
//                    return actionMode != null;
//                }
//            });

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
            initialized = true;
        }

        if (noData) {
            fastAdapter.clear();
            fastAdapter.set(noDataHeaderAsList());
        } else if (mSortedCoverMessages != null) {
            resetPage();
        } else if (waitingForInit != null) {
            EventBus.getDefault().post(waitingForInit);
            waitingForInit = null;
        } else {
            fastAdapter.clear();
            System.out.println("Request Permission");
            EventBus.getDefault().post(new Event.LoadPermissionRequest());
        }

        return view;
    }

    public boolean tooManySubjects(CoverMessageItem item) {
        CoverMessage coverMessage = item.getCoverMessage();

        String subject = coverMessage.get(CoverMessage.SUBJECT);
        App.logTrace("subject = " + subject);

        String newSubject = coverMessage.get(CoverMessage.NEW_SUBJECT);
        App.logTrace("newSubject = " + newSubject);

        if (!subject.equals("") && !newSubject.equals("") && !subject.equals(newSubject)) {
            System.out.println("Too many subjects for multi-selection");
            Toast.makeText(getActivity(), subject + " und  " + newSubject + " in Meldung enthalten - nur Einzelauswahl möglich", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }


    @Subscribe
    public void onLoadPermitted(Event.LoadPermission loadPermission) {
        if (!initialized) {
            System.out.println("Not initialized: " + position);
            waitingForInit = loadPermission;
            return;
        }

        if (loadPermission.isPermitted(position) && alreadyLoading) {
            System.out.println("FragmentPage.onLoadPermitted");
            System.out.println("ALREADY LOADING");
        }
        if (loadPermission.isPermitted(position) && mSortedCoverMessages == null && !noData && !alreadyLoading) {
            System.out.println("onLoadPermitted for position " + position);
            noData = false;
            fastAdapter.set(loadingHeaderAsList());
            alreadyLoading = true;
            new HbgDownload(weekNumber, progressBar, getContext()).executeAsync(new ResultCallback<SortedCoverMessages>() {
                @Override
                public void onResult(SortedCoverMessages sortedCoverMessages) {
                    setRefreshing(false);
                    if (sortedCoverMessages == null) {
                        fastAdapter.set(noDataHeaderAsList()); // TODO: 24.10.2016 todo getContext returned null, check if reproducible
                        noData = true;
                    } else {
                        resetRecyclerView(sortedCoverMessages);
                        mSortedCoverMessages = sortedCoverMessages;
                    }
                }

                @Override
                public void onError(Throwable t) {
                    setRefreshing(false);
                    System.out.println("Error: " + t.getMessage());
                    showError(t);
                }
            });
        }

    }

    private void showError(Throwable t) {
        progressBar.setProgress(100);
        errorView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        errorView.setSubtitle(t.getMessage());
    }


    @Subscribe
    public void onCanScrollUpRequested(Event.CanScrollUpRequest request) {
        if (request != null && request.forPosition() != position) {
            System.out.println("not for position " +  position);
            return;
        }
        boolean canScrollUp = true;

        recyclerView.canScrollVertically(1);
        if (recyclerView != null) {
            canScrollUp = canScrollUp(recyclerView);
        }
        System.out.print("canScrollUp = " + canScrollUp);
        System.out.println(", position = " + position);
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
//        System.out.println("FragmentPage.resetPage, position : " + position);
//        int index = recyclerView.getFirstVisiblePosition();
//        View v = recyclerView.getChildAt(0);
//        int top = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());

        resetRecyclerView(mSortedCoverMessages);


//        recyclerView.setSelectionFromTop(index, top);

    }

    public void resetRecyclerView(SortedCoverMessages sortedCoverMessages) {
        fastAdapter.set(toRecyclerViewItems(sortedCoverMessages.getListItems(new CustomNameReplacer(getContext()))));
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity) getActivity()).waitForSwipeRefreshLayout(new App.WaitFor<android.support.v4.widget.SwipeRefreshLayout>() {
                    @Override
                    public void onResult(android.support.v4.widget.SwipeRefreshLayout result) {
                        result.setRefreshing(refreshing);
                    }
                });
            }
        });
    }

    public static List<IItem> toRecyclerViewItems(ArrayList<HBGMessage> data) {
        List<IItem> items = new ArrayList<>(data.size());
        for (int i = 0, sortedCoverMessagesSize = data.size(); i < sortedCoverMessagesSize; i++) {
            HBGMessage message = data.get(i);
            System.out.print("i = " + i);
            if (message instanceof CoverMessage) {
                System.out.println(" ,  CoverMessage");
                items.add(new CoverMessageItem((CoverMessage) message).withIdentifier(i));
            } else if (message instanceof HeaderMessage) {
                System.out.println(" ,  HeaderMessage");
                items.add(new DateHeaderItem((HeaderMessage) message).withIdentifier(i));
            }
        }

        return items;
    }

    private List<IItem> noDataHeaderAsList() {
        return Collections.singletonList(((IItem) new DateHeaderItem(new HeaderMessage("Keine Vertretungsdaten"))));
    }

    private List<IItem> loadingHeaderAsList() {
        return Collections.singletonList(((IItem) new DateHeaderItem(new HeaderMessage("Lade Vertretungsdaten..."))));
    }

    public ActionMode getActionMode() {
        return actionModeHelper != null ? actionModeHelper.getActionMode() : null;
    }


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
//            System.out.println("imageFile.delete() = " + imageFile.delete());
//
//            //noinspection deprecation
//            FileOutputStream outputStream = context.openFileOutput("image.jpg", Context.MODE_WORLD_READABLE);//new FileOutputStream(imageFile);//
//            int quality = 100;
//            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
//            outputStream.flush();
//            outputStream.close();
//
//            System.out.println("imageFile exists? : " + imageFile.exists());
//            for (File file :
//                    context.getFilesDir().listFiles()) {
//                System.out.println("file = " + file);
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
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_delete:
                    final boolean whitelistModeActive = Whitelist.isWhitelistModeActive(getActivity());
                    App.dialog("Bestätigen", "Sollen Meldungen zu den ausgewählten Fächern nicht mehr angezeigt werden?" + (whitelistModeActive ? "\n\nAchtung, der Whitelist-Modus ist aktiv!" : ""), getActivity())
                            .setPositiveButton("Ja", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final CustomNames customNames;

                                    if (recyclerView == null) {
                                        return;
                                    }

                                    customNames = CustomNames.get(getContext(), fastAdapter.getSelectedItems().size());

                                    System.out.println("Not showing following subjects anymore:");

                                    final Whitelist whiteList;
                                    whiteList = Whitelist.get(getContext());

                                    for (IItem item : fastAdapter.getSelectedItems()) {
                                        CoverMessage coverMessage = ((CoverMessageItem) item).getCoverMessage();
                                        String subject = coverMessage.get(CoverMessage.SUBJECT);
                                        if (subject.equals("")) {
                                            subject = coverMessage.get(CoverMessage.NEW_SUBJECT);
                                        }

                                        String originalSubject = null;

                                        for (Map.Entry<String, String> entry : customNames.entrySet()) {
                                            if (entry.getValue().equals(subject)) {
                                                originalSubject = entry.getKey();
                                            }
                                        }
                                        if (originalSubject == null) {
                                            originalSubject = subject;
                                        }

                                        System.out.println(originalSubject);

                                        if (whitelistModeActive) {
                                            for (int j = 0; j < whiteList.size(); j++) {
                                                String whitelistedSubject = whiteList.get(j);
                                                System.out.println("whitelistedSubject = " + whitelistedSubject);
                                                if (whitelistedSubject.equals(originalSubject)) {
                                                    whiteList.remove(j);
                                                    break;
                                                }
                                            }
                                        } else {
                                            customNames.put(originalSubject, "Nicht anzeigen");
                                        }
                                    }

                                    if (whitelistModeActive) {
                                        whiteList.save();
                                    } else {
                                        customNames.save();
                                    }

                                    EventBus.getDefault().post(new Event.ResetRequest());
                                    mode.finish();
                                }
                            })
                            .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mode.finish();
                                }
                            })
                            .show();
                    return true;
                case R.id.item_share:
                    StringBuilder sb = new StringBuilder();
                    for (IItem item2 : fastAdapter.getSelectedItems()) {
                        CoverMessage coverMessage = ((CoverMessageItem) item2).getCoverMessage();
                        sb.append(coverMessage.toString());
                        sb.append("\n");
                    }
                    int classNum = Preferences.getPreference(Preferences.Preference.MAIN_PREFERENCE, getContext()).getInt(Preferences.Key.SELECTED_CLASS, 0);
                    sb.append("\n")
                            .append(HbgDataDownload.makeURL(weekNumber, classNum));

                    String shareMessage = sb.toString();
                    System.out.println("shareMessage:\n" + shareMessage);
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

    public void updateCabTitle(ActionMode mode) {
        mode.setTitle(String.valueOf(fastAdapter.getSelectedItems().size()));
    }
}
