package jmsklienten;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
    public static final String PROPERTIES_FILE = "/properties/default.properties";
    public static final String FXML_FILE = "/fxml/nyjmsklienten.fxml";
    public static final String currentDirectory = System.getProperty("user.dir");

    public static void main(String[] args) throws Exception {
        log.debug("Current directory: " + currentDirectory);
        setUp();
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        log.info("Starting JMS-Klienten version " + System.getProperties().getProperty("jmsklienten.version"));
        log.debug("Loading FXML for main view from: ", FXML_FILE);
        Parent rootNode = initController(stage);
        Scene scene = initScene(rootNode);
        initStage(stage, scene);
    }

    private static void setUp() throws FileNotFoundException {
        Properties p = new Properties();
        InputStream in = null;
        try {
            File file = new File(currentDirectory + "\\JmsKlienten.properties");
            in = MainApp.class.getResourceAsStream(PROPERTIES_FILE);
            p.load(in);
            if (file.exists()) {
                log.debug("Found properties file");
                in = new FileInputStream(file);
                p.load(in);
            }

        } catch (IOException e) {
            log.error("Unable to load properties from " + PROPERTIES_FILE + e.getMessage());
        } finally {
            IOUtils.closeQuietly(in);
        }
        for (String name : p.stringPropertyNames()) {
            String value = p.getProperty(name);
            log.debug(name + "=" + value);
            System.setProperty(name, value);
        }
    }

    private void initStage(Stage stage, Scene scene) {
        stage.setTitle("JMS-klienten " +System.getProperties().getProperty("jmsklienten.version") );
        stage.setScene(scene);
        stage.show();
    }

    private Scene initScene(Parent rootNode) {
        Scene scene = new Scene(rootNode, 600, 400);
        scene.getStylesheets().add("/styles/styles.css");
        return scene;
    }

    private Parent initController(final Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(FXML_FILE));
        JmsKlientenController jmsKlientenController = (JmsKlientenController) loader.getController();
        jmsKlientenController.setStage(stage);
        return rootNode;
    }
}
