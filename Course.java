package com.evaluation;

public class Course {
    private String courseId;      // 课程编号
    private String courseName;    // 课程名称
    private int credit;           // 学分
    private String teacherId;     // 授课教师编号（外键关联 Teacher）

    // 无参构造函数
    public Course() {}

    // 带参构造函数
    public Course(String courseId, String courseName, int credit, String teacherId) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.credit = credit;
        this.teacherId = teacherId;
    }

    // Getter 和 Setter 方法
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public int getCredit() {
        return credit;
    }

    public void setCredit(int credit) {
        this.credit = credit;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    // 打印课程信息
    @Override
    public String toString() {
        return "Course {" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", credit=" + credit +
                ", teacherId='" + teacherId + '\'' +
                '}';
    }
}
