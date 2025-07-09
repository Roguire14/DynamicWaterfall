package fr.roguire.dynamicwaterfall;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import fr.roguire.dynamicwaterfall.listeners.OnPlayerJoinEvent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DynamicWaterfall extends Plugin {

    private ProxyServer server;
    private Logger logger;
    private OkHttpClient client;
    private Request request;
    ;
    private final Set<String> knownServers = new HashSet<>();
    private final Gson gson = new Gson();
    private ScheduledTask refreshTask;

    @Override
    public void onEnable() {
        super.onEnable();
        server = getProxy();
        logger = getLogger();
        client = new OkHttpClient();
        request = new Request.Builder().url("http://localhost:25550/get-servers").build();
        getLogger().info("DynamicWaterfall is enabled");
        refreshTask = server.getScheduler().schedule(this, this::checkServers, 0, 2, TimeUnit.SECONDS);
        server.getPluginManager().registerListener(this, new OnPlayerJoinEvent(this));
    }

    private void checkServers() {
        List<ServerData> activeServers = fetchServers();
        if (activeServers == null) {
            logger.info("No active servers found");
            return;
        }
        Set<String> activeServerNames = new HashSet<>();

        for (ServerData server : activeServers) {
            activeServerNames.add(server.name);
            if (!knownServers.contains(server.name)) {
                addServer(server.name, server.port);
            }
        }

        knownServers.removeIf(serverName -> {
            if (!activeServerNames.contains(serverName)) {
                removeServer(serverName);
                return true;
            }
            return false;
        });
    }

    public void addServer(String name, int port) {
        ServerInfo serverInfo = this.server.constructServerInfo(name, new InetSocketAddress("localhost", port), "", true);
        server.getConfig().addServer(serverInfo);
        knownServers.add(name);
        logger.info("Serveur ajouté: " + serverInfo.getName());
    }

    private void removeServer(String name) {
        ServerInfo server = this.server.getServerInfo(name);
        if (server != null) {
            this.server.getConfig().removeServer(server);
        }
        logger.info("Serveur supprimé : " + name);
    }

    private List<ServerData> fetchServers() {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            Type listType = new TypeToken<List<ServerData>>() {
            }.getType();
            if (response.body() == null) return null;
            JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (jsonResponse.get("code").getAsInt() != 200) return null;
            String message = jsonResponse.get("message").getAsString();
            return gson.fromJson(message, listType);
        } catch (IOException e) {
            return null;
        }
    }

    private static class ServerData {
        String name;
        int port;

        @Override
        public String toString() {
            return "ServerData{" + name + "," + port + "}";
        }
    }


    @Override
    public void onDisable() {
        refreshTask.cancel();
        super.onDisable();
    }
}
