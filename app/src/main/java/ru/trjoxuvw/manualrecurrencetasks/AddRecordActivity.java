package ru.trjoxuvw.manualrecurrencetasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import database.RecordData;
import database.TagData;
import database.DatabaseHelper;
import notification.NotificationUtils;
import utils.Utils;

public class AddRecordActivity extends AppCompatActivity {
    private static final String YEAR_TAG = "year";
    private static final String MONTH_TAG = "monthOfYear";
    private static final String DAY_TAG = "dayOfMonth";
    private static final String HOUR_TAG = "hourOfDay";
    private static final String MINUTE_TAG = "minute";

    public static final String OPERATION = "ACTIVITY_OPERATION";
    public static final int OPERATION_ADD = 0;
    public static final int OPERATION_EDIT = 1;

    public static final String INIT_TAG_INDEX = "initTagIndex";
    public static final String EDIT_RECORD_ID = "EDIT_RECORD_ID";

    private EditText labelEditText;
    private Button pickDateButton, pickTimeButton;
    private CheckBox checkedCheckBox;

    private ArrayList<TagData> tags;
    private int selectedTagPosition;
    private boolean useCheckbox;

    private Calendar calendar;
    private int operation;
    private RecordData editRecord;

    private void updateDateTimeText()
    {
        Date date = new Date(calendar.getTimeInMillis());
        pickDateButton.setText(SimpleDateFormat.getDateInstance().format(date));
        pickTimeButton.setText(SimpleDateFormat.getTimeInstance().format(date));
    }

    private void switchTag(int position) {
        selectedTagPosition = position;
        useCheckbox = tags.get(position).isChecklist;
        checkedCheckBox.setEnabled(useCheckbox);
    }

    private RecordData layoutDataToRecordData(long id) {
        return new RecordData(
                id,
                tags.get(selectedTagPosition).id,
                labelEditText.getText().toString(),
                calendar.getTimeInMillis(),
                useCheckbox && checkedCheckBox.isChecked()
        );
    }

    private static void setButtonWidth(Button button) {
        ViewGroup.LayoutParams lp = button.getLayoutParams();
        lp.width = 100;
        button.setLayoutParams(lp);
    }

    private void addRecord() {
        NotificationUtils.unregisterTag(AddRecordActivity.this, tags.get(selectedTagPosition));

        RecordData newRecord = layoutDataToRecordData(0);
        newRecord.id = DatabaseHelper.getInstance(getApplicationContext()).add(newRecord);
        NotificationUtils.registerRecord(AddRecordActivity.this, tags.get(selectedTagPosition), newRecord);

        NotificationUtils.registerTag(AddRecordActivity.this, tags.get(selectedTagPosition));

        setResult(1);
    }

    private void updateRecord() {
        TagData oldTag = tags.get(Utils.getPositionById(tags, editRecord.tagId));

        RecordData newEditRecord = layoutDataToRecordData(editRecord.id);
        DatabaseHelper.getInstance(getApplicationContext()).update(newEditRecord);

        NotificationUtils.unregisterTag(AddRecordActivity.this, oldTag);
        NotificationUtils.unregisterTag(AddRecordActivity.this, tags.get(selectedTagPosition));

        NotificationUtils.unregisterRecord(AddRecordActivity.this, editRecord.id);
        NotificationUtils.registerRecord(AddRecordActivity.this, tags.get(selectedTagPosition), newEditRecord);

        NotificationUtils.registerTag(AddRecordActivity.this, oldTag);
        NotificationUtils.registerTag(AddRecordActivity.this, tags.get(selectedTagPosition));

        setResult(1);
        editRecord = newEditRecord;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        labelEditText = (EditText) findViewById(R.id.labelEditText);
        pickDateButton = (Button) findViewById(R.id.pickDateButton);
        pickTimeButton = (Button) findViewById(R.id.pickTimeButton);
        checkedCheckBox = (CheckBox) findViewById(R.id.checkedCheckBox);

        assert labelEditText != null;
        assert pickDateButton != null;
        assert pickTimeButton != null;
        assert checkedCheckBox != null;

        final Spinner tagSpinner = (Spinner) findViewById(R.id.tagSpinner);
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

        {
            tags = DatabaseHelper.getInstance(getApplicationContext()).getTags();

            ArrayList<String> tagStrings = new ArrayList<>();
            for (TagData tag : tags)
            {
                tagStrings.add(tag.getLabel());
            }

            ArrayAdapter<String> tagStringsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tagStrings);
            tagStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tagSpinner.setAdapter(tagStringsAdapter);
        }

        pickDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = DatePickerFragment.newInstance(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                newFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });

        pickDateButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Calendar calendarNow = Calendar.getInstance();

                calendar.set(Calendar.YEAR, calendarNow.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, calendarNow.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, calendarNow.get(Calendar.DAY_OF_MONTH));

                updateDateTimeText();
                return true;
            }
        });

        pickTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = TimePickerFragment.newInstance(
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)
                );
                newFragment.show(getSupportFragmentManager(), "timePicker");
            }
        });

        pickTimeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0) {
                    Calendar calendarNow = Calendar.getInstance();

                    calendar.set(Calendar.HOUR_OF_DAY, calendarNow.get(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, calendarNow.get(Calendar.MINUTE));
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                }

                updateDateTimeText();
                return true;
            }
        });

        final LinearLayout buttonPanel = (LinearLayout) findViewById(R.id.buttonPanel);
        assert buttonPanel != null;

        if (savedInstanceState == null)
            operation = getIntent().getExtras().getInt(OPERATION);
        else
            operation = savedInstanceState.getInt(OPERATION);

        if (operation == OPERATION_EDIT) {
            final long editRecordId;
            if (savedInstanceState == null)
                editRecordId = getIntent().getExtras().getLong(EDIT_RECORD_ID);
            else
                editRecordId = savedInstanceState.getLong(EDIT_RECORD_ID);
            editRecord = DatabaseHelper.getInstance(getApplicationContext()).getRecord(editRecordId);
        } else {
            editRecord = null;
        }

        calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (savedInstanceState != null)
        {
            calendar.set(Calendar.YEAR, savedInstanceState.getInt(YEAR_TAG));
            calendar.set(Calendar.MONTH, savedInstanceState.getInt(MONTH_TAG));
            calendar.set(Calendar.DAY_OF_MONTH, savedInstanceState.getInt(DAY_TAG));
            calendar.set(Calendar.HOUR_OF_DAY, savedInstanceState.getInt(HOUR_TAG));
            calendar.set(Calendar.MINUTE, savedInstanceState.getInt(MINUTE_TAG));
        }
        else if (operation == OPERATION_EDIT)
        {
            calendar.setTimeInMillis(editRecord.nextAppear);
        }

        if (savedInstanceState == null)
        {
            switch (operation)
            {
                case OPERATION_ADD:
                    tagSpinner.setSelection(getIntent().getExtras().getInt(INIT_TAG_INDEX));
                    break;

                case OPERATION_EDIT:
                    tagSpinner.setSelection(Utils.getPositionById(tags, editRecord.tagId));
                    labelEditText.setText(editRecord.label);
                    checkedCheckBox.setChecked(editRecord.isChecked);
                    break;
            }
        }

        final Button addButton = new Button(this);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRecord();
                finish();
            }
        });
        addButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                addRecord();
                return true;
            }
        });

        switch (operation)
        {
            case OPERATION_ADD:
                addButton.setText("Add");
                buttonPanel.addView(addButton);
                setButtonWidth(addButton);
                break;

            case OPERATION_EDIT:
                final Button updateButton = new Button(AddRecordActivity.this);
                updateButton.setText("Update");
                updateButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateRecord();
                        finish();
                    }
                });
                updateButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        updateRecord();
                        return true;
                    }
                });
                buttonPanel.addView(updateButton);
                setButtonWidth(updateButton);

                final Button deleteButton = new Button(AddRecordActivity.this);
                deleteButton.setText("Delete");
                deleteButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        TagData oldTag = tags.get(Utils.getPositionById(tags, editRecord.tagId));

                        NotificationUtils.unregisterTag(AddRecordActivity.this, oldTag);

                        DatabaseHelper.getInstance(getApplicationContext()).deleteRecord(editRecord.id);
                        NotificationUtils.unregisterRecord(AddRecordActivity.this, editRecord.id);

                        NotificationUtils.registerTag(AddRecordActivity.this, oldTag);

                        setResult(1);
                        finish();

                        return true;
                    }
                });
                buttonPanel.addView(deleteButton);
                setButtonWidth(deleteButton);

                addButton.setText("Copy");
                buttonPanel.addView(addButton);
                setButtonWidth(addButton);

                break;
        }

        final Button cancelButton = new Button(AddRecordActivity.this);
        cancelButton.setText("Cancel");
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (operation == OPERATION_EDIT) {
            cancelButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    tagSpinner.setSelection(Utils.getPositionById(tags, editRecord.tagId));
                    labelEditText.setText(editRecord.label);

                    calendar.setTimeInMillis(editRecord.nextAppear);
                    updateDateTimeText();

                    checkedCheckBox.setChecked(editRecord.isChecked);
                    return true;
                }
            });
        }
        buttonPanel.addView(cancelButton);
        setButtonWidth(cancelButton);

        updateDateTimeText();
        setResult(0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(YEAR_TAG, calendar.get(Calendar.YEAR));
        outState.putInt(MONTH_TAG, calendar.get(Calendar.MONTH));
        outState.putInt(DAY_TAG, calendar.get(Calendar.DAY_OF_MONTH));
        outState.putInt(HOUR_TAG, calendar.get(Calendar.HOUR_OF_DAY));
        outState.putInt(MINUTE_TAG, calendar.get(Calendar.MINUTE));

        outState.putInt(OPERATION, operation);
        if (operation == OPERATION_EDIT)
            outState.putLong(EDIT_RECORD_ID, editRecord.id);
    }

    public static class DatePickerFragment extends DialogFragment {
        public static DatePickerFragment newInstance(int year, int monthOfYear, int dayOfMonth) {
            DatePickerFragment pickerFragment = new DatePickerFragment();

            Bundle bundle = new Bundle();
            bundle.putInt(YEAR_TAG, year);
            bundle.putInt(MONTH_TAG, monthOfYear);
            bundle.putInt(DAY_TAG, dayOfMonth);
            pickerFragment.setArguments(bundle);

            return pickerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final Bundle bundle = getArguments();
            final AddRecordActivity parent = (AddRecordActivity) getActivity();

            return new DatePickerDialog(
                    parent,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            parent.calendar.set(Calendar.YEAR, year);
                            parent.calendar.set(Calendar.MONTH, monthOfYear);
                            parent.calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            parent.updateDateTimeText();
                        }
                    },
                    bundle.getInt(YEAR_TAG),
                    bundle.getInt(MONTH_TAG),
                    bundle.getInt(DAY_TAG)
            );
        }
    }

    public static class TimePickerFragment extends DialogFragment {
        public static TimePickerFragment newInstance(int hourOfDay, int minute) {
            TimePickerFragment pickerFragment = new TimePickerFragment();

            Bundle bundle = new Bundle();
            bundle.putInt(HOUR_TAG, hourOfDay);
            bundle.putInt(MINUTE_TAG, minute);
            pickerFragment.setArguments(bundle);

            return pickerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final Bundle bundle = getArguments();
            final AddRecordActivity parent = (AddRecordActivity) getActivity();

            return new TimePickerDialog(
                    parent,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            parent.calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            parent.calendar.set(Calendar.MINUTE, minute);
                            parent.updateDateTimeText();
                        }
                    },
                    bundle.getInt(HOUR_TAG),
                    bundle.getInt(MINUTE_TAG),
                    DateFormat.is24HourFormat(getActivity())
            );
        }
    }
}
