package com.letrans.android.translator.settings.wifi;

import android.content.Context;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.letrans.android.translator.R;

import java.util.ArrayList;
import java.util.Collections;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.WifiHolder> {

    private final Context mContext;
    private ArrayList<AccessPoint> mAccessPointList = null;
    private boolean isConnected = false;

    private OnItemClickListener mOnItemClickListener;

    public WifiAdapter(Context context) {
        this.mContext = context;
        mAccessPointList = new ArrayList<>();
    }

    public Object getItem(int position) {
        return mAccessPointList.get(position);
    }

    @NonNull
    @Override
    public WifiHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.wifi_list_item,
                parent, false);
        return new WifiHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiHolder holder, int position) {
        holder.bindView(mAccessPointList.get(position), position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mAccessPointList.size();
    }

    public void setAccessPointList(ArrayList<AccessPoint> accessPointList) {
        mAccessPointList.clear();
        if (accessPointList != null) {
            mAccessPointList.addAll(accessPointList);
            sort();
        } else {
            notifyDataSetChanged();
        }
    }

    public void remove(int position) {
        mAccessPointList.remove(position);
        notifyDataSetChanged();
    }

    public ArrayList<AccessPoint> getAccessPointList() {
        return mAccessPointList;
    }

    public void sort() {
        if (mAccessPointList != null) {
            Collections.sort(mAccessPointList);
            notifyDataSetChanged();
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    class WifiHolder extends RecyclerView.ViewHolder {
        private TextView mTvTitle;
        private ImageView mIvSignal;
        private ImageView mWifiConnectedStatus;
        private ImageView mWifiDetailsArrow;
        private ImageView mLockStateView;
        private TextView mSummaryView;

        public WifiHolder(View itemView) {
            super(itemView);
            mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            mIvSignal = (ImageView) itemView.findViewById(R.id.iv_signal);
            mWifiConnectedStatus = (ImageView) itemView.findViewById(R.id.wifi_connected_status);
            mWifiDetailsArrow = (ImageView) itemView.findViewById(R.id.wifi_details_arrow);
            mLockStateView = (ImageView) itemView.findViewById(R.id.wifi_lock_state);
            mSummaryView = (TextView) itemView.findViewById(R.id.wifi_summary);
        }

        public void bindView(AccessPoint accessPoint, final int position) {
            mTvTitle.setText(accessPoint.ssid);
            NetworkInfo.DetailedState state = accessPoint.getState();
            if (state != null) {
                if ("CONNECTED".equals(state.toString())) {
                    maybeSetDetailsListener(this, position);
                    mWifiConnectedStatus.setVisibility(View.VISIBLE);
                    isConnected = true;
                    if (accessPoint.connectedNoInternet) {
                        mSummaryView.setText(accessPoint.summary);
                        mSummaryView.setVisibility(View.VISIBLE);
                    } else {
                        mSummaryView.setVisibility(View.GONE);
                    }
                } else {
                    maybeSetClickListener(this, position);
                    mWifiConnectedStatus.setVisibility(View.INVISIBLE);
                    isConnected = false;
                    mSummaryView.setText(accessPoint.summary);
                    mSummaryView.setVisibility(View.VISIBLE);
                }
            } else {
                maybeSetClickListener(this, position);
                mWifiConnectedStatus.setVisibility(View.INVISIBLE);
                isConnected = false;
                mSummaryView.setVisibility(View.GONE);
            }
            maybeSetLongClickListener(this, position);
            accessPoint.updateSecurityView(mLockStateView);
            accessPoint.setImageSignal(mIvSignal);
        }
    }

    private void maybeSetDetailsListener(WifiHolder holder, final int position) {
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onDetailsClick(position);
                }
            });
        }
    }

    private void maybeSetClickListener(WifiHolder holder, final int position) {
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(position);
                }
            });
        }
    }

    private void maybeSetLongClickListener(WifiHolder holder, final int position) {
        if (mOnItemClickListener != null) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickListener.onItemLongClick(position);
                    return true;
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onItemLongClick(int position);

        void onDetailsClick(int position);
    }
}
