package io.vertx.grpc.auth.jwt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.auth.handler.GrpcJWTServerAuthHandler;
import io.vertx.grpc.auth.handler.impl.GrpcJWTServerAuthHandlerImpl;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerResponse;
import io.vertx.grpc.server.ServerTestBase;
import io.vertx.grpc.test.examples.helloworld.GreeterGrpc;
import io.vertx.grpc.test.examples.helloworld.HelloReply;
import io.vertx.grpc.test.examples.helloworld.HelloRequest;

@RunWith(VertxUnitRunner.class)
public class JWTAuthTest extends ServerTestBase {

  @Rule
  public final RunTestOnContext rule = new RunTestOnContext();

  private Vertx vertx;

  private JWTAuthOptions authConfig = new JWTAuthOptions()
    .setKeyStore(new KeyStoreOptions()
      .setType("jceks")
      .setPath("keystore.jceks")
      .setPassword("secret"));

  @Before
  public void setup() {
    vertx = Vertx.vertx();
  }

  public void setupClientServer(TestContext should, JWTAuth authProvider, boolean expectUser) {
    GrpcJWTServerAuthHandler jwtAuthHandler = GrpcJWTServerAuthHandler.create(authProvider);
    GrpcServer jwtServer = GrpcServer.server(vertx);
    jwtServer.callHandler(GreeterGrpc.getSayHelloMethod(), request -> {
      for (String key : request.headers().names()) {
        System.out.println("Header: " + key + "=" + request.headers().get(key));
      }
      // ((GrpcJWTServerAuthHandlerImpl)jwtAuthHandler).authenticate(request.context()).andThen(res -> {
      // System.out.println("Got: " + res.result().subject());
      request.handler(hello -> {
        jwtAuthHandler.handle(request.context());
        GrpcServerResponse<HelloRequest, HelloReply> response = request.response();
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + hello.getName()).build();
        response.end(reply);
        User user = request.context().user().get();
        if (expectUser) {
          should.assertNotNull(user);
          should.assertEquals("johannes", user.subject());
          System.out.println("Got user");
        } else {
          should.assertNull(user);
        }
      }).errorHandler(error -> {
        should.fail("Error should not happen " + error);
      });

      // });
    });

    startServer(jwtServer);

  }

  @Test
  public void testAuthentication(TestContext should) {
    JWTAuth authProvider = JWTAuth.create(vertx, authConfig);
    String token = authProvider.generateToken(new JsonObject().put("sub", "johannes"));
    System.out.println("Token: " + token);

    setupClientServer(should, authProvider, true);

    GrpcClient client = GrpcClient.client(vertx);
    Future<HelloReply> clientReply = sayHello(client, token);

    final Async test = should.async();
    clientReply
      .onFailure(should::fail)
      .onSuccess(reply -> {
        System.out.println("Reply: " + reply.getMessage());
        test.complete();
      });

  }

  private Future<HelloReply> sayHello(GrpcClient grpcClient, String token) {
    return grpcClient
      .request(SocketAddress.inetSocketAddress(8080, "localhost"), GreeterGrpc.getSayHelloMethod())
      .compose(request -> {
        request.headers().add(HttpHeaders.AUTHORIZATION, new TokenCredentials(token).toHttpAuthorization());
        request.end(HelloRequest
          .newBuilder()
          .setName("Johannes")
          .build());

        return request.response()
          .compose(GrpcClientResponse::last);
      });
  }
}
