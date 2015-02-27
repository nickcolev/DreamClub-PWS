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
				where = " AND _a='"+tag.toUpperCase()+"'";
		if (key != null) {
			String[] a = key.split(" ");
			String s, not;
			for (int i=0; i<a.length; i++) {
				if (a[i].startsWith("-")) {
					not = " NOT";
					s = a[i].substring(1);
				} else {
					not = "";
					s = a[i];
				}
				where += " AND _what" + not + " LIKE '%"+s+"%'";
			}
		}
		return where.replaceFirst("AND", "WHERE");
	}

	public String getLog(String tag, String key) {
		String sql = "SELECT _when,_a,_what FROM log" + setWhere(tag, key);
		String none = "Nothing to display", output = "";
		Cursor curs = null;
		SQLiteDatabase db = null;
		String when, a, what;
		try {
			db = getReadableDatabase();
			curs = db.rawQuery(sql, null);
			if (curs == null) return none;
			if (curs.getCount() == 0) return none;
			curs.moveToFirst();
			do {
				when = ""+curs.getString(0);
				a = curs.getString(1);
				what = curs.getString(2);
				output += when + "\t" + a + "/" + what + "\n";
			} while (curs.moveToNext());
			if (output.length() == 0) output = none;
		} catch (SQLiteException e) {
			output = e.getMessage();
			Lib.errlog(TAG, output);
		} catch (CursorIndexOutOfBoundsException be) {
			output = be.getMessage();
			Lib.errlog(TAG, output);
		} finally {
			curs.close();
			db.close();
		}
		return output;
	}

	public boolean putLog(String tag, String who, String msg, long ms) {
		boolean ok = false;
		String sql = "INSERT INTO log (_a,_what,_ms) VALUES ('"+tag+"','"+msg+"',"+ms+")";
		try {
			SQLiteDatabase db = getWritableDatabase();
			db.execSQL(sql);
			db.close();
			ok = true;
		} catch (SQLiteException e) {
			Lib.errlog(TAG, e.getMessage());
		}
		return ok;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE log (_when DATETIME DEFAULT CURRENT_TIMESTAMP, _a CHAR(1) NOT NULL, _who VARCHAR(40), _what VARCHAR(512), _ms INT)");
		db.execSQL("CREATE INDEX log_a ON log (_a)");
		db.execSQL("CREATE INDEX log_who ON log (_who)");
		db.execSQL("CREATE TABLE cookie (url VARCHAR(80),name VARCHAR(80),value VARCHAR(80),expire DATETIME)");
		db.execSQL("CREATE INDEX cookie_url ON cookie (url)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Add code -- http://www.drdobbs.com/database/using-sqlite-on-android/232900584
	}

}
