package adapter;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import database.DatabaseHelper;
import database.GroupData;
import database.RecordData;
import notification.NotificationUtils;
import ru.trjoxuvw.manualrecurrencetasks.MainActivity;
import ru.trjoxuvw.manualrecurrencetasks.R;
import ru.trjoxuvw.manualrecurrencetasks.RecordActivity;
import utils.Utils;

public class RecordListAdapter extends BaseAdapter {
    private final MainActivity parentActivity;
    private ArrayList<RecordData> recordsList = new ArrayList<>();
    private final LayoutInflater mInflater;

    public RecordListAdapter(MainActivity parentActivity) {
        this.parentActivity = parentActivity;
        mInflater = LayoutInflater.from(parentActivity);
    }

    public void ResetList(ArrayList<RecordData> recordsList) {
        this.recordsList = recordsList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return recordsList.size();
    }

    @Override
    public Object getItem(int position) {
        return recordsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private View createView() {
        View convertView = mInflater.inflate(
                R.layout.record_list_item,
                null
        );

        ViewHolder holder = new ViewHolder(
                (LinearLayout) convertView.findViewById(R.id.textLinearLayout),
                (TextView) convertView.findViewById(R.id.label),
                (TextView) convertView.findViewById(R.id.nextAppear),
                (CheckBox) convertView.findViewById(R.id.checkBox)
        );

        convertView.setTag(holder);

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = createView();
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();
        final RecordData record = (RecordData) getItem(position);

        holder.textLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(parentActivity, RecordActivity.class);
                intent.putExtra(RecordActivity.OPERATION, RecordActivity.OPERATION_UPDATE);
                intent.putExtra(RecordActivity.EDIT_RECORD_ID, record.id);
                parentActivity.startActivityForResult(intent, MainActivity.VIEW_RECORD_REQUEST);
            }
        });

        holder.textLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                DeleteRecordFragment.createAndShow(parentActivity.getSupportFragmentManager(), record.id);
                return true;
            }
        });

        holder.label.setText(record.label);
        holder.nextAppear.setText(Utils.formatDateTime(new Date(record.nextAppear)).getDateTime());
        holder.checkBox.setOnCheckedChangeListener(null);

        final ArrayList<GroupData> groupsList = parentActivity.getGroups();
        final boolean canBeChecked = Utils.recordCanBeChecked(groupsList, record.groupId);

        holder.checkBox.setEnabled(canBeChecked);
        holder.checkBox.setChecked(record.isChecked);

        if (canBeChecked) {
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    GroupData group = groupsList.get(Utils.getPositionById(groupsList, record.groupId));
                    NotificationUtils.unregisterGroup(parentActivity, group);

                    record.isChecked = isChecked;
                    DatabaseHelper.getInstance(parentActivity).update(record);

                    NotificationUtils.registerGroup(parentActivity, group);
                }
            });
        }

        return convertView;
    }

    private static class ViewHolder {
        public final LinearLayout textLinearLayout;
        public final TextView label;
        public final TextView nextAppear;
        public final CheckBox checkBox;

        public ViewHolder(LinearLayout textLinearLayout, TextView label, TextView nextAppear, CheckBox checkBox) {
            this.textLinearLayout = textLinearLayout;
            this.label = label;
            this.nextAppear = nextAppear;
            this.checkBox = checkBox;
        }
    }

    public static class DeleteRecordFragment extends DialogFragment {
        private static final String RECORD_INDEX_TAG = "RECORD_INDEX_TAG";

        public static void createAndShow(FragmentManager manager, long recordId) {
            final DeleteRecordFragment fragment = new DeleteRecordFragment();

            final Bundle bundle = new Bundle();
            bundle.putLong(RECORD_INDEX_TAG, recordId);
            fragment.setArguments(bundle);

            fragment.show(manager, "DeleteRecordFragment");
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            final MainActivity parent = (MainActivity) getActivity();
            final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            builder.setMessage("Delete record?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            final Bundle bundle = getArguments();
                            final long recordId = bundle.getLong(RECORD_INDEX_TAG);

                            final RecordData record = DatabaseHelper.getInstance(parent.getApplicationContext()).getRecord(recordId);
                            final ArrayList<GroupData> groupsList = parent.getGroups();
                            final GroupData recordGroup = groupsList.get(Utils.getPositionById(groupsList, record.groupId));

                            NotificationUtils.unregisterGroup(parent, recordGroup);

                            DatabaseHelper.getInstance(parent.getApplicationContext()).deleteRecord(record.id);
                            NotificationUtils.unregisterRecord(parent, recordGroup, record.id);

                            NotificationUtils.registerGroup(parent, recordGroup);
                            parent.refreshRecords();
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
