package cc.riskswap.trader.executor.pubsub.subscriber;

/**
 * Interface for Redis Pub/Sub message handlers
 */
public interface BaseSubscriber {

    /**
     * Get the topic (channel) this handler listens to
     * @return Topic name
     */
    String getChannel();

    /**
     * Handle the message
     * @param message Message content
     */
    void processMessage(String message);
}
