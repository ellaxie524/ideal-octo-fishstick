package com.evaluation;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private int id;              // 学生编号
    private String name;         // 学生姓名
    private String password;     // 学生登录密码
    private List<String> evaluatedCourses;   // 已评价课程
    private List<String> unevaluatedCourses; // 未评价课程

    // 构造方法
    public Student(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.evaluatedCourses = new ArrayList<>();
        this.unevaluatedCourses = new ArrayList<>();
    }

    // 无参构造方法（数据库查询等需要）
    public Student() {
        this.evaluatedCourses = new ArrayList<>();
        this.unevaluatedCourses = new ArrayList<>();
    }

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getEvaluatedCourses() {
        return evaluatedCourses;
    }

    public List<String> getUnevaluatedCourses() {
        return unevaluatedCourses;
    }

    // 添加课程（已评或未评）
    public void addEvaluatedCourse(String courseName) {
        if (!evaluatedCourses.contains(courseName)) {
            evaluatedCourses.add(courseName);
        }
        unevaluatedCourses.remove(courseName);
    }

    public void addUnevaluatedCourse(String courseName) {
        if (!unevaluatedCourses.contains(courseName) &&
            !evaluatedCourses.contains(courseName)) {
            unevaluatedCourses.add(courseName);
        }
    }

    // 学生评价课程的方法
    public void evaluateCourse(String courseName, String comment) {
        if (unevaluatedCourses.contains(courseName)) {
            System.out.println("学生 " + name + " 对课程《" + courseName + "》的评价: " + comment);
            addEvaluatedCourse(courseName);
        } else {
            System.out.println("该课程已评价或不存在。");
        }
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", evaluatedCourses=" + evaluatedCourses +
                ", unevaluatedCourses=" + unevaluatedCourses +
                '}';
    }
}
