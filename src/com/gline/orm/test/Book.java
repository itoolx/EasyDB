package com.gline.orm.test;

import com.gline.orm.base.ColumnType;
import com.gline.orm.base.IColumn;
import com.gline.orm.base.INullable;
import com.gline.orm.base.IPrimaryKey;
import com.gline.orm.base.ISchema;
import com.gline.orm.base.ITable;

@ISchema(name = "ibook", version = 1)
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
