package com.msw.java;

import java.util.ArrayList;
import java.util.List;

public class TableData {
    private String no;
    private String tableName;

    private List<ColumnData> priColumnList = new ArrayList<>();

    private String ddlSql;
    private String tableComment;

    private List<ColumnData> columnList = new ArrayList<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<ColumnData> getPriColumnList() {
        return priColumnList;
    }

    public void setPriColumnList(List<ColumnData> priColumnList) {
        this.priColumnList = priColumnList;
    }

    public String getDdlSql() {
        return ddlSql;
    }

    public void setDdlSql(String ddlSql) {
        this.ddlSql = ddlSql;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public List<ColumnData> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<ColumnData> columnList) {
        this.columnList = columnList;
    }
}
