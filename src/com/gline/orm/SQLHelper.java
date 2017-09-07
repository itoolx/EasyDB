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

    public SQLHelper(Context context, TableInfoBuilder infos) {
        super(context, infos.getDBName(), null, infos.getVersion());
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
