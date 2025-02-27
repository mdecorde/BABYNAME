package fr.hnit.babyname;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class OriginAdapter extends ArrayAdapter<Object> {
    private final OnChangeListener listener;
    public final ArrayList<String> origins;
    public final ArrayList<Boolean> checked;

    public interface OnChangeListener {
        void onSelectionChange();
    }

    public OriginAdapter(ArrayList<String> origins, Context mContext, OnChangeListener listener) {
        super(mContext, R.layout.item_origin, Collections.singletonList(origins));
        this.origins = origins;
        this.checked = new ArrayList<>(Collections.nCopies(origins.size(), Boolean.FALSE));
        this.listener = listener;
    }

    private static class ViewHolder {
        TextView txtName;
        CheckBox checkBox;
    }

    @Override
    public int getCount() {
        return origins.size();
    }

    @Override
    public String getItem(int position) {
        return origins.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_origin, parent, false);
        }

        TextView txtName = convertView.findViewById(R.id.txtName);
        CheckBox checkBox = convertView.findViewById(R.id.checkBox);

        txtName.setText(origins.get(position));
        checkBox.setChecked(checked.get(position));

        convertView.setOnClickListener(v -> {
            boolean newChecked = !checkBox.isChecked();
            checkBox.setChecked(newChecked);
            checked.set(position, newChecked);
            listener.onSelectionChange();
        });

        return convertView;
    }
}
