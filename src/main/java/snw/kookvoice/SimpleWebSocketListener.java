package snw.kookvoice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

// A simple WebSocket listener for processing all the jobs.
public class SimpleWebSocketListener extends WebSocketListener {
    private final CompletableFuture<String> future;
    private int stage = 1;
    private volatile boolean dead = false;
    private final Callable<Void> onDead;

    public SimpleWebSocketListener(CompletableFuture<String> future, Callable<Void> onDead) {
        this.future = future;
        this.onDead = onDead;
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        super.onMessage(webSocket, text);
        if (dead) return;
        JsonObject res = JsonParser.parseString(text).getAsJsonObject();
        if (res.has("notification")) {
            if (res.has("method")) {
                if (Objects.equals(res.get("method").getAsString(), "disconnect")) { // connection is dead
                    dead = true;
                    if (onDead != null) {
                        try {
                            onDead.call();
                        } catch (Exception ignored) {
                        }
                    }
                    webSocket.close(1000, "User Closed Service");
                }
            }
            return;
        }
        if (stage++ == 3) {
            JsonObject data = res.getAsJsonObject("data");
            String addr = data.get("ip").getAsString();
            int port = data.get("port").getAsInt();
            int rtcpPort = data.get("rtcpPort").getAsInt();
            String id = data.get("id").getAsString();
            String rtp = String.format("rtp://%s:%s?rtcpport=%s", addr, port, rtcpPort);
            future.complete(rtp);

            JsonObject object = JsonParser.parseString(Constants.STAGE_4).getAsJsonObject();
            JsonObject data1 = object.getAsJsonObject("data");
            data1.remove("transportId");
            data1.addProperty("transportId", id);
            webSocket.send(new Gson().toJson(object));
        }
    }
}
