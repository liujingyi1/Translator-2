package com.letrans.android.translator.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.letrans.android.translator.R;

import java.util.ArrayList;

public class InfoAdapter extends RecyclerView.Adapter<InfoAdapter.MyViewHolder>
        implements View.OnClickListener {
    private Context mContext;
    private LayoutInflater mInflater;

    private ArrayList<InfoObject> mList;

    public OnItemClickListener mItemClickListener;

    public InfoAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mList = new ArrayList<>();
    }

    public void addInfoObject(InfoObject infoObject) {
        mList.add(infoObject);
        notifyDataSetChanged();
    }

    public void addInfoObjects(ArrayList<InfoObject> infoObjects) {
        mList.clear();
        if (infoObjects != null) {
            mList.addAll(infoObjects);
        }
        notifyDataSetChanged();
    }

    public void clearInfoObjects() {
        mList.clear();
        notifyDataSetChanged();
    }

    public ArrayList<InfoObject> getDataList() {
        return mList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.key_value_item, parent, false);
        view.setOnClickListener(this);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bindView(mList.get(position));
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mLabel;
        TextView mValue;

        public MyViewHolder(View itemView) {
            super(itemView);
            mLabel = (TextView) itemView.findViewById(R.id.label);
            mValue = (TextView) itemView.findViewById(R.id.value);
        }

        public void bindView(InfoObject infoObject) {
            mLabel.setText(infoObject.label);
            mValue.setText(infoObject.value);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setItemClickListener(OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    @Override
    public void onClick(View view) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemClick((Integer) view.getTag());
        }
    }
}