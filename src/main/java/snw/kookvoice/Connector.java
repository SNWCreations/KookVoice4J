package snw.kookvoice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Represents a connector with a voice channel. <p>
 * You can use this instance again and again.
 */
public class Connector {
    private final String channelId;
    private final String token;
    private WebSocket webSocket;

    /**
     * Main constructor.
     *
     * @param channelId The voice channel ID
     * @param token The Bot Token
     */
    public Connector(String channelId, String token) {
        this.channelId = channelId;
        this.token = token;
    }

    /**
     * Call this to create connection with the channel that specified by this instance.
     *
     * @param onDead Just a callback, called when the connection is dead for some reason (e.g. another connection created)
     * @return The Future representation, it contains the RTP link for you to pushing stream using ffmpeg or other program.
     * @throws IllegalStateException Thrown if something unexpected happened, is the Bot not in your guild?
     */
    public Future<String> connect(Callable<Void> onDead) throws IllegalStateException {
        disconnect(); // make sure the connection actually dead, or something unexpected will happen?
        webSocket = null; // help GC

        OkHttpClient client = new OkHttpClient.Builder().pingInterval(30, TimeUnit.SECONDS).build();

        // region Get Gateway
        String gatewayWs;
        String fullGatewayUrl = "https://www.kookapp.cn/api/v3/gateway/voice?channel_id=" + channelId;
        try (Response response = client.newCall(
                new Request.Builder()
                        .get()
                        .url(fullGatewayUrl)
                        .addHeader("Authorization", String.format("Bot %s", token))
                        .build()
        ).execute()) {
            if (response.code() != 200) {
                throw new IllegalStateException();
            }
            assert response.body() != null;
            JsonObject element = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (element.get("code").getAsInt() != 0) {
                throw new IllegalStateException();
            }
            gatewayWs = element.getAsJsonObject("data").get("gateway_url").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        // endregion
        webSocket = client.newWebSocket(
                new Request.Builder()
                        .url(gatewayWs)
                        .build(),
                new SimpleWebSocketListener(future, onDead)
        );

        webSocket.send(randomId(Constants.STAGE_1));
        webSocket.send(randomId(Constants.STAGE_2));
        webSocket.send(randomId(Constants.STAGE_3));
        return future;
    }

    /**
     * Disconnect with the specified channel.
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User Closed Service");
        }
    }

    private static String randomId(String constant) {
        JsonObject object = JsonParser.parseString(constant).getAsJsonObject();
        object.remove("id");
        object.addProperty("id", new SecureRandom().nextInt(8999999) + 1000000);
        return new Gson().toJson(object);
    }
}
