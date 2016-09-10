package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import ru.trjoxuvw.manualrecurrencetasks.R;

import java.util.ArrayList;

import database.TagData;

public class TagListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private ArrayList<TagData> tagsList = new ArrayList<>();

    public TagListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public void ResetList(ArrayList<TagData> tagsList)
    {
        this.tagsList = tagsList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tagsList.size();
    }

    @Override
    public Object getItem(int position) {
        return tagsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tag_list_item, null);
            holder = new ViewHolder((TextView) convertView.findViewById(R.id.name));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final TagData task = (TagData) getItem(position);
        holder.name.setText(task.name);

        return convertView;
    }

    private static class ViewHolder {
        public final TextView name;

        public ViewHolder(TextView name) {
            this.name = name;
        }
    }
}
