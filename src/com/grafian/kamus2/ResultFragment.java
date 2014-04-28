package com.grafian.kamus2;

import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ResultFragment extends Fragment {

	private class Range {
		public Range(int first, int last) {
			this.first = first;
			this.last = last;
		}

		public int first;
		public int last;
	};

	private ListView mResultView;
	private ArrayList<Range> mResult = new ArrayList<Range>();
	private Dictionary mDict = new Dictionary();
	private TextToSpeech mTts;
	private int mType;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mResultView = (ListView) inflater.inflate(R.layout.fragment_result, container, false);
		mResultView.setAdapter(new MyAdapter());
		Bundle args = getArguments();
		mType = args.getInt("type");
		if (mType == MainActivity.EN_TO_ID) {
			mDict.load(getActivity(), R.raw.en_to_id);
		} else {
			mDict.load(getActivity(), R.raw.id_to_en);
		}
		return mResultView;
	}

	@Override
	public void onPause() {
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		if (mType == MainActivity.EN_TO_ID) {
			mTts = new TextToSpeech(getActivity(), new OnInitListener() {
				@Override
				public void onInit(int status) {
					if (status == TextToSpeech.ERROR) {
						mTts = null;
					} else {
						mTts.setLanguage(Locale.US);
					}
				}
			});
		}
		super.onResume();
	}

	@SuppressLint("DefaultLocale")
	public void translate(String s) {
		if (mResultView == null) {
			return;
		}

		StringTokenizer tokens = new StringTokenizer(s, " ");
		ArrayList<String> used = new ArrayList<String>();
		mResult.clear();
		while (tokens.hasMoreTokens()) {
			String word = tokens.nextToken().toLowerCase();
			if (word.length() > 0 && !used.contains(word)) {
				used.add(word);
				int first = mDict.searchFirst(word);
				if (first != -1) {
					int last = mDict.searchLast(word);
					mResult.add(new Range(first, last));
				} else {
					first = mDict.searchNearest(word);
					if (first != -1) {
						mResult.add(new Range(first, first));
					}
				}
			}
		}
		if (used.size() > 1) {
			for (int i = 0; i < mResult.size(); i++) {
				Range range = mResult.get(i);
				range.last = range.first;
			}
		}
		((MyAdapter) mResultView.getAdapter()).notifyDataSetChanged();
	}

	private class MyAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			int count = 0;
			for (int i = 0; i < mResult.size(); i++) {
				Range range = mResult.get(i);
				count += range.last - range.first + 1;
			}
			return count;
		}

		@Override
		public Object getItem(int position) {
			for (int i = 0; i < mResult.size(); i++) {
				Range range = mResult.get(i);
				int count = range.last - range.first + 1;
				if (position < count) {
					return mDict.get(range.first + position);
				}
				position -= count;
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = View.inflate(getActivity(), R.layout.item_result, null);
			}

			final Dictionary.Entry e = (Dictionary.Entry) getItem(position);
			TextView key = (TextView) convertView.findViewById(R.id.key);
			TextView val = (TextView) convertView.findViewById(R.id.val);
			Button tts = (Button) convertView.findViewById(R.id.tts);
			if (mType == MainActivity.ID_TO_EN || mTts == null) {
				tts.setVisibility(View.GONE);
			} else {
				tts.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mTts.speak(e.key, TextToSpeech.QUEUE_ADD, null);
					}
				});
			}
			key.setText(e.key);
			val.setText(e.val);
			return convertView;
		}
	}
}
