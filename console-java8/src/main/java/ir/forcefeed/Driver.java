package ir.forcefeed;

import org.json.JSONObject;

import java.io.IOException;

/**
 * @author Mostafa Asgari
 * @since 8/6/17
 */
public class Driver {

    private static final String URL = "http://forcefeed.ir/sse";

    public static void main(String[] args) throws IOException {

        SseClient sseClient = SseClient.newBuilder(URL)
                                       .onConnect( ()-> System.out.println("Successfully connected to " + URL) )
                                       .onDisconnect( ()-> System.out.println("**Disconnected**") )
                                       .onError( exc -> exc.printStackTrace() )
                                       .build();

        sseClient.events().filter(e -> e.getData()!=null).forEach( e -> {
            JSONObject json = new JSONObject(e.getData());
            System.out.println(json.toString(3));
        });
    }

}
