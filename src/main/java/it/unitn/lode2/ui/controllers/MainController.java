package it.unitn.lode2.ui.controllers;

import it.unitn.lode2.IOC;
import it.unitn.lode2.asset.Lecture;
import it.unitn.lode2.camera.Camera;
import it.unitn.lode2.camera.Previewer;
import it.unitn.lode2.recorder.Chronometer;
import it.unitn.lode2.recorder.Recorder;
import it.unitn.lode2.projector.Projector;
import it.unitn.lode2.recorder.ipcam.FFMpegStreamGobbler;
import it.unitn.lode2.ui.skin.AwesomeIcons;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * User: tiziano
 * Date: 07/11/14
 * Time: 09:39
 */
public class MainController implements Initializable {

    private Font fontAwesome;

    private final String RECORDING_COLOR = "red";
    private final String PAUSE_COLOR = "yellow";
    private final String IDLE_COLOR = "black";
    private final String RECORD_LABEL = "REC";
    private final String PAUSE_LABEL = "PAUSE";
    private final String IDLE_LABEL = "";

    private Camera camera;
    private Previewer previewer;
    private Recorder recorder=null;
    private Projector projector;
    private Lecture lecture;
    private ImageView slideImageView=null;
    private Rectangle2D slideScreenBounds;
    private Chronometer chronometer = new Chronometer();

    @FXML private ProgressBar vuMeterProgressBar;

    @FXML private ImageView currentSlideImageView;
    @FXML private ImageView preparedSlideImageView;
    @FXML private ImageView next1SlideImageView;
    @FXML private ImageView next2SlideImageView;

    @FXML private Button firstSlideButton;
    @FXML private Button prevSlideButton;
    @FXML private Button showSlideButton;
    @FXML private Button nextSlideButton;
    @FXML private Button lastSlideButton;
    @FXML private Button goToSlideButton;
    @FXML private TextField goToSlideTextField;

    @FXML private Label currentSlideLabel;
    @FXML private Label preparedSlideLabel;
    @FXML private Label next1SlideLabel;
    @FXML private Label next2SlideLabel;

    @FXML private ImageView previewImageView;
    @FXML private ToggleButton previewToggleButton;

    @FXML private Button setupButton;

    @FXML private Button setupSceneButton;
    @FXML private ToggleButton preset1ToggleButton;
    @FXML private ToggleButton preset2ToggleButton;
    @FXML private ToggleButton preset3ToggleButton;
    @FXML private ToggleButton preset4ToggleButton;
    @FXML private ToggleButton preset1SlideToggleButton;
    @FXML private ToggleButton preset2SlideToggleButton;
    @FXML private ToggleButton preset3SlideToggleButton;
    @FXML private ToggleButton preset4SlideToggleButton;

    @FXML private CheckBox preset1zoomCheckBox;
    @FXML private CheckBox preset2zoomCheckBox;
    @FXML private CheckBox preset3zoomCheckBox;
    @FXML private CheckBox preset4zoomCheckBox;

    @FXML private ToggleButton recordToggleButton;
    @FXML private ToggleButton pauseToggleButton;
    @FXML private Button stopButton;

    private List<ToggleButton> presetsToggleButtons;
    private List<ToggleButton> presetsSlideButtons;
    private List<CheckBox> presetsZoomCheckBoxes;
    private List<ImageView> nextImageViews;
    private List<Label> nextLabels;

    private Timeline timeline;
    private LogsController logsController;

    @FXML private Label recordingLabel;

    //private StreamGobbler errorStreamGobbler = new StreamGobbler();
    //private StreamGobbler standardStreamGobbler = new StreamGobbler();
    private FFMpegStreamGobbler gobbler;

    enum SecondDisplayMode {
        SLIDES, PREVIEW
    }

    SecondDisplayMode secondDisplayMode = SecondDisplayMode.SLIDES;

    // SimpleBooleanProperty per gestire la disabilitazione dei pulsanti


    @Override
    public void initialize(URL location, ResourceBundle resources) {


        // Second screen (to move in Projector)
        ObservableList<Screen> screens = Screen.getScreens();
        if( screens.size()>1 ){
            Screen screen = screens.get(1);
            slideScreenBounds = screen.getBounds();
            slideImageView = new ImageView();
            Pane slidePane = new Pane();
            slidePane.getChildren().add(slideImageView);
            Scene scene = new Scene(slidePane);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setX(slideScreenBounds.getMinX());
            stage.setY(slideScreenBounds.getMinY());
            stage.setWidth(slideScreenBounds.getWidth());
            stage.setHeight(slideScreenBounds.getHeight());
            stage.setFullScreen(true);
            stage.toFront();
            stage.show();
        }

        // IOC to inject the implementations of Camera, Recorder, and Projector
        camera = IOC.queryUtility(Camera.class);
        previewer = IOC.queryUtility(Previewer.class);
        recorder = IOC.queryUtility(Recorder.class);
        projector = IOC.queryUtility(Projector.class);
        lecture = IOC.queryUtility(Lecture.class);

        presetsToggleButtons = Arrays.asList(preset1ToggleButton, preset2ToggleButton, preset3ToggleButton, preset4ToggleButton);
        presetsSlideButtons = Arrays.asList(preset1SlideToggleButton, preset2SlideToggleButton, preset3SlideToggleButton, preset4SlideToggleButton);
        presetsZoomCheckBoxes = Arrays.asList(preset1zoomCheckBox, preset2zoomCheckBox, preset3zoomCheckBox, preset4zoomCheckBox);
        nextImageViews = Arrays.asList(next1SlideImageView, next2SlideImageView);
        nextLabels = Arrays.asList(next1SlideLabel, next2SlideLabel);


        configHandlers();

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(40), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                refreshPreview();
            }
        }));

        //recordToggleButton.disableProperty().bind((new SimpleBooleanProperty(recorder.isIdle())).not());

        projector.first();
        refreshSlides();
        refreshRecorderButtons();

        fontAwesome = Font.loadFont(MainController.class.getResource("/fonts/FontAwesome.otf").
                toExternalForm(), 24);
        setFontAwesome(previewToggleButton, AwesomeIcons.ICON_VIDEO_CAMERA, "black");
        setFontAwesome(setupSceneButton, AwesomeIcons.ICON_COGS, "black");
        setFontAwesome(setupButton, AwesomeIcons.ICON_INFO_SIGN, "black");

        // recorder
        setFontAwesome(recordToggleButton, AwesomeIcons.ICON_CIRCLE, "red");
        setFontAwesome(pauseToggleButton, AwesomeIcons.ICON_PAUSE, "yellow");
        setFontAwesome(stopButton, AwesomeIcons.ICON_STOP, "blue");

        // slide
        setFontAwesome(firstSlideButton, AwesomeIcons.ICON_FAST_BACKWARD, "black");
        setFontAwesome(prevSlideButton, AwesomeIcons.ICON_BACKWARD, "black");
        setFontAwesome(nextSlideButton, AwesomeIcons.ICON_FORWARD, "black");
        setFontAwesome(lastSlideButton, AwesomeIcons.ICON_FAST_FORWARD, "black");

        setFontAwesome(showSlideButton, AwesomeIcons.ICON_CARET_UP, "black");
        setFontAwesome(goToSlideButton, AwesomeIcons.ICON_SIGNIN, "black");

    }

    private void setFontAwesome(ButtonBase button, String iconName, String color) {
        button.setText(iconName);
        button.setFont(fontAwesome);
        button.setStyle("-fx-padding: 0; -fx-text-fill: " + color + ";");
    }

    private Boolean hasSecondDisplay(){
        return slideImageView!=null;
    }

    public void keyBindings() {

        setKeyButtonBinding(KeyCode.UP, showSlideButton);
        setKeyButtonBinding(KeyCode.ENTER, showSlideButton);
        setKeyButtonBinding(KeyCode.LEFT, prevSlideButton);
        setKeyButtonBinding(KeyCode.RIGHT, nextSlideButton);

        // keypad
        setKeyButtonBinding(KeyCode.KP_UP, showSlideButton);
        setKeyButtonBinding(KeyCode.KP_LEFT, prevSlideButton);
        setKeyButtonBinding(KeyCode.KP_RIGHT, nextSlideButton);

        // asdw
        setKeyButtonBinding(KeyCode.W, showSlideButton);
        setKeyButtonBinding(KeyCode.A, prevSlideButton);
        setKeyButtonBinding(KeyCode.D, nextSlideButton);

        setKeyButtonBinding(KeyCode.DIGIT1, preset1ToggleButton);
        setKeyButtonBinding(KeyCode.DIGIT2, preset2ToggleButton);
        setKeyButtonBinding(KeyCode.DIGIT3, preset3ToggleButton);
        setKeyButtonBinding(KeyCode.DIGIT4, preset4ToggleButton);
    }

    private void setKeyButtonBinding(KeyCode code, ButtonBase button){
        button.getScene().getAccelerators().put(new KeyCodeCombination(code), () -> button.fire());
    }

    private void configHandlers() {

        previewToggleButton.setOnAction(handlerPreview);
        setupButton.setOnAction(handlerSetup);
        recordToggleButton.setOnAction(handlerRecord);
        pauseToggleButton.setOnAction(handlerPause);
        stopButton.setOnAction(handlerStop);
        preset1ToggleButton.setOnAction(handlerPreset);
        preset2ToggleButton.setOnAction(handlerPreset);
        preset3ToggleButton.setOnAction(handlerPreset);
        preset4ToggleButton.setOnAction(handlerPreset);
        setupSceneButton.setOnAction(handlerSetupScene);

        firstSlideButton.setOnAction(handlerFirstSlide);
        prevSlideButton.setOnAction(handlerPrevSlide);
        showSlideButton.setOnAction(handlerShowSlide);
        nextSlideButton.setOnAction(handlerNextSlide);
        lastSlideButton.setOnAction(handlerLastSlide);
        goToSlideButton.setOnAction(handlerGoToSlide);
    }

    /* Preview */
    private void refreshPreview() {
        try {
            previewer.snapshot().ifPresent(s -> previewImageView.setImage(new Image(s)));
            if( hasSecondDisplay() && SecondDisplayMode.PREVIEW.equals(secondDisplayMode) ){
                previewer.snapshot().ifPresent(s -> slideImageView.setImage(new Image(s)));
            }
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    private void playPreview() {
        previewer.start();
        timeline.play();
    }

    private void stopPreview() {
        timeline.stop();
        previewer.stop();
        previewImageView.setImage(null);
    }

    private void resetPresetButtons() {
        for( ToggleButton button: presetsToggleButtons){
            button.setSelected(false);
        }
    }

    private void setSlideLabel(Label label, Integer n){
        if( n==-1 ){
            label.setText("--");
        } else {
            label.setText(n.toString());
        }
    }

    /* Slides */
    private void refreshSlides(){

        // XXX: questo è veramente brutto...
        currentSlideLabel.setText("--");
        preparedSlideLabel.setText("--");

        projector.shownSlide().ifPresent(s -> {
            currentSlideImageView.setImage(s.createPreview(400.0, 300.0));
            projector.showSlideNumber(s).ifPresent(n -> currentSlideLabel.setText(n.toString()));
        });
        projector.preparedSlide().ifPresent(s -> {preparedSlideImageView.setImage(s.createPreview(400.0, 300.0));
            projector.showSlideNumber(s).ifPresent(n -> preparedSlideLabel.setText(n.toString()));});
        for( Integer i=0; i<nextImageViews.size(); i++ ) {
            final Integer j=i;
            nextLabels.get(j).setText("--");
            projector.slideDelta(i+1).ifPresent(s -> {nextImageViews.get(j).setImage(s.createPreview(160.0, 120.0));
                projector.showSlideNumber(s).ifPresent(n -> nextLabels.get(j).setText(n.toString()));});
            if (!projector.slideDelta(i+1).isPresent()) {
                nextImageViews.get(i).setImage(null);
            }
        }
    }


    /* Event handling */

    private EventHandler<ActionEvent> handlerZoomInAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            handlerZoomIn.handle(event);
        }
    };
    private EventHandler<Event> handlerZoomIn = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.zoomIn();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
            resetPresetButtons();
        }
    };

    private EventHandler<ActionEvent> handlerZoomOutAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            handlerZoomOut.handle(event);
        }
    };
    private EventHandler<Event> handlerZoomOut = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.zoomOut();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
            resetPresetButtons();
        }
    };
    private EventHandler<Event> handlerZoomStop = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.zoomStop();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
        }
    };

    private EventHandler<ActionEvent> handlerPanLeftAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            handlerPanLeft.handle(event);
        }
    };
    private EventHandler<Event> handlerPanLeft = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.panLeft();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
            resetPresetButtons();
        }
    };
    private EventHandler<ActionEvent> handlerPanRightAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            handlerPanRight.handle(event);
        }
    };
    private EventHandler<Event> handlerPanRight = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.panRight();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
            resetPresetButtons();
        }
    };
    private EventHandler<Event> handlerPanStop = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.panStop();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
        }
    };

    private EventHandler<ActionEvent> handlerTiltUpAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            handlerTiltUp.handle(event);
        }
    };
    private EventHandler<Event> handlerTiltUp = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.tiltUp();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
            resetPresetButtons();
        }
    };
    private EventHandler<ActionEvent> handlerTiltDownAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            handlerTiltDown.handle(event);
        }
    };
    private EventHandler<Event> handlerTiltDown = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera.tiltDown();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            });
            thread.start();
            resetPresetButtons();
        }
    };
    private EventHandler<Event> handlerTiltStop = new EventHandler<Event>() {
        @Override
        public void handle(Event event){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    camera.tiltStop();
                } catch (IOException e) {
                    handleIOException(e);
                }
            }
        });
        thread.start();
        }
    };

    private EventHandler<ActionEvent> handlerPreview = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if( previewToggleButton.isSelected() ){
                playPreview();
            } else {
                stopPreview();
            }
        }
    };

    private EventHandler<ActionEvent> handlerSetup = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/logs.fxml"));
                Parent root = loader.load();
                logsController = loader.getController();
                Scene scene = new Scene(root, 600, 700);
                Stage stage = new Stage();
                stage.setTitle("Acquisition logs");
                stage.setScene(scene);
                stage.show();
                //recorder.errorLog().ifPresent(s -> errorStreamGobbler.stream(s).widget(logsController.getStderrorTextArea()).start());
                //recorder.outputLog().ifPresent(s -> standardStreamGobbler.stream(s).widget(logsController.getStdoutTextArea()).start());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private EventHandler<ActionEvent> handlerRecord = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if( recorder.isIdle() || recorder.isPaused() ) {
                try {
                    recorder.record();
                    chronometer.reset();
                    chronometer.start();

                    // ffmpeg gobbler
                    recorder.errorLog().ifPresent(s -> {
                        gobbler = new FFMpegStreamGobbler(s, vuMeterProgressBar);
                        gobbler.start();
                    });

                    recordingLabel.setText(RECORD_LABEL);
                    recordingLabel.setTextFill(Paint.valueOf(RECORDING_COLOR));
                    // XXX: clear timed slides?
                } catch (IOException e) {
                    handleIOException(e);
                }
            }
            recordToggleButton.setSelected(true);
            refreshRecorderButtons();
        }
    };

    private void refreshRecorderButtons() {
        Recorder recorder = IOC.queryUtility(Recorder.class);
        recordToggleButton.setDisable(!recorder.isIdle());
        pauseToggleButton.setDisable(recorder.isIdle());
        stopButton.setDisable(recorder.isIdle());
    }

    private EventHandler<ActionEvent> handlerPause = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if( recorder.isRecording() ){
                recorder.pause();
                chronometer.stop();
                recordingLabel.setText(PAUSE_LABEL);
                recordingLabel.setTextFill(Paint.valueOf(PAUSE_COLOR));
                gobbler.terminate();
            } else if( recorder.isPaused() ){
                try {
                    recorder.wakeup();
                    recordingLabel.setText(RECORD_LABEL);
                    recordingLabel.setTextFill(Paint.valueOf(RECORDING_COLOR));
                } catch (IOException e) {
                    handleIOException(e);
                }
                chronometer.start();
                recorder.errorLog().ifPresent(s -> {
                    gobbler = new FFMpegStreamGobbler(s, vuMeterProgressBar);
                    gobbler.start();
                });
            }
            else {
                pauseToggleButton.setSelected(false);
            }
            refreshRecorderButtons();
        }
    };

    private EventHandler<ActionEvent> handlerStop = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if( recorder.isRecording() || recorder.isPaused() ){
                recorder.stop();
                chronometer.stop();
                long length = chronometer.elapsed() / 1000;
                System.out.println(length);
                lecture.setVideoLength(length);
                lecture.save();
                //terminateGobblers();
                //logsController.stop();
                recordingLabel.setText(IDLE_LABEL);
                recordingLabel.setTextFill(Paint.valueOf(IDLE_COLOR));
                recordToggleButton.setSelected(false);
                pauseToggleButton.setSelected(false);
                gobbler.terminate();
            }
            refreshRecorderButtons();
        }
    };

    /*
    private void terminateGobblers() {
        if( errorStreamGobbler != null && errorStreamGobbler.isAlive() ) {
            errorStreamGobbler.terminate();
        }
        if( standardStreamGobbler != null && standardStreamGobbler.isAlive() ) {
            standardStreamGobbler.terminate();
        }
    }*/

    private EventHandler<ActionEvent> handlerPreset = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            ToggleButton pressedButton = (ToggleButton) event.getSource();
            Integer i = 0;
            for( ToggleButton button: presetsToggleButtons){
                i++;
                button.setSelected(button == pressedButton);
                if( button == pressedButton ) {
                    try {
                        camera.goToPreset(i.toString());
                        if( presetsZoomCheckBoxes.get(i-1).isSelected() ){
                            camera.zoomIn();
                        } else if( !presetsZoomCheckBoxes.get(i-1).isSelected() ){
                            camera.zoomOut();
                        } else if (presetsZoomCheckBoxes.get(i-1).isIndeterminate() ){
                            // NOP
                        }
                        if( hasSecondDisplay() ) {
                            if (presetsSlideButtons.get(i - 1).isSelected()) {
                                secondDisplayMode = SecondDisplayMode.SLIDES;
                                projector.shownSlide().ifPresent(s -> slideImageView.setImage(s.createPreview(slideScreenBounds.getWidth(), slideScreenBounds.getHeight())));
                            } else {
                                secondDisplayMode = SecondDisplayMode.PREVIEW;
                            }
                        }
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            }
        }
    };

    private EventHandler<ActionEvent> handlerSetupScene = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/camera.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 300, 300);
                Stage stage = new Stage();
                stage.setTitle("Camera controller");
                stage.setScene(scene);
                stage.setX(100);
                stage.setY(100);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

    private EventHandler<ActionEvent> handlerFirstSlide = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if( !projector.isFirst() ){
                projector.first();
                refreshSlides();
            }
        }
    };
    private EventHandler<ActionEvent> handlerPrevSlide = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            projector.previous();
            refreshSlides();
        }
    };
    private EventHandler<ActionEvent> handlerShowSlide = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            projector.show();
            projector.shownSlide().ifPresent(s -> {
                if (slideImageView != null) {
                    slideImageView.setImage(s.createPreview(slideScreenBounds.getWidth(), slideScreenBounds.getHeight()));
                }
                if (recorder.isRecording()) {
                    projector.shownSlideSeqNumber().ifPresent(n -> lecture.addTimedSlide(lecture.slide(n), chronometer.elapsed() / 1000));
                }
            });
            refreshSlides();
        }
    };
    private EventHandler<ActionEvent> handlerNextSlide = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            projector.next();
            refreshSlides();
        }
    };
    private EventHandler<ActionEvent> handlerLastSlide = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            projector.last();
            refreshSlides();
        }
    };
    private EventHandler<ActionEvent> handlerGoToSlide = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            Integer i = Integer.parseInt(goToSlideTextField.getText());
            projector.goTo(i);
            refreshSlides();
        }
    };
    public EventHandler<WindowEvent> handlerClose = new EventHandler<WindowEvent>() {
        @Override
        public void handle(WindowEvent event) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm exit");
            alert.setHeaderText("Do you really want to exit?");
            if( recorder.isRecording() ) {
                alert.setContentText("The current recording session will end.\n\n");
            }
            Optional<ButtonType> result = alert.showAndWait();
            if( ButtonType.OK.equals(result.get()) ) {
                //terminateGobblers();
                previewer.stop();
                if( recorder.isRecording() ){
                    recorder.stop();
                    chronometer.stop();
                    lecture.setVideoLength(chronometer.elapsed() / 1000);
                    lecture.save();
                }
            } else {
                event.consume();
            }
        }
    };

    private void handleIOException(IOException e) {
        e.printStackTrace();
    }

}
