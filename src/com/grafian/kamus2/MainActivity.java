package com.grafian.kamus2;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.appbrain.AppBrain;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.millennialmedia.android.MMSDK;

public class MainActivity extends ActionBarActivity {

	final public static int EN_TO_ID = 0;
	final public static int ID_TO_EN = 1;

	private AdView mAdView;
	private EditText mQuery;
	private ViewPager mViewPager;
	private ArrayList<ResultFragment> mFragments;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		AppBrain.init(this);

		mQuery = (EditText) findViewById(R.id.query);
		mQuery.addTextChangedListener(onQueryChange);
		mQuery.setOnEditorActionListener(onEditorAction);

		mFragments = new ArrayList<ResultFragment>();
		for (int i = EN_TO_ID; i <= ID_TO_EN; i++) {
			ResultFragment fragment = new ResultFragment();
			Bundle args = new Bundle();
			args.putInt("type", i);
			fragment.setArguments(args);
			mFragments.add(fragment);
		}

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(new ResultFragmentAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (position == 0) {
					getSupportActionBar().setSubtitle(R.string.en_to_id_long);
				} else {
					getSupportActionBar().setSubtitle(R.string.id_to_en_long);
				}
			}
		});

		getSupportActionBar().setSubtitle(R.string.en_to_id_long);
		forceOverflowMenu();
		setupAd();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdView != null) {
			mAdView.resume();
		}
	}

	@Override
	public void onPause() {
		if (mAdView != null) {
			mAdView.pause();
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		if (mAdView != null) {
			mAdView.destroy();
		}
		super.onDestroy();
	}

	private void setupAd() {
		MMSDK.initialize(this);
		mAdView = (AdView) findViewById(R.id.ad);
		mAdView.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				mAdView.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAdFailedToLoad(int errorCode) {
				mAdView.setVisibility(View.GONE);
			}
		});
		AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
				.build();
		mAdView.loadAd(adRequest);
	}

	private void forceOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
		}
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

	private void onShare() {
		Resources res = getResources();
		String share = res.getString(R.string.share_via);
		String subject = res.getString(R.string.share_subject);
		String body = res.getString(R.string.share_body);
		body = String.format(body, getPackageName());

		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(android.content.Intent.EXTRA_TEXT, body);
		startActivity(Intent.createChooser(intent, share));

	}

	private void onAbout() {
		View about = getLayoutInflater().inflate(R.layout.about, null);
		TextView title = (TextView) about.findViewById(R.id.app_title);
		String version;
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = info.versionName;
		} catch (NameNotFoundException e) {
			version = "";
		}
		title.setText(title.getText().toString() + " " + version);
		new AlertDialog.Builder(MainActivity.this)
				.setCustomTitle(about)
				.setPositiveButton(R.string.more_apps, onMoreApps)
				.setNegativeButton(R.string.okay, onClose)
				.setCancelable(true)
				.show();
	}

	@Override
	public void onBackPressed() {
		AppBrain.getAds().showInterstitial(this);
		finish();
	}

	private final DialogInterface.OnClickListener onMoreApps = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://play.google.com/store/search?q=pub:Grafian"));
			startActivity(intent);
			dialog.dismiss();
		}
	};

	private final DialogInterface.OnClickListener onClose = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
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
			mFragments.get(0).translate(s.toString());
			mFragments.get(1).translate(s.toString());
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.en_to_id:
			mViewPager.setCurrentItem(0);
			break;
		case R.id.id_to_en:
			mViewPager.setCurrentItem(1);
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

	private class ResultFragmentAdapter extends FragmentPagerAdapter {
		public ResultFragmentAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int index) {
			return mFragments.get(index);
		}

		@Override
		public int getCount() {
			return 2;
		}
	}
}
