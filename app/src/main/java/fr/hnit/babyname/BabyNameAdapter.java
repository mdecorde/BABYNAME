package fr.hnit.babyname;
/*
The babyname app is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation,
either version 2 of the License, or (at your option) any
later version.

The babyname app is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General
Public License along with the TXM platform. If not, see
http://www.gnu.org/licenses
 */
/**
 * Created by mdecorde on 04/06/16.
 */
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class BabyNameAdapter extends ArrayAdapter<BabyNameProject>
{
    private final MainActivity context;
    private final ArrayList<BabyNameProject> itemsArrayList;

    public BabyNameAdapter(Context context, ArrayList<BabyNameProject> itemsArrayList) {
        super(context, R.layout.item_project, itemsArrayList);

        this.context = (MainActivity)context;
        this.itemsArrayList = itemsArrayList;
    }

    @Override
    public View getView(int position, final View convertView, ViewGroup parent) {
        final BabyNameProject project = itemsArrayList.get(position);

        // 1. Create inflater
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 2. Get rowView from inflater
        View rowView = inflater.inflate(R.layout.item_project, parent, false);

        // 3. Get the two text view from the rowView
        TextView text = rowView.findViewById(R.id.list_text);
        ImageButton goButton = rowView.findViewById(R.id.list_go);
        goButton.setOnClickListener(view -> context.doFindName(project));
        ImageButton resetButton = rowView.findViewById(R.id.list_reset);
        resetButton.setOnClickListener(view -> context.doResetBaby(project));
        ImageButton topButton = rowView.findViewById(R.id.list_top);
        topButton.setOnClickListener(view -> context.doShowTop10(project));
        ImageButton deleteButton = rowView.findViewById(R.id.list_delete);
        deleteButton.setOnClickListener(view -> context.doDeleteBaby(project));

        // 4. Set the text for textView
        text.setText(context.projectToString(project));

        // 5. return rowView
        return rowView;
    }
}
