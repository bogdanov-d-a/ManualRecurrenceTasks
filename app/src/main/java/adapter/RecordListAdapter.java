package adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import database.DatabaseHelper;
import ru.trjoxuvw.manualrecurrencetasks.AddRecordActivity;
import ru.trjoxuvw.manualrecurrencetasks.MainActivity;
import ru.trjoxuvw.manualrecurrencetasks.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import database.RecordData;

public class RecordListAdapter extends BaseAdapter {
    private final MainActivity parentActivity;
    private final ArrayList<RecordData> recordsList;
    private final boolean showCheckboxes;
    private final LayoutInflater mInflater;

    public RecordListAdapter(MainActivity parentActivity, ArrayList<RecordData> recordsList, boolean showCheckboxes) {
        this.parentActivity = parentActivity;
        this.recordsList = recordsList;
        this.showCheckboxes = showCheckboxes;
        mInflater = LayoutInflater.from(parentActivity);
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(
                    showCheckboxes ? R.layout.record_checklist_item : R.layout.record_list_item,
                    null
            );

            holder = new ViewHolder(
                    (LinearLayout) convertView.findViewById(R.id.textLinearLayout),
                    (TextView) convertView.findViewById(R.id.label),
                    (TextView) convertView.findViewById(R.id.nextAppear),
                    showCheckboxes ? (CheckBox) convertView.findViewById(R.id.checkBox) : null
            );

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final RecordData record = (RecordData) getItem(position);

        holder.textLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(parentActivity, AddRecordActivity.class);
                intent.putExtra(AddRecordActivity.OPERATION, AddRecordActivity.OPERATION_EDIT);
                intent.putExtra(AddRecordActivity.EDIT_RECORD_ID, record.id);
                parentActivity.startActivityForResult(intent, MainActivity.ADD_RECORD_REQUEST);
            }
        });

        holder.label.setText(record.label);
        holder.nextAppear.setText(
                SimpleDateFormat.getDateTimeInstance().format(new Date(record.nextAppear)) +
                (record.needNotice ? " (notification)" : "")
        );

        if (holder.checkBox != null) {
            holder.checkBox.setChecked(record.isChecked);
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    record.isChecked = isChecked;
                    DatabaseHelper.getInstance(parentActivity).update(record);
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
}
