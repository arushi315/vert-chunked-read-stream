import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkedReadStream implements ReadStream<Buffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedReadStream.class);

    private final int chunkSize;
    private final Buffer buffer;
    private final int length;
    private final Context context = Vertx.currentContext();
    private final String transactionId;
    public int sent;
    private boolean paused;
    private boolean written;
    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;

    public ChunkedReadStream(final Buffer buffer, final int chunkSize, final String transactionId) {
        this.chunkSize = chunkSize;
        this.buffer = buffer;
        this.length = buffer.length();
        this.transactionId = transactionId;
    }

    @Override
    public ReadStream<Buffer> handler(final Handler<Buffer> handler) {
        dataHandler = handler;
        LOGGER.debug("{} - Data handling for {} bytes", transactionId, sent);
        send();
        return this;
    }

    public void send() {
        if (sent < length) {
            if (!paused) {
                int start = sent;
                int end = sent + chunkSize;
                if (end > length) {
                    end = length;
                }
                sent = end;
                LOGGER.debug("{} - writing {} bytes", transactionId, sent);
                dataHandler.handle(buffer.slice(start, end));
                context.runOnContext(v -> {
                    send();
                });
            }
        } else {
            if (endHandler != null && !written) {
                LOGGER.debug("{} - calling end handler for {} bytes and length {}", transactionId, sent, length);
                endHandler.handle(null);
                written = true;
            }

        }
    }

    @Override
    public ReadStream<Buffer> pause() {
        paused = true;
        LOGGER.debug("{} - going to pause writing {} bytes ", transactionId, sent);
        return this;
    }

    @Override
    public ReadStream<Buffer> resume() {
        paused = false;
        LOGGER.debug("{} - going to resume writing {} bytes ", transactionId, sent);
        send();
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(final Handler<Void> handler) {
        endHandler = handler;
        return this;
    }

    @Override
    public ReadStream<Buffer> exceptionHandler(final Handler<Throwable> handler) {
        return this;
    }
}