package com.gline.orm;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;

import com.gline.orm.model.TableInfoBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

class SQLHelper extends SQLiteOpenHelper {

    private final TableInfoBuilder mTableInfos;

    public static final class DatabaseContext extends ContextWrapper {

        public DatabaseContext(Context context) {
            super(context);
        }

        /**
         * 获得数据库路径，如果不存在，则创建对象对象
         *
         * @param name
         */
        @Override
        public File getDatabasePath(String name) {
            boolean sdExist = android.os.Environment.MEDIA_MOUNTED.equals(
                    android.os.Environment.getExternalStorageState());
            if (!sdExist) {
                return super.getDatabasePath(name);
            } else {
                File dbDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "db");
                if (!dbDir.exists()) {
                    dbDir.mkdirs();
                }
                try {
                    File dbFile = new File(dbDir, name);
                    if (!dbFile.exists()) {
                        dbFile.createNewFile();
                    }
                    return dbFile;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        /**
         * 重载这个方法，是用来打开SD卡上的数据库的，android 2.3及以下会调用这个方法。
         *
         * @param name
         * @param mode
         * @param factory
         */
        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
            return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        }

        /**
         * Android 4.0会调用此方法获取数据库。
         *
         * @param name
         * @param mode
         * @param factory
         * @param errorHandler
         * @see android.content.ContextWrapper#openOrCreateDatabase(java.lang.String, int,
         * android.database.sqlite.SQLiteDatabase.CursorFactory,
         * android.database.DatabaseErrorHandler)
         */
        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
            return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        }

        public static final Context wrapper(Context context) {
            return new DatabaseContext(context);
        }
    }

    public SQLHelper(Context context, TableInfoBuilder infos) {
        super(DatabaseContext.wrapper(context), infos.getDBName(), null, infos.getVersion());
        mTableInfos = infos;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        List<String> sqlList = mTableInfos.getCreationSQL();
        if (sqlList == null || sqlList.isEmpty()) {
            return;
        }
        for (String sql : sqlList) {
            try {
                db.execSQL(sql);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        List<String> sqlList = mTableInfos.getUpgradationSQL();
        if (sqlList == null || sqlList.isEmpty()) {
            return;
        }
        for (String sql : sqlList) {
            try {
                db.execSQL(sql);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
