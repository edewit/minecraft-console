package ch.nerdin.minecraft.wrapper;

import com.google.common.collect.EvictingQueue;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

/**
 *
 */
public class MinecraftProcessControllerVentricle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MinecraftProcessControllerVentricle.class.getName());
    public static final String INIT_ADDRESS = "ch.nerdin.minecraft.connect";
    public static final String LOG_ADDRESS = "ch.nerdin.minecraft.console";
    public static final String COMMAND_ADDRESS = "ch.nerdin.minecraft.command";

    private Process minecraftProcess;
    private EvictingQueue<String> queue = EvictingQueue.create(100);

    @Override
    public void start() throws Exception {
        EventBus eb = vertx.eventBus();

        minecraftProcess = new ProcessBuilder("java", "-jar", "minecraft_server.1.9.jar", "nogui")
                .redirectErrorStream(true).directory(new File("server")).start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(minecraftProcess.getInputStream()));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(minecraftProcess.getOutputStream()));

        eb.consumer(INIT_ADDRESS, event -> {
            event.reply(new JsonArray(new ArrayList<>(queue)));
        });

        eb.<String>consumer(COMMAND_ADDRESS, event -> {
            try {
                bufferedWriter.write(event.body() + "\n");
                bufferedWriter.flush();
            } catch (IOException e) {
                logger.error("could not send command to mincraft process", e);
            }
        });

        vertx.setPeriodic(1, t -> vertx.<String>executeBlocking(future -> {
            try {
                future.complete(bufferedReader.readLine());
            } catch (IOException e) {
                logger.error("failed to read minecraft process output", e);
            }
        }, res -> {
            String line = res.result();
            eb.send(LOG_ADDRESS, new JsonObject().put("line", res.result()));
            queue.add(line);
        }));
    }

    @Override
    public void stop() throws Exception {
        queue.clear();
        minecraftProcess.destroy();
    }
}
