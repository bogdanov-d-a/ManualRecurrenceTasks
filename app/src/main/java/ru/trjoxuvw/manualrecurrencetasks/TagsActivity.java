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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import adapter.TagListAdapter;
import data.TagData;
import database.DatabaseHelper;
import notification.NotificationUtils;

public class TagsActivity extends AppCompatActivity {
    private ListView tagListView;
    private EditText tagNameEditText;

    private static final String RESULT_CODE_TAG = "RESULT_CODE_TAG";
    private int resultCode;

    private ArrayList<TagData> tags;
    private long pressedTagId;
    private String pressedTagName;

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
                pressedTagId = tags.get(position).id;
                pressedTagName = tags.get(position).name;
                TagRenameDialogFragment.newInstance(TagsActivity.this).show(getSupportFragmentManager(), "tagRenamer");
            }
        });

        tagListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                pressedTagId = tags.get(position).id;

                TagDeleteDialogFragment.newInstance(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NotificationUtils.unregisterTagRecords(TagsActivity.this, pressedTagId);
                        DatabaseHelper.getInstance(getApplicationContext()).deleteTag(pressedTagId);
                        refreshTags();
                        mySetResult(1);
                    }
                }).show(getSupportFragmentManager(), "tagDeleter");

                return true;
            }
        });

        refreshTags();

        tagNameEditText = (EditText) findViewById(R.id.tagNameEditText);
        assert tagNameEditText != null;

        Button addTagButton = (Button) findViewById(R.id.addTagButton);
        assert addTagButton != null;
        addTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseHelper.getInstance(getApplicationContext()).addTag(new TagData(0, tagNameEditText.getText().toString()));
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
        private TagsActivity parentActivity;
        private EditText tagRenameEditText;

        public static TagRenameDialogFragment newInstance(TagsActivity parentActivity) {
            TagRenameDialogFragment pickerFragment = new TagRenameDialogFragment();
            pickerFragment.parentActivity = parentActivity;
            return pickerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.tag_rename, null);

            tagRenameEditText = (EditText) view.findViewById(R.id.tagRenameEditText);
            tagRenameEditText.setText(parentActivity.pressedTagName);

            builder.setView(view)
                    .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            TagData newTag = new TagData(parentActivity.pressedTagId, tagRenameEditText.getText().toString());

                            NotificationUtils.unregisterTagRecords(parentActivity, parentActivity.pressedTagId);
                            DatabaseHelper.getInstance(parentActivity.getApplicationContext()).updateTag(newTag);
                            NotificationUtils.registerTagRecords(parentActivity, newTag);

                            parentActivity.refreshTags();
                            parentActivity.mySetResult(1);
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
        private DialogInterface.OnClickListener onDeleteClickListener;

        public static TagDeleteDialogFragment newInstance(DialogInterface.OnClickListener onDeleteClickListener) {
            TagDeleteDialogFragment pickerFragment = new TagDeleteDialogFragment();
            pickerFragment.onDeleteClickListener = onDeleteClickListener;
            return pickerFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Delete tag with all its records?")
                    .setPositiveButton("Delete", onDeleteClickListener)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }
}
