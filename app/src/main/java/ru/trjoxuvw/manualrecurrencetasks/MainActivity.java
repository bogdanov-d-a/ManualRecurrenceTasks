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
import data.RecordData;
import data.TagData;
import database.DatabaseHelper;

// TODO: lock database once for operation series
// TODO: update view on every show
// TODO: use black text everywhere
// TODO: fix borders for activities and list items
// TODO: place date and time buttons on the same row

public class MainActivity extends AppCompatActivity {
    public static final int ADD_RECORD_REQUEST = 0;
    public static final int OPTIONS_REQUEST = 1;

    private Spinner tagSpinner;
    private CheckBox activeOnlyCheckBox;
    private ListView recordListView;
    private Button addRecordButton;

    private ArrayList<TagData> tags;
    private int selectedTagPosition;

    private ArrayList<RecordData> records;

    private void refreshTags()
    {
        tags = DatabaseHelper.getInstance(getApplicationContext()).getTags();

        ArrayList<String> tagStrings = new ArrayList<>();
        tagStrings.add("All tags");
        for (TagData tag : tags)
        {
            tagStrings.add(tag.name);
        }

        ArrayAdapter<String> tagStringsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tagStrings);
        tagStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagSpinner.setAdapter(tagStringsAdapter);

        addRecordButton.setEnabled(tags.size() > 0);

        switchTag(0);
    }

    private void switchTag(int position)
    {
        selectedTagPosition = position;
        refreshRecords(position);
    }

    private void refreshRecords(int position) {
        long maxTime = Long.MIN_VALUE;
        if (activeOnlyCheckBox.isChecked())
        {
            Calendar calendar = Calendar.getInstance();
            maxTime = calendar.getTimeInMillis();
        }

        records = DatabaseHelper.getInstance(getApplicationContext()).getRecords(position == 0 ? Long.MIN_VALUE : tags.get(position - 1).id, maxTime);
        ((RecordListAdapter)recordListView.getAdapter()).ResetList(records);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case ADD_RECORD_REQUEST:
                if (resultCode == 1)
                    refreshRecords(selectedTagPosition);
                break;

            case OPTIONS_REQUEST:
                if (resultCode == 1)
                    refreshTags();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button optionsButton = (Button) findViewById(R.id.optionsButton);
        assert optionsButton != null;
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TagsActivity.class);
                startActivityForResult(intent, OPTIONS_REQUEST);
            }
        });

        Button debugButton = (Button) findViewById(R.id.debugButton);
        assert debugButton != null;
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DebugActivity.class);
                startActivity(intent);
            }
        });

        addRecordButton = (Button) findViewById(R.id.addRecordButton);
        assert addRecordButton != null;
        addRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddRecordActivity.class);
                intent.putExtra(AddRecordActivity.OPERATION, AddRecordActivity.OPERATION_ADD);
                intent.putExtra(AddRecordActivity.INIT_TAG_INDEX, selectedTagPosition > 0 ? selectedTagPosition - 1 : 0);
                startActivityForResult(intent, ADD_RECORD_REQUEST);
            }
        });

        tagSpinner = (Spinner) findViewById(R.id.tagSpinner);
        assert tagSpinner != null;
        tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switchTag(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                switchTag(0);
            }
        });

        activeOnlyCheckBox = (CheckBox) findViewById(R.id.activeOnlyCheckBox);
        assert activeOnlyCheckBox != null;
        activeOnlyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshRecords(selectedTagPosition);
            }
        });

        recordListView = (ListView) findViewById(R.id.recordListView);
        assert recordListView != null;
        recordListView.setAdapter(new RecordListAdapter(this));
        recordListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, AddRecordActivity.class);
                intent.putExtra(AddRecordActivity.OPERATION, AddRecordActivity.OPERATION_EDIT);
                intent.putExtra(AddRecordActivity.EDIT_RECORD_ID, records.get(position).id);
                startActivityForResult(intent, ADD_RECORD_REQUEST);
            }
        });

        refreshTags();
    }
}
