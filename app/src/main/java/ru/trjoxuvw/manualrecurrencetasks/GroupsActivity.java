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
                GroupRenameDialogFragment.newInstance(position).show(getSupportFragmentManager(), "groupRenamer");
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
                GroupRenameDialogFragment.newInstance().show(getSupportFragmentManager(), "groupRenamer");
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

    public static class GroupRenameDialogFragment extends DialogFragment {
        private EditText groupRenameEditText;
        private CheckBox groupRenameIsChecklist;
        private CheckBox groupRenameIsInbox;
        private CheckBox groupRenameIsNotification;
        private Spinner filterModeSpinner;
        private int selectedFilterMode;
        private boolean enableValidation = false;

        public static GroupRenameDialogFragment newInstance() {
            return new GroupRenameDialogFragment();
        }

        public static GroupRenameDialogFragment newInstance(int groupIndex) {
            GroupRenameDialogFragment pickerFragment = new GroupRenameDialogFragment();

            Bundle bundle = new Bundle();
            bundle.putInt(GROUP_INDEX_TAG, groupIndex);
            pickerFragment.setArguments(bundle);

            return pickerFragment;
        }

        private GroupData groupDataFromLayout(long id) {
            return new GroupData(
                    id,
                    groupRenameEditText.getText().toString(),
                    groupRenameIsChecklist.isChecked(),
                    groupRenameIsInbox.isChecked(),
                    groupRenameIsNotification.isChecked(),
                    GroupData.ID_TO_FILTER_MODE.get(selectedFilterMode)
            );
        }

        private boolean isStateValid() {
            if (!enableValidation) {
                return true;
            }

            if (groupRenameIsNotification.isChecked()) {
                if (groupRenameIsChecklist.isChecked() || groupRenameIsInbox.isChecked()) {
                    return false;
                }
            }

            if (GroupData.ID_TO_FILTER_MODE.get(selectedFilterMode) != GroupData.FilterMode.ONLY_ALL) {
                if (groupRenameIsChecklist.isChecked() || groupRenameIsInbox.isChecked()) {
                    return false;
                }
            } else {
                if (groupRenameIsNotification.isChecked()) {
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
            View view = inflater.inflate(R.layout.group_rename, null);

            final TextView captionTextView = (TextView) view.findViewById(R.id.captionTextView);
            groupRenameEditText = (EditText) view.findViewById(R.id.groupRenameEditText);
            groupRenameIsChecklist = (CheckBox) view.findViewById(R.id.groupRenameIsChecklist);
            groupRenameIsInbox = (CheckBox) view.findViewById(R.id.groupRenameIsInbox);
            groupRenameIsNotification = (CheckBox) view.findViewById(R.id.groupRenameIsNotification);

            groupRenameIsChecklist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!isStateValid()) {
                        groupRenameIsChecklist.setChecked(!b);
                    }
                }
            });

            groupRenameIsInbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!isStateValid()) {
                        groupRenameIsInbox.setChecked(!b);
                    }
                }
            });

            groupRenameIsNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!isStateValid()) {
                        groupRenameIsNotification.setChecked(!b);
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
                groupRenameEditText.setText(pressedGroupData.name);
                groupRenameIsChecklist.setChecked(pressedGroupData.isChecklist);
                groupRenameIsChecklist.setEnabled(!Utils.groupHasCheckedRecords(parent.getApplicationContext(), pressedGroupData.id));
                groupRenameIsInbox.setChecked(pressedGroupData.isInbox);
                groupRenameIsNotification.setChecked(pressedGroupData.isNotification);
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

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
