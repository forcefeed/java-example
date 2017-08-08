package ir.forcefeed;

import org.json.JSONObject;

import java.io.IOException;

/**
 * @author Mostafa Asgari
 * @since 8/8/17
 */
public class Driver {

    private static final String URL = "http://forcefeed.ir/sse";

    public static void main(String[] args) throws IOException {

        final SseClient sseClient = SseClient.newBuilder(URL)
                .onConnect(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Successfully connected to " + URL);
                    }
                })
                .onDisconnect(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("**Disconnected**");
                    }
                })
                .onError(new Action1<Exception>() {
                    @Override
                    public void accept(Exception exc) {
                        exc.printStackTrace();
                    }
                })
                .onEvent(new Action1<Event>() {
                    @Override
                    public void accept(Event event) {
                        if (event.getData() != null) {
                            JSONObject json = new JSONObject(event.getData());
                            System.out.println(json.toString(3));
                        }
                    }
                })
                .build();

        sseClient.startStreamingAsync();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                sseClient.stopStreaming();
            }
        });


    }

}
