package ru.trjoxuvw.manualrecurrencetasks;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;

import adapter.RecordListAdapter;
import database.RecordData;
import database.GroupData;
import database.DatabaseHelper;
import utils.Utils;

// TODO: lock database once for operation series
// TODO: notify time countdown + show day of week

public class MainActivity extends AppCompatActivity {
    public static final String GROUP_ID_TAG = "GROUP_ID_TAG";

    public static final int ADD_RECORD_REQUEST = 0;
    public static final int OPTIONS_REQUEST = 1;

    private Spinner groupSpinner;
    private CheckBox activeOnlyCheckBox;
    private ListView recordListView;
    private Button createRecordButton;

    private ArrayList<GroupData> groups;
    private int selectedGroupPosition;

    public ArrayList<GroupData> getGroups()
    {
        return groups;
    }

    private void refreshGroups()
    {
        groups = DatabaseHelper.getInstance(getApplicationContext()).getGroups();

        ArrayList<String> groupStrings = new ArrayList<>();
        groupStrings.add("Notifications");
        for (GroupData group : groups)
        {
            groupStrings.add(group.getLabel());
        }

        ArrayAdapter<String> groupStringsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupStrings);
        groupStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupStringsAdapter);

        createRecordButton.setEnabled(groups.size() > 0);

        switchGroup(0);
    }

    private void switchGroup(int position)
    {
        selectedGroupPosition = position;

        if (position == 0) {
            activeOnlyCheckBox.setEnabled(true);
            activeOnlyCheckBox.setChecked(false);
        } else {
            final GroupData.FilterMode fm = groups.get(position - 1).filterMode;
            activeOnlyCheckBox.setEnabled(fm != GroupData.FilterMode.ONLY_ALL);
            activeOnlyCheckBox.setChecked(fm == GroupData.FilterMode.DEFAULT_FILTERED);
        }

        refreshRecords(position);
    }

    private void refreshRecords(int position) {
        long maxTime = Long.MIN_VALUE;
        if (activeOnlyCheckBox.isChecked())
        {
            Calendar calendar = Calendar.getInstance();
            maxTime = calendar.getTimeInMillis();
        }

        ArrayList<RecordData> records = DatabaseHelper.getInstance(getApplicationContext()).getRecords(position == 0 ? Long.MIN_VALUE : groups.get(position - 1).id, maxTime, position == 0);
        ((RecordListAdapter)recordListView.getAdapter()).ResetList(records);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case ADD_RECORD_REQUEST:
                if (resultCode == 1)
                    refreshRecords(selectedGroupPosition);
                break;

            case OPTIONS_REQUEST:
                if (resultCode == 1)
                    refreshGroups();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button optionsButton = (Button) findViewById(R.id.optionsButton);
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

        createRecordButton = (Button) findViewById(R.id.createRecordButton);
        assert createRecordButton != null;
        createRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddRecordActivity.class);
                intent.putExtra(AddRecordActivity.OPERATION, AddRecordActivity.OPERATION_CREATE);
                intent.putExtra(AddRecordActivity.INIT_GROUP_INDEX, selectedGroupPosition > 0 ? selectedGroupPosition - 1 : 0);
                startActivityForResult(intent, ADD_RECORD_REQUEST);
            }
        });

        groupSpinner = (Spinner) findViewById(R.id.groupSpinner);
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

        activeOnlyCheckBox = (CheckBox) findViewById(R.id.activeOnlyCheckBox);
        assert activeOnlyCheckBox != null;
        activeOnlyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshRecords(selectedGroupPosition);
            }
        });

        recordListView = (ListView) findViewById(R.id.recordListView);
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
        refreshGroups();

        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                long groupId = extras.getLong(GROUP_ID_TAG, -1);
                if (groupId != -1) {
                    groupSpinner.setSelection(Utils.getPositionById(groups, groupId) + 1);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecords(selectedGroupPosition);
    }
}
