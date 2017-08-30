package com.gline.db;

import java.lang.reflect.Field;
import java.util.List;

import com.gline.db.base.ColumnType;
import com.gline.db.base.IAuto;
import com.gline.db.base.IColumnName;
import com.gline.db.base.INullable;
import com.gline.db.base.IPrimaryKey;
import com.gline.db.base.ITableName;
import com.gline.db.base.IUnique;
import com.gline.db.utils.ClassUtils;

public class DatabaseHelper {

	private static final DatabaseHelper INSTANCE = new DatabaseHelper();

	private DatabaseHelper() {
		super();
	}

	public static final DatabaseHelper getInstance() {
		return INSTANCE;
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

	private String createTable(Class<?> clazz) {
		ITableName cTableName = clazz.getAnnotation(ITableName.class);
		if (cTableName == null) {
			return null;
		}
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("CREATE TABLE IF NOT EXISTS ");
		sBuffer.append(cTableName.name());
		Field[] fields = clazz.getDeclaredFields();
		sBuffer.append("(");
		for (int index = 0; index < fields.length; index++) {
			IColumnName cColumnName = fields[index].getAnnotation(IColumnName.class);
			sBuffer.append(cColumnName.name());
			sBuffer.append(" ");
			IAuto cAuto = fields[index].getAnnotation(IAuto.class);
			if (cAuto != null && cColumnName.type() == ColumnType.DATE) {
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
			}
			if (cAuto != null) {
				sBuffer.append(" AUTOINCREMENT");
			}
			IUnique cUnique = fields[index].getAnnotation(IUnique.class);
			if (cUnique != null) {
				sBuffer.append(" UNIQUE");
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
