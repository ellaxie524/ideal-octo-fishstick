package com.evaluation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Stage primaryStage;
    private Scene loginScene, studentScene, teacherScene, adminScene;
    private int loggedStudentId = -1;
    private int loggedTeacherId = -1;

    // 学生界面
    private ListView<String> lvUnevaluated = new ListView<>();
    private ListView<String> lvEvaluated = new ListView<>();

    // 教师界面
    private ListView<String> lvTeacherCourses = new ListView<>();
    private ListView<String> lvCourseComments = new ListView<>();

    // 管理员界面
    private ListView<String> lvAdminList = new ListView<>();
    private VBox detailBox = new VBox();
    private ComboBox<String> adminMenu = new ComboBox<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("课程评价系统（GUI）");

        initLoginScene();
        initStudentScene();
        initTeacherScene();
        initAdminScene();

        primaryStage.setScene(loginScene);
        primaryStage.setWidth(900);
        primaryStage.setHeight(600);
        primaryStage.show();
    }

    // ------------------ 登录界面 ------------------
    private void initLoginScene() {
        Label roleLabel = new Label("选择身份：");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("学生", "老师", "管理员");
        roleCombo.getSelectionModel().select(0);

        Label idLabel = new Label("账号(ID)：");
        TextField idField = new TextField();
        Label pwdLabel = new Label("密码：");
        PasswordField pwdField = new PasswordField();

        Button loginBtn = new Button("登录");
        Label msgLabel = new Label();

        loginBtn.setOnAction(e -> {
            String role = roleCombo.getValue();
            String idText = idField.getText().trim();
            String pwd = pwdField.getText().trim();
            if (role.equals("学生")) {
                if (loginStudent(idText, pwd)) {
                    refreshStudentScene();
                    primaryStage.setScene(studentScene);
                } else msgLabel.setText("学生登录失败！");
            } else if (role.equals("老师")) {
                if (loginTeacher(idText, pwd)) {
                    refreshTeacherScene();
                    primaryStage.setScene(teacherScene);
                } else msgLabel.setText("教师登录失败！");
            } else {
                // 管理员现在需要输入正确的 ID 和密码才能登录
                if (loginAdmin(idText, pwd)) {
                    refreshAdminList();
                    primaryStage.setScene(adminScene);
                } else msgLabel.setText("管理员登录失败！(请输入正确的 ID 和密码)");
            }
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(roleLabel, 0, 0);
        grid.add(roleCombo, 1, 0);
        grid.add(idLabel, 0, 1);
        grid.add(idField, 1, 1);
        grid.add(pwdLabel, 0, 2);
        grid.add(pwdField, 1, 2);
        grid.add(loginBtn, 1, 3);
        grid.add(msgLabel, 1, 4);

        loginScene = new Scene(grid);
    }

    // ------------------ 学生界面 ------------------
    private void initStudentScene() {
        Label title = new Label("学生主界面");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        VBox leftBox = new VBox(new Label("未评价课程"), lvUnevaluated);
        VBox rightBox = new VBox(new Label("已评价课程"), lvEvaluated);
        leftBox.setSpacing(8); rightBox.setSpacing(8);
        lvUnevaluated.setPrefWidth(350); lvUnevaluated.setPrefHeight(250);
        lvEvaluated.setPrefWidth(350); lvEvaluated.setPrefHeight(250);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("写下你的评价...");
        commentArea.setPrefRowCount(3);

    TextField ratingField = new TextField();
    ratingField.setPromptText("评分 (0-100)");
    ratingField.setPrefWidth(80);

    ComboBox<String> ratingMode = new ComboBox<>();
    ratingMode.getItems().addAll("总体评分", "分项评分");
    ratingMode.getSelectionModel().select(0);

    // detailed ratings
    TextField tfAtmos = new TextField(); tfAtmos.setPromptText("课堂氛围 (0-100)"); tfAtmos.setPrefWidth(100);
    TextField tfTeach = new TextField(); tfTeach.setPromptText("教学能力 (0-100)"); tfTeach.setPrefWidth(100);
    TextField tfNeed = new TextField(); tfNeed.setPromptText("课程需求度 (0-100)"); tfNeed.setPrefWidth(100);

    HBox detailedBox = new HBox(new Label("氛围:"), tfAtmos, new Label("教学:"), tfTeach, new Label("需求:"), tfNeed);
    detailedBox.setSpacing(8);
    detailedBox.setVisible(false);

        Button evalBtn = new Button("提交评价");
        ratingMode.setOnAction(ae -> {
            boolean detailed = "分项评分".equals(ratingMode.getValue());
            ratingField.setVisible(!detailed);
            detailedBox.setVisible(detailed);
        });

        evalBtn.setOnAction(e -> {
            String selectedCourse = lvUnevaluated.getSelectionModel().getSelectedItem();
            if (selectedCourse == null) { showAlert(Alert.AlertType.WARNING, "请选择未评价课程"); return; }
            int courseId = Integer.parseInt(selectedCourse.split(" - ")[0]);
            String comment = commentArea.getText().trim();
            if ("总体评分".equals(ratingMode.getValue())) {
                String ratingText = ratingField.getText().trim();
                int rating;
                try { rating = Integer.parseInt(ratingText); } catch (NumberFormatException ex) { showAlert(Alert.AlertType.WARNING, "请输入有效的整数评分 (0-100)"); return; }
                if (rating < 0 || rating > 100) { showAlert(Alert.AlertType.WARNING, "评分范围应为 0-100"); return; }
                if (rating < 50) { showAlert(Alert.AlertType.WARNING, "评分低于50被视为恶意评分，无法提交"); return; }
                if (submitEvaluation(loggedStudentId, courseId, rating, comment)) {
                    showAlert(Alert.AlertType.INFORMATION, "评价提交成功");
                    refreshStudentScene(); commentArea.clear(); ratingField.clear();
                } else showAlert(Alert.AlertType.ERROR, "评价提交失败");
            } else {
                // detailed mode
                int a,t,n;
                try { a = Integer.parseInt(tfAtmos.getText().trim()); t = Integer.parseInt(tfTeach.getText().trim()); n = Integer.parseInt(tfNeed.getText().trim()); }
                catch(NumberFormatException ex){ showAlert(Alert.AlertType.WARNING, "请为所有分项输入 0-100 的整数"); return; }
                if (a<0||a>100||t<0||t>100||n<0||n>100){ showAlert(Alert.AlertType.WARNING, "分项评分范围应为 0-100"); return; }
                int overall = (int)Math.round((a + t + n) / 3.0);
                // try to insert detailed ratings; submitEvaluationDetailed will fall back if DB lacks columns
                if (submitEvaluationDetailed(loggedStudentId, courseId, overall, a, t, n, comment)){
                    showAlert(Alert.AlertType.INFORMATION, "评价提交成功"); refreshStudentScene(); commentArea.clear(); tfAtmos.clear(); tfTeach.clear(); tfNeed.clear();
                } else showAlert(Alert.AlertType.ERROR, "评价提交失败");
            }
        });

    HBox evalControls = new HBox(new Label("评分模式:"), ratingMode, ratingField, evalBtn);
        evalControls.setSpacing(10);

        HBox lists = new HBox(leftBox, rightBox);
        lists.setSpacing(20);

        Button back = new Button("登出");
        back.setOnAction(ev -> { loggedStudentId=-1; primaryStage.setScene(loginScene); });

        VBox root = new VBox(title, lists, commentArea, detailedBox, evalControls, back);
        root.setSpacing(12);
        root.setPadding(new Insets(12));
        studentScene = new Scene(root);
    }

    private void refreshStudentScene() {
        if (loggedStudentId <= 0) return;
        ObservableList<String> unevaluated = FXCollections.observableArrayList();
        ObservableList<String> evaluated = FXCollections.observableArrayList();
        String qUnevalFallback = "SELECT id, name FROM course WHERE id NOT IN (SELECT course_id FROM evaluation WHERE student_id=?)";
        String qUnevalEnroll = "SELECT c.id, c.name FROM course c JOIN course_student cs ON c.id=cs.course_id WHERE cs.student_id=? AND c.id NOT IN (SELECT course_id FROM evaluation WHERE student_id=?)";
        String qEval = "SELECT c.id, c.name, e.rating, e.comment FROM course c JOIN evaluation e ON c.id=e.course_id WHERE e.student_id=?";
        try (Connection conn = DBUtil.getConnection()) {
            // 优先尝试基于 course_student 的报名过滤（仅显示该学生被分配的课程），若表不存在则回退到全部课程
            try {
                PreparedStatement ps1 = conn.prepareStatement(qUnevalEnroll);
                ps1.setInt(1, loggedStudentId);
                ps1.setInt(2, loggedStudentId);
                ResultSet rs1 = ps1.executeQuery();
                while (rs1.next()) unevaluated.add(rs1.getInt("id") + " - " + rs1.getString("name"));
            } catch (SQLException exEnroll) {
                // 回退：若 course_student 表不存在或查询失败，则显示全部课程仍未评价的
                PreparedStatement ps1 = conn.prepareStatement(qUnevalFallback);
                ps1.setInt(1, loggedStudentId);
                ResultSet rs1 = ps1.executeQuery();
                while (rs1.next()) unevaluated.add(rs1.getInt("id") + " - " + rs1.getString("name"));
            }
            PreparedStatement ps2 = conn.prepareStatement(qEval);
            ps2.setInt(1, loggedStudentId);
            ResultSet rs2 = ps2.executeQuery();
            while(rs2.next()) {
                evaluated.add(rs2.getInt("id") + " - " + rs2.getString("name") +
                        " (评分:" + rs2.getInt("rating") + ", 评论:" + rs2.getString("comment") + ")");
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    lvUnevaluated.setItems(unevaluated);
    }

    private boolean submitEvaluation(int studentId, int courseId, int rating, String comment) {
        String sql = "INSERT INTO evaluation(student_id, course_id, rating, comment, status) VALUES (?,?,?,?, '已评')";
        try (Connection conn = DBUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.executeUpdate();
            return true;
        } catch(SQLException ex) {
            // log and return false
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * 尝试以分项形式插入评价（如果 evaluation 表支持分项列），如果不支持则回退到仅插入总体评分并把分项写入 comment 中。
     */
    private boolean submitEvaluationDetailed(int studentId, int courseId, int overall, int atmosphere, int teaching, int need, String comment){
        // 首先尝试插入带分项列的 SQL（如果表有这些列）
        String sqlDetailed = "INSERT INTO evaluation(student_id, course_id, rating, comment, status, atmosphere, teaching, need) VALUES (?,?,?,?, '已评',?,?,?)";
        try (Connection conn = DBUtil.getConnection()){
            try (PreparedStatement ps = conn.prepareStatement(sqlDetailed)){
                ps.setInt(1, studentId); ps.setInt(2, courseId); ps.setInt(3, overall); ps.setString(4, comment);
                ps.setInt(5, atmosphere); ps.setInt(6, teaching); ps.setInt(7, need);
                ps.executeUpdate();
                return true;
            }
        } catch(SQLException ex){
            // 如果失败（很可能是表没有这些列），回退：把分项内容追加到 comment，然后调用普通插入
            String appended = comment + "\n(分项评分 - 氛围:"+atmosphere+", 教学:"+teaching+", 需求:"+need+")";
            return submitEvaluation(studentId, courseId, overall, appended);
        }
    }

    // ------------------ 教师界面 ------------------
    private void initTeacherScene() {
        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(10);

        Label title = new Label("教师主界面");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        VBox leftBox = new VBox(new Label("教授课程及平均分"), lvTeacherCourses);
        VBox rightBox = new VBox(new Label("课程评价"), lvCourseComments);
        leftBox.setSpacing(8); rightBox.setSpacing(8);
        lvTeacherCourses.setPrefWidth(350); lvTeacherCourses.setPrefHeight(250);
        lvCourseComments.setPrefWidth(350); lvCourseComments.setPrefHeight(250);

        HBox lists = new HBox(leftBox, rightBox);
        lists.setSpacing(20);

        Button back = new Button("登出");
        back.setOnAction(e -> { loggedTeacherId=-1; primaryStage.setScene(loginScene); });

        // teacher controls
        TextField teacherRatingField = new TextField();
        teacherRatingField.setPromptText("评分 (0-100)");
        teacherRatingField.setPrefWidth(100);
        TextArea teacherCommentArea = new TextArea();
        teacherCommentArea.setPromptText("老师备注（可选）");
        teacherCommentArea.setPrefRowCount(2);
        Button teacherRateBtn = new Button("评分课程");
        Button appealBtn = new Button("申请复议");

        HBox teacherControls = new HBox(new Label("评分:"), teacherRatingField, teacherRateBtn, appealBtn);
        teacherControls.setSpacing(10);

        // replaced lists container to include controls on the right
        VBox rightWithControls = new VBox(new Label("课程评价"), lvCourseComments, teacherCommentArea, teacherControls);
        rightWithControls.setSpacing(8);
        HBox listsWithControls = new HBox(leftBox, rightWithControls);
        listsWithControls.setSpacing(20);

        root.getChildren().addAll(title, listsWithControls, back);
        teacherScene = new Scene(root);

        // when a course is selected, load anonymous evaluations
        lvTeacherCourses.getSelectionModel().selectedItemProperty().addListener((obs,o,n)->{
            lvCourseComments.getItems().clear();
            teacherRatingField.clear(); teacherCommentArea.clear();
            if(n!=null){
                // list item format: "<id> - <name> 平均分:..."
                int courseId = Integer.parseInt(n.split(" - ")[0]);
                try(Connection conn = DBUtil.getConnection()){
                    String sql="SELECT e.id, e.student_id, e.rating, e.comment FROM evaluation e WHERE e.course_id=?";
                    PreparedStatement ps=conn.prepareStatement(sql);
                    ps.setInt(1, courseId);
                    ResultSet rs=ps.executeQuery();
                    while(rs.next()){
                        int evalId = rs.getInt("id");
                        Object sidObj = rs.getObject("student_id");
                        Integer sid = sidObj==null ? null : ((Number)sidObj).intValue();
                        int rating = rs.getInt("rating");
                        String comment = rs.getString("comment");
                        String who = sid!=null ? "匿名学生" : "教师评分";
                        lvCourseComments.getItems().add(evalId + " | " + who + " - 评分:" + rating + " 评论:" + (comment==null?"":comment));
                    }
                }catch(SQLException ex){ ex.printStackTrace(); }
            }
        });

        // teacher rate action: insert evaluation with student_id = 0
        teacherRateBtn.setOnAction(e -> {
            String sel = lvTeacherCourses.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert(Alert.AlertType.WARNING, "请选择课程"); return; }
            int courseId;
            try {
                courseId = Integer.parseInt(sel.split(" - ")[0].trim());
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "无法解析课程 ID：" + ex.getMessage());
                return;
            }

            String rt = teacherRatingField.getText().trim(); int rating;
            try { rating = Integer.parseInt(rt); } catch (Exception ex) { showAlert(Alert.AlertType.WARNING, "请输入整数评分 0-100"); return; }
            if (rating < 0 || rating > 100) { showAlert(Alert.AlertType.WARNING, "评分范围为 0-100"); return; }
            if (rating < 50) { showAlert(Alert.AlertType.WARNING, "评分低于50被视为恶意评分，无法提交"); return; }
            String comment = teacherCommentArea.getText().trim();

            // 先校验课程是否存在，再尝试插入，捕获并显示具体 SQL 异常信息以便排查
            String checkSql = "SELECT COUNT(*) AS cnt FROM course WHERE id=?";
            String insertSql = "INSERT INTO evaluation(student_id, course_id, rating, comment, status) VALUES (?,?,?,?, '教师评')";
            try (Connection conn = DBUtil.getConnection()) {
                try (PreparedStatement pcs = conn.prepareStatement(checkSql)) {
                    pcs.setInt(1, courseId);
                    ResultSet rcheck = pcs.executeQuery();
                    if (rcheck.next() && rcheck.getInt("cnt") == 0) {
                        showAlert(Alert.AlertType.ERROR, "所选课程在数据库中不存在，无法提交评分");
                        return;
                    }
                }

                // 防止教师对同一门课程重复评分（若已存在 student_id IS NULL 的教师评分，则阻止重复）
                try (PreparedStatement psDup = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM evaluation WHERE course_id=? AND student_id IS NULL")){
                    psDup.setInt(1, courseId); ResultSet rd = psDup.executeQuery(); if(rd.next() && rd.getInt("cnt")>0){ showAlert(Alert.AlertType.WARNING, "该课程已存在教师评分，若需修改请申请复议或联系管理员"); return; }
                }

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    // 插入教师评分时把 student_id 写为 NULL，以避免触发 student_id 的外键约束（若存在）
                    ps.setNull(1, Types.INTEGER);
                    ps.setInt(2, courseId);
                    ps.setInt(3, rating);
                    ps.setString(4, comment);
                    ps.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "评分提交成功");
                    refreshTeacherScene();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "评分提交失败: " + ex.getMessage());
            }
        });

        // appeal action: update evaluation.status to '申诉'
        appealBtn.setOnAction(e -> {
            String selEval = lvCourseComments.getSelectionModel().getSelectedItem();
            if(selEval==null){ showAlert(Alert.AlertType.WARNING, "请选择一条评价来申诉"); return; }
            int evalId = Integer.parseInt(selEval.split("\\s*\\|\\s*")[0].trim());
            String sql = "UPDATE evaluation SET status='申诉' WHERE id=?";
            try(Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
                ps.setInt(1, evalId); ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "已提交复议申请");
            }catch(SQLException ex){ ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "申诉失败"); }
        });
    }

    private void refreshTeacherScene() {
        if(loggedTeacherId<=0) return;
        ObservableList<String> courseList = FXCollections.observableArrayList();
        try(Connection conn = DBUtil.getConnection()){
            String sql="SELECT c.id, c.name FROM course c WHERE c.teacher_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, loggedTeacherId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                int cid = rs.getInt("id");
                String name = rs.getString("name");
                // compute student avg (student_id>0)
                double studentAvg = 0.0; double teacherAvg = 0.0;
                try (PreparedStatement ps1 = conn.prepareStatement("SELECT AVG(rating) AS avg_rating FROM evaluation WHERE course_id=? AND student_id>0")){
                    ps1.setInt(1, cid); ResultSet r1 = ps1.executeQuery(); if(r1.next()) studentAvg = r1.getDouble("avg_rating");
                }
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT AVG(rating) AS avg_rating FROM evaluation WHERE course_id=? AND student_id IS NULL")){
                    ps2.setInt(1, cid); ResultSet r2 = ps2.executeQuery(); if(r2.next()) teacherAvg = r2.getDouble("avg_rating");
                }
                // handle nulls
                if(Double.isNaN(studentAvg)) studentAvg = 0.0;
                if(Double.isNaN(teacherAvg)) teacherAvg = 0.0;
                double overall = studentAvg * 0.9 + teacherAvg * 0.1;
                courseList.add(cid + " - " + name + " 平均分:" + String.format("%.2f", overall));
            }
        } catch(SQLException ex){ ex.printStackTrace(); }
        lvTeacherCourses.setItems(courseList);
        lvCourseComments.getItems().clear();
    }

    // ------------------ 管理员界面 ------------------
    private void initAdminScene() {
        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(10);

        Label title = new Label("管理员主界面");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        adminMenu.getItems().addAll("学生管理","教师管理","课程管理");
        adminMenu.getSelectionModel().select(0);

        HBox mainBox = new HBox(lvAdminList, detailBox);
        mainBox.setSpacing(20);
        lvAdminList.setPrefWidth(300); lvAdminList.setPrefHeight(400);
        detailBox.setPrefWidth(550); detailBox.setPrefHeight(400);
        detailBox.setSpacing(8);

    Button back = new Button("登出"); back.setOnAction(e -> primaryStage.setScene(loginScene));
    Button validateIdsBtn = new Button("校验所有 ID");
    validateIdsBtn.setOnAction(e -> {
            StringBuilder sb = new StringBuilder();
            try(Connection conn = DBUtil.getConnection()){
                PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM student");
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    String id = String.valueOf(rs.getInt("id"));
                    if(!id.matches("\\d{10}")) sb.append("学生: ").append(id).append(" - ").append(rs.getString("name")).append("\n");
                }
                PreparedStatement ps2 = conn.prepareStatement("SELECT id, name FROM teacher");
                ResultSet rs2 = ps2.executeQuery();
                while(rs2.next()){
                    String id = String.valueOf(rs2.getInt("id"));
                    if(!id.matches("\\d{10}")) sb.append("教师: ").append(id).append(" - ").append(rs2.getString("name")).append("\n");
                }
            } catch(SQLException ex){ ex.printStackTrace(); }
            if(sb.length()==0) showAlert(Alert.AlertType.INFORMATION, "所有 ID 格式均正确");
            else showAlert(Alert.AlertType.WARNING, "以下 ID 格式不正确:\n"+sb.toString());
        });

        // 管理员新增按钮：新增学生、教师、课程
        HBox addBox = new HBox();
        addBox.setSpacing(8);
        Button addStudentBtn = new Button("新增学生");
        Button addTeacherBtn = new Button("新增教师");
        Button addCourseBtn = new Button("新增课程");
        addBox.getChildren().addAll(addStudentBtn, addTeacherBtn, addCourseBtn);
    Button viewAppealsBtn = new Button("处理申诉");

        // 新增学生对话
        addStudentBtn.setOnAction(ev -> {
            Stage dlg = new Stage(); dlg.initOwner(primaryStage); dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            GridPane gp = new GridPane(); gp.setPadding(new Insets(10)); gp.setHgap(8); gp.setVgap(8);
            TextField tfId = new TextField(); TextField tfName = new TextField(); PasswordField tfPwd = new PasswordField();
            gp.add(new Label("ID:"), 0, 0); gp.add(tfId, 1, 0);
            gp.add(new Label("姓名:"), 0, 1); gp.add(tfName, 1, 1);
            gp.add(new Label("密码:"), 0, 2); gp.add(tfPwd, 1, 2);
            Button ok = new Button("保存"); Button cancel = new Button("取消");
            HBox hb = new HBox(ok, cancel); hb.setSpacing(8); gp.add(hb, 1, 3);
            ok.setOnAction(ae -> {
                String idText = tfId.getText().trim();
                if (!idText.matches("\\d{10}")) { showAlert(Alert.AlertType.WARNING, "学生 ID 必须为 10 位数字"); return; }
                try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO student(id,name,password) VALUES(?,?,?)")){
                    ps.setInt(1, Integer.parseInt(idText));
                    ps.setString(2, tfName.getText().trim()); ps.setString(3, tfPwd.getText().trim()); ps.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "新增学生成功"); dlg.close(); refreshAdminList();
                } catch (Exception ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "新增学生失败: " + ex.getMessage()); }
            });
            cancel.setOnAction(ae -> dlg.close());
            dlg.setScene(new Scene(gp)); dlg.setTitle("新增学生"); dlg.showAndWait();
        });

        // 新增教师对话
        addTeacherBtn.setOnAction(ev -> {
            Stage dlg = new Stage(); dlg.initOwner(primaryStage); dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            GridPane gp = new GridPane(); gp.setPadding(new Insets(10)); gp.setHgap(8); gp.setVgap(8);
            TextField tfId = new TextField(); TextField tfName = new TextField(); PasswordField tfPwd = new PasswordField();
            gp.add(new Label("ID:"), 0, 0); gp.add(tfId, 1, 0);
            gp.add(new Label("姓名:"), 0, 1); gp.add(tfName, 1, 1);
            gp.add(new Label("密码:"), 0, 2); gp.add(tfPwd, 1, 2);
            Button ok = new Button("保存"); Button cancel = new Button("取消");
            HBox hb = new HBox(ok, cancel); hb.setSpacing(8); gp.add(hb, 1, 3);
            ok.setOnAction(ae -> {
                String idText = tfId.getText().trim();
                if (!idText.matches("\\d{10}")) { showAlert(Alert.AlertType.WARNING, "教师 ID 必须为 10 位数字"); return; }
                try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO teacher(id,name,password) VALUES(?,?,?)")){
                    ps.setInt(1, Integer.parseInt(idText));
                    ps.setString(2, tfName.getText().trim()); ps.setString(3, tfPwd.getText().trim()); ps.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "新增教师成功"); dlg.close(); refreshAdminList();
                } catch (Exception ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "新增教师失败: " + ex.getMessage()); }
            });
            cancel.setOnAction(ae -> dlg.close());
            dlg.setScene(new Scene(gp)); dlg.setTitle("新增教师"); dlg.showAndWait();
        });

        // 新增课程对话
        addCourseBtn.setOnAction(ev -> {
            Stage dlg = new Stage(); dlg.initOwner(primaryStage); dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            GridPane gp = new GridPane(); gp.setPadding(new Insets(10)); gp.setHgap(8); gp.setVgap(8);
            TextField tfName = new TextField(); ComboBox<String> cbTeacher = new ComboBox<>();
            gp.add(new Label("课程名:"), 0, 0); gp.add(tfName, 1, 0);
            gp.add(new Label("授课教师(可选):"), 0, 1); gp.add(cbTeacher, 1, 1);
            try (Connection conn = DBUtil.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id,name FROM teacher")){
                while (rs.next()) cbTeacher.getItems().add(rs.getInt("id") + " - " + rs.getString("name"));
            } catch (SQLException ex) { ex.printStackTrace(); }
            Button ok = new Button("保存"); Button cancel = new Button("取消");
            HBox hb = new HBox(ok, cancel); hb.setSpacing(8); gp.add(hb, 1, 2);
            ok.setOnAction(ae -> {
                try (Connection conn = DBUtil.getConnection()){
                        // 课程名、教师选择，并可选择多个学生报名
                        ListView<String> lvStudents = new ListView<>(); lvStudents.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
                        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id,name FROM student")){
                            while (rs.next()) lvStudents.getItems().add(rs.getInt("id") + " - " + rs.getString("name"));
                        } catch (SQLException ex) { /* ignore */ }
                        gp.add(new Label("选修学生(可多选):"), 0, 2); gp.add(lvStudents, 1, 2);

                        PreparedStatement ps = conn.prepareStatement("INSERT INTO course(name,teacher_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, tfName.getText().trim());
                        String tsel = cbTeacher.getValue();
                        if (tsel == null) ps.setNull(2, java.sql.Types.INTEGER);
                        else ps.setInt(2, Integer.parseInt(tsel.split(" - ")[0]));
                        ps.executeUpdate();
                        int newCourseId = -1; try (ResultSet gk = ps.getGeneratedKeys()){ if (gk.next()) newCourseId = gk.getInt(1); }
                        // 如果无法获取生成键，尝试通过最近插入的同名记录获取（fallback）
                        if (newCourseId == -1) {
                            try (PreparedStatement p2 = conn.prepareStatement("SELECT id FROM course WHERE name=? ORDER BY id DESC LIMIT 1")){
                                p2.setString(1, tfName.getText().trim()); ResultSet r2 = p2.executeQuery(); if (r2.next()) newCourseId = r2.getInt("id");
                            }
                        }
                        // 为所选学生插入 course_student（若表不存在，会捕获异常并提示）
                        try {
                            if (newCourseId!=-1) {
                                PreparedStatement psIns = conn.prepareStatement("INSERT INTO course_student(student_id,course_id) VALUES(?,?)");
                                for (String s : lvStudents.getSelectionModel().getSelectedItems()){
                                    int sid = Integer.parseInt(s.split(" - ")[0]);
                                    psIns.setInt(1, sid); psIns.setInt(2, newCourseId); psIns.executeUpdate();
                                }
                            }
                        } catch (SQLException exEnroll) {
                            // 普通提示：告诉管理员需要先创建 course_student 表
                            showAlert(Alert.AlertType.WARNING, "课程已创建，但未能分配学生：未检测到 course_student 表或发生错误。请运行初始化脚本以创建该表，或稍后手动分配学生。错误: " + exEnroll.getMessage());
                        }
                        showAlert(Alert.AlertType.INFORMATION, "新增课程成功"); dlg.close(); refreshAdminList();
                    } catch (Exception ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "新增课程失败: " + ex.getMessage()); }
            });
            cancel.setOnAction(ae -> dlg.close());
            dlg.setScene(new Scene(gp)); dlg.setTitle("新增课程"); dlg.showAndWait();
        });

    // 申诉处理按钮放在新增按钮旁
    HBox topBtns = new HBox(); topBtns.setSpacing(8); topBtns.getChildren().addAll(addBox, viewAppealsBtn);
    root.getChildren().addAll(title, adminMenu, validateIdsBtn, topBtns, mainBox, back);
        adminScene = new Scene(root);

        adminMenu.setOnAction(e -> refreshAdminList());
        lvAdminList.getSelectionModel().selectedItemProperty().addListener((obs,o,n)->{ if(n!=null) showAdminDetails(n); });

        // 处理申诉按钮行为：弹窗列出所有 status='申诉' 的评价，管理员可选择处理
        viewAppealsBtn.setOnAction(ev -> {
            Stage dlg = new Stage(); dlg.initOwner(primaryStage); dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            VBox box = new VBox(); box.setPadding(new Insets(10)); box.setSpacing(8);
            ListView<String> lvAppeals = new ListView<>(); lvAppeals.setPrefSize(800, 400);
            // 格式: evalId | 课程ID - 课程名 | student_id/null | rating | comment
            try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT e.id,e.course_id,c.name,e.student_id,e.rating,e.comment FROM evaluation e LEFT JOIN course c ON e.course_id=c.id WHERE e.status='申诉'")){
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    int eid = rs.getInt("id"); int cid = rs.getInt("course_id"); String cname = rs.getString("name");
                    Object sidObj = rs.getObject("student_id"); String who = sidObj==null?"教师":"学生"+String.valueOf(sidObj);
                    int rating = rs.getInt("rating"); String comment = rs.getString("comment");
                    lvAppeals.getItems().add(eid + " | 课程:" + cid + " - " + cname + " | 来自:" + who + " | 评分:" + rating + " | 评论:" + (comment==null?"":comment));
                }
            } catch(SQLException ex){ ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "加载申诉失败: "+ex.getMessage()); return; }

            HBox hb = new HBox(); hb.setSpacing(8);
            Button btnHandle = new Button("要求重评");
            Button btnIgnore = new Button("忽略申诉");
            Button btnClose = new Button("关闭");
            hb.getChildren().addAll(btnHandle, btnIgnore, btnClose);
            box.getChildren().addAll(new Label("待处理申诉列表"), lvAppeals, hb);

            btnHandle.setOnAction(ae -> {
                String sel = lvAppeals.getSelectionModel().getSelectedItem();
                if(sel==null){ showAlert(Alert.AlertType.WARNING, "请选择一条申诉"); return; }
                int evalId = Integer.parseInt(sel.split("\\s*\\|\\s*")[0].trim());
                // 要求重评：删除该条评价，以便老师/学生重新提交
                try (Connection conn2 = DBUtil.getConnection(); PreparedStatement psd = conn2.prepareStatement("DELETE FROM evaluation WHERE id=?")){
                    psd.setInt(1, evalId); psd.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "已删除该评价，相关用户可重新评分");
                    lvAppeals.getItems().remove(sel);
                } catch(SQLException ex){ ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "处理失败: "+ex.getMessage()); }
            });

            btnIgnore.setOnAction(ae -> {
                String sel = lvAppeals.getSelectionModel().getSelectedItem();
                if(sel==null){ showAlert(Alert.AlertType.WARNING, "请选择一条申诉"); return; }
                int evalId = Integer.parseInt(sel.split("\\s*\\|\\s*")[0].trim());
                try (Connection conn2 = DBUtil.getConnection(); PreparedStatement psu = conn2.prepareStatement("UPDATE evaluation SET status='已否决' WHERE id=?")){
                    psu.setInt(1, evalId); psu.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "申诉已忽略"); lvAppeals.getItems().remove(sel);
                } catch(SQLException ex){ ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "忽略失败: "+ex.getMessage()); }
            });

            btnClose.setOnAction(ae -> dlg.close());
            dlg.setScene(new Scene(box)); dlg.setTitle("申诉处理"); dlg.showAndWait();
        });
    }

    private void refreshAdminList() {
        String menu = adminMenu.getValue();
        ObservableList<String> items = FXCollections.observableArrayList();
        try(Connection conn = DBUtil.getConnection()) {
            String sql = "";
            if(menu.equals("学生管理")) sql="SELECT id,name FROM student";
            else if(menu.equals("教师管理")) sql="SELECT id,name FROM teacher";
            else if(menu.equals("课程管理")) sql="SELECT c.id, c.name, t.name AS teacher_name FROM course c LEFT JOIN teacher t ON c.teacher_id=t.id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                if(menu.equals("课程管理")) items.add(rs.getInt("id")+" - "+rs.getString("name")+" (教师:"+rs.getString("teacher_name")+")");
                else items.add(rs.getInt("id")+" - "+rs.getString("name"));
            }
        } catch(SQLException ex){ ex.printStackTrace(); }
        lvAdminList.setItems(items);
        if(!items.isEmpty()) { lvAdminList.getSelectionModel().select(0); showAdminDetails(items.get(0)); }
    }

    private void showAdminDetails(String selected) {
    detailBox.getChildren().clear();
    String menu = adminMenu.getValue();
    int id = Integer.parseInt(selected.split(" - ")[0].trim());
    
    try (Connection conn = DBUtil.getConnection()) {
        if (menu.equals("学生管理")) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM student WHERE id=?");
            ps.setInt(1, id); ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TextField tfName = new TextField(rs.getString("name"));
                TextField tfPwd = new TextField(rs.getString("password"));

                // 显示学生的评价列表
                ListView<String> lvStuEvals = new ListView<>();
                ObservableList<String> stuEvals = FXCollections.observableArrayList();
                try (PreparedStatement pse = conn.prepareStatement("SELECT c.name, e.rating, e.comment FROM evaluation e JOIN course c ON e.course_id=c.id WHERE e.student_id=?")){
                    pse.setInt(1, id); ResultSet rse = pse.executeQuery();
                    while(rse.next()){
                        stuEvals.add(rse.getString("name") + " - 评分:" + rse.getInt("rating") + " 评论:" + (rse.getString("comment")==null?"":rse.getString("comment")));
                    }
                } catch(SQLException ex) { ex.printStackTrace(); }
                lvStuEvals.setItems(stuEvals);

                Button btnUpdate = new Button("修改信息");
                Button btnDelete = new Button("删除学生");
                btnUpdate.setOnAction(ev -> {
                    try(PreparedStatement psu = conn.prepareStatement("UPDATE student SET name=?, password=? WHERE id=?")){
                        psu.setString(1, tfName.getText().trim());
                        psu.setString(2, tfPwd.getText().trim());
                        psu.setInt(3, id); psu.executeUpdate();
                        showAlert(Alert.AlertType.INFORMATION, "修改成功"); refreshAdminList();
                    } catch(SQLException ex){ ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "修改失败"); }
                });
                btnDelete.setOnAction(ev -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确认删除学生及其评价？此操作不可撤销。", ButtonType.OK, ButtonType.CANCEL);
                    confirm.setTitle("确认删除");
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            try (Connection delConn = DBUtil.getConnection()){
                                try (PreparedStatement psd = delConn.prepareStatement("DELETE FROM evaluation WHERE student_id=?")){
                                    psd.setInt(1, id); psd.executeUpdate();
                                }
                                try (PreparedStatement psd2 = delConn.prepareStatement("DELETE FROM student WHERE id=?")){
                                    psd2.setInt(1, id); psd2.executeUpdate();
                                }
                                showAlert(Alert.AlertType.INFORMATION, "删除成功"); refreshAdminList();
                            } catch (SQLException ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "删除失败: " + ex.getMessage()); }
                        }
                    });
                });

                detailBox.getChildren().addAll(new Label("学生名："), tfName, new Label("密码："), tfPwd, new Label("学生评价："), lvStuEvals, btnUpdate, btnDelete);
            }
        } else if (menu.equals("教师管理")) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM teacher WHERE id=?");
            ps.setInt(1, id); ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TextField tfName = new TextField(rs.getString("name"));
                TextField tfPwd = new TextField(rs.getString("password"));

                Button btnUpdate = new Button("修改信息");
                Button btnDelete = new Button("删除教师");
                btnUpdate.setOnAction(ev -> {
                    try(PreparedStatement psu = conn.prepareStatement("UPDATE teacher SET name=?, password=? WHERE id=?")){
                        psu.setString(1, tfName.getText().trim());
                        psu.setString(2, tfPwd.getText().trim());
                        psu.setInt(3, id); psu.executeUpdate();
                        showAlert(Alert.AlertType.INFORMATION, "修改成功"); refreshAdminList();
                    } catch(SQLException ex){ ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "修改失败"); }
                });
                btnDelete.setOnAction(ev -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确认删除教师？将把其授课课程的教师清空。", ButtonType.OK, ButtonType.CANCEL);
                    confirm.setTitle("确认删除");
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            try (Connection delConn = DBUtil.getConnection()){
                                try (PreparedStatement pset = delConn.prepareStatement("UPDATE course SET teacher_id=NULL WHERE teacher_id=?")){
                                    pset.setInt(1, id); pset.executeUpdate();
                                }
                                try (PreparedStatement psd2 = delConn.prepareStatement("DELETE FROM teacher WHERE id=?")){
                                    psd2.setInt(1, id); psd2.executeUpdate();
                                }
                                showAlert(Alert.AlertType.INFORMATION, "删除成功"); refreshAdminList();
                            } catch (SQLException ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "删除失败: " + ex.getMessage()); }
                        }
                    });
                });

                detailBox.getChildren().addAll(new Label("教师名："), tfName, new Label("密码："), tfPwd, btnUpdate, btnDelete);
            }
        } else if (menu.equals("课程管理")) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM course WHERE id=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                TextField tfName = new TextField(rs.getString("name"));
                ComboBox<String> cbTeacher = new ComboBox<>();
                
                // 加载教师列表
                try (Statement st = conn.createStatement();
                     ResultSet rsT = st.executeQuery("SELECT id, name FROM teacher")) {
                    while (rsT.next()) {
                        cbTeacher.getItems().add(rsT.getInt("id") + " - " + rsT.getString("name"));
                    }
                }
                
                // 设置当前教师
                if (rs.getInt("teacher_id") > 0) {
                    for (String s : cbTeacher.getItems()) {
                        if (Integer.parseInt(s.split(" - ")[0]) == rs.getInt("teacher_id")) {
                            cbTeacher.getSelectionModel().select(s);
                            break;
                        }
                    }
                }

                // 更新按钮 - 使用新的连接
                    // 计算综合评分（学生90% + 教师10%），以百分制显示
                    double studentAvg = 0.0, teacherAvg = 0.0;
                    try (PreparedStatement psStu = conn.prepareStatement("SELECT AVG(rating) AS avg_rating FROM evaluation WHERE course_id=? AND student_id>0")) {
                        psStu.setInt(1, id);
                        ResultSet rsStu = psStu.executeQuery();
                        if (rsStu.next()) studentAvg = rsStu.getDouble("avg_rating");
                    } catch (SQLException ex) { /* ignore */ }
                    try (PreparedStatement psTea = conn.prepareStatement("SELECT AVG(rating) AS avg_rating FROM evaluation WHERE course_id=? AND student_id IS NULL")) {
                        psTea.setInt(1, id);
                        ResultSet rsTea = psTea.executeQuery();
                        if (rsTea.next()) teacherAvg = rsTea.getDouble("avg_rating");
                    } catch (SQLException ex) { /* ignore */ }
                    if (Double.isNaN(studentAvg)) studentAvg = 0.0;
                    if (Double.isNaN(teacherAvg)) teacherAvg = 0.0;
                    double overall = studentAvg * 0.9 + teacherAvg * 0.1;
                    Label avgLabel = new Label("综合评分：" + String.format("%.2f", overall));
                    avgLabel.setStyle("-fx-font-weight:bold;");
                    // 如果低于60分，显示提示
                    Label lowWarn = null;
                    if (overall < 60) {
                        lowWarn = new Label("注意：该课程综合评分低于60分");
                        lowWarn.setStyle("-fx-text-fill:darkred;");
                    }
                        // 加载该课程的所有评价（包括学生和教师）供管理员查看
                        ListView<String> lvCourseEvals = new ListView<>(); lvCourseEvals.setPrefSize(800, 200);
                        ObservableList<String> evalItems = FXCollections.observableArrayList();
                        try (PreparedStatement pse = conn.prepareStatement("SELECT e.id, e.student_id, s.name AS student_name, e.rating, e.comment, e.status FROM evaluation e LEFT JOIN student s ON e.student_id=s.id WHERE e.course_id=? ORDER BY e.id DESC")){
                            pse.setInt(1, id); ResultSet rse = pse.executeQuery();
                            while(rse.next()){
                                int eid = rse.getInt("id"); Object sidObj = rse.getObject("student_id");
                                String who = sidObj==null?"教师":"学生"+String.valueOf(sidObj)+(rse.getString("student_name")==null?"":" - "+rse.getString("student_name"));
                                int rating = rse.getInt("rating"); String comment = rse.getString("comment"); String status = rse.getString("status");
                                evalItems.add(eid + " | 来自:" + who + " | 评分:" + rating + " | 评论:" + (comment==null?"":comment) + " | 状态:" + (status==null?"":status));
                            }
                        } catch(SQLException ex) { ex.printStackTrace(); }
                        lvCourseEvals.setItems(evalItems);
                Button btnUpdate = new Button("修改信息");
                btnUpdate.setOnAction(ev -> {
                    String tsel = cbTeacher.getValue();
                    int tid = tsel == null ? 0 : Integer.parseInt(tsel.split(" - ")[0]);
                    
                    try (Connection updateConn = DBUtil.getConnection();
                         PreparedStatement psu = updateConn.prepareStatement("UPDATE course SET name=?, teacher_id=? WHERE id=?")) {
                        psu.setString(1, tfName.getText().trim());
                        psu.setInt(2, tid);
                        psu.setInt(3, id);
                        psu.executeUpdate();
                        showAlert(Alert.AlertType.INFORMATION, "修改成功");
                        refreshAdminList();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "修改失败");
                    }
                });

                // 删除按钮 - 使用新的连接
                Button btnDelete = new Button("删除课程");
                Button manageEnrollBtn = new Button("管理选课");
                // 管理选课弹窗
                manageEnrollBtn.setOnAction(ev -> {
                    Stage dlg = new Stage(); dlg.initOwner(primaryStage); dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                    VBox box = new VBox(); box.setPadding(new Insets(10)); box.setSpacing(8);
                    Label info = new Label("为课程选择需要评价的学生（多选）：");
                    ListView<String> lvAllStudents = new ListView<>(); lvAllStudents.setPrefSize(400, 300);
                    lvAllStudents.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
                    try (Connection c2 = DBUtil.getConnection(); Statement st2 = c2.createStatement(); ResultSet rsS = st2.executeQuery("SELECT id,name FROM student ORDER BY id")){
                        while(rsS.next()) lvAllStudents.getItems().add(rsS.getInt("id") + " - " + rsS.getString("name"));
                    } catch (SQLException ex) {
                        ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "加载学生列表失败: " + ex.getMessage()); dlg.close(); return;
                    }
                    // 预选已分配的学生
                    try (Connection c3 = DBUtil.getConnection(); PreparedStatement psSel = c3.prepareStatement("SELECT student_id FROM course_student WHERE course_id=?")){
                        psSel.setInt(1, id); ResultSet rsSel = psSel.executeQuery();
                        java.util.Set<Integer> assigned = new java.util.HashSet<>();
                        while(rsSel.next()) assigned.add(rsSel.getInt("student_id"));
                        for (int i=0;i<lvAllStudents.getItems().size();i++){
                            String it = lvAllStudents.getItems().get(i);
                            int sid = Integer.parseInt(it.split(" - ")[0]);
                            if (assigned.contains(sid)) lvAllStudents.getSelectionModel().select(i);
                        }
                    } catch (SQLException ex) {
                        // 如果没有 course_student 表，提示并允许创建
                        showAlert(Alert.AlertType.WARNING, "无法读取分配信息（可能尚未创建 course_student 表）：" + ex.getMessage());
                    }

                    HBox hb = new HBox(); hb.setSpacing(8);
                    Button okEnroll = new Button("保存"); Button cancelEnroll = new Button("取消");
                    hb.getChildren().addAll(okEnroll, cancelEnroll);
                    box.getChildren().addAll(info, lvAllStudents, hb);
                    okEnroll.setOnAction(ae -> {
                        // 保存：在事务中先删除旧的再插入新的
                        try (Connection connEnroll = DBUtil.getConnection()){
                            try {
                                connEnroll.setAutoCommit(false);
                                try (PreparedStatement pdel = connEnroll.prepareStatement("DELETE FROM course_student WHERE course_id=?")){
                                    pdel.setInt(1, id); pdel.executeUpdate();
                                }
                                try (PreparedStatement pins = connEnroll.prepareStatement("INSERT INTO course_student(student_id,course_id) VALUES(?,?)")){
                                    for (String s : lvAllStudents.getSelectionModel().getSelectedItems()){
                                        int sid = Integer.parseInt(s.split(" - ")[0]);
                                        pins.setInt(1, sid); pins.setInt(2, id); pins.executeUpdate();
                                    }
                                }
                                connEnroll.commit();
                                showAlert(Alert.AlertType.INFORMATION, "选课分配已保存"); dlg.close();
                            } catch (SQLException ex) {
                                connEnroll.rollback();
                                ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "保存失败: " + ex.getMessage());
                            } finally { connEnroll.setAutoCommit(true); }
                        } catch (SQLException ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "保存失败: " + ex.getMessage()); }
                    });
                    cancelEnroll.setOnAction(ae -> dlg.close());
                    dlg.setScene(new Scene(box)); dlg.setTitle("管理选课 - 课程 " + id); dlg.showAndWait();
                });

                btnDelete.setOnAction(ev -> {
                    try (Connection deleteConn = DBUtil.getConnection();
                         PreparedStatement psd = deleteConn.prepareStatement("DELETE FROM course WHERE id=?")) {
                        psd.setInt(1, id);
                        psd.executeUpdate();
                        showAlert(Alert.AlertType.INFORMATION, "删除成功");
                        refreshAdminList();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "删除失败");
                    }
                });

                Button exportBtn = new Button("导出评价");
                exportBtn.setOnAction(ev -> {
                    // 打开保存对话框并导出该课程的评价到 txt
                    FileChooser fc = new FileChooser();
                    fc.setTitle("导出课程评价");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
                    fc.setInitialFileName("course_" + id + "_evaluations.txt");
                    File out = fc.showSaveDialog(primaryStage);
                    if (out == null) return;
                    try (Connection cexp = DBUtil.getConnection(); PreparedStatement pexp = cexp.prepareStatement("SELECT e.id, e.student_id, s.name AS student_name, e.rating, e.comment, e.status FROM evaluation e LEFT JOIN student s ON e.student_id=s.id WHERE e.course_id=? ORDER BY e.id DESC")){
                        pexp.setInt(1, id); ResultSet rsex = pexp.executeQuery();
                        try (PrintWriter pw = new PrintWriter(out, java.nio.charset.StandardCharsets.UTF_8.name())){
                            while(rsex.next()){
                                int eid = rsex.getInt("id"); Object sidObj = rsex.getObject("student_id");
                                String who = sidObj==null?"教师":"学生"+String.valueOf(sidObj)+(rsex.getString("student_name")==null?"":" - "+rsex.getString("student_name"));
                                int rating = rsex.getInt("rating"); String comment = rsex.getString("comment"); String status = rsex.getString("status");
                                pw.println("评价ID: " + eid);
                                pw.println("来自: " + who);
                                pw.println("评分: " + rating);
                                pw.println("状态: " + (status==null?"":status));
                                pw.println("评论: " + (comment==null?"":comment));
                                pw.println("--------------------------------------------------");
                            }
                        }
                        showAlert(Alert.AlertType.INFORMATION, "导出成功: " + out.getAbsolutePath());
                    } catch (IOException | SQLException ex) { ex.printStackTrace(); showAlert(Alert.AlertType.ERROR, "导出失败: " + ex.getMessage()); }
                });

                HBox btnBox = new HBox(btnUpdate, manageEnrollBtn, btnDelete, exportBtn);
                btnBox.setSpacing(10);
                    if (lowWarn != null) {
                    detailBox.getChildren().addAll(
                        new Label("课程名："), tfName,
                        new Label("授课教师："), cbTeacher,
                        avgLabel,
                        lowWarn,
                        new Label("课程评价："), lvCourseEvals,
                        btnBox
                     );
                } else {
                    detailBox.getChildren().addAll(
                        new Label("课程名："), tfName,
                        new Label("授课教师："), cbTeacher,
                        avgLabel,
                        new Label("课程评价："), lvCourseEvals,
                        btnBox
                    );
                }
            }
        }
        // ... 其他管理类型的代码（学生管理、教师管理）
    } catch (SQLException ex) {
        ex.printStackTrace();
    }
} // ------------------ 登录方法 ------------------
    private boolean loginStudent(String idText, String pwd){
        try(Connection conn=DBUtil.getConnection()){
            // 使用字符串比较 id，兼容 numeric 或 string 类型的 id 存储，避免解析异常
            PreparedStatement ps=conn.prepareStatement("SELECT * FROM student WHERE CAST(id AS CHAR)=? AND password=?");
            ps.setString(1, idText);
            ps.setString(2,pwd);
            ResultSet rs=ps.executeQuery();
            if(rs.next()){ loggedStudentId=rs.getInt("id"); return true; }
        }catch(SQLException ex){ ex.printStackTrace(); }
        return false;
    }

    private boolean loginTeacher(String idText, String pwd){
        try(Connection conn=DBUtil.getConnection()){
            PreparedStatement ps=conn.prepareStatement("SELECT * FROM teacher WHERE CAST(id AS CHAR)=? AND password=?");
            ps.setString(1, idText);
            ps.setString(2,pwd);
            ResultSet rs=ps.executeQuery();
            if(rs.next()){ loggedTeacherId=rs.getInt("id"); return true; }
        }catch(SQLException ex){ ex.printStackTrace(); }
        return false;
    }

    private boolean loginAdmin(String idText, String pwd){
        // 简单校验：要求管理员输入固定 ID 和密码；可替换为数据库表或更复杂的认证
        if (idText == null) return false;
        return idText.equals("admin") && pwd.equals("admin123");
    }

    private void showAlert(Alert.AlertType type, String msg){
        Alert a=new Alert(type,msg); a.showAndWait();
    }
}
