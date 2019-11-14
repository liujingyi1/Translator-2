package com.letrans.android.translator.settings.common;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.letrans.android.translator.R;

import java.util.List;


public class SystemLanguageAdapter extends RecyclerView.Adapter<SystemLanguageAdapter.LanguageViewHolder> {
    private Context mContext;
    private List<FragmentSystemLanguages.LanguageItemInfo> mData;
    private LayoutInflater inflater;
    private OnItemClickListener mOnItemClickListener;
    private static final String TAG = "SystemLanguageAdapter";

    public SystemLanguageAdapter(Context context, List<FragmentSystemLanguages.LanguageItemInfo> data) {
        mData = data;
        mContext = context;
        inflater = LayoutInflater.from(context);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public void setData( List<FragmentSystemLanguages.LanguageItemInfo> data) {
        mData = data;
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        View view = inflater.inflate(R.layout.setting_system_language_item, parent, false);

        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final LanguageViewHolder holder, final int position) {
        Log.d(TAG, "mData.get(position) = " + mData.get(position));
        holder.textView.setText(mData.get(position).getLabel());
        Log.d(TAG, "onBindViewHolder: mData.get(position).isChecked()" + mData.get(position).isChecked());
        holder.imageView.setVisibility(mData.get(position).isChecked() ? View.VISIBLE : View.INVISIBLE);

        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: mData.size() = " + mData.size());
        return mData.size();
    }

    class LanguageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public LanguageViewHolder(View itemView) {
            super(itemView);
            Log.d(TAG, "LanguageViewHolder: ");
            imageView = (ImageView) itemView.findViewById(R.id.id_languages_status);
            textView = (TextView) itemView.findViewById(R.id.id_languages_title);
        }
    }

    interface OnItemClickListener {
        void onItemClick(int position);
    }
}
