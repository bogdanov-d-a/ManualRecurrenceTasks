package ru.trjoxuvw.manualrecurrencetasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
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

import database.GroupData;
import database.RecordData;
import notification.NotificationUtils;
import utils.DatePickerHelper;
import utils.ObjectCache;
import utils.TimePickerHelper;
import utils.Utils;

public class RecordActivity extends AppCompatActivity {
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

    private int selectedGroupPosition;
    private boolean useCheckbox;

    private Calendar calendar;
    private int operation;
    private RecordData editRecord;

    private ArrayList<GroupData> getGroups() {
        return ObjectCache.getGroups(getApplicationContext());
    }

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
        useCheckbox = getGroups().get(position).isChecklist;
        checkedCheckBox.setEnabled(useCheckbox);
        updateButtonState();
    }

    private RecordData layoutDataToRecordData(long id) {
        return new RecordData(
                id,
                getGroups().get(selectedGroupPosition).id,
                labelEditText.getText().toString(),
                calendar.getTimeInMillis(),
                useCheckbox && checkedCheckBox.isChecked()
        );
    }

    private void createRecord() {
        final GroupData selectedGroup = getGroups().get(selectedGroupPosition);

        NotificationUtils.unregisterGroup(RecordActivity.this, selectedGroup);

        RecordData newRecord = layoutDataToRecordData(0);
        newRecord.id = ObjectCache.getDbInstance(getApplicationContext()).create(newRecord);
        NotificationUtils.registerRecord(RecordActivity.this, selectedGroup, newRecord);

        NotificationUtils.registerGroup(RecordActivity.this, selectedGroup);

        setResult(1);

        if (operation == OPERATION_UPDATE) {
            editRecord = newRecord;
            updateButtonState();
        }
    }

    private void updateRecord() {
        GroupData oldGroup = getGroups().get(Utils.getPositionById(getGroups(), editRecord.groupId));
        GroupData newGroup = getGroups().get(selectedGroupPosition);

        RecordData newEditRecord = layoutDataToRecordData(editRecord.id);
        ObjectCache.getDbInstance(getApplicationContext()).update(newEditRecord);

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
            editRecord = ObjectCache.getDbInstance(getApplicationContext()).getRecord(editRecordId);
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
            ArrayList<String> groupStrings = new ArrayList<>();
            for (GroupData group : getGroups()) {
                groupStrings.add(group.getLabel());
            }

            ArrayAdapter<String> groupStringsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupStrings);
            groupStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            groupSpinner.setAdapter(groupStringsAdapter);
        }

        pickDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerHelper.createAndShow(
                        new DatePickerForSetDate(),
                        getSupportFragmentManager(),
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DATE)
                );
            }
        });

        pickDateButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Calendar calendarNow = Calendar.getInstance();

                DatePickerHelper.createAndShow(
                        new DatePickerForSetDate(),
                        getSupportFragmentManager(),
                        calendarNow.get(Calendar.YEAR),
                        calendarNow.get(Calendar.MONTH),
                        calendarNow.get(Calendar.DATE)
                );

                return true;
            }
        });

        pickTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerHelper.createAndShow(
                        new TimePickerForSetTime(),
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

                TimePickerHelper.createAndShow(
                        new TimePickerForSetTime(),
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
            calendar.set(savedInstanceState.getInt(Utils.YEAR_TAG),
                    savedInstanceState.getInt(Utils.MONTH_TAG), savedInstanceState.getInt(Utils.DAY_TAG));
            calendar.set(Calendar.HOUR_OF_DAY, savedInstanceState.getInt(Utils.HOUR_TAG));
            calendar.set(Calendar.MINUTE, savedInstanceState.getInt(Utils.MINUTE_TAG));
        } else if (operation == OPERATION_UPDATE) {
            calendar.setTimeInMillis(editRecord.nextAppear);
        }

        if (savedInstanceState == null) {
            switch (operation) {
                case OPERATION_CREATE:
                    groupSpinner.setSelection(getIntent().getExtras().getInt(INIT_GROUP_INDEX));
                    break;

                case OPERATION_UPDATE:
                    groupSpinner.setSelection(Utils.getPositionById(getGroups(), editRecord.groupId));
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
                    groupSpinner.setSelection(Utils.getPositionById(getGroups(), editRecord.groupId));
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

        Utils.putDateToBundle(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE), outState);
        outState.putInt(Utils.HOUR_TAG, calendar.get(Calendar.HOUR_OF_DAY));
        outState.putInt(Utils.MINUTE_TAG, calendar.get(Calendar.MINUTE));

        outState.putInt(OPERATION, operation);
        if (operation == OPERATION_UPDATE)
            outState.putLong(EDIT_RECORD_ID, editRecord.id);
    }

    public static class DatePickerForSetDate extends DatePickerHelper {
        @Override
        public DatePickerDialog.OnDateSetListener getOnDateSetListener() {
            return new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    final RecordActivity parent = (RecordActivity) getActivity();
                    parent.calendar.set(year, monthOfYear, dayOfMonth);
                    parent.updateDateTimeText();
                    parent.updateButtonState();
                }
            };
        }
    }

    public static class TimePickerForSetTime extends TimePickerHelper {
        @Override
        public TimePickerDialog.OnTimeSetListener getOnTimeSetListener() {
            return new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    final RecordActivity parent = (RecordActivity) getActivity();
                    parent.calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    parent.calendar.set(Calendar.MINUTE, minute);
                    parent.updateDateTimeText();
                    parent.updateButtonState();
                }
            };
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
                            Utils.deleteRecord(parent.getApplicationContext(), parent.getGroups(), parent.editRecord);

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
