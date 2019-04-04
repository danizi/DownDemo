package demo.xm.com.demo.down2.db

import demo.xm.com.demo.down2.DownTask

/**
 * 数据库契约类
 */
class DownDBContract {
    /**
     * 下载表相关
     */
    object DownTable {
        private const val TABLE_NAME = "down"             //表名称
        private const val _ID = "_id"                     //主键ID
        private const val COLUMN_UUID = "uuid"            //下载名称
        private const val COLUMN_URL = "url"            //下载名称
        private const val COLUMN_NAME = "name"            //下载名称
        private const val COLUMN_PRESENT = "present"      //下载百分比(单位 1/100)
        private const val COLUMN_TOTAL = "total"          //下载总大小(单位：字节)
        private const val COLUMN_PROCESS = "process"      //下载进度(单位：字节)
        private const val COLUMN_ABSOLUTE_PATH = "path"   //下载绝对路径

        /* 创建表
         * 语法：CREATE TABLE <table_name> (<column_name_1> <data_type_1>, <column_name_2> <data_type_2>, ...)
         */
        const val SQL_CREATE_DOWN_TABLE = "CREATE TABLE $TABLE_NAME(" +
                "$_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_UUID TEXT NOT NULL," +
                "$COLUMN_URL TEXT NOT NULL," +
                "$COLUMN_NAME TEXT NOT NULL," +
                "$COLUMN_PRESENT INTEGER NOT NULL DEFAULT 0," +
                "$COLUMN_TOTAL INTEGER NOT NULL DEFAULT 0," +
                "$COLUMN_PROCESS INTEGER NOT NULL DEFAULT 0," +
                "$COLUMN_ABSOLUTE_PATH TEXT NOT NULL);"


        /* 插入数据
         * 语法：INSERT INTO 表名称 VALUES (值1, 值2,....)
         *       INSERT INTO table_name (列1, 列2,...) VALUES (值1, 值2,....)
         */
        const val SQL_QUERY_INSERT = "INSERT INTO $TABLE_NAME(" +
                "$COLUMN_UUID," +
                "$COLUMN_URL," +
                "$COLUMN_NAME," +
                "$COLUMN_PRESENT," +
                "$COLUMN_TOTAL," +
                "$COLUMN_PROCESS," +
                "$COLUMN_ABSOLUTE_PATH) VALUES(?,?,?,?,?,?,?)"


        /* 查询
         * 语法：SELECT (列1, 列2,...) FROM 表名称
         *       SELECT * FROM 表名称 查询所有
         */
        const val SQL_QUERY_ALL = "SELECT * FROM $TABLE_NAME"
        const val SQL_QUERY_UUID = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_UUID = ?"


        /* 更新
         * 语法：UPDATE 表名称 SET 列名称 = 新值 WHERE 列名称 = 某值
         */
        const val SQL_UPDATE = "UPDATE $TABLE_NAME SET " +
                "$COLUMN_URL = ?," +
                "$COLUMN_PRESENT = ?," +
                "$COLUMN_TOTAL = ?," +
                "$COLUMN_PROCESS = ? WHERE $COLUMN_UUID = ?"


        /* 删除
         * 语法：DELETE FROM 表名称 WHERE 列名称 = 值
         */
        const val SQL_DELETE = "DELETE FROM $TABLE_NAME WHERE $COLUMN_UUID = ?"
    }

    /**
     * 实体
     */
    class DownInfo {
        var uuid = ""    //通过url MD5获取的保证唯一值
        var name = ""
        var present = 0
        var progress = 0
        var total = 0
        var absolutePath = ""
        var state = ""
        var url = ""

        constructor(uuid: String = "", name: String = "", present: Int = 0, progress: Int = 0, total: Int = 0, absolutePath: String = "", state: String = "", url: String = "") {
            this.uuid = uuid
            this.name = name
            this.present = present
            this.progress = progress
            this.total = total
            this.absolutePath = absolutePath
            this.state = state
            this.url = url
        }

        override fun toString(): String {
            return "DownInfo(uuid='$uuid', name='$name', present=$present, progress=$progress, total=$total, state='$state', absolutePath='$absolutePath', url='$url')"
        }

    }
}