package com.grafian.kamus2;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

public class Dictionary {
	private byte mBuffer[];
	private int mIndex;
	private int mSize;

	public class Entry {
		String key;
		String val;
	}

	static private int getInt(byte[] buf, int off) {
		return buf[off] & 255 |
				(buf[off + 1] & 255) << 8 |
				(buf[off + 2] & 255) << 16 |
				(buf[off + 3] & 255) << 24;
	}

	public void load(Context context, int resid) {
		InputStream is = new BufferedInputStream(context.getResources().openRawResource(resid));
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			byte buf[] = new byte[8192];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			mBuffer = os.toByteArray();
			os.close();

			mIndex = getInt(mBuffer, 0);
			mSize = getInt(mBuffer, 4);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int size() {
		return mSize;
	}

	public Entry get(int index) {
		int keyOffset = getInt(mBuffer, mIndex + index * 4);
		int valOffset;
		for (int i = keyOffset;; i++) {
			if (mBuffer[i] == 0) {
				valOffset = i + 1;
				break;
			}
		}
		int nextOffset = index >= mSize - 1 ? mIndex :
				getInt(mBuffer, mIndex + index * 4 + 4);
		Entry e = new Entry();
		e.key = new String(mBuffer, keyOffset, valOffset - keyOffset - 1);
		e.val = new String(mBuffer, valOffset, nextOffset - valOffset - 1);
		return e;
	}

	public int searchFirst(String key) {
		boolean found = false;
		int l = 0;
		int r = mSize - 1;
		while (l <= r) {
			int m = (l + r) / 2;
			Entry e = get(m);
			if (e.key.length() > key.length()) {
				e.key = e.key.substring(0, key.length());
			}
			int cmp = e.key.compareToIgnoreCase(key);
			if (!found) {
				if (cmp == 0) {
					r = m;
					found = true;
				} else if (cmp < 0) {
					l = m + 1;
				} else {
					r = m - 1;
				}
			} else {
				if (cmp == 0) {
					if (r == m) {
						break;
					}
					r = m;
				} else {
					l = m + 1;
				}
			}
		}
		return found ? r : -1;
	}

	public int searchLast(String key) {
		boolean found = false;
		int l = 0;
		int r = mSize - 1;
		while (l <= r) {
			int m = (l + r) / 2;
			Entry e = get(m);
			if (e.key.length() > key.length()) {
				e.key = e.key.substring(0, key.length());
			}
			int cmp = e.key.compareToIgnoreCase(key);
			if (!found) {
				if (cmp == 0) {
					l = m;
					found = true;
				} else if (cmp < 0) {
					l = m + 1;
				} else {
					r = m - 1;
				}
			} else {
				if (cmp == 0) {
					if (l == m) {
						break;
					}
					l = m;
				} else {
					r = m - 1;
				}
			}
		}
		return found ? l : -1;
	}

	public int searchNearest(String key) {
		int longest = searchFirst(key);
		if (longest < 0) {
			int l = 1;
			int r = key.length();
			while (l <= r) {
				int m = (l + r) / 2;
				int pos = searchFirst(key.substring(0, m));
				if (pos < 0) {
					r = m - 1;
				} else {
					l = m + 1;
					longest = pos;
				}
			}
		}
		return longest;
	}
}
