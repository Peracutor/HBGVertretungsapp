package com.eissler.micha.hbgvertretungsapp.settings;

import android.view.View;
import android.widget.TextView;

import com.eissler.micha.hbgvertretungsapp.FilterDialog;
import com.eissler.micha.hbgvertretungsapp.R;

import java.util.ArrayList;
import java.util.Map;


public class HiddenSubjects extends SubjectListActivity {

    CustomNames customNames;

    @Override
    protected void initialize() {
        customNames = CustomNames.get(getApplicationContext());
    }

    @Override
    protected SubjectListAdapter getSubjectListAdapter() {
        return new HiddenSubjectsAdapter();
    }

    @Override
    protected ArrayList<String> getData() {
        ArrayList<String> data = new ArrayList<>(12);

        for (Map.Entry<String, String> entry : customNames.entrySet()) {
            if (entry.getValue().equals("Nicht anzeigen")) {
                data.add(entry.getKey());
            }
        }
        return data;
    }

    @Override
    protected void addToData(String subject) {
        customNames.put(subject, "Nicht anzeigen");
    }

    @Override
    protected void removeFromData(ArrayList<Integer> indices) {
        ArrayList<String> data = getData();
        for (Integer index : indices) {
            customNames.remove(data.get(index));
        }
    }

    @Override
    protected void saveData() {
        customNames.save();
    }

    @Override
    protected int getLabelResource() {
        return R.string.label_hidden_subjects;
    }


//            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    String originalSubject = parent.getItemAtPosition(position).toString();
//                    System.out.println("originalSubject = " + originalSubject);
//                    new FilterDialog("Nicht anzeigen", originalSubject, customNames,
//                            new FilterDialog.PostExecuteInterface() {
//                                @Override
//                                public void onPostExecute() {
//                                    HiddenSubjects.this.setAdapter();
//                                }
//                            },
//                            HiddenSubjects.this).show(false);
//
//                }
//            });
//        }

    class HiddenSubjectsAdapter extends SubjectListAdapter {
        public HiddenSubjectsAdapter() {
            super(getData(), HiddenSubjects.this);
        }

        @Override
        protected String getNoItemsString() {
            return "Es werden alle Kurse angezeigt";
        }

        @Override
        protected View.OnClickListener getNonSelectiveListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String originalSubject = ((TextView) view.findViewById(R.id.textView)).getText().toString();
                    System.out.println("originalSubject = " + originalSubject);
                    new FilterDialog("Nicht anzeigen", originalSubject, customNames,
                            new FilterDialog.PostExecuteInterface() {
                                @Override
                                public void onPostExecute() {
                                    updateList();
                                }
                            },
                            HiddenSubjects.this).show(false);

                }
            };
        }
    }
}
