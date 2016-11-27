package com.eissler.micha.hbgvertretungsapp;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.HashMap;

/**
 * Created by Micha.
 * 15.08.2016
 */
public class AlertDialogFragment extends DialogFragment {

    public static AlertDialogFragment newInstance(DialogCreator dialogCreator, int id) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        fragment.setId(id);
        VariableHolder.setDialogCreator(id, dialogCreator);
        return fragment;
    }

    private void setId(int id) {
//        id.setOnCancelListener(null);
        Bundle args = new Bundle();
        args.putInt("id", id);
        setArguments(args);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int id = getFragmentId();
        return VariableHolder.get(id);
    }

    private int getFragmentId() {
        return getArguments().getInt("id");
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        DialogInterface.OnCancelListener onCancelListener = VariableHolder.getCancelListener(getFragmentId());
        if (onCancelListener != null) {
            onCancelListener.onCancel(dialog);
        }
    }

    @Override
    public void onDestroy() {
//        VariableHolder.destroy(getFragmentId());
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, "DialogFragment" + getFragmentId());
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        VariableHolder.setOnCancelListener(getFragmentId(), onCancelListener);
    }

    private static class VariableHolder {

        private static HashMap<Integer, DialogCreator> creators;
        private static HashMap<Integer, DialogInterface.OnCancelListener> cancelListeners;

        public static void setDialogCreator(int id, DialogCreator dialogCreator) {
            if (creators == null) {
                creators = new HashMap<>(1);
            }
            creators.put(id, dialogCreator);
        }


        public static Dialog get(int id) {
            return creators.get(id).getDialog();
        }

        public static void setOnCancelListener(int id, DialogInterface.OnCancelListener onCancelListener) {
            if (cancelListeners == null) {
                cancelListeners = new HashMap<>(1);
            }
            cancelListeners.put(id, onCancelListener);
        }

        public static DialogInterface.OnCancelListener getCancelListener(int id) {
            return cancelListeners.get(id);
        }

//        public static void destroy(int fragmentId) {
//            creators.remove(fragmentId);
//            if (cancelListeners != null) cancelListeners.remove(fragmentId);
//        }
    }

    interface DialogCreator {
        Dialog getDialog();
    }


}
