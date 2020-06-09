package org.dp.facedetection.db;

import android.content.Context;

import com.greendao.gen.DaoMaster;
import com.greendao.gen.DaoSession;
import com.greendao.gen.UserDao;

import org.dp.facedetection.BaseApplication;
import org.dp.facedetection.utils.LogUtils;

import java.util.List;

public class DBManager {


    private static DBManager instance;
    private static DBHelper dbHelper;
    private static  UserDao userDao;

    public static synchronized DBManager getInstance() {
        if (instance == null) {
            instance = new DBManager();
        }
        return instance;
    }

    /**
     * 数据库初始化
     *
     */
    public void init() {
        if (BaseApplication.getContext() == null) {
            return;
        }
        if (dbHelper == null) {
            dbHelper = new DBHelper(BaseApplication.getContext().getApplicationContext());
        }
    }

    public synchronized UserDao getUserDao(){
        if (dbHelper==null){
            dbHelper = new DBHelper(BaseApplication.getContext());
        }
        if (userDao==null){
            DaoMaster daoMaster = new DaoMaster(dbHelper.getWritableDb());
            DaoSession daoSession = daoMaster.newSession();
            userDao = daoSession.getUserDao();
        }
       return userDao;
    }

    /**
     * 释放数据库
     */
    public void release() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
        instance = null;
        if (instance!=null){
            instance=null;
        }
        if (userDao!=null){
            userDao=null;
        }
    }

    public List<User> getUser(){
        List<User> users = getUserDao().loadAll();
        return users;
    }

    public void addUser(User user){
        long insert = getUserDao().insert(user);
        LogUtils.d("mine","adduser"+insert);
    }
}
