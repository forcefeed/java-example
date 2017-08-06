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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Mostafa Asgari
 * @since 8/6/17
 */
public class SseClient {

    private String url;
    private Consumer<Exception> errorCallback;
    private Runnable connectCallback;
    private Runnable disconnectCallback;
    private Stream<Event> streamOfEvents;

    public static class Builder {

        private SseClient sseClient;

        public Builder(String url){
            sseClient = new SseClient(url);
        }

        public Builder onConnect(Runnable connectCallback){
            sseClient.connectCallback = connectCallback;
            return this;
        }

        public Builder onDisconnect(Runnable disconnectCallback){
            sseClient.disconnectCallback = disconnectCallback;
            return this;
        }

        public Builder onError(Consumer<Exception> errorCallback){
            sseClient.errorCallback = errorCallback;
            return this;
        }

        public SseClient build(){
            return sseClient;
        }

    }

    public static SseClient.Builder newBuilder(String url){
        return new Builder(url);
    }

    private SseClient(String url){
        this.url = url;
    }

    public Stream<Event> events() throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Cache-Control", "no-cache");
        httpGet.addHeader("Accept", "text/event-stream");
        httpGet.addHeader("Connection", "keep-alive");

        CloseableHttpResponse response = httpclient.execute(httpGet);
        if(connectCallback!=null){
            connectCallback.run();
        }
        InputStream inputStream = response.getEntity().getContent();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                try {
                    if( streamOfEvents!=null ){
                        streamOfEvents.close();
                    }
                    if(response!=null) {
                        response.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        streamOfEvents = Stream.generate(new EventSupplier(inputStream));
        if( disconnectCallback!=null ) {
            streamOfEvents.onClose(disconnectCallback);
        }

        return streamOfEvents;
    }

    private class EventSupplier implements Supplier<Event> {

        private BufferedReader reader;

        EventSupplier(InputStream inputStream){
            reader = new BufferedReader(new InputStreamReader( inputStream , StandardCharsets.UTF_8));
        }

        @Override
        public Event get() {
            Event event = new Event();
            try {
                String line;
                while (true){

                    line = reader.readLine();

                    if(line.equals("")){
                        break;
                    }
                    if (line==null){ //the end of the stream has been reached
                        if(disconnectCallback!=null){
                            disconnectCallback.run();
                        }
                        break;
                    }

                    if( line.startsWith("event:") ){
                        event.setEvent(line.substring( line.indexOf(':')+1 ));
                    }
                    else if (line.startsWith("data:")){
                        event.setData(line.substring( line.indexOf(':')+1 ));
                    }
                }
            }
            catch (IOException exc) {
                if( errorCallback!=null ){
                    errorCallback.accept(exc);
                }
            }

            return event;
        }
    }

}

