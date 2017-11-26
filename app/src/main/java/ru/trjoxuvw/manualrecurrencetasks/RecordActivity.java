package ru.trjoxuvw.manualrecurrencetasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import database.DatabaseHelper;
import database.GroupData;
import database.RecordData;
import notification.NotificationUtils;
import utils.Utils;

public class RecordActivity extends AppCompatActivity {
    private static final String YEAR_TAG = "YEAR_TAG";
    private static final String MONTH_TAG = "MONTH_TAG";
    private static final String DAY_TAG = "DAY_TAG";
    private static final String HOUR_TAG = "HOUR_TAG";
    private static final String MINUTE_TAG = "MINUTE_TAG";

    public static final String OPERATION = "ACTIVITY_OPERATION";
    public static final int OPERATION_CREATE = 0;
    public static final int OPERATION_UPDATE = 1;

    public static final String INIT_GROUP_INDEX = "INIT_GROUP_INDEX";
    public static final String EDIT_RECORD_ID = "EDIT_RECORD_ID";

    private EditText labelEditText;
    private Button pickDateButton, pickTimeButton;
    private CheckBox checkedCheckBox;
    private Button updateButton;
    private Button dismissRevertButton;

    private ArrayList<GroupData> groups;
    private int selectedGroupPosition;
    private boolean useCheckbox;

    private Calendar calendar;
    private int operation;
    private RecordData editRecord;

    private void updateDateTimeText() {
        Date date = new Date(calendar.getTimeInMillis());
        Utils.DateTimeFormatted formattedDateTime = Utils.formatDateTime(date);
        pickDateButton.setText(formattedDateTime.date);
        pickTimeButton.setText(formattedDateTime.time);
    }

    private class IsSameRecordCache {
        private Boolean sameRecord;

        public boolean get() {
            if (sameRecord == null) {
                sameRecord = layoutDataToRecordData(editRecord.id).equalsRecord(editRecord);
            }
            return sameRecord;
        }
    }

    private void updateButtonState() {
        IsSameRecordCache sameRecord = new IsSameRecordCache();

        if (updateButton != null) {
            updateButton.setEnabled(!sameRecord.get());
        }

        if (dismissRevertButton != null) {
            dismissRevertButton.setText(operation == OPERATION_CREATE || !sameRecord.get() ? "Discard" : "Close");
        }
    }

    private void switchGroup(int position) {
        selectedGroupPosition = position;
        useCheckbox = groups.get(position).isChecklist;
        checkedCheckBox.setEnabled(useCheckbox);
        updateButtonState();
    }

    private RecordData layoutDataToRecordData(long id) {
        return new RecordData(
                id,
                groups.get(selectedGroupPosition).id,
                labelEditText.getText().toString(),
                calendar.getTimeInMillis(),
                useCheckbox && checkedCheckBox.isChecked()
        );
    }

    private void createRecord() {
        NotificationUtils.unregisterGroup(RecordActivity.this, groups.get(selectedGroupPosition));

        RecordData newRecord = layoutDataToRecordData(0);
        newRecord.id = DatabaseHelper.getInstance(getApplicationContext()).create(newRecord);
        NotificationUtils.registerRecord(RecordActivity.this, groups.get(selectedGroupPosition), newRecord);

        NotificationUtils.registerGroup(RecordActivity.this, groups.get(selectedGroupPosition));

        setResult(1);

        if (operation == OPERATION_UPDATE) {
            editRecord = newRecord;
            updateButtonState();
        }
    }

    private void updateRecord() {
        GroupData oldGroup = groups.get(Utils.getPositionById(groups, editRecord.groupId));
        GroupData newGroup = groups.get(selectedGroupPosition);

        RecordData newEditRecord = layoutDataToRecordData(editRecord.id);
        DatabaseHelper.getInstance(getApplicationContext()).update(newEditRecord);

        NotificationUtils.unregisterGroup(RecordActivity.this, oldGroup);
        NotificationUtils.unregisterGroup(RecordActivity.this, newGroup);

        NotificationUtils.unregisterRecord(RecordActivity.this, oldGroup, editRecord.id);
        NotificationUtils.registerRecord(RecordActivity.this, newGroup, newEditRecord);

        NotificationUtils.registerGroup(RecordActivity.this, oldGroup);
        NotificationUtils.registerGroup(RecordActivity.this, newGroup);

        setResult(1);
        editRecord = newEditRecord;
        updateButtonState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        if (savedInstanceState == null)
            operation = getIntent().getExtras().getInt(OPERATION);
        else
            operation = savedInstanceState.getInt(OPERATION);

        if (operation == OPERATION_UPDATE) {
            final long editRecordId;
            if (savedInstanceState == null)
                editRecordId = getIntent().getExtras().getLong(EDIT_RECORD_ID);
            else
                editRecordId = savedInstanceState.getLong(EDIT_RECORD_ID);
            editRecord = DatabaseHelper.getInstance(getApplicationContext()).getRecord(editRecordId);
        } else {
            editRecord = null;
        }

        labelEditText = findViewById(R.id.labelEditText);
        pickDateButton = findViewById(R.id.pickDateButton);
        pickTimeButton = findViewById(R.id.pickTimeButton);
        checkedCheckBox = findViewById(R.id.checkedCheckBox);

        assert labelEditText != null;
        assert pickDateButton != null;
        assert pickTimeButton != null;
        assert checkedCheckBox != null;

        labelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateButtonState();
            }
        });

        checkedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateButtonState();
            }
        });

        final Spinner groupSpinner = findViewById(R.id.groupSpinner);
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

        {
            groups = DatabaseHelper.getInstance(getApplicationContext()).getGroups();

            ArrayList<String> groupStrings = new ArrayList<>();
            for (GroupData group : groups) {
                groupStrings.add(group.getLabel());
            }

            ArrayAdapter<String> groupStringsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupStrings);
            groupStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            groupSpinner.setAdapter(groupStringsAdapter);
        }

        pickDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment.createAndShow(
                        getSupportFragmentManager(),
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
            }
        });

        pickDateButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Calendar calendarNow = Calendar.getInstance();

                DatePickerFragment.createAndShow(
                        getSupportFragmentManager(),
                        calendarNow.get(Calendar.YEAR),
                        calendarNow.get(Calendar.MONTH),
                        calendarNow.get(Calendar.DAY_OF_MONTH)
                );

                return true;
            }
        });

        pickTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment.createAndShow(
                        getSupportFragmentManager(),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)
                );
            }
        });

        pickTimeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final int hour;
                final int minute;

                if (calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0) {
                    Calendar calendarNow = Calendar.getInstance();
                    hour = calendarNow.get(Calendar.HOUR_OF_DAY);
                    minute = calendarNow.get(Calendar.MINUTE);
                } else {
                    hour = 0;
                    minute = 0;
                }

                TimePickerFragment.createAndShow(
                        getSupportFragmentManager(),
                        hour,
                        minute
                );

                return true;
            }
        });

        calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (savedInstanceState != null) {
            calendar.set(Calendar.YEAR, savedInstanceState.getInt(YEAR_TAG));
            calendar.set(Calendar.MONTH, savedInstanceState.getInt(MONTH_TAG));
            calendar.set(Calendar.DAY_OF_MONTH, savedInstanceState.getInt(DAY_TAG));
            calendar.set(Calendar.HOUR_OF_DAY, savedInstanceState.getInt(HOUR_TAG));
            calendar.set(Calendar.MINUTE, savedInstanceState.getInt(MINUTE_TAG));
        } else if (operation == OPERATION_UPDATE) {
            calendar.setTimeInMillis(editRecord.nextAppear);
        }

        if (savedInstanceState == null) {
            switch (operation) {
                case OPERATION_CREATE:
                    groupSpinner.setSelection(getIntent().getExtras().getInt(INIT_GROUP_INDEX));
                    break;

                case OPERATION_UPDATE:
                    groupSpinner.setSelection(Utils.getPositionById(groups, editRecord.groupId));
                    labelEditText.setText(editRecord.label);
                    checkedCheckBox.setChecked(editRecord.isChecked);
                    break;
            }
        }

        final Button dataCreationButton = operation == OPERATION_CREATE ?
                (Button) findViewById(R.id.footerButton1) :
                (Button) findViewById(R.id.footerButton3);
        dataCreationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRecord();
                finish();
            }
        });
        dataCreationButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                createRecord();
                return true;
            }
        });

        switch (operation) {
            case OPERATION_CREATE:
                dataCreationButton.setText("Create");
                break;

            case OPERATION_UPDATE:
                updateButton = findViewById(R.id.footerButton1);
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

                final Button deleteButton = findViewById(R.id.footerButton2);
                deleteButton.setText("Delete");
                deleteButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        DeleteRecordFragment.createAndShow(getSupportFragmentManager());
                        return true;
                    }
                });

                dataCreationButton.setText("Copy");

                break;
        }

        dismissRevertButton = operation == OPERATION_CREATE ?
                (Button) findViewById(R.id.footerButton2) :
                (Button) findViewById(R.id.footerButton4);
        dismissRevertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (operation == OPERATION_UPDATE) {
            dismissRevertButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    groupSpinner.setSelection(Utils.getPositionById(groups, editRecord.groupId));
                    labelEditText.setText(editRecord.label);

                    calendar.setTimeInMillis(editRecord.nextAppear);
                    updateDateTimeText();

                    checkedCheckBox.setChecked(editRecord.isChecked);

                    updateButtonState();
                    return true;
                }
            });
        }

        if (operation == OPERATION_CREATE) {
            final Button button3 = findViewById(R.id.footerButton3);
            final Button button4 = findViewById(R.id.footerButton4);
            button3.setVisibility(View.INVISIBLE);
            button4.setVisibility(View.INVISIBLE);
        }

        updateDateTimeText();
        updateButtonState();
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
        if (operation == OPERATION_UPDATE)
            outState.putLong(EDIT_RECORD_ID, editRecord.id);
    }

    public static class DatePickerFragment extends DialogFragment {
        public static void createAndShow(FragmentManager manager, int year, int monthOfYear, int dayOfMonth) {
            final DatePickerFragment fragment = new DatePickerFragment();

            final Bundle bundle = new Bundle();
            bundle.putInt(YEAR_TAG, year);
            bundle.putInt(MONTH_TAG, monthOfYear);
            bundle.putInt(DAY_TAG, dayOfMonth);
            fragment.setArguments(bundle);

            fragment.show(manager, "DatePickerFragment");
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final Bundle bundle = getArguments();
            final RecordActivity parent = (RecordActivity) getActivity();

            return new DatePickerDialog(
                    parent,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            parent.calendar.set(Calendar.YEAR, year);
                            parent.calendar.set(Calendar.MONTH, monthOfYear);
                            parent.calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            parent.updateDateTimeText();
                            parent.updateButtonState();
                        }
                    },
                    bundle.getInt(YEAR_TAG),
                    bundle.getInt(MONTH_TAG),
                    bundle.getInt(DAY_TAG)
            );
        }
    }

    public static class TimePickerFragment extends DialogFragment {
        public static void createAndShow(FragmentManager manager, int hourOfDay, int minute) {
            final TimePickerFragment fragment = new TimePickerFragment();

            final Bundle bundle = new Bundle();
            bundle.putInt(HOUR_TAG, hourOfDay);
            bundle.putInt(MINUTE_TAG, minute);
            fragment.setArguments(bundle);

            fragment.show(manager, "TimePickerFragment");
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final Bundle bundle = getArguments();
            final RecordActivity parent = (RecordActivity) getActivity();

            return new TimePickerDialog(
                    parent,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            parent.calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            parent.calendar.set(Calendar.MINUTE, minute);
                            parent.updateDateTimeText();
                            parent.updateButtonState();
                        }
                    },
                    bundle.getInt(HOUR_TAG),
                    bundle.getInt(MINUTE_TAG),
                    DateFormat.is24HourFormat(parent)
            );
        }
    }

    public static class DeleteRecordFragment extends DialogFragment {
        public static void createAndShow(FragmentManager manager) {
            (new DeleteRecordFragment()).show(manager, "DeleteRecordFragment");
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            final RecordActivity parent = (RecordActivity) getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            builder.setMessage("Delete record?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            final GroupData oldGroup = parent.groups.get(Utils.getPositionById(parent.groups, parent.editRecord.groupId));

                            NotificationUtils.unregisterGroup(parent, oldGroup);

                            DatabaseHelper.getInstance(parent.getApplicationContext()).deleteRecord(parent.editRecord.id);
                            NotificationUtils.unregisterRecord(parent, oldGroup, parent.editRecord.id);

                            NotificationUtils.registerGroup(parent, oldGroup);

                            parent.setResult(1);
                            parent.finish();
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
