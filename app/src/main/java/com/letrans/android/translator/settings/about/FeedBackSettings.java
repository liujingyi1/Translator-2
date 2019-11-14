package com.letrans.android.translator.settings.about;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import java.io.File;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.letrans.android.translator.R;
import com.letrans.android.translator.recorder.AudioRecordFunc;
import com.letrans.android.translator.recorder.IRecorderListener;
import com.letrans.android.translator.settings.SettingsActivity;
import com.letrans.android.translator.settings.TouchEventController;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.utils.ToastUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedBackSettings extends Fragment {
    private static final String TAG = "RTranslator/FeedBackSettings";
    private Context mContext;
    private RecyclerView feedbackRecyclerView;
    private ArrayList<ItemInfo> infoList = new ArrayList<>();
    private String[] data;
    private static final int TYPE_TOP = 0;
    private static final int TYPE_FEEDBACK_TYPE = 1;
    private static final int TYPE_DESCRIPTION = 2;
    private static final int TYPE_PHONE = 3;
    private static final int TYPE_END = 4;
    private static final int TYPE_DESCRIPTION_START = 5;
    private static final int TYPE_DESCRIPTION_FINISH = 6;
    private static final int FEEDBACK_SUCCEEDED = 0;
    private static final int FEEDBACK_FAILED = 1;
    private static final int FEEDBACK_TIMEOUT = 2;
    FeedBackAdapter adapter;
    String phoneNumber;
    String description = "";
    private ProgressDialog mProgressDialog;
    private FeedBackManager feedbackManager;
    //private ISTT mSTT;
    private AudioRecordFunc mAudioRecordFunc;
    private StringBuilder sttString = new StringBuilder();
    private VoiceButton voiceButton;
    private TouchEventController mTouchEventControler;
    private EditText editText;
    private String mWavFile = Environment.getExternalStorageDirectory() + "/feedback/feedback.wav";
    File mFile;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        mContext = getContext();
        mAudioRecordFunc = AudioRecordFunc.getInstance();
        View v = inflater.inflate(R.layout.setting_feedback, null);
        data = getResources().getStringArray(R.array.feedback_info);
        infoList.clear();
        for (String label : data) {
            infoList.add(new ItemInfo(label, false));
        }
        initViews(v);
        //initSTT((Activity) mContext);
        feedbackManager = new FeedBackManager();
        mTouchEventControler = new TouchEventController((SettingsActivity) getActivity());
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTouchEventControler.onDestroyView();
    }

    private void initViews(View v) {
        feedbackRecyclerView = (RecyclerView) v.findViewById(R.id.feedback_recycler_view);
        feedbackRecyclerView.setLayoutManager(new LinearLayoutManager(mContext,
                LinearLayoutManager.VERTICAL, false));
        adapter = new FeedBackAdapter(mContext, infoList);
        feedbackRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(OnItemClickListener);
    }

    /*private void initSTT(Activity activity) {
        mSTT = STTFactory.getSTT(activity, AppContext.BAIDU);
        if (mSTT != null) {
            mSTT.setSTTFinishedListener(mSTTFinishedListener);
            mSTT.setSTTVoiceLevelListener(mSTTVoiceLevelListener);
            mSTT.setLanguageCode("zh-CN");
        }
    }

    ISTTFinishedListener mSTTFinishedListener = new ISTTFinishedListener() {
        @Override
        public void onSTTFinish(ISTT.FinalResponseStatus status, String text) {
            Logger.v(TAG, "onSTTFinish, status=" + status + ", text=" + text);

            if (ISTT.FinalResponseStatus.OK == status) {
                sttString.append(text);

            } else if (ISTT.FinalResponseStatus.NotReceived == status) {
                sttString.append(text);
            } else if (ISTT.FinalResponseStatus.Timeout == status) {
                sttString.append("");
            } else if (ISTT.FinalResponseStatus.Finished == status) {
                String stt = sttString.toString();
                if (voiceButton != null) {
                    voiceButton.dismissRecordingDialog();
                }

                int index = editText.getSelectionStart();
                editText.getText().insert(index, stt);
                if (editText.getText() != null) {
                    editText.setSelection(editText.getText().length());
                }
                //adapter.setData(infoList, phoneNumber, description);
                //adapter.notifyItemRangeChanged(8, 1);
            } else if (ISTT.FinalResponseStatus.Error == status) {
                if (voiceButton != null) {
                    voiceButton.dismissRecordingDialog();
                }
            }
        }
    };

    ISTTVoiceLevelListener mSTTVoiceLevelListener = new ISTTVoiceLevelListener() {
        @Override
        public void updateVoiceLevel(int level) {
            if (voiceButton != null) {
                voiceButton.updateVoiceLevel(level);
            }
        }
    };*/

    private OnItemClickListener OnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(int type, int position) {
            Logger.d(TAG, "onItemClick type : " + type + " position : " + position);
            switch (type) {
                case TYPE_FEEDBACK_TYPE: {
                    saveTypeData(position);
                    break;
                }
                case TYPE_END: {
                    submit();
                    break;
                }

                default:
                    break;
            }
        }

        @Override
        public void onTextChanged(int type, String text) {
            Logger.d(TAG, "onTextChanged type : " + type + " text : " + text);
            switch (type) {
                case TYPE_DESCRIPTION: {
                    saveTextDescription(text);
                    break;
                }
                case TYPE_PHONE: {
                    savePhoneNumber(text);
                    break;
                }
                default:
                    break;
            }
        }

        /*@Override
        public void onVoiceItemClick(int type, int position, FeedBackAdapter.DescriptionViewHolder holder) {
            switch (type) {
                case TYPE_DESCRIPTION_START: {
                    getVoiceDescription(holder.voice_view, holder.description_text);
                    break;
                }
                case TYPE_DESCRIPTION_FINISH: {
                    if (mSTT != null) {
                        mSTT.stopWithMicrophone();
                    }
                    break;
                }
            }

        }*/
    };

    private void saveTypeData(int position) {
        if (infoList.size() == 0) return;
        for (int i = 0; i < infoList.size(); i++) {
            if (i == position) {
                Logger.d(TAG, "saveTypeData position : " + position);
                infoList.get(i).setChecked(true);
            } else {
                infoList.get(i).setChecked(false);
            }
        }
        adapter.setData(infoList, phoneNumber, description);
        adapter.notifyItemRangeChanged(1, infoList.size(), 0);
    }

    private void savePhoneNumber(String phone) {
        phoneNumber = phone;
    }

    private void saveTextDescription(String text) {
        description = text;
    }


    /*private void getVoiceDescription(VoiceButton button, EditText edit) {
        if (!NetUtil.isNetworkConnected(getContext())) {
            ToastUtils.showLong(R.string.feedback_no_network);
            return;
        }
        voiceButton = button;
        editText = edit;
        sttString = new StringBuilder();
        if (mSTT != null) {
            mSTT.startWithMicrophone(AppContext.SOUND_ABSOLUTE_FILE_DIR + "/Temp/feedback.wav");
        }
    }*/

    private String getTypeLabel() {
        if (infoList.size() == 0) return null;
        for (int i = 0; i < infoList.size(); i++) {
            if (infoList.get(i).isChecked()) {
                return infoList.get(i).getLabel();
            }
        }
        return null;
    }

    private void submit() {
        String label = getTypeLabel();
        boolean fileExists = mFile!=null && mFile.exists();
        if(!fileExists) {
            mFile = null;
        }
        if (TextUtils.isEmpty(label)) {
            ToastUtils.showLong(R.string.feedback_type);
            return;
        }
        if ((!fileExists && TextUtils.isEmpty(description)) || TextUtils.isEmpty(phoneNumber)) {
            ToastUtils.showLong(R.string.feedback_description_phone);
            return;
        }
        if (!isPhoneNumber(String.valueOf(phoneNumber))) {
            ToastUtils.showLong(R.string.feedback_phone_error);
            return;
        }
        commitUserFeedback(label, description, phoneNumber);
    }

    public boolean isPhoneNumber(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (TextUtils.isEmpty(str) || str.length() < 7) {
            return false;
        }
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    private void commitUserFeedback(final String label, final String description, final String phone) {
        if (!NetUtil.isNetworkConnected(getContext())) {
            ToastUtils.showLong(R.string.feedback_no_network);
            return;
        }
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setTitle(R.string.app_name);
        mProgressDialog.setMessage(getString(R.string.feedback_pro_message));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        new Thread() {
            @Override
            public void run() {
                feedbackManager.commitUserFeedbackToService(feedbackHandler, label, description, phone, mFile);
            }
        }.start();
    }

    Handler feedbackHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FEEDBACK_SUCCEEDED: {
                    showFeedbackToast(R.string.feedback_succeed);
                    break;
                }
                case FEEDBACK_FAILED: {
                    showFeedbackToast(R.string.feedback_failed);
                    break;
                }
                case FEEDBACK_TIMEOUT: {
                    showFeedbackToast(R.string.feedback_timeout);
                    break;
                }
            }
        }
    };

    private void showFeedbackToast(int id) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        ToastUtils.showLong(id);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        /*if (mSTT != null) {
            mSTT.onDestroy();
        }*/
    }

    public class FeedBackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList<ItemInfo> data;
        private LayoutInflater inflater;
        OnItemClickListener listener;
        private CharSequence phoneNumber, description;

        public FeedBackAdapter(Context context, ArrayList<ItemInfo> data) {
            this.data = data;

            inflater = LayoutInflater.from(context);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_TOP: {
                    View view = inflater.inflate(R.layout.feed_header, parent, false);
                    return new TypeTopHolder(view);
                }
                case TYPE_FEEDBACK_TYPE: {
                    return new TypeViewHolder(inflater.inflate(R.layout.list_feedback_item, parent, false));
                }
                case TYPE_DESCRIPTION: {
                    return new DescriptionViewHolder(inflater.inflate(R.layout.feedback_discription, parent, false));
                }
                case TYPE_PHONE: {
                    return new PhoneViewHolder(inflater.inflate(R.layout.feedback_phone, parent, false));
                }
                case TYPE_END: {
                    return new EndViewHolder(inflater.inflate(R.layout.feedback_submit, parent, false));
                }
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (data.size() == 0) return;
            if (holder instanceof TypeTopHolder) {
            } else if (holder instanceof DescriptionViewHolder) {
                initDescriptionViewHolder((DescriptionViewHolder) holder, position);
            } else if (holder instanceof TypeViewHolder) {
                initTypeViewHolder((TypeViewHolder) holder, position);
            } else if (holder instanceof PhoneViewHolder) {
                initPhoneViewHolder((PhoneViewHolder) holder, position);
            } else if (holder instanceof EndViewHolder) {
                initEndViewHolder((EndViewHolder) holder, position);
            }
        }

        private void initPhoneViewHolder(final PhoneViewHolder holder, int position) {
            if (!TextUtils.isEmpty(phoneNumber)) {
                holder.phone_text.setText(phoneNumber);
            }
            holder.phone_text.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (listener != null) {
                        listener.onTextChanged(TYPE_PHONE, String.valueOf(holder.phone_text.getText()));
                    }
                }
            });
        }

        private void initEndViewHolder(final EndViewHolder holder, final int position) {
            holder.submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(TYPE_END, position);
                    }
                }
            });
        }

        private void initDescriptionViewHolder(final DescriptionViewHolder holder, final int position) {
            if (!TextUtils.isEmpty(description)) {
                holder.description_text.setText(description);
            }

            holder.voice_view.setAudioRecorderStateListener(new VoiceButton.onAudioRecorderStateListener() {
                @Override
                public void onFinish(boolean isToShort, int stats) {
                    Logger.i(TAG, "AudioRecorderStateListener - onFinish");
                    holder.voice_view.dismissRecordingDialog();
                    if (isToShort) {
                        ToastUtils.showLong(R.string.recoder_too_short);
                        mAudioRecordFunc.cancel();
                        return;
                    }
                    mAudioRecordFunc.stopRecord();
                }

                @Override
                public void onStart() {
                    Logger.i(TAG, "AudioRecorderStateListener - onStart");
                    mAudioRecordFunc.prepare(Environment.getExternalStorageDirectory() + "/feedback","feedback");
                    mAudioRecordFunc.setOnRecorderFinishedListener(new IRecorderListener() {
                        @Override
                        public void onRecordFinished() {
                            mFile = new File(mWavFile);
                            if(mFile.exists()){
                                holder.voice_view.setVisibility(View.INVISIBLE);
                                holder.delete_view.setVisibility(View.VISIBLE);
                                mFile = null;
                            }
                        }

                        @Override
                        public void onSoundLevelChanged(int level) {
                            holder.voice_view.updateVoiceLevel(level / 2500 + 1);
                        }
                    });
                    int err = mAudioRecordFunc.startRecord();
                    if(err != mAudioRecordFunc.SUCCESS){
                        Logger.e(TAG, "startRecord error = "+err);
                    }
                }
            });
            holder.description_text.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (listener != null) {
                        listener.onTextChanged(TYPE_DESCRIPTION, String.valueOf(holder.description_text.getText()));
                    }
                }
            });
        }

        private void initTypeViewHolder(final TypeViewHolder holder, int position) {
            position = position - 1;
            holder.textView.setText(data.get(position).getLabel());
            holder.imageView.setVisibility(data.get(position).isChecked() ? View.VISIBLE : View.INVISIBLE);
            if (data.size() == position + 1) {
                holder.view.setVisibility(View.GONE);
            } else {
                holder.view.setVisibility(View.VISIBLE);
            }
            final int finalPosition = position;
            holder.ll_feedback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(TYPE_FEEDBACK_TYPE, finalPosition);
                    }
                }
            });
        }


        @Override
        public int getItemCount() {
            return data.size() + 4;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_TOP;
            } else if (position < data.size() + 1) {
                return TYPE_FEEDBACK_TYPE;
            } else if (position < data.size() + 2) {
                return TYPE_DESCRIPTION;
            } else if (position < data.size() + 3) {
                return TYPE_PHONE;
            } else {
                return TYPE_END;
            }

        }

        public void setData(ArrayList<ItemInfo> infoList, CharSequence phoneNumber, CharSequence description) {
            this.data = infoList;
            this.phoneNumber = phoneNumber;
            this.description = description;
        }


        class TypeViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textView;
            LinearLayout ll_feedback;
            View view;

            public TypeViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.feedback_view);
                textView = (TextView) itemView.findViewById(R.id.feedback_textView);
                ll_feedback = (LinearLayout) itemView.findViewById(R.id.ll_feedback);
                view = itemView.findViewById(R.id.feedback_decoration_view);
            }
        }

        private class TypeTopHolder extends RecyclerView.ViewHolder {
            public TypeTopHolder(View view) {
                super(view);
            }
        }

        private class DescriptionViewHolder extends RecyclerView.ViewHolder {
            VoiceButton voice_view;
            EditText description_text;
            ImageView delete_view;

            public DescriptionViewHolder(View view) {
                super(view);
                description_text = (EditText) view.findViewById(R.id.feedback_description_text);
                voice_view = (VoiceButton) view.findViewById(R.id.feedback_voice_view);
                mTouchEventControler.addEditText(description_text);
                delete_view = (ImageView)view.findViewById(R.id.feedback_voice_delete);
                delete_view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mFile !=null && mFile.exists()){
                            mFile.delete();
                        }
                        voice_view.setVisibility(View.VISIBLE);
                        delete_view.setVisibility(View.INVISIBLE);
                    }
                });
                //mTouchEventControler.addEditText(voice_view);
            }
        }

        private class PhoneViewHolder extends RecyclerView.ViewHolder {
            EditText phone_text;

            public PhoneViewHolder(View view) {
                super(view);
                phone_text = (EditText) view.findViewById(R.id.feedback_phone_text);
                mTouchEventControler.addEditText(phone_text);
            }
        }

        private class EndViewHolder extends RecyclerView.ViewHolder {
            TextView submitButton;

            public EndViewHolder(View view) {
                super(view);
                submitButton = (TextView) view.findViewById(R.id.feedback_submit_button);
            }
        }

    }

    interface OnItemClickListener {
        void onItemClick(int type, int position);

        void onTextChanged(int type, String text);

        //void onVoiceItemClick(int typeDescriptionFinish, int position, FeedBackAdapter.DescriptionViewHolder holder);
    }

    private class ItemInfo {
        String label;
        boolean checked;

        ItemInfo(String label, boolean checked) {
            this.checked = checked;
            this.label = label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }

        public boolean isChecked() {
            return this.checked;
        }
    }
}
