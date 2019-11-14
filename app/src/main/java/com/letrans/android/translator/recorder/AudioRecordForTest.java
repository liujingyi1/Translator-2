package com.letrans.android.translator.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.letrans.android.translator.mpush.TMPushService;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioRecordForTest implements IRecorder {
	private static final String TAG = "RTranslator/AudioRecordForTest";

	private static final int MSG_FINISH = 100;
	private static final int MSG_LEVEL = 101;

	private AudioRecord audioRecord;
	private boolean isRecord = false;
	private boolean isPrepared = false;

	private int bufferSizeInBytes = 0;
	private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
	private final static int AUDIO_SAMPLE_RATE = 16000;
	//声道 单声道
	private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
	//编码
	private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private int bitsPerSample = 16;

	private String tempAudioPath = "";
	private String wavAudioPath = "";


	private static AudioRecordForTest mInstance;

	private IRecorderListener mRecorderListener;

	int index = 0;
	private String pcmBasePath;
	private String pcmFileName;

	private AudioRecordForTest() {}

	private List<String> pcmPathList = new ArrayList<>();

	public synchronized static AudioRecordForTest getInstance() {
		if (mInstance == null)
			mInstance = new AudioRecordForTest();
		return mInstance;
	}

	private Handler H = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_FINISH: {
					if (null != mRecorderListener) {
						mRecorderListener.onRecordFinished();
					}
					break;
				}
				case MSG_LEVEL: {
					if (null != mRecorderListener) {
						mRecorderListener.onSoundLevelChanged(msg.arg1);
					}
					break;
				}
			}
		}
	};

	private static boolean isSdcardExit() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED))
			return true;
		else
			return false;
	}

	private void creatAudioRecord() {
		bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
				AUDIO_CHANNEL, AUDIO_ENCODING);
		audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE,
				AUDIO_CHANNEL, AUDIO_ENCODING,
				bufferSizeInBytes);
	}

	private static void deleteFile(String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
	}

	private static long getFileSize(String path) {
		File mFile = new File(path);
		if (!mFile.exists())
			return -1;
		return mFile.length();
	}

	public long getRecordFileSize() {
		return getFileSize(wavAudioPath);
	}

	class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			writeDateTOFile();
			H.sendEmptyMessage(MSG_FINISH);
		}
	}

	private void writeDateTOFile() {
		byte[] audiodata = new byte[bufferSizeInBytes];
		FileOutputStream fos = null;
		int readsize = 0;

		while (isRecord) {
			readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
			Logger.v(TAG, "index:" + index);
			Logger.v(TAG, "readsize = " + readsize);
			new Thread(new WritePcmThread(audiodata, readsize, index)).start();
			index++;
		}
	}

	class WritePcmThread implements Runnable {
		byte[] audiodata;
		int readsize;
		int index;

		public WritePcmThread(byte[] audiodata, int readsize, int index) {
			this.audiodata =audiodata;
			this.readsize = readsize;
			this.index =index;
		}
		@Override
		public void run() {
			writeToPcm(audiodata, readsize, index);
		}
	}

	private void writeToPcm(byte[] audiodata, int readsize, int index) {
		FileOutputStream fos = null;
		String tmpPcmFileName = pcmBasePath + "/" + pcmFileName + "_"  + index + ".pcm";
		Logger.v(TAG, "writeToPcm tmpPcmFileName:" + tmpPcmFileName);
		pcmPathList.add(tmpPcmFileName);
		try {
			File file = new File(tmpPcmFileName);
			if (file.exists()) {
				file.delete();
			}
			fos = new FileOutputStream(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Logger.v(TAG, "writeToPcm readsize = " + readsize);
		if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
			try {
				fos.write(audiodata);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			if (fos != null)
				fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.v(TAG, "writeToPcm index:" + index);
	}


	public void copyWaveFiles(List<String> inFilename, String outFilename, int start, int end) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = AUDIO_SAMPLE_RATE;
		int channels = (AUDIO_CHANNEL == AudioFormat.CHANNEL_IN_MONO) ? 1: 2;//GOOGLE STT need single channel
		long byteRate = bitsPerSample * AUDIO_SAMPLE_RATE * channels / 8;
		byte[] data = new byte[bufferSizeInBytes];

		try {
			File file = new File(outFilename);
			if (file.exists()) {
				file.delete();
			}
			out = new FileOutputStream(outFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for(int i = start; i < inFilename.size() && i < end; i++) {
			try {
				in = new FileInputStream(inFilename.get(i));
				totalAudioLen += in.getChannel().size();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {

			totalDataLen = totalAudioLen + 36;
			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);
			for(int i = start; i < inFilename.size() && i < end; i++) {
				in = new FileInputStream(inFilename.get(i));
				while (in.read(data) != -1) {
					out.write(data);
				}
				in.close();
			}

			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = AUDIO_SAMPLE_RATE;
		int channels = (AUDIO_CHANNEL == AudioFormat.CHANNEL_IN_MONO) ? 1: 2;//GOOGLE STT need single channel
		long byteRate = bitsPerSample * AUDIO_SAMPLE_RATE * channels / 8;
		byte[] data = new byte[bufferSizeInBytes];

		try {
			in = new FileInputStream(inFilename);
			try {
				File file = new File(outFilename);
				if (file.exists()) {
					file.delete();
				}
				out = new FileOutputStream(outFilename);
			} catch (Exception e) {
				e.printStackTrace();
			}

			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);
			while (in.read(data) != -1) {
				out.write(data);
			}
			out.write(-1);
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
									 long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (channels * bitsPerSample / 8); // block align
		header[33] = 0;
		header[34] = (byte)bitsPerSample; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}

	public List<String> getPcmPathList() {
		return pcmPathList;
	}

	@Override
	public void prepare(String basePath, String fileName) {
		pcmBasePath = basePath;
		pcmFileName = fileName;
		isPrepared = true;
		index = 0;
		pcmPathList.clear();
		File dirFile = new File(basePath);
		if (!dirFile.exists()) {
			Logger.v(TAG, "dir NOT exit");
			dirFile.mkdirs();
		}
		wavAudioPath = basePath + "/" + fileName + ".wav";
		tempAudioPath = basePath + "/" + fileName + ".pcm";
	}

	@Override
	public int startRecord() {
		if (isSdcardExit()) {
			if (isRecord) {
				return E_STATE_RECODING;
			} else if (!isPrepared) {
				return E_NOT_RREPARE;
			} else {
				if (audioRecord == null)
					creatAudioRecord();
				audioRecord.startRecording();
				// is recording
				isRecord = true;
				new Thread(new AudioRecordThread()).start();
				return SUCCESS;
			}
		} else {
			return E_NOSDCARD;
		}
	}

	@Override
	public void stopRecord() {
		if (audioRecord != null) {
			isRecord = false;
			isPrepared = false;
			audioRecord.stop();
			audioRecord.release();
			audioRecord = null;
		}
	}

	@Override
	public void cancel() {
		stopRecord();
		// delete file
		//deleteFile(wavAudioPath);
	}

	@Override
	public String getSoundPath() {
		return wavAudioPath;
	}

    @Override
    public void setOnRecorderFinishedListener(IRecorderListener listener) {
        mRecorderListener = listener;
    }

	@Override
	public void onDestroy() {

	}
}