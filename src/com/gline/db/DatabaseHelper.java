package com.gline.db;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import com.gline.db.base.ColumnType;
import com.gline.db.base.IColumn;
import com.gline.db.base.INullable;
import com.gline.db.base.IPrimaryKey;
import com.gline.db.base.ITable;
import com.gline.db.test.Book;
import com.gline.db.utils.ClassUtils;

public class DatabaseHelper {

	private static final DatabaseHelper INSTANCE = new DatabaseHelper();

	private DatabaseHelper() {
		super();
	}

	public static final DatabaseHelper getInstance() {
		return INSTANCE;
	}

	public static void main(String[] args) throws Exception {
		DatabaseHelper.getInstance().init("com.gline.db.test");
	}

	public void init(String packageName) {
		List<Class<?>> classes = ClassUtils.getClasses(packageName);
		StringBuffer sBuffer = new StringBuffer();
		for (Class<?> clazz : classes) {
			sBuffer.append(createTable(clazz));
		}
		String sql = sBuffer.toString();
		System.out.println(sql);
		Book cBook = new Book();
		cBook.name = "ddd";
		System.out.println(select(cBook));
	}

	public List<Object> exe(Class<?> clazz, String sql) {
		return null;
	}

	private String select(Object object) {
		Class<?> clazz = object.getClass();
		ITable cTableName = clazz.getAnnotation(ITable.class);
		if (cTableName == null) {
			return null;
		}
		Field[] fields = clazz.getDeclaredFields();
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("SELECT * FROM ");
		sBuffer.append(cTableName.name());
		sBuffer.append(" WHERE");
		Object value = null;
		IColumn cColumnName = null;
		for (int index = 0; index < fields.length; index++) {
			fields[index].setAccessible(true);
			try {
				value = fields[index].get(object);
				if (value == null) {
					continue;
				}
				if (index > 0) {
					sBuffer.append(" AND");
				}
				cColumnName = fields[index].getAnnotation(IColumn.class);
				sBuffer.append(" ");
				sBuffer.append(cColumnName.name());
				if (cColumnName.type() == ColumnType.INTEGER) {
					sBuffer.append(" == ");
					sBuffer.append(((Integer) value).intValue());
				} else if (cColumnName.type() == ColumnType.TEXT) {
					sBuffer.append(" like '");
					sBuffer.append((String) value);
					sBuffer.append("'");
				} else if (cColumnName.type() == ColumnType.DATE) {
					sBuffer.append(" == ");
					sBuffer.append(((Date) value).getTime());
				} else {
					sBuffer.append(" == '");
					sBuffer.append((String) value);
					sBuffer.append("'");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sBuffer.append(";");
		return sBuffer.toString();
	}

	private String createTable(Class<?> clazz) {
		ITable cTableName = clazz.getAnnotation(ITable.class);
		if (cTableName == null) {
			return null;
		}
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("CREATE TABLE IF NOT EXISTS ");
		sBuffer.append(cTableName.name());
		Field[] fields = clazz.getDeclaredFields();
		sBuffer.append("(");
		for (int index = 0; index < fields.length; index++) {
			IColumn cColumnName = fields[index].getAnnotation(IColumn.class);
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
			if (index < fields.length - 1) {
				sBuffer.append(", ");
			}
		}
		sBuffer.append(");");
		return sBuffer.toString();
	}

}
