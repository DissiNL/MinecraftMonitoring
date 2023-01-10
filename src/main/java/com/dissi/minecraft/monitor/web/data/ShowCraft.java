package com.dissi.minecraft.monitor.web.data;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import me.dilley.MineStat;

@Getter
public class ShowCraft {

    private final String status;
    private final int port;
    private final int currentPlayer;
    private final int maxPlayers;
    private final long latency;
    private final String currentStatus;
    private final Timestamp lastCheck;
    private final String version;
    private final List<MinecraftPlayer> players;

    public ShowCraft(MineStat status, List<MinecraftPlayer> players) {
        this.lastCheck = Timestamp.from(Instant.now());
        this.status = status.getAddress();
        this.port = status.getPort();
        this.currentPlayer = status.getCurrentPlayers();
        this.maxPlayers = status.getMaximumPlayers();
        this.latency = status.getLatency();
        this.currentStatus = status.getConnectionStatus();
        this.version = status.getVersion();
        this.players = players;
    }
}
