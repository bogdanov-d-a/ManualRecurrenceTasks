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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import adapter.TagListAdapter;
import database.TagData;
import database.DatabaseHelper;
import notification.NotificationUtils;
import utils.Utils;

public class TagsActivity extends AppCompatActivity {
    private ListView tagListView;
    private EditText tagNameEditText;

    private static final String TAG_INDEX_TAG = "TAG_INDEX_TAG";
    private static final String RESULT_CODE_TAG = "RESULT_CODE_TAG";
    private int resultCode;

    private ArrayList<TagData> tags;

    private void mySetResult(int code)
    {
        resultCode = code;
        setResult(code);
    }

    private void refreshTags()
    {
        tags = DatabaseHelper.getInstance(getApplicationContext()).getTags();
        ((TagListAdapter)tagListView.getAdapter()).ResetList(tags);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tags);

        tagListView = (ListView) findViewById(R.id.tagListView);
        assert tagListView != null;
        tagListView.setAdapter(new TagListAdapter(this));

        tagListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TagRenameDialogFragment.newInstance(position).show(getSupportFragmentManager(), "tagRenamer");
            }
        });

        tagListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                TagDeleteDialogFragment.newInstance(position).show(getSupportFragmentManager(), "tagDeleter");
                return true;
            }
        });

        refreshTags();

        tagNameEditText = (EditText) findViewById(R.id.tagNameEditText);
        assert tagNameEditText != null;

        final Button addTagButton = (Button) findViewById(R.id.addTagButton);
        assert addTagButton != null;
        addTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseHelper.getInstance(getApplicationContext()).add(new TagData(
                        0,
                        tagNameEditText.getText().toString(),
                        false,
                        false,
                        false,
                        TagData.FilterMode.DEFAULT_ALL
                ));
                tagNameEditText.setText("");
                refreshTags();
                mySetResult(1);
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

    public static class TagRenameDialogFragment extends DialogFragment {
        private int selectedFilterMode;

        public static TagRenameDialogFragment newInstance(int tagIndex) {
            TagRenameDialogFragment pickerFragment = new TagRenameDialogFragment();

            Bundle bundle = new Bundle();
            bundle.putInt(TAG_INDEX_TAG, tagIndex);
            pickerFragment.setArguments(bundle);

            return pickerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final Bundle bundle = getArguments();
            final TagsActivity parent = (TagsActivity) getActivity();
            final TagData pressedTagData = parent.tags.get(bundle.getInt(TAG_INDEX_TAG));

            AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            LayoutInflater inflater = parent.getLayoutInflater();
            View view = inflater.inflate(R.layout.tag_rename, null);

            final EditText tagRenameEditText = (EditText) view.findViewById(R.id.tagRenameEditText);
            tagRenameEditText.setText(pressedTagData.name);

            final CheckBox tagRenameIsChecklist = (CheckBox) view.findViewById(R.id.tagRenameIsChecklist);
            tagRenameIsChecklist.setChecked(pressedTagData.isChecklist);
            tagRenameIsChecklist.setEnabled(!Utils.tagHasCheckedRecords(parent.getApplicationContext(), pressedTagData.id));

            final CheckBox tagRenameIsInbox = (CheckBox) view.findViewById(R.id.tagRenameIsInbox);
            tagRenameIsInbox.setChecked(pressedTagData.isInbox);

            final CheckBox tagRenameIsNotification = (CheckBox) view.findViewById(R.id.tagRenameIsNotification);
            tagRenameIsNotification.setChecked(pressedTagData.isNotification);

            final Spinner filterModeSpinner = (Spinner) view.findViewById(R.id.filterModeSpinner);
            filterModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedFilterMode = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedFilterMode = 0;
                }
            });
            ArrayAdapter<String> filterModeStringsAdapter = new ArrayAdapter<>(parent, android.R.layout.simple_spinner_item, TagData.ID_TO_FILTER_MODE_LABEL);
            filterModeStringsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filterModeSpinner.setAdapter(filterModeStringsAdapter);
            filterModeSpinner.setSelection(TagData.FILTER_MODE_TO_ID.get(pressedTagData.filterMode));

            builder.setView(view)
                    .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            pressedTagData.name = tagRenameEditText.getText().toString();
                            pressedTagData.isChecklist = tagRenameIsChecklist.isChecked();
                            pressedTagData.isInbox = tagRenameIsInbox.isChecked();
                            pressedTagData.isNotification = tagRenameIsNotification.isChecked();
                            pressedTagData.filterMode = TagData.ID_TO_FILTER_MODE.get(selectedFilterMode);

                            NotificationUtils.unregisterTagData(parent, pressedTagData);
                            DatabaseHelper.getInstance(parent.getApplicationContext()).update(pressedTagData);
                            NotificationUtils.registerTagData(parent, pressedTagData);

                            parent.refreshTags();
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

    public static class TagDeleteDialogFragment extends DialogFragment {
        public static TagDeleteDialogFragment newInstance(int tagIndex) {
            TagDeleteDialogFragment pickerFragment = new TagDeleteDialogFragment();

            Bundle bundle = new Bundle();
            bundle.putInt(TAG_INDEX_TAG, tagIndex);
            pickerFragment.setArguments(bundle);

            return pickerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final Bundle bundle = getArguments();
            final TagsActivity parent = (TagsActivity) getActivity();
            final TagData pressedTagData = parent.tags.get(bundle.getInt(TAG_INDEX_TAG));

            AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            builder.setMessage("Delete tag " + pressedTagData.name + " with all its records?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            NotificationUtils.unregisterTagData(parent, pressedTagData);
                            DatabaseHelper.getInstance(parent.getApplicationContext()).deleteTag(pressedTagData.id);
                            parent.refreshTags();
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
