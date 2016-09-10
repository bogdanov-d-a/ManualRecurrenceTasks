package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import ru.trjoxuvw.manualrecurrencetasks.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import database.RecordData;

public class RecordListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private ArrayList<RecordData> recordsList = new ArrayList<>();

    public RecordListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.record_list_item, null);
            holder = new ViewHolder((TextView) convertView.findViewById(R.id.label), (TextView) convertView.findViewById(R.id.nextAppear));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final RecordData record = (RecordData) getItem(position);
        holder.label.setText(record.label);
        holder.nextAppear.setText(
                SimpleDateFormat.getDateTimeInstance().format(new Date(record.nextAppear)) +
                (record.needNotice ? " (notification)" : "")
        );

        return convertView;
    }

    private static class ViewHolder {
        public final TextView label;
        public final TextView nextAppear;

        public ViewHolder(TextView label, TextView nextAppear) {
            this.label = label;
            this.nextAppear = nextAppear;
        }
    }
}
