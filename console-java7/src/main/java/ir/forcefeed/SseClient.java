package ir.forcefeed;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author Mostafa Asgari
 * @since 8/6/17
 */
public class SseClient {

    private String url;
    private Action1<Event> eventCallback;
    private Action1<Exception> errorCallback;
    private Runnable connectCallback;
    private Runnable disconnectCallback;
    private transient boolean stop = false;
    private Thread backgroundThread;

    private SseClient(String url) {
        this.url = url;
    }

    public static SseClient.Builder newBuilder(String url) {
        return new Builder(url);
    }

    public void startStreamingAsync() throws IOException {
        startStreamingAsync(false);
    }

    public void startStreamingAsync(boolean daemon) throws IOException {

        backgroundThread = new Thread() {

            @Override
            public void run() {

                try {
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    HttpGet httpGet = new HttpGet(url);
                    httpGet.addHeader("Cache-Control", "no-cache");
                    httpGet.addHeader("Accept", "text/event-stream");
                    httpGet.addHeader("Connection", "keep-alive");

                    CloseableHttpResponse response = httpclient.execute(httpGet);
                    if (connectCallback != null) {
                        connectCallback.run();
                    }
                    InputStream inputStream = response.getEntity().getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                    while (!stop) {
                        String line;
                        Event event = null;
                        while (!stop) {

                            line = reader.readLine();

                            if (line.equals("")) {
                                break;
                            }
                            if (line == null) { //the end of the stream has been reached
                                stop = true;
                                if (disconnectCallback != null) {
                                    disconnectCallback.run();
                                }
                                break;
                            }

                            event = new Event();

                            if (line.startsWith("event:")) {
                                event.setEvent(line.substring(line.indexOf(':') + 1));
                            } else if (line.startsWith("data:")) {
                                event.setData(line.substring(line.indexOf(':') + 1));
                            }
                        }

                        if (event != null && eventCallback != null) {
                            eventCallback.accept(event);
                        }
                    }
                } catch (Exception exc) {
                    if (errorCallback != null) {
                        errorCallback.accept(exc);
                    }
                }
            }
        };

        backgroundThread.setDaemon(daemon);
        backgroundThread.start();

    }

    public void stopStreaming() {
        stop = true;
        try {
            backgroundThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class Builder {

        private SseClient sseClient;

        public Builder(String url) {
            sseClient = new SseClient(url);
        }

        public Builder onConnect(Runnable connectCallback) {
            sseClient.connectCallback = connectCallback;
            return this;
        }

        public Builder onDisconnect(Runnable disconnectCallback) {
            sseClient.disconnectCallback = disconnectCallback;
            return this;
        }

        public Builder onError(Action1<Exception> errorCallback) {
            sseClient.errorCallback = errorCallback;
            return this;
        }

        public Builder onEvent(Action1<Event> eventCallback) {
            sseClient.eventCallback = eventCallback;
            return this;
        }

        public SseClient build() {
            return sseClient;
        }

    }


}

