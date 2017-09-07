package com.gline.orm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.gline.orm.base.ColumnType;
import com.gline.orm.base.IColumn;
import com.gline.orm.base.INullable;
import com.gline.orm.base.IPrimaryKey;
import com.gline.orm.base.ISchema;
import com.gline.orm.base.ITable;
import com.gline.orm.model.TableInfoBuilder;
import com.gline.orm.utils.ClassUtils;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EasyDB {

    private static class Pair<Key, Value> {
        private Key t;
        private Value v;

        public Pair(Key t, Value v) {
            this.t = t;
            this.v = v;
        }

        public Key getK() {
            return t;
        }

        public Value getV() {
            return v;
        }

    }

    private static class TriplePair<Key, Value1, Value2> {
        private Key t;
        private Value1 v1;
        private Value2 v2;

        public TriplePair(Key t, Value1 v1, Value2 v2) {
            this.t = t;
            this.v1 = v1;
            this.v2 = v2;
        }

        public Key getK() {
            return t;
        }

        public Value1 getV1() {
            return v1;
        }

        public Value2 getV2() {
            return v2;
        }

    }

    private static final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String DEFAULT_DATABASE_NAME = "gorm.db";

    private static final int DEFAULT_DATABASE_VERSION = 1;

    private static final EasyDB INSTANCE = new EasyDB();

    private ConcurrentMap<String, TableInfoBuilder> mDatabaseMap = new ConcurrentHashMap<>();

    private ConcurrentMap<String, SQLHelper> mDatabaseHelperMap = new ConcurrentHashMap<>();

    private EasyDB() {
        super();
    }

    public static final EasyDB getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        List<Class<?>> mTableClasses = ClassUtils.getAllClasses(context, ClassUtils.TABLE_FILTER);
        TriplePair<ISchema, String, String> tableInfoPair;
        ISchema iSchema;
        TableInfoBuilder cTableInfoBuilder;
        String cDatabaseName;
        int cVersion;
        for (Class<?> clazz : mTableClasses) {
            tableInfoPair = onCreateTable(clazz);
            if (tableInfoPair == null) {
                continue;
            }
            iSchema = tableInfoPair.getK();
            if (iSchema == null) {
                cDatabaseName = DEFAULT_DATABASE_NAME;
                cVersion = DEFAULT_DATABASE_VERSION;
            } else {
                cDatabaseName = iSchema.name();
                cVersion = iSchema.version();
            }
            cTableInfoBuilder = mDatabaseMap.get(TableInfoBuilder.getTableInfoID(cDatabaseName, cVersion));
            if (cTableInfoBuilder == null) {
                cTableInfoBuilder = new TableInfoBuilder(cDatabaseName, cVersion);
                mDatabaseMap.put(cTableInfoBuilder.getId(), cTableInfoBuilder);
            }
            cTableInfoBuilder.append(tableInfoPair.getV1(), tableInfoPair.getV2());
        }
        for (TableInfoBuilder tableInfoBuilder : mDatabaseMap.values()) {
            mDatabaseHelperMap.put(tableInfoBuilder.getId(), new SQLHelper(context, tableInfoBuilder));
        }
    }

    private SQLiteOpenHelper getHelperByTableName(String tableName) {
        for (TableInfoBuilder tableInfoBuilder : mDatabaseMap.values()) {
            if (tableInfoBuilder.contains(tableName)) {
                return mDatabaseHelperMap.get(tableInfoBuilder.getId());
            }
        }
        return null;
    }

    public <T> boolean onInsert(T... objects) {
        try {
            if (objects == null || objects.length <= 0) {
                return false;
            }
            for (Object obj : objects) {
                if (onInsert(obj) < 0) {
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private <T> long onInsert(T object) throws IllegalAccessException {
        Class<?> clazz = object.getClass();
        ITable cTableName = clazz.getAnnotation(ITable.class);
        if (cTableName == null) {
            return -1;
        }
        SQLiteOpenHelper cSQLiteOpenHelper = getHelperByTableName(cTableName.name());
        SQLiteDatabase cDatabase = cSQLiteOpenHelper.getWritableDatabase();
        Field[] cFields = clazz.getDeclaredFields();
        ContentValues values = new ContentValues();
        for (int index = 0; index < cFields.length; index++) {
            cFields[index].setAccessible(true);
            IColumn cColumnName = cFields[index].getAnnotation(IColumn.class);
            Class fieldType = cFields[index].getType();
            if (cColumnName.autoIncrement() || cColumnName.type() == ColumnType.AUTO_DATE) {
                continue;
            } else if (cColumnName.type() == ColumnType.DATE) {
                values.put(cColumnName.name(), SIMPLE_DATE_FORMAT.format((Date) cFields[index].get(object)));
            } else if (fieldType.equals(int.class)) {
                values.put(cColumnName.name(), cFields[index].getInt(object));
            } else if (fieldType.equals(float.class)) {
                values.put(cColumnName.name(), cFields[index].getFloat(object));
            } else if (fieldType.equals(double.class)) {
                values.put(cColumnName.name(), cFields[index].getDouble(object));
            } else if (fieldType.equals(long.class)) {
                values.put(cColumnName.name(), cFields[index].getLong(object));
            } else if (fieldType.equals(short.class)) {
                values.put(cColumnName.name(), cFields[index].getShort(object));
            } else if (fieldType.equals(String.class)) {
                values.put(cColumnName.name(), (String) cFields[index].get(object));
            } else if (fieldType.equals(byte[].class)) {
                values.put(cColumnName.name(), (byte[]) cFields[index].get(object));
            }
        }
        return cDatabase.insert(cTableName.name(), null, values);
    }

    public <T> int count(Class<T> clazz, String where) {
        ITable cTableName = clazz.getAnnotation(ITable.class);
        if (cTableName == null) {
            return -1;
        }
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("SELECT count(*) FROM ");
        sBuffer.append(cTableName.name());
        if (!TextUtils.isEmpty(where)) {
            sBuffer.append(" WHERE ");
            sBuffer.append(where);
        }
        sBuffer.append(";");
        SQLiteOpenHelper cSQLiteOpenHelper = getHelperByTableName(cTableName.name());
        SQLiteDatabase cDatabase = cSQLiteOpenHelper.getReadableDatabase();
        Cursor cursor = cDatabase.rawQuery(sBuffer.toString(), null);
        cursor.moveToFirst();
        int result = cursor.getInt(0);
        cursor.close();
        cDatabase.close();
        return result;
    }

    public <T> List<T> onQuery(Class<T> clazz, String where) {
        ITable cTableName = clazz.getAnnotation(ITable.class);
        if (cTableName == null) {
            return null;
        }
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("SELECT * FROM ");
        sBuffer.append(cTableName.name());
        if (!TextUtils.isEmpty(where)) {
            sBuffer.append(" WHERE ");
            sBuffer.append(where);
        }
        sBuffer.append(";");
        SQLiteOpenHelper cSQLiteOpenHelper = getHelperByTableName(cTableName.name());
        SQLiteDatabase cDatabase = cSQLiteOpenHelper.getReadableDatabase();
        Cursor cursor = cDatabase.rawQuery(sBuffer.toString(), null);
        List<T> result = null;
        if (cursor != null) {
            result = new ArrayList<>();
            while (cursor.moveToNext()) {
                try {
                    result.add(fromCursor(cursor, clazz));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        cDatabase.close();
        return result;
    }

    private static <T> T fromCursor(Cursor cursor, Class<T> cls) throws IllegalAccessException, InstantiationException {
        T instance = cls.newInstance();
        List<Field> fields = Arrays.asList(cls.getDeclaredFields());
        for (Field field : fields) {
            field.setAccessible(true);
            IColumn cColumnName = field.getAnnotation(IColumn.class);
            if (cColumnName == null) {
                continue;
            }
            int columnIndex = cursor.getColumnIndex(cColumnName.name());
            if (columnIndex < 0) {
                continue;
            }
            Class fieldType = field.getType();
            if (fieldType.equals(int.class)) {
                field.setInt(instance, cursor.getInt(columnIndex));
            } else if (fieldType.equals(float.class)) {
                field.setFloat(instance, cursor.getFloat(columnIndex));
            } else if (fieldType.equals(double.class)) {
                field.setDouble(instance, cursor.getDouble(columnIndex));
            } else if (fieldType.equals(long.class)) {
                field.setLong(instance, cursor.getLong(columnIndex));
            } else if (fieldType.equals(short.class)) {
                field.setShort(instance, cursor.getShort(columnIndex));
            } else if (fieldType.equals(String.class)) {
                field.set(instance, cursor.getString(columnIndex));
            } else if (fieldType.equals(byte[].class)) {
                field.set(instance, cursor.getBlob(columnIndex));
            } else if (fieldType.equals(Date.class)) {
                try {
                    field.set(instance, SIMPLE_DATE_FORMAT.parse(cursor.getString(columnIndex)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return instance;
    }

    public <T> int onUpdate(T object, String where) throws IllegalAccessException {
        Class<?> clazz = object.getClass();
        ITable cTableName = clazz.getAnnotation(ITable.class);
        if (cTableName == null) {
            return -1;
        }
        SQLiteOpenHelper cSQLiteOpenHelper = getHelperByTableName(cTableName.name());
        SQLiteDatabase cDatabase = cSQLiteOpenHelper.getReadableDatabase();
        Field[] cFields = clazz.getDeclaredFields();
        ContentValues values = new ContentValues();
        for (int index = 0; index < cFields.length; index++) {
            cFields[index].setAccessible(true);
            IColumn cColumnName = cFields[index].getAnnotation(IColumn.class);
            Class fieldType = cFields[index].getType();
            if (cColumnName.autoIncrement() || cColumnName.type() == ColumnType.AUTO_DATE) {
                continue;
            } else if (cColumnName.type() == ColumnType.DATE) {
                values.put(cColumnName.name(), SIMPLE_DATE_FORMAT.format((Date) cFields[index].get(object)));
            } else if (fieldType.equals(int.class)) {
                values.put(cColumnName.name(), cFields[index].getInt(object));
            } else if (fieldType.equals(float.class)) {
                values.put(cColumnName.name(), cFields[index].getFloat(object));
            } else if (fieldType.equals(double.class)) {
                values.put(cColumnName.name(), cFields[index].getDouble(object));
            } else if (fieldType.equals(long.class)) {
                values.put(cColumnName.name(), cFields[index].getLong(object));
            } else if (fieldType.equals(short.class)) {
                values.put(cColumnName.name(), cFields[index].getShort(object));
            } else if (fieldType.equals(String.class)) {
                values.put(cColumnName.name(), (String) cFields[index].get(object));
            } else if (fieldType.equals(byte[].class)) {
                values.put(cColumnName.name(), (byte[]) cFields[index].get(object));
            }
        }
        int result = cDatabase.update(cTableName.name(), values, where, null);
        cDatabase.close();
        return result;
    }

    public <T> int onDelete(Class<T> clazz, String where) {
        ITable cTableName = clazz.getAnnotation(ITable.class);
        if (cTableName == null) {
            return -1;
        }
        SQLiteOpenHelper cSQLiteOpenHelper = getHelperByTableName(cTableName.name());
        SQLiteDatabase cDatabase = cSQLiteOpenHelper.getReadableDatabase();
        int result = cDatabase.delete(cTableName.name(), where, null);
        cDatabase.close();
        return result;
    }

    private static final TriplePair<ISchema, String, String> onCreateTable(Class<?> clazz) {
        ITable cTableName = clazz.getAnnotation(ITable.class);
        if (cTableName == null) {
            return null;
        }
        ISchema cSchema = clazz.getAnnotation(ISchema.class);
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE IF NOT EXISTS ");
        sBuffer.append(cTableName.name());
        Field[] fields = clazz.getDeclaredFields();
        sBuffer.append("(");
        for (int index = 0; index < fields.length; index++) {
            IColumn cColumnName = fields[index].getAnnotation(IColumn.class);
            if (cColumnName == null) {
                continue;
            }
            if (index > 0) {
                sBuffer.append(", ");
            }
            sBuffer.append(cColumnName.name());
            sBuffer.append(" ");
            if (cColumnName.type() == ColumnType.AUTO_DATE) {
                sBuffer.append("TimeStamp DEFAULT(datetime('now', 'localtime'))");
            } else {
                sBuffer.append(cColumnName.type());
                if (cColumnName.length() > 0) {
                    sBuffer.append("(");
                    sBuffer.append(cColumnName.length());
                    sBuffer.append(")");
                }
            }
            IPrimaryKey cPrimaryKey = fields[index].getAnnotation(IPrimaryKey.class);
            if (cPrimaryKey != null) {
                sBuffer.append(" PRIMARY KEY");
                if (cPrimaryKey.unique()) {
                    sBuffer.append(" UNIQUE");
                }
            }
            if (cColumnName.autoIncrement()) {
                sBuffer.append(" AUTOINCREMENT");
            }
            INullable cNullable = fields[index].getAnnotation(INullable.class);
            if (cNullable != null && !cNullable.value()) {
                sBuffer.append(" NOT NULL");
            }
        }
        sBuffer.append(");");
        return new TriplePair(cSchema, cTableName.name(), sBuffer.toString());
    }
}
