package com.gline.db.test;

import com.gline.db.base.ColumnType;
import com.gline.db.base.IAuto;
import com.gline.db.base.IColumnName;
import com.gline.db.base.INullable;
import com.gline.db.base.IPrimaryKey;
import com.gline.db.base.ITableName;

@ITableName(name = "Book")
public class Book {

	@IPrimaryKey
	@IAuto
	@IColumnName(name = "ID", type = ColumnType.INTEGER)
	private int mId;

	@INullable(false)
	@IColumnName(name = "name", type = ColumnType.TEXT)
	public String name;

	@IAuto
	@INullable(true)
	@IColumnName(name = "date", type = ColumnType.DATE)
	public String date;

}
