package com.gline.db.test;

import com.gline.db.base.ColumnType;
import com.gline.db.base.IColumn;
import com.gline.db.base.INullable;
import com.gline.db.base.IPrimaryKey;
import com.gline.db.base.ITable;

@ITable(name = "Book")
public class Book {

	@IPrimaryKey
	@IColumn(name = "ID", type = ColumnType.INTEGER, autoIncrement = true)
	private int mId = -1;

	@INullable(false)
	@IColumn(name = "name", type = ColumnType.TEXT)
	public String name;

	@INullable(true)
	@IColumn(name = "date", type = ColumnType.AUTO_DATE)
	public String date;

}
