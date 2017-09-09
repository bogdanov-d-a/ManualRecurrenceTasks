package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import ru.trjoxuvw.manualrecurrencetasks.R;

import java.util.ArrayList;

import database.GroupData;

public class GroupListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private ArrayList<GroupData> groupsList = new ArrayList<>();

    public GroupListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public void ResetList(ArrayList<GroupData> groupsList)
    {
        this.groupsList = groupsList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return groupsList.size();
    }

    @Override
    public Object getItem(int position) {
        return groupsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.group_list_item, null);
            holder = new ViewHolder((TextView) convertView.findViewById(R.id.name));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final GroupData group = (GroupData) getItem(position);
        holder.name.setText(group.getLabel());

        return convertView;
    }

    private static class ViewHolder {
        public final TextView name;

        public ViewHolder(TextView name) {
            this.name = name;
        }
    }
}
