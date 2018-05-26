package ru.trjoxuvw.manualrecurrencetasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;

import adapter.RecordListAdapter;
import database.GroupData;
import database.RecordData;
import utils.DatePickerHelper;
import utils.ObjectCache;
import utils.TimePickerHelper;
import utils.Utils;

public class MainActivity extends AppCompatActivity {
    public static final String GROUP_ID_TAG = "GROUP_ID_TAG";

    public static final int VIEW_RECORD_REQUEST = 0;
    public static final int OPTIONS_REQUEST = 1;

    private Spinner groupSpinner;
    private CheckBox activeOnlyCheckBox;
    private ListView recordListView;
    private Button createRecordButton;

    private int selectedGroupPosition;

    public ArrayList<GroupData> getGroups() {
        return ObjectCache.getGroups(getApplicationContext());
    }

    private void invalidateGroupSpinner() {
        ArrayList<String> groupStrings = new ArrayList<>();
        groupStrings.add("Notifications");
        for (GroupData group : getGroups()) {
            groupStrings.add(group.getLabel());
        }

        ArrayAdapter<String> groupStringsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupStrings);
        groupStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupStringsAdapter);

        createRecordButton.setEnabled(getGroups().size() > 0);

        switchGroup(0);
    }

    private void switchGroup(int position) {
        selectedGroupPosition = position;

        if (position == 0) {
            activeOnlyCheckBox.setEnabled(true);
            activeOnlyCheckBox.setChecked(false);
        } else {
            final GroupData.FilterMode fm = getGroups().get(position - 1).filterMode;
            activeOnlyCheckBox.setEnabled(fm != GroupData.FilterMode.ONLY_ALL);
            activeOnlyCheckBox.setChecked(fm == GroupData.FilterMode.DEFAULT_FILTERED);
        }

        refreshRecords(position);
    }

    private void refreshRecords(int position) {
        long maxTime = Long.MIN_VALUE;
        if (activeOnlyCheckBox.isChecked()) {
            Calendar calendar = Calendar.getInstance();
            maxTime = calendar.getTimeInMillis();
        }

        ArrayList<RecordData> records = ObjectCache.getDbInstance(getApplicationContext()).getRecords(position == 0 ? Long.MIN_VALUE : getGroups().get(position - 1).id, maxTime, position == 0);
        ((RecordListAdapter) recordListView.getAdapter()).ResetList(records);
    }

    public void refreshRecords() {
        refreshRecords(selectedGroupPosition);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case VIEW_RECORD_REQUEST:
                if (resultCode == 1)
                    refreshRecords();
                break;

            case OPTIONS_REQUEST:
                if (resultCode == 1)
                    invalidateGroupSpinner();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button optionsButton = findViewById(R.id.optionsButton);
        assert optionsButton != null;
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GroupsActivity.class);
                startActivityForResult(intent, OPTIONS_REQUEST);
            }
        });
        optionsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(MainActivity.this, DebugActivity.class);
                startActivity(intent);
                return true;
            }
        });

        final Button toolboxButton = findViewById(R.id.toolboxButton);
        assert toolboxButton != null;
        toolboxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolboxFragment.createAndShow(getSupportFragmentManager());
            }
        });

        createRecordButton = findViewById(R.id.createRecordButton);
        assert createRecordButton != null;
        createRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                intent.putExtra(RecordActivity.OPERATION, RecordActivity.OPERATION_CREATE);
                intent.putExtra(RecordActivity.INIT_GROUP_INDEX, selectedGroupPosition > 0 ? selectedGroupPosition - 1 : 0);
                startActivityForResult(intent, VIEW_RECORD_REQUEST);
            }
        });

        groupSpinner = findViewById(R.id.groupSpinner);
        assert groupSpinner != null;
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switchGroup(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                switchGroup(0);
            }
        });

        activeOnlyCheckBox = findViewById(R.id.activeOnlyCheckBox);
        assert activeOnlyCheckBox != null;
        activeOnlyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshRecords();
            }
        });

        recordListView = findViewById(R.id.recordListView);
        assert recordListView != null;
        recordListView.setAdapter(new RecordListAdapter(this));

        handleIntent(savedInstanceState == null ? getIntent() : null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        invalidateGroupSpinner();

        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                long groupId = extras.getLong(GROUP_ID_TAG, -1);
                if (groupId != -1) {
                    groupSpinner.setSelection(Utils.getPositionById(getGroups(), groupId) + 1);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecords();
    }

    private ArrayList<RecordData> getRecords() {
        return ((RecordListAdapter) recordListView.getAdapter()).GetList();
    }

    public static class ToolboxFragment extends DialogFragment {
        public static void createAndShow(FragmentManager manager) {
            new ToolboxFragment().show(manager, "ToolboxFragment");
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            final MainActivity parent = (MainActivity) getActivity();
            final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            builder.setTitle("Choose tool to use")
                    .setItems(R.array.toolbox_items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: {
                                    Calendar calendarNow = Calendar.getInstance();
                                    DatePickerHelper.createAndShow(
                                            new DatePickerForSetVisibleRecordsDate(),
                                            parent.getSupportFragmentManager(),
                                            calendarNow.get(Calendar.YEAR),
                                            calendarNow.get(Calendar.MONTH),
                                            calendarNow.get(Calendar.DATE)
                                    );
                                    break;
                                }

                                case 1: {
                                    TimePickerHelper.createAndShow(
                                            new TimePickerForSetVisibleRecordsTime(),
                                            parent.getSupportFragmentManager(),
                                            0,
                                            0
                                    );
                                    break;
                                }

                                case 2: {
                                    ArrayList<RecordData> records = parent.getRecords();
                                    for (RecordData record : records) {
                                        record.isChecked = false;
                                        ObjectCache.getDbInstance(parent.getApplicationContext()).update(record);
                                    }
                                    parent.refreshRecords();
                                    break;
                                }
                            }
                        }})
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }

    public static class DatePickerForSetVisibleRecordsDate extends DatePickerHelper {
        @Override
        public DatePickerDialog.OnDateSetListener getOnDateSetListener() {
            return new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    final MainActivity parent = (MainActivity) getActivity();

                    for (final RecordData record: parent.getRecords()) {
                        final Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(record.nextAppear);
                        calendar.set(year, monthOfYear, dayOfMonth);

                        record.nextAppear = calendar.getTimeInMillis();
                        ObjectCache.getDbInstance(parent.getApplicationContext()).update(record);
                    }

                    parent.refreshRecords();
                }
            };
        }
    }

    public static class TimePickerForSetVisibleRecordsTime extends TimePickerHelper {
        @Override
        public TimePickerDialog.OnTimeSetListener getOnTimeSetListener() {
            return new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    final MainActivity parent = (MainActivity) getActivity();

                    for (final RecordData record: parent.getRecords()) {
                        final Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(record.nextAppear);
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);

                        record.nextAppear = calendar.getTimeInMillis();
                        ObjectCache.getDbInstance(parent.getApplicationContext()).update(record);
                    }

                    parent.refreshRecords();
                }
            };
        }
    }
}
