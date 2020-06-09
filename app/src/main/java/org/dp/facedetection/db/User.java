package org.dp.facedetection.db;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Unique;
import org.opencv.core.Mat;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class User {

    @Id(autoincrement = true)
    private Long id;

    @NotNull
    @Unique
    @Property(nameInDb = "userid")
    private String userId;

    @Property(nameInDb = "name")
    @NotNull
    private String name;


    @Property(nameInDb = "ctime")
    @NotNull
    private Long ctime;

    @Property(nameInDb = "path")
    private String paht;

    @Generated(hash = 1346106560)
    public User(Long id, @NotNull String userId, @NotNull String name,
            @NotNull Long ctime, String paht) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.ctime = ctime;
        this.paht = paht;
    }

    @Generated(hash = 586692638)
    public User() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCtime() {
        return this.ctime;
    }

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    public String getPaht() {
        return this.paht;
    }

    public void setPaht(String paht) {
        this.paht = paht;
    }


   
}
