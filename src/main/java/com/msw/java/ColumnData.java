package com.msw.java;

public class ColumnData {
    private String ordinalPosition;
    private String columnName;
    private String dataType;
    private String nullable;
    private String prk;
    private String columnDefault;
    private String columnComment;
    private String qbd;

    public String getOrdinalPosition() {
        return ordinalPosition;
    }

    public void setOrdinalPosition(String ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getNullable() {
        return nullable;
    }

    public void setNullable(String nullable) {
        this.nullable = nullable;
    }

    public String getPrk() {
        return prk;
    }

    public void setPrk(String prk) {
        this.prk = prk;
    }

    public String getColumnDefault() {
        return columnDefault;
    }

    public void setColumnDefault(String columnDefault) {
        this.columnDefault = columnDefault;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public String getQbd() {
        return qbd;
    }

    public void setQbd(String qbd) {
        this.qbd = qbd;
    }
}
