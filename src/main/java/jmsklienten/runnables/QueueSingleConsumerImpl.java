package jmsklienten.runnables;

import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

/**
 * Created with IntelliJ IDEA.
 * User: Daniel
 * Date: 2013-09-25
 * Time: 14:03
 * To change this template use File | Settings | File Templates.
 */
public class QueueSingleConsumerImpl implements QueueConsumer {
    private MessageConsumer consumer;

    public QueueSingleConsumerImpl(final MessageConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public String call() throws Exception {
        String returnValue = null;
         TextMessage textMessage = (TextMessage) consumer.receive(500);  //To change body of implemented methods use File | Settings | File Templates.
        if (textMessage != null) {
        returnValue = textMessage.getText();
        }
        return returnValue;
    }
}
