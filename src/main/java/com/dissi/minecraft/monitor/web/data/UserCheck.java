package com.dissi.minecraft.monitor.web.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import me.dilley.MineStat;

@Log
@UtilityClass
public class UserCheck {

    public static List<MinecraftPlayer> getPlayers(MineStat stats) {
        List<MinecraftPlayer> players = new ArrayList<>();
        try (Socket clientSocket = new Socket()) {
            clientSocket.connect(new InetSocketAddress(stats.getAddress(), stats.getPort()), stats.getTimeout());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream payload = new DataOutputStream(stream);
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            payload.writeByte(0);
            sendVarInt(payload, 0);
            sendVarInt(payload, stats.getAddress().length());
            payload.writeBytes(stats.getAddress());
            payload.writeShort(stats.getPort());
            sendVarInt(payload, 1);
            sendVarInt(dos, stream.size());
            dos.write(stream.toByteArray());
            dos.writeByte(1);
            dos.writeByte(0);
            recvVarInt(dis);
            recvVarInt(dis);
            int jsonLength = recvVarInt(dis);
            byte[] rawData = new byte[jsonLength];
            dis.readFully(rawData);

            JsonObject json = (new Gson()).fromJson(new String(rawData), JsonObject.class);

            for (JsonElement jsonElement : json.getAsJsonObject("players").getAsJsonArray("sample")) {
                players.add(new MinecraftPlayer(jsonElement.getAsJsonObject().get("id").getAsString(),
                    jsonElement.getAsJsonObject().get("name").getAsString()));
            }
        } catch (Exception e) {
            log.log(Level.FINE, "Can't load players", e);
        }
        players.sort(Comparator.comparing(MinecraftPlayer::name));
        return players;
    }

    private static void sendVarInt(DataOutputStream dos, int intData) throws IOException {
        while ((intData & -128) != 0) {
            dos.writeByte(intData & 127 | 128);
            intData >>>= 7;
        }

        dos.writeByte(intData);
    }

    private static int recvVarInt(DataInputStream dis) throws IOException {
        int intData = 0;
        int width = 0;

        byte varInt;
        do {
            varInt = dis.readByte();
            intData |= (varInt & 127) << width++ * 7;
            if (width > 5) {
                return MineStat.Retval.UNKNOWN.getRetval();
            }
        } while ((varInt & 128) == 128);

        return intData;
    }

}
