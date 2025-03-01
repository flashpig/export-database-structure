package com.msw.java;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.DocxRenderData;
import com.deepoove.poi.data.RowRenderData;
import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.policy.HackLoopTableRenderPolicy;
import org.apache.commons.lang3.ClassPathUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把数据库中的表结构导出word中
 *
 * @author MOSHUNWEI
 * @version 1.0
 */
public class App {
    public static void main(String[] args) throws Exception {


        //使用jar的命令，参数有4个，如果没有4个直接退出，没得商量
        if (args.length < 6) {
            System.out.println("参数：");
            System.out.println("-n=数据库名称");
            System.out.println("-u=用户名");
            System.out.println("-p=密码");
            System.out.println("-d=文件输出路径");
            System.out.println("h=数据库地址");
            System.out.println("p=数据库端口");
            System.out.println("-t=mysql or oracle defalut mysql");
            System.exit(0);
        }

        Map<String, String> map = Check(args);

        if (map.get("-t") == null || map.get("-t").equals("mysql")) {
            MySQL(map);
        } else if (map.get("-t").equals("mysql2")) {
            MySQLDetail(map);
        } else if (map.get("-t").equals("oracle")) {
            Oracle(map);
        } else {
            System.out.println("-t=mysql or oracle defalut mysql");
            System.exit(0);
        }

    }


    public static void MySQLDetail(Map<String, String> map) throws Exception {
        //默认生成的文件名
        String outFile = map.get("-d") + "/数据库表结构(MySQL).docx";
        //查询表的名称以及一些表需要的信息
        String mysqlSql1 = "SELECT table_name, table_type , ENGINE,table_collation,table_comment, create_options FROM information_schema.TABLES WHERE table_schema='" + map.get("-n") + "'";
        //查询表的结构信息
        String mysqlSql2 = "SELECT ordinal_position,column_name,column_type, column_key, extra ,is_nullable, column_default, column_comment,data_type,character_maximum_length "
                + "FROM information_schema.columns WHERE table_schema='" + map.get("-n") + "' and table_name='";
        String mysqlSql3 = "SHOW CREATE TABLE ";
        ResultSet rs = SqlUtils.getResultSet(SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p")), mysqlSql1);
        Connection con = SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p"));
        SqlUtils.executeSQL(con, "use " + map.get("-n"));
        createDocDetail(rs, mysqlSql2, map, outFile, true, "MySQL数据库表结构", con, mysqlSql3);
    }

    private static void createDocDetail(ResultSet rs, String sqls, Map<String, String> map, String outFile, boolean type, String title, Connection con, String sql3) throws IOException, SQLException {
        System.out.println("开始生成文件");
        List<Map<String, String>> list = getTableName(rs);
        Map<String, Object> datas = new HashMap<>();
        datas.put("title", title);
        List<TableData> tableList = new ArrayList<>();
        int i = 0;
        datas.put("tableInfoList", list);
        for (Map<String, String> str : list) {
            if (str.get("table_name").startsWith("QRTZ") || str.get("table_name").startsWith("qrtz")) {
                continue;
            }
            System.out.println(str);
            i++;
            String sql = sql3 + str.get("table_name");
            ResultSet set = SqlUtils.getResultSet(con, sql);
            String ddl = "";
            if (set.next()) {
                ddl = set.getString("create table");
            }
            set.close();
            sql = sqls + str.get("table_name") + "'";
            set = SqlUtils.getResultSet(con, sql);

            TableData table = new TableData();
            List<ColumnData> columnList = getRowRenderDataDetail(set, table);
            table.setNo(String.valueOf(i));
            table.setTableComment(str.get("table_comment"));
            table.setTableName(str.get("table_name"));
            table.setColumnList(columnList);
            table.setDdlSql(ddl);
            tableList.add(table);
        }

        datas.put("tableList", tableList);
        HackLoopTableRenderPolicy policy = new HackLoopTableRenderPolicy();
        Configure config = Configure.builder().bind("tableInfoList", policy).bind("columnList", policy).build();
        XWPFTemplate template = XWPFTemplate.compile(FileUtils.TemplateInputStream(), config).render(datas);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            System.out.println("生成文件结束");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("生成文件失败");
        } finally {
            try {
                template.write(out);
                out.flush();
                out.close();
                template.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<ColumnData> getRowRenderDataDetail(ResultSet set, TableData tableData) {
        List<ColumnData> result = new ArrayList<>();

        try {
            int i = 0;
            while (set.next()) {
                i++;
                String cml = "";
                if (set.getString("character_maximum_length") != null) {
                    cml = "(" + set.getString("character_maximum_length") + ")";
                }
                String autoIncrement = "";
                if ("auto_increment".equalsIgnoreCase(set.getString("extra"))) {
                    autoIncrement = "是";
                }
                String columnDefault = "";
                if (set.getString("column_default") != null) {
                    columnDefault = set.getString("column_default");
                }
                String prk = "";
                if ("PRI".equalsIgnoreCase(set.getString("column_key"))) {
                    prk = "Y";
                    tableData.setPriColumn(tableData.getPriColumn() + set.getString("column_name") + ",");
                }
                String qbd = "";
                ColumnData column = new ColumnData();
                column.setOrdinalPosition(set.getString("ordinal_position"));
                column.setColumnName(set.getString("column_name"));
                column.setDataType(set.getString("data_type") + cml);
                column.setNullable(set.getString("is_nullable").substring(0, 1) + "");
                column.setPrk(prk);
                column.setQbd(qbd);
                column.setColumnDefault(columnDefault);
                column.setColumnComment(set.getString("column_comment"));
                result.add(column);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (tableData.getPriColumn() != null && tableData.getPriColumn().length() > 0) {
            tableData.setPriColumn(tableData.getPriColumn().substring(0, tableData.getPriColumn().length() - 1));
        }

        return result;
    }

    public static void Oracle(Map<String, String> map) throws IOException {
        //默认生成的文件名
        String outFile = map.get("-d") + "/数据库表结构(ORACLE).docx";
        //查询表的名称以及一些表需要的信息
        String oracleSql1 = "select ut.table_name as table_name,ut.tablespace_name as engine,ut.buffer_pool as table_collation, uc.table_type as table_type,uc.comments as table_comment,ut.last_analyzed as create_options from user_tables ut,user_tab_comments uc where ut.table_name=uc.table_name";
        String oracleSql2 = "select rownum as ordinal_position,c.nullable as is_nullable,c.data_default as column_default,c.data_type as data_type,c.data_length as character_maximum_length,t.column_name as column_name,t.comments as column_comment from user_col_comments t,user_tab_columns c where c.column_name=t.column_name and c.table_name=t.table_name and t.table_name='";
        ResultSet rs = OracleUtils.getResultSet(OracleUtils.getConnnection(String.format("jdbc:oracle:thin:@%s:%s:ORCL", map.get("h"), map.get("p")), map.get("-u"), map.get("-p")), oracleSql1);
        Connection con = OracleUtils.getConnnection(String.format("jdbc:oracle:thin:@%s:%s:ORCL", map.get("h"), map.get("p")), map.get("-u"), map.get("-p"));
        createDoc(rs, oracleSql2, map, outFile, false, "Oracle数据库表结构", con);
    }

    public static void MySQL(Map<String, String> map) throws IOException {
        //默认生成的文件名
        String outFile = map.get("-d") + "/数据库表结构(MySQL).docx";
        //查询表的名称以及一些表需要的信息
        String mysqlSql1 = "SELECT table_name, table_type , ENGINE,table_collation,table_comment, create_options FROM information_schema.TABLES WHERE table_schema='" + map.get("-n") + "'";
        //查询表的结构信息
        String mysqlSql2 = "SELECT ordinal_position,column_name,column_type, column_key, extra ,is_nullable, column_default, column_comment,data_type,character_maximum_length "
                + "FROM information_schema.columns WHERE table_schema='" + map.get("-n") + "' and table_name='";
        ResultSet rs = SqlUtils.getResultSet(SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p")), mysqlSql1);
        Connection con = SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p"));
        createDoc(rs, mysqlSql2, map, outFile, true, "MySQL数据库表结构", con);

    }

    /**
     * 数据库详细设计文档
     *
     * @param map
     * @throws IOException
     */
    public static void MySQL2(Map<String, String> map) throws IOException {
        //默认生成的文件名
        String outFile = map.get("-d") + "/数据库表结构(MySQL)-Detail.docx";
        //查询表的名称以及一些表需要的信息
        String mysqlSql1 = "SELECT table_name, table_type , ENGINE,table_collation,table_comment, create_options FROM information_schema.TABLES WHERE table_schema='" + map.get("-n") + "'";
        //查询表的结构信息
        String mysqlSql2 = "SELECT ordinal_position,column_name,column_type, column_key, extra ,is_nullable, column_default, column_comment,data_type,character_maximum_length "
                + "FROM information_schema.columns WHERE table_schema='" + map.get("-n") + "' and table_name='";
        ResultSet rs = SqlUtils.getResultSet(SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p")), mysqlSql1);
        Connection con = SqlUtils.getConnnection(String.format("jdbc:mysql://%s:%s", map.get("h"), map.get("p")), map.get("-u"), map.get("-p"));
        createDoc2(rs, mysqlSql2, map, outFile, true, "MySQL数据库表结构", con);

    }


    private static void createDoc(ResultSet rs, String sqls, Map<String, String> map, String outFile, boolean type, String title, Connection con) throws IOException {
        System.out.println("开始生成文件");
        List<Map<String, String>> list = getTableName(rs);
        RowRenderData header = getHeader2();
        Map<String, Object> datas = new HashMap<>();
        datas.put("title", title);
        List<Map<String, Object>> tableList = new ArrayList<Map<String, Object>>();
        int i = 0;
        for (Map<String, String> str : list) {
            System.out.println(str);
            i++;
            String sql = sqls + str.get("table_name") + "'";
            ResultSet set = SqlUtils.getResultSet(con, sql);
            List<RowRenderData> rowList = getRowRenderData2(set);
            Map<String, Object> data = new HashMap<>();
            data.put("no", "" + i);
            data.put("table_comment", str.get("table_comment") + "");
            data.put("engine", str.get("engine") + "");
            data.put("table_collation", str.get("table_collation") + "");
            data.put("table_type", str.get("table_type") + "");
            data.put("name", new TextRenderData(str.get("table_name"), POITLStyle.getHeaderStyle()));
//			data.put("table", new MiniTableRenderData(header, rowList));
            data.put("rowList", rowList);
            tableList.add(data);
        }

//		datas.put("tablelist", new DocxRenderData(FileUtils.Base64ToFile(outFile,type), tableList));
//		XWPFTemplate template = XWPFTemplate.compile(FileUtils.Base64ToInputStream()).render(datas);
        //渲染表格  动态行
//		DynamicTableRenderPolicy  policy = new DynamicTableRenderPolicy();
//		Configure config = Configure.builder().bind("tablelist", policy).build();

        datas.put("tablelist", new DocxRenderData(FileUtils.newTemplateFile(outFile), tableList));
        XWPFTemplate template = XWPFTemplate.compile(FileUtils.Base64ToInputStream()).render(datas);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            System.out.println("生成文件结束");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("生成文件失败");
        } finally {
            try {
                template.write(out);
                out.flush();
                out.close();
                template.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 数据库详细设计文档
     *
     * @param rs
     * @param sqls
     * @param map
     * @param outFile
     * @param type
     * @param title
     * @param con
     * @throws IOException
     */
    private static void createDoc2(ResultSet rs, String sqls, Map<String, String> map, String outFile, boolean type, String title, Connection con) throws IOException {
        System.out.println("开始生成文件");
        List<Map<String, String>> list = getTableName(rs);
        RowRenderData header = getHeader2();
        Map<String, Object> datas = new HashMap<>();
        datas.put("title", title);
        List<Map<String, Object>> tableList = new ArrayList<Map<String, Object>>();
        int i = 0;
        for (Map<String, String> str : list) {
            System.out.println(str);
            i++;
            String sql = sqls + str.get("table_name") + "'";
            ResultSet set = SqlUtils.getResultSet(con, sql);
            List<RowRenderData> rowList = getRowRenderData2(set);
            Map<String, Object> data = new HashMap<>();
            data.put("no", "" + i);
            data.put("table_comment", str.get("table_comment") + "");
            data.put("engine", str.get("engine") + "");
            data.put("table_collation", str.get("table_collation") + "");
            data.put("table_type", str.get("table_type") + "");
            data.put("name", new TextRenderData(str.get("table_name"), POITLStyle.getHeaderStyle()));
//			data.put("table", new MiniTableRenderData(header, rowList));
            tableList.add(data);
        }

        datas.put("tablelist", new DocxRenderData(FileUtils.newTemplateFile(outFile), tableList));
        XWPFTemplate template = XWPFTemplate.compile(FileUtils.Base64ToInputStream()).render(datas);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            System.out.println("生成文件结束");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("生成文件失败");
        } finally {
            try {
                template.write(out);
                out.flush();
                out.close();
                template.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查缺少的参数
     */
    private static Map<String, String> Check(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String str : args) {
            String[] split = str.split("=");
            map.put(split[0], split[1]);
        }

        if (!map.containsKey("-n")) {
            System.out.println("请输入数据库名称！");
            System.exit(0);
        }
        if (!map.containsKey("-u")) {
            System.out.println("请输入数据库用户名！");
            System.exit(0);
        }
        if (!map.containsKey("-p")) {
            System.out.println("请输入数据库密码！");
            System.exit(0);
        }
        if (!map.containsKey("-d")) {
            System.out.println("请输入保存文件的目录！");
            System.exit(0);
        }
        if (!map.containsKey("h")) {
            System.out.println("请输入地址！");
            System.exit(0);
        }
        if (!map.containsKey("p")) {
            System.out.println("请输入端口");
            System.exit(0);
        }
        return map;
    }

    /**
     * table的表头
     *
     * @return RowRenderData
     */
    private static RowRenderData getHeader() {
//    	RowRenderData header = RowRenderData.build(
////				new TextRenderData("序号", POITLStyle.getHeaderStyle()),
//				new TextRenderData("字段名称", POITLStyle.getHeaderStyle()),
//				new TextRenderData("字段类型", POITLStyle.getHeaderStyle()),
//				new TextRenderData("默认值", POITLStyle.getHeaderStyle()),
//				new TextRenderData("允许空", POITLStyle.getHeaderStyle()),
//				new TextRenderData("自动递增", POITLStyle.getHeaderStyle()),
//				new TextRenderData("备注", POITLStyle.getHeaderStyle())
//		);
//		header.setStyle(POITLStyle.getHeaderTableStyle());
//		return header;
        return null;
    }

    /**
     * 数据库详细设计文档
     *
     * @return
     */
    private static RowRenderData getHeader2() {
//		RowRenderData header = RowRenderData.build(
////				new TextRenderData("序号", POITLStyle.getHeaderStyle()),
//				new TextRenderData("字段名称", POITLStyle.getHeaderStyle()),
//				new TextRenderData("数据类型（精度范围）", POITLStyle.getHeaderStyle()),
//				new TextRenderData("允许为空Y/N", POITLStyle.getHeaderStyle()),
//				new TextRenderData("唯一Y/N", POITLStyle.getHeaderStyle()),
//				new TextRenderData("区别度", POITLStyle.getHeaderStyle()),
//				new TextRenderData("默认值", POITLStyle.getHeaderStyle()),
//				new TextRenderData("约束条件/说明", POITLStyle.getHeaderStyle())
//		);
//		header.setStyle(POITLStyle.getHeaderTableStyle());
//		return header;
        return null;
    }

    /**
     * 获取一张表的结构数据
     *
     * @return List<RowRenderData>
     */
    private static List<RowRenderData> getRowRenderData(ResultSet set) {
        List<RowRenderData> result = new ArrayList<>();
//
//    	try {
//    		int i = 0;
//			while(set.next()){
//				i++;
//				String cml = "";
//				if (set.getString("character_maximum_length") != null) {
//					cml = "(" + set.getString("character_maximum_length") + ")";
//				}
//				String autoIncrement = "";
//				if ("auto_increment".equalsIgnoreCase(set.getString("extra"))) {
//					autoIncrement = "是";
//				}
//				String columnDefault = "";
//				if (set.getString("column_default") != null) {
//					columnDefault = set.getString("column_default");
//				}
//				RowRenderData row = RowRenderData.build(
////						new TextRenderData(set.getString("ordinal_position")+""),
//						new TextRenderData(set.getString("column_name")+""),
//						new TextRenderData(set.getString("data_type")+cml),
//						new TextRenderData(columnDefault+""),
//						new TextRenderData(set.getString("is_nullable")+""),
//						new TextRenderData(autoIncrement),
//						new TextRenderData(set.getString("column_comment")+"")
//						);
//				if(i%2==0){
//					row.setStyle(POITLStyle.getBodyTableStyle());
//					result.add(row);
//				}else{
//					result.add(row);
//				}
//
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}

        return result;
    }

    /**
     * 数据库详细设计文档
     *
     * @param set
     * @return
     */
    private static List<RowRenderData> getRowRenderData2(ResultSet set) {
        List<RowRenderData> result = new ArrayList<>();
//
//		try {
//			int i = 0;
//			while(set.next()){
//				i++;
//				String cml = "";
//				if (set.getString("character_maximum_length") != null) {
//					cml = "(" + set.getString("character_maximum_length") + ")";
//				}
//				String autoIncrement = "";
//				if ("auto_increment".equalsIgnoreCase(set.getString("extra"))) {
//					autoIncrement = "是";
//				}
//				String columnDefault = "";
//				if (set.getString("column_default") != null) {
//					columnDefault = set.getString("column_default");
//				}
//				String prk = "";
//				if ("PRI".equalsIgnoreCase(set.getString("column_key"))) {
//					prk = "Y";
//				}
//				String qbd = "";
//				RowRenderData row = RowRenderData.build(
//						new TextRenderData(set.getString("ordinal_position")+""),
//						new TextRenderData(set.getString("column_name")+""),
//						new TextRenderData(set.getString("data_type")+cml),
//						new TextRenderData(set.getString("is_nullable").substring(0, 1)+""),
//						new TextRenderData(prk),
//						new TextRenderData(qbd),
//						new TextRenderData(columnDefault+""),
//						new TextRenderData(set.getString("column_comment")+"")
//				);
//				if(i%2==0){
//					row.setStyle(POITLStyle.getBodyTableStyle());
//					result.add(row);
//				}else{
//					result.add(row);
//				}
//
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}

        return result;
    }


    /**
     * 获取数据库的所有表名及表的信息
     *
     * @return list
     */
    private static List<Map<String, String>> getTableName(ResultSet rs) {
        List<Map<String, String>> list = new ArrayList<>();

        try {
            while (rs.next()) {
                Map<String, String> result = new HashMap<>();
                result.put("table_name", rs.getString("table_name") + "");
                result.put("table_type", rs.getString("table_type") + "");
                result.put("engine", rs.getString("engine") + "");
                result.put("table_collation", rs.getString("table_collation") + "");
                result.put("table_comment", rs.getString("table_comment") + "");
                result.put("create_options", rs.getString("create_options") + "");
                list.add(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

}
