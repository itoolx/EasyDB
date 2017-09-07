package com.gline.orm.model;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class TableInfoBuilder {
    private String mDatabaseName = null;
    private int mVersion = -1;

    private List<String> mTables = null;

    private List<String> mCreatedTables = null;

    private List<String> mUpdatedTables = null;

    public TableInfoBuilder(String name, int version) {
        super();
        mDatabaseName = name;
        mVersion = version;
    }

    public static final String getTableInfoID(String name, int version) {
        return String.format("%s-%s", name, version);
    }

    public String getId() {
        return getTableInfoID(mDatabaseName, mVersion);
    }

    public TableInfoBuilder append(String tableName, String sql) {
        if (mTables == null) {
            mTables = new ArrayList<>();
        }
        mTables.add(tableName);
        if (mCreatedTables == null) {
            mCreatedTables = new ArrayList<>();
        }
        mCreatedTables.add(sql);
        if (mUpdatedTables == null) {
            mUpdatedTables = new ArrayList<>();
        }
        mUpdatedTables.add(String.format("DROP TABLE IF EXISTS %s", tableName));
        mUpdatedTables.add(sql);
        return this;
    }

    public boolean contains(String tableName) {
        return mTables.contains(tableName);
    }

    public List<String> getCreationSQL() {
        return mCreatedTables;
    }

    public List<String> getUpgradationSQL() {
        return mUpdatedTables;
    }

    public int getVersion() {
        return mVersion;
    }

    public String getDBName() {
        return mDatabaseName;
    }
}
