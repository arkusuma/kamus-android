package com.grafian.kamus;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockActivity {

	final private static int EN_TO_ID = 0;
	final private static int ID_TO_EN = 1;

	private class Range {
		public Range(int first, int last) {
			this.first = first;
			this.last = last;
		}

		public int first;
		public int last;
	};

	final private Dictionary mDict = new Dictionary();
	private EditText mQuery;
	private ListView mResultView;
	private int mDictType;
	private ArrayList<Range> mResult = new ArrayList<Range>();
	private Menu mMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mQuery = (EditText) findViewById(R.id.query);
		mQuery.addTextChangedListener(onQueryChange);
		mQuery.setOnEditorActionListener(onEditorAction);

		mResultView = (ListView) findViewById(R.id.result_list);
		mResultView.setAdapter(new MyAdapter());

		findViewById(R.id.clear).setOnClickListener(onClear);

		loadDict(EN_TO_ID);
	}

	final private OnEditorActionListener onEditorAction = new OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				in.hideSoftInputFromWindow(v.getWindowToken(), 0);
				return true;
			}
			return false;
		}
	};

	final private OnClickListener onClear = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mQuery.getText().clear();
			mQuery.requestFocus();
			search("");
		}
	};

	private void onShare() {
		Resources res = getResources();
		String share = res.getString(R.string.share_via);
		String subject = res.getString(R.string.share_subject);
		String body = res.getString(R.string.share_body);

		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(android.content.Intent.EXTRA_TEXT, body);
		startActivity(Intent.createChooser(intent, share));

	}

	private void onAbout() {
		View about = getLayoutInflater().inflate(R.layout.about, null);
		new AlertDialog.Builder(MainActivity.this)
				.setCustomTitle(about)
				.setPositiveButton(R.string.visit_web, onAboutDialog)
				.setNeutralButton(R.string.more_apps, onAboutDialog)
				.setNegativeButton(R.string.okay, onAboutDialog)
				.setCancelable(true)
				.show();
	}

	private final DialogInterface.OnClickListener onAboutDialog = new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {
			Intent intent;
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://grafian.com"));
				startActivity(intent);
				break;
			case DialogInterface.BUTTON_NEUTRAL:
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/search?q=pub:Grafian%20Software%20Crafter"));
				startActivity(intent);
				break;
			}
			dialog.dismiss();
		}
	};

	final private TextWatcher onQueryChange = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			search(s.toString());
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		mMenu = menu;
		updateDictType();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_dict_type:
			if (mDictType == EN_TO_ID) {
				loadDict(ID_TO_EN);
			} else {
				loadDict(EN_TO_ID);
			}
			updateDictType();
			search(mQuery.getText().toString());
			break;
		case R.id.menu_about:
			onAbout();
			break;
		case R.id.menu_share:
			onShare();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void loadDict(int type) {
		mDictType = type;
		if (mDictType == EN_TO_ID) {
			mDict.load(this, R.raw.en_to_id);
		} else {
			mDict.load(this, R.raw.id_to_en);
		}
	}

	private void updateDictType() {
		MenuItem item = mMenu.findItem(R.id.menu_dict_type);
		if (mDictType == EN_TO_ID) {
			item.setTitle(R.string.menu_en_to_id);
		} else {
			item.setTitle(R.string.menu_id_to_en);
		}
	}

	@SuppressLint("DefaultLocale")
	private void search(String s) {
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
				convertView = getLayoutInflater().inflate(R.layout.item_result, null);
			}

			Dictionary.Entry e = (Dictionary.Entry) getItem(position);
			TextView key = (TextView) convertView.findViewById(R.id.key);
			TextView val = (TextView) convertView.findViewById(R.id.val);
			key.setText(e.key);
			val.setText(e.val);
			return convertView;
		}
	}
}
