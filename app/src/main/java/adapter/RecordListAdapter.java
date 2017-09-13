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
import database.GroupData;
import notification.NotificationUtils;
import ru.trjoxuvw.manualrecurrencetasks.RecordActivity;
import ru.trjoxuvw.manualrecurrencetasks.MainActivity;
import ru.trjoxuvw.manualrecurrencetasks.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import database.RecordData;
import utils.Utils;

public class RecordListAdapter extends BaseAdapter {
    private final MainActivity parentActivity;
    private ArrayList<RecordData> recordsList = new ArrayList<>();
    private final LayoutInflater mInflater;

    public RecordListAdapter(MainActivity parentActivity) {
        this.parentActivity = parentActivity;
        mInflater = LayoutInflater.from(parentActivity);
    }

    public void ResetList(ArrayList<RecordData> recordsList)
    {
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
                R.layout.record_checklist_item,
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

        holder.label.setText(record.label);
        holder.nextAppear.setText(SimpleDateFormat.getDateTimeInstance().format(new Date(record.nextAppear)));
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
}
