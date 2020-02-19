package com.rnstepcounterdemo.steps.bean;

import android.content.Context;

import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.litesuits.orm.db.assit.WhereBuilder;
import com.litesuits.orm.db.model.ConflictAlgorithm;

import java.util.List;

/**
 * Created by dylan on 2016/1/31.
 */
public class DbUtils {

    private static LiteOrm liteOrm;

    public static void createDb(Context _activity, String DB_NAME) {
        DB_NAME = DB_NAME + ".db";
        if (liteOrm == null) {
            liteOrm = LiteOrm.newCascadeInstance(_activity, DB_NAME);
            liteOrm.setDebugged(true);
        } else {
            liteOrm = LiteOrm.newCascadeInstance(_activity, DB_NAME);
            liteOrm.setDebugged(true);
        }
    }

    public static LiteOrm getLiteOrm() {
        return liteOrm;
    }

    /**
     * Insert a record
     */
    public static <T> void insert(T t) {
        liteOrm.save(t);
    }

    /**
     * Insert all records
     */
    public static <T> void insertAll(List<T> list) {
        liteOrm.save(list);
    }

    /**
     * Query all
     */
    public static <T> List<T> getQueryAll(Class<T> cla) {
        return liteOrm.query(cla);
    }

    /**
     * Query a field equal to the value of Value
     */
    public static <T> List<T> getQueryByWhere(Class<T> cla, String field, String[] value) {
        return liteOrm.<T>query(new QueryBuilder(cla).where(field + "=?", value));
    }

    /**
     * Query a field equal to the value of Value can be specified from 1-20, that is, paging
     */
    public static <T> List<T> getQueryByWhereLength(Class<T> cla, String field, String[] value, int start, int length) {
        return liteOrm.<T>query(new QueryBuilder(cla).where(field + "=?", value).limit(start, length));
    }

    /**
     * Delete all
     */
    public static <T> void deleteAll(Class<T> cla) {
        liteOrm.deleteAll(cla);
    }

    /**
     * Delete all fields with a value equal to Vlaue
     */
    public static <T> int deleteWhere(Class<T> cla, String field, String[] value) {
        return liteOrm.delete(cla, new WhereBuilder(cla).where(field + "!=?", value));
    }

    /**
     * Update only when it exists
     */
    public static <T> void update(T t) {
        liteOrm.update(t, ConflictAlgorithm.Replace);
    }

    /**
     * update all field
     */
    public static <T> void updateALL(List<T> list) {
        liteOrm.update(list);
    }

    /**
     * close database
     */
    public static void closeDb() {
        liteOrm.close();
    }

}
