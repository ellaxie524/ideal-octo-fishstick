package com.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Teacher {
    private int id;
    private String name;
    private String password;
    private List<String> teachingCourses;
    private Map<String, Double> courseGrades;
    private List<String> reevaluationRequests;

    public Teacher(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.teachingCourses = new ArrayList<>();
        this.courseGrades = new HashMap<>();
        this.reevaluationRequests = new ArrayList<>();
    }

    public Teacher() {
        this.teachingCourses = new ArrayList<>();
        this.courseGrades = new HashMap<>();
        this.reevaluationRequests = new ArrayList<>();
    }

    // getter & setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<String> getTeachingCourses() { return teachingCourses; }
    public Map<String, Double> getCourseGrades() { return courseGrades; }
    public List<String> getReevaluationRequests() { return reevaluationRequests; }

    public void addCourse(String courseName) {
        if (!teachingCourses.contains(courseName)) {
            teachingCourses.add(courseName);
        }
    }

    public void setCourseGrade(String courseName, double grade) {
        courseGrades.put(courseName, grade);
    }

    public void applyReevaluation(String courseName, String reason) {
        reevaluationRequests.add("课程：" + courseName + " 原因：" + reason);
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", teachingCourses=" + teachingCourses +
                ", courseGrades=" + courseGrades +
                ", reevaluationRequests=" + reevaluationRequests +
                '}';
    }
}
