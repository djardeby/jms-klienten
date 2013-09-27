package jmsklienten;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import jmsklienten.runnables.QueueSingleConsumerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class JmsKlientenController {
    private static final Logger log = LoggerFactory.getLogger(JmsKlientenController.class);

    // Set up all the default values
    private static final String DEFAULT_MESSAGE = "Daniel";
    private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
    private static final String DEFAULT_DESTINATION = "jms/queue/test";
    private static final String DEFAULT_MESSAGE_COUNT = "1";
    private static final String DEFAULT_USERNAME = "jmstest";
    private static final String DEFAULT_PASSWORD = "jmstestpwd";
    private static final String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
    private static final String PROVIDER_URL = "remote://localhost:4447";
    private static final int maxLength = 5;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private Destination destination;
    private TextMessage message;
    private Context context;


    @FXML
    private TextArea fileNameTextField;
    @FXML
    private TextField sliderValueTextField;
    @FXML
    private Label messageLabel;
    @FXML
    private Slider slider;
    @FXML
    private Button sendButton;

    private Stage stage;
    private FileChooser fileChooser ;
    private File file;

    public void initialize() {
        initFileChooser();
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                                Number oldValue, Number newValue) {
                sliderValueTextField.setText("" + newValue.intValue());
            }
        });
        sliderValueTextField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue,      String oldValue, String newValue) {
                try {
                    // force numeric value by resetting to old value if exception is thrown
                    Integer.parseInt(newValue);
                    // force correct length by resetting to old value if longer than maxLength
                    if(newValue.length() > maxLength || !newValue.matches("[0-9]{1,}"))
                        sliderValueTextField.setText(oldValue);
                } catch (Exception e) {
                    sliderValueTextField.setText(oldValue);
                }
            }
        });
        sliderValueTextField.setText("" + (int) slider.getValue());
    }

    private void initConnectionFactory() throws NamingException {
        // Set up the context for the JNDI lookup
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, PROVIDER_URL));
        env.put(Context.SECURITY_PRINCIPAL, System.getProperty("username", DEFAULT_USERNAME));
        env.put(Context.SECURITY_CREDENTIALS, System.getProperty("password", DEFAULT_PASSWORD));
        context = new InitialContext(env);

        // Perform the JNDI lookups
        String connectionFactoryString = System.getProperty("connection.factory", DEFAULT_CONNECTION_FACTORY);
        log.info("Attempting to acquire connection factory \"" + connectionFactoryString + "\"");
        connectionFactory = (ConnectionFactory) context.lookup(connectionFactoryString);
        log.info("Found connection factory \"" + connectionFactoryString + "\" in JNDI");
    }

    private void initFileChooser() {
        fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setTitle("JMS-klienten message chooser");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Alla filer", "*.*"),
                new FileChooser.ExtensionFilter("XML", "*.xml")
        );
    }

    public void sendMessage() throws Exception {
        try {
            String everything = fileNameTextField.getText();
            initConnectionFactory();

//            if (builder.length() > 0) {
//                String name = builder.toString();
//                log.debug("Saying hello to " + name);
//                messageLabel.setText("Hello " + name);
//            } else {
//                log.debug("Neither first name nor last name was set, saying hello to anonymous person");
//                messageLabel.setText("Hello mysterious person");
//            }


            String destinationString = System.getProperty("destination", DEFAULT_DESTINATION);
            log.info("Attempting to acquire destination \"" + destinationString + "\"");
            destination = (Destination) context.lookup(destinationString);
            log.info("Found destination \"" + destinationString + "\" in JNDI");

            // Create the JMS connection, session, producer, and consumer
            connection = connectionFactory.createConnection(System.getProperty("username", DEFAULT_USERNAME), System.getProperty("password", DEFAULT_PASSWORD));
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer(destination);
            connection.start();

            int count = Integer.parseInt(System.getProperty("jmsklienten.consume.message.count", DEFAULT_MESSAGE_COUNT));
            String content = everything != "" ? everything :
                    System.getProperty("jmsklienten.send.message.content", DEFAULT_MESSAGE);

            log.info("Sending " + count + " messages with content: " + content);

            // Send the specified number of messages
            for (int i = 0; i < 100; i++) {
                message = session.createTextMessage("id: " + i);
//                message = session.createTextMessage(content);
                producer.send(message);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        } finally {
            if (context != null) {
                context.close();
            }

            // closing the connection takes care of the session, producer, and consumer
            if (connection != null) {
                connection.close();
            }
        }


    }

    public void consumeMessage() throws Exception {

        int count = (Integer.parseInt(sliderValueTextField.getText()) == (int)slider.getValue() ?
                (int) slider.getValue() : Integer.parseInt(sliderValueTextField.getText()));

        log.debug("Consuming " + count + (count == 1 ? " message" : " messages"));
        try {
            initConnectionFactory();
            String destinationString = System.getProperty("destination", DEFAULT_DESTINATION);
            log.info("Attempting to acquire destination \"" + destinationString + "\"");
            destination = (Destination) context.lookup(destinationString);
            log.info("Found destination \"" + destinationString + "\" in JNDI");

            // Create the JMS connection, session, producer, and consumer
            connection = connectionFactory.createConnection(System.getProperty("username", DEFAULT_USERNAME), System.getProperty("password", DEFAULT_PASSWORD));
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            long before = System.currentTimeMillis();
            QueueBrowser browser = session.createBrowser((Queue) destination);
            Enumeration enumeration = browser.getEnumeration();
            List<Message> messageList = new ArrayList<Message>();
            while (enumeration.hasMoreElements()) {
                messageList.add((Message) enumeration.nextElement());
            }
            log.debug("Messages on queue: " + messageList.size());
            messageList.get(0).getJMSCorrelationID();
            consumer = session.createConsumer(destination);
            connection.start();





            if (System.getProperty("jmsklienten.multithreaded", "no").equals("yes")) {

                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                List<Future<String>> list = new ArrayList<Future<String>>();
                count = 0;
                for (int i = 0; i < count; i++) {
                    Callable<String> worker = new QueueSingleConsumerImpl(consumer);
                    Future<String> submit = executor.submit(worker);
                    list.add(submit);
                }

                log.debug("Created futurelist of " + list.size());

                for (Future<String> future : list) {
                    try {
                        String messageText = future.get();
                        if (messageText!= null) {
                            log.debug(messageText);
                        } else {
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                executor.shutdown();
            } else {
                for (int i = 0 ; i < count ; i++) {
                    message = (TextMessage) consumer.receive(500);
                    if (message != null) {
                        log.info("Received message with content " + message.getText());
                    } else {
                        log.info("Received no messages");
                        break;
                    }
                }
            }
            long after = System.currentTimeMillis();
            log.debug("Time: " + (after-before) + " ms");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        } finally {
            log.debug("Cleaning up");
            if (context != null) {
                context.close();
            }
            // closing the connection takes care of the session, and consumer
            if (connection != null) {
                connection.close();
            }
        }
    }


    public void handleAboutAction() {
        log.debug("About JmsKlienten");
        final Label hello = new Label();
        hello.setText("Detta är en varning");
        final TextField name2 = new TextField();
        Button ok = new Button("ok");
        Button cencel = new Button("cancel");

        VBox popUpVBox = new VBox();
        popUpVBox.getChildren().add(hello);
        popUpVBox.getChildren().add(name2);
        popUpVBox.getChildren().add(ok);
        popUpVBox.getChildren().add(cencel);
        Popup popupWindow = new Popup();
        popupWindow.setX(300);
        popupWindow.setY(200);
        popupWindow.setOpacity(0.78);
        popupWindow.setAutoHide(true);
        popupWindow.getContent().addAll(popUpVBox);
        popupWindow.show(getStage());

    }

    public void handleKeyInput() {
    }

    public void chooseFile() throws IOException, UnsupportedEncodingException {
        log.debug("Choosing file");
        file = fileChooser.showOpenDialog(getStage());

        if (file != null) {
//            messageLabel.setText(file.getAbsolutePath());
//            String firstName = fileNameTextField.getText();
//            file = new File(firstName);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(file), "UTF8"));
//            StringBuilder builder = new StringBuilder();
            String everything = "";
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append('\n');
                    line = br.readLine();
                }
                everything = sb.toString();
            } catch (IOException e) {


            } finally {
                br.close();
            }
////            log.debug(everything);
//            if (!StringUtils.isEmpty(firstName)) {
//                builder.append(firstName);
//            }
            fileNameTextField.setText(everything);

            sendButton.setDisable(false);
        }
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
