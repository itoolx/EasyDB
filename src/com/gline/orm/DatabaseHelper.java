package com.gline.orm;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import com.gline.orm.base.ColumnType;
import com.gline.orm.base.IColumn;
import com.gline.orm.base.INullable;
import com.gline.orm.base.IPrimaryKey;
import com.gline.orm.base.ITable;
import com.gline.orm.test.Book;
import com.gline.orm.utils.ClassUtils;

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
		Book cBook = new Book();
		cBook.name = "ddd";
		DatabaseHelper.getInstance().select(cBook, new IColumn[]{});
	}

	public void init(String packageName) {
		List<Class<?>> classes = ClassUtils.getClasses(packageName);
		StringBuffer sBuffer = new StringBuffer();
		for (Class<?> clazz : classes) {
			sBuffer.append(createTable(clazz));
		}
		String sql = sBuffer.toString();
		System.out.println(sql);
	}

	private void select(Book cBook, IColumn[] iColumns) {
		select(cBook, "*");
	}

	private List<Object> select(Object object, String fieldFilter) {
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getDeclaredFields();
		StringBuffer sBuffer = new StringBuffer();
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
		return selectBy(clazz, fieldFilter,  sBuffer.toString());
	}

	public List<Object> selectBy(Class<?> clazz, String fieldFilter,  String where) {
		ITable cTableName = clazz.getAnnotation(ITable.class);
		if (cTableName == null) {
			return null;
		}
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("SELECT ");
		sBuffer.append(fieldFilter);
		sBuffer.append(" FROM ");
		sBuffer.append(cTableName.name());
		sBuffer.append(" WHERE");
		sBuffer.append(where);
		sBuffer.append(";");
		return selectRaw(sBuffer.toString());
	}
	
	public List<Object> selectRaw(String sql) {
		System.out.println(sql);
		return null;
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
