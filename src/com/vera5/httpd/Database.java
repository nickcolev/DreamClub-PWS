package com.vera5.httpd;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {

  static final String TAG = "PWS.Database";
  private final static int DB_VERSION = 1;
  private final static String DB_NAME = "pws.db";
  private Context context;

	public Database(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	private String setWhere(String tag, String key) {
		String where = "";
		if (tag != null)
			if (!tag.startsWith("*"))
				where = " AND _who='"+tag.toUpperCase()+"'";
		if (key != null) {
			String[] a = key.split(" ");
			for (int i=0; i<a.length; i++)
				where += " AND _what LIKE '%"+a[i]+"%'";
		}
		return where.replaceFirst("AND", "WHERE");
	}

	public String getLog(String tag, String key) {
		String sql = "SELECT _when,_who,_what FROM log" + setWhere(tag, key);
		String none = "Nothing to display", output = "";
		Cursor curs = null;
		SQLiteDatabase db = null;
		try {
			db = getReadableDatabase();
			curs = db.rawQuery(sql, null);
			if (curs == null) return none;
			if (curs.getCount() == 0) return none;
			curs.moveToFirst();
			do {
				String when = ""+curs.getString(0);
				String who = curs.getString(1);
				String what = curs.getString(2);
				output += when + "\t" + who + "/" + what + "\n";
			} while (curs.moveToNext());
			if (output.length() == 0) output = none;
		} catch (SQLiteException e) {
			output = e.getMessage();
			Log.e(TAG, output);
		} catch (CursorIndexOutOfBoundsException be) {
			output = be.getMessage();
			Log.e(TAG, output);
		} finally {
			curs.close();
			db.close();
		}
		return output;
	}

	public boolean putLog(String tag, String msg) {
		boolean ok = false;
		try {
			SQLiteDatabase db = getWritableDatabase();
			db.execSQL("INSERT INTO log (_who,_what) VALUES ('"+tag+"','"+msg+"')");
			db.close();
			ok = true;
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		}
		return ok;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS log (_when DATETIME DEFAULT CURRENT_TIMESTAMP, _who VARCHAR(80), _what VARCHAR(512))");
		db.execSQL("CREATE TABLE IF NOT EXISTS cookie (url VARCHAR(80),name VARCHAR(80),value VARCHAR(80),expire DATETIME)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Add code -- http://www.drdobbs.com/database/using-sqlite-on-android/232900584
	}

}
