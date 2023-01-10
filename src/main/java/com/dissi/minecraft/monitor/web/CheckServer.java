package com.dissi.minecraft.monitor.web;

import static java.util.logging.Level.WARNING;

import com.dissi.minecraft.monitor.discord.DiscordWebhook;
import com.dissi.minecraft.monitor.web.data.MinecraftPlayer;
import com.dissi.minecraft.monitor.web.data.ShowCraft;
import com.dissi.minecraft.monitor.web.data.UserCheck;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import lombok.extern.java.Log;
import me.dilley.MineStat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Log
public class CheckServer {

    @Value("${minecraft.server.address}")
    private String address;

    @Value("${minecraft.server.port}")
    private int port;

    @Value("${discord.token}")
    private String discordToken;

    private boolean isLive = true;
    private ShowCraft show;

    @GetMapping(value = "current")
    public ShowCraft getCurrent() {
        return show;
    }

    @GetMapping("status")
    public String getInformation() {
        return isLive ? "OK" : "DOWN";
    }

    @Scheduled(fixedDelay = 5000)
    public void doTest() throws UnknownHostException {
        MineStat status = new MineStat(address, port);
        List<MinecraftPlayer> players = UserCheck.getPlayers(status);
        this.show = new ShowCraft(status, players);
        log.info("\n" +
            "Server: " + status.getAddress() + ":" + status.getPort() + "\n" +
            "\tConnection: " + status.getConnectionStatus() + "\n" +
            "\tVersion: " + status.getVersion() + "\n" +
            "\tPlayers: " + status.getCurrentPlayers() + "/" + status.getMaximumPlayers() + "\n" +
            "\tLatency: " + status.getLatency() + "ms");
        setLive(status.isServerUp());
    }

    private void setLive(boolean isLive) {
        if (isLive == this.isLive) {
            return;
        }

        sendNotification(isLive);
        this.isLive = isLive;
    }

    private void sendNotification(boolean isLive) {
        DiscordWebhook discordWebhook = new DiscordWebhook("https://discord.com/api/webhooks/" + discordToken);
        discordWebhook.setUsername("McDownDetector");
        if (isLive) {
            discordWebhook.setContent(
                "@everyone Server [" + address + ":" + port + "] is back \\n```asciidoc\\nONLINE\\n--\\n```\\n");
        } else {
            discordWebhook.setContent(
                "@everyone Server [" + address + ":" + port + "] is currently \\n```arm\\nDOWN\\n```\\n");
        }
        try {
            log.info("Sending message to discord.");
            discordWebhook.execute();
        } catch (IOException e) {
            log.log(WARNING, "Can not send message!", e);
        }
    }


}
