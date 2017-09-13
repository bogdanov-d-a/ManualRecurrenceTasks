package ru.trjoxuvw.manualrecurrencetasks;

import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import adapter.GroupListAdapter;
import database.GroupData;
import database.DatabaseHelper;
import notification.NotificationUtils;
import utils.Utils;

public class GroupsActivity extends AppCompatActivity {
    private ListView groupListView;

    private static final String GROUP_INDEX_TAG = "GROUP_INDEX_TAG";
    private static final String RESULT_CODE_TAG = "RESULT_CODE_TAG";
    private int resultCode;

    private ArrayList<GroupData> groups;

    private void mySetResult(int code)
    {
        resultCode = code;
        setResult(code);
    }

    private void refreshGroups()
    {
        groups = DatabaseHelper.getInstance(getApplicationContext()).getGroups();
        ((GroupListAdapter) groupListView.getAdapter()).ResetList(groups);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        groupListView = (ListView) findViewById(R.id.groupListView);
        assert groupListView != null;
        groupListView.setAdapter(new GroupListAdapter(this));

        groupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GroupViewDialogFragment.newInstance(position).show(getSupportFragmentManager(), "groupView");
            }
        });

        groupListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (!Utils.groupHasRecords(getApplicationContext(), groups.get(position).id)) {
                    GroupDeleteDialogFragment.newInstance(position).show(getSupportFragmentManager(), "groupDeleter");
                }
                return true;
            }
        });

        refreshGroups();

        final Button createGroupButton = (Button) findViewById(R.id.createGroupButton);
        assert createGroupButton != null;
        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroupViewDialogFragment.newInstance().show(getSupportFragmentManager(), "groupView");
            }
        });

        if (savedInstanceState == null)
            mySetResult(0);
        else
            mySetResult(savedInstanceState.getInt(RESULT_CODE_TAG));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(RESULT_CODE_TAG, resultCode);
    }

    public static class GroupViewDialogFragment extends DialogFragment {
        private EditText groupViewEditText;
        private CheckBox groupViewIsChecklist;
        private CheckBox groupViewIsInbox;
        private CheckBox groupViewIsNotification;
        private Spinner filterModeSpinner;
        private int selectedFilterMode;
        private boolean enableValidation = false;

        public static GroupViewDialogFragment newInstance() {
            return new GroupViewDialogFragment();
        }

        public static GroupViewDialogFragment newInstance(int groupIndex) {
            GroupViewDialogFragment pickerFragment = new GroupViewDialogFragment();

            Bundle bundle = new Bundle();
            bundle.putInt(GROUP_INDEX_TAG, groupIndex);
            pickerFragment.setArguments(bundle);

            return pickerFragment;
        }

        private GroupData groupDataFromLayout(long id) {
            return new GroupData(
                    id,
                    groupViewEditText.getText().toString(),
                    groupViewIsChecklist.isChecked(),
                    groupViewIsInbox.isChecked(),
                    groupViewIsNotification.isChecked(),
                    GroupData.ID_TO_FILTER_MODE.get(selectedFilterMode)
            );
        }

        private boolean isStateValid() {
            if (!enableValidation) {
                return true;
            }

            if (groupViewIsNotification.isChecked()) {
                if (groupViewIsChecklist.isChecked() || groupViewIsInbox.isChecked()) {
                    return false;
                }
            }

            if (GroupData.ID_TO_FILTER_MODE.get(selectedFilterMode) != GroupData.FilterMode.ONLY_ALL) {
                if (groupViewIsChecklist.isChecked() || groupViewIsInbox.isChecked()) {
                    return false;
                }
            } else {
                if (groupViewIsNotification.isChecked()) {
                    return false;
                }
            }

            return true;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final GroupsActivity parent = (GroupsActivity) getActivity();
            LayoutInflater inflater = parent.getLayoutInflater();
            View view = inflater.inflate(R.layout.group_view, null);

            final TextView captionTextView = (TextView) view.findViewById(R.id.captionTextView);
            groupViewEditText = (EditText) view.findViewById(R.id.groupViewEditText);
            groupViewIsChecklist = (CheckBox) view.findViewById(R.id.groupViewIsChecklist);
            groupViewIsInbox = (CheckBox) view.findViewById(R.id.groupViewIsInbox);
            groupViewIsNotification = (CheckBox) view.findViewById(R.id.groupViewIsNotification);

            groupViewIsChecklist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!isStateValid()) {
                        groupViewIsChecklist.setChecked(!b);
                    }
                }
            });

            groupViewIsInbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!isStateValid()) {
                        groupViewIsInbox.setChecked(!b);
                    }
                }
            });

            groupViewIsNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!isStateValid()) {
                        groupViewIsNotification.setChecked(!b);
                    }
                }
            });

            filterModeSpinner = (Spinner) view.findViewById(R.id.filterModeSpinner);
            filterModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    final int oldFilterMode = selectedFilterMode;
                    selectedFilterMode = position;
                    if (!isStateValid()) {
                        selectedFilterMode = oldFilterMode;
                        filterModeSpinner.setSelection(selectedFilterMode);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedFilterMode = 0;
                }
            });
            ArrayAdapter<String> filterModeStringsAdapter = new ArrayAdapter<>(parent, android.R.layout.simple_spinner_item, GroupData.ID_TO_FILTER_MODE_LABEL);
            filterModeStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filterModeSpinner.setAdapter(filterModeStringsAdapter);

            AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            builder.setView(view);

            final Bundle bundle = getArguments();
            if (bundle != null) {
                final GroupData pressedGroupData = parent.groups.get(bundle.getInt(GROUP_INDEX_TAG));

                captionTextView.setText("Edit group");
                groupViewEditText.setText(pressedGroupData.name);
                groupViewIsChecklist.setChecked(pressedGroupData.isChecklist);
                groupViewIsChecklist.setEnabled(!Utils.groupHasCheckedRecords(parent.getApplicationContext(), pressedGroupData.id));
                groupViewIsInbox.setChecked(pressedGroupData.isInbox);
                groupViewIsNotification.setChecked(pressedGroupData.isNotification);
                filterModeSpinner.setSelection(GroupData.FILTER_MODE_TO_ID.get(pressedGroupData.filterMode));

                builder.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                GroupData newGroupData = groupDataFromLayout(pressedGroupData.id);

                                NotificationUtils.unregisterGroupData(parent, pressedGroupData);
                                DatabaseHelper.getInstance(parent.getApplicationContext()).update(newGroupData);
                                NotificationUtils.registerGroupData(parent, newGroupData);

                                parent.refreshGroups();
                                parent.mySetResult(1);
                            }
                        });
            } else {
                captionTextView.setText("Create group");

                builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                DatabaseHelper.getInstance(parent.getApplicationContext()).create(groupDataFromLayout(0));
                                parent.refreshGroups();
                                parent.mySetResult(1);
                            }
                        });
            }

            builder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dismiss();
                        }
                    });

            enableValidation = true;
            return builder.create();
        }
    }

    public static class GroupDeleteDialogFragment extends DialogFragment {
        public static GroupDeleteDialogFragment newInstance(int groupIndex) {
            GroupDeleteDialogFragment pickerFragment = new GroupDeleteDialogFragment();

            Bundle bundle = new Bundle();
            bundle.putInt(GROUP_INDEX_TAG, groupIndex);
            pickerFragment.setArguments(bundle);

            return pickerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final Bundle bundle = getArguments();
            final GroupsActivity parent = (GroupsActivity) getActivity();
            final GroupData pressedGroupData = parent.groups.get(bundle.getInt(GROUP_INDEX_TAG));

            AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            builder.setMessage("Delete group " + pressedGroupData.name + "?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            NotificationUtils.unregisterGroupData(parent, pressedGroupData);
                            DatabaseHelper.getInstance(parent.getApplicationContext()).deleteGroup(pressedGroupData.id);
                            parent.refreshGroups();
                            parent.mySetResult(1);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }
}
