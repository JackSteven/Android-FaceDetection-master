package org.dp.facedetection.db;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.greendao.gen.DaoMaster;
import com.greendao.gen.DaoSession;
import com.greendao.gen.UserDao;

import org.dp.facedetection.BaseApplication;


public class DBHelper extends DaoMaster.OpenHelper{


    public DBHelper(Context context) {
        super(context, "user.db");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);
    }

}
