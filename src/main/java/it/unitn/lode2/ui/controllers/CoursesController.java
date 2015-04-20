package it.unitn.lode2.ui.controllers;

import it.unitn.lode2.asset.Course;
import it.unitn.lode2.asset.Lecture;
import it.unitn.lode2.asset.Slide;
import it.unitn.lode2.asset.xml.XmlCourseImpl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * User: tiziano
 * Date: 16/04/15
 * Time: 11:22
 */
public class CoursesController implements Initializable {

    private final static String LODE_HOME = System.getProperty("user.home") + "/_LODE/"; // XXX: to move outside...
    private final static String LODE_CURSES = LODE_HOME + "/COURSES/";

    private List<Course> courses;

    private Map<TreeItem, Object> items = new HashMap<>();

    @FXML
    private TextField titleTextField;

    @FXML
    private TextField numberTextField;

    @FXML
    private ImageView previewImageView;

    @FXML
    private TextField fileNameTextField;

    @FXML
    private TextArea textTextArea;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private Button newCourseButton;

    @FXML
    private Button delCourseButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("init");
        treeView.getSelectionModel().selectedItemProperty().addListener(selectionListener);

        newCourseButton.setOnAction(newCourseHandler);
    }

    public EventHandler<WindowEvent> handlerClose = new EventHandler<WindowEvent>() {
        @Override
        public void handle(WindowEvent event) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm exit");
            alert.setHeaderText("Do you really want to exit?");
            Optional<ButtonType> result = alert.showAndWait();
            if( ButtonType.OK.equals(result.get()) ) {
                // TODO: finalize
                System.out.println("ok, exit");
            } else {
                event.consume();
            }
        }
    };

    public void setCourses(List<Course> courses) {
        this.courses = courses;
        refresh();
    }

    private void refresh() {

        treeView.setRoot(null);

        TreeItem<String> root = new TreeItem<>("COURSES");
        root.setExpanded(true);
        root.setGraphic(createGraphicNode("house"));
        for( Course course: courses ){
            TreeItem<String> courseTreeItem = new TreeItem<>(course.name());
            courseTreeItem.setGraphic(createGraphicNode("book"));
            root.getChildren().add(courseTreeItem);
            items.put(courseTreeItem, course);
            for( Lecture lecture: course.lectures() ){
                TreeItem lectureTreeItem = new TreeItem(lecture.name());
                lectureTreeItem.setGraphic(createGraphicNode("report"));
                courseTreeItem.getChildren().add(lectureTreeItem);
                items.put(lectureTreeItem, lecture);
                for( Slide slide: lecture.slides() ){
                    TreeItem slideTreeItem = new TreeItem(slide.title());
                    slideTreeItem.setGraphic(createGraphicNode("image"));
                    lectureTreeItem.getChildren().add(slideTreeItem);
                    items.put(slideTreeItem, slide);
                }
            }
        }
        treeView.setRoot(root);
    }


    private Node createGraphicNode(String name){
        return new ImageView(new Image(CoursesController.class.getResourceAsStream("/images/" + name + ".png")));
    }

    ChangeListener selectionListener = new ChangeListener(){

        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            TreeItem treeItem = (TreeItem) newValue;
            Object item = items.get(treeItem);
            if( item instanceof Slide ){
                Slide slide = (Slide) item;
                titleTextField.setText(slide.title());
                textTextArea.setText(slide.text());
                TreeItem lectureTreeItem = treeItem.getParent();
                Lecture lecture = (Lecture) items.get(lectureTreeItem);
                TreeItem courseTreeItem = lectureTreeItem.getParent();
                Course course = (Course) items.get(courseTreeItem);
                fileNameTextField.setText(slide.filename());
                previewImageView.setImage(new Image("file://" + lecture.path() + "/" + slide.filename()));
            }
        }
    };

    EventHandler<ActionEvent> newCourseHandler = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            TextInputDialog dialog = new TextInputDialog("New course name");
            dialog.setTitle("New course creation");
            dialog.setHeaderText("Insert the name of the news course");
            dialog.setContentText("Course name:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                String courseFolderName = LODE_CURSES + name; // XXX: check name...
                if( new File(courseFolderName).mkdir() ) {
                    XmlCourseImpl course = new XmlCourseImpl(courseFolderName);
                    course.setName(name);
                    course.save();
                    courses.add(course);
                    refresh();
                }
            });
        }
    };
}
