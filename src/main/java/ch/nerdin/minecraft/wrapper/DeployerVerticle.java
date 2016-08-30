package ch.nerdin.minecraft.wrapper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/**
 */
public class DeployerVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(DeployerVerticle.class.getName());

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        BridgeOptions opts = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress(MinecraftProcessControllerVentricle.COMMAND_ADDRESS))
                .addInboundPermitted(new PermittedOptions().setAddress(MinecraftProcessControllerVentricle.INIT_ADDRESS))
                .addOutboundPermitted(new PermittedOptions().setAddress(MinecraftProcessControllerVentricle.LOG_ADDRESS));

        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
        router.route("/eventbus/*").handler(ebHandler);

        router.route().handler(StaticHandler.create());
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);

        vertx.deployVerticle("ch.nerdin.minecraft.wrapper.MinecraftProcessControllerVentricle");

        logger.info("started...");
    }
}