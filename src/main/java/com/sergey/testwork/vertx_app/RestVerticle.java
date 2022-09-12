package com.sergey.testwork.vertx_app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Date;

import java.util.UUID;


 public class RestVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(RestVerticle.class);

  private final String uuid = UUID.randomUUID().toString();




  @Override
  public void start(Promise<Void> startPromise) {

    Router router = Router.router(vertx);
    router.get("/data").handler(this::getData);

    router.route().handler(BodyHandler.create());



    router.post("/login").handler(this::login);
    router.post("/logout").handler(this::logout);
    router.post("/order").handler(this::addOrder);
    router.get("/orders").handler(this::getOrders);

    vertx.createHttpServer().requestHandler(router).listen(8080)
        .onSuccess(ok -> {
          logger.info("server started on port 8080");
          startPromise.complete();
        }).onFailure(startPromise::fail);

  }
  private void getData(RoutingContext context) {
    JsonObject payload = createPayload();
    context.response()
      .putHeader("Content-Type", "application/json")
      .setStatusCode(200)
      .end(payload.encode());
  }


   private void getOrders(RoutingContext context) {
     FileSystem fs = vertx.fileSystem();
     OpenOptions options = new OpenOptions();
     fs.open("target/classes/products.txt", options, res -> {
       if (res.succeeded()) {
         AsyncFile file = res.result();
        vertx.eventBus()
           .publish("get.products", file);
       } else {
         //error
       }
     });
   }

  private void login(RoutingContext routingContext) {
    JsonObject json = routingContext.getBodyAsJson();
      System.out.println(json.getString("name"));
    // todo open session for current user
    routingContext.response().putHeader("Content-Type", "application/json")
      .end(Json.encodePrettily("session was opened"));
  }

  private void logout(RoutingContext routingContext) {
   // todo close session for current user
    routingContext.response().putHeader("Content-Type", "application/json")
      .end(Json.encodePrettily("session was closed"));
  }

  private void addOrder(RoutingContext routingContext) {
    JsonObject json = routingContext.getBodyAsJson();
    String name = json.getString("name");
    Order order = new Order();
    order.name = name;
    order.id = uuid;
    order.date = new Date();
    vertx.eventBus()
      .publish("add.order", name);
    FileSystem fs = vertx.fileSystem();
    vertx.fileSystem().writeFile("target/classes/products.txt", Buffer.buffer(name), result -> {
      if (result.succeeded()) {
        System.out.println("write success");
      } else {
        System.err.println("Error ..." + result.cause());
      }
    });

    routingContext.response().putHeader("Content-Type", "application/json")
      .end(Json.encodePrettily("order was added !"));
  }
  private JsonObject createPayload() {
    return new JsonObject()
      .put("uuid", uuid)
      .put("timestamp", System.currentTimeMillis());
  }



  public class Order{
    public String id;
    public String name;
    public Date date;
  }
}
