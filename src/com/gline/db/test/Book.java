package com.gline.db.test;

import com.gline.db.base.ColumnType;
import com.gline.db.base.IColumnName;
import com.gline.db.base.INullable;
import com.gline.db.base.IPrimaryKey;
import com.gline.db.base.ITableName;

@ITableName(name = "Book")
public class Book {

	@IPrimaryKey
	@IColumnName(name = "ID", type = ColumnType.INTEGER, autoIncrement = true)
	private int mId = -1;

	@INullable(false)
	@IColumnName(name = "name", type = ColumnType.TEXT)
	public String name;

	@INullable(true)
	@IColumnName(name = "date", type = ColumnType.AUTO_DATE)
	public String date;

}
