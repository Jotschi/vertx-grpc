package io.vertx.grpc.it;

import org.junit.Before;
import org.junit.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcException;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServiceBridge;
import io.vertx.grpc.server.ServerTestBase;
import io.vertx.grpc.server.auth.JWTAuthInterceptor;
import io.vertx.grpc.server.auth.JWTGrpcClient;

public class JWTAuthTest extends ServerTestBase {

  private GrpcClient client;
  private GrpcServer server;
  private String validToken;
  private static final String INVALID_TOKEN = "this-token-value-is-bogus";

  public void setupClientServer(TestContext should) {
    client = GrpcClient.client(vertx);
    server = GrpcServer.server(vertx);

    // Prepare JWT auth and generate token to be used for the client
    JWTAuthOptions config = new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setPath("keystore.jceks")
        .setPassword("secret")
        .setType("jceks"));
    JWTAuth authProvider = JWTAuth.create(vertx, config);
    validToken = authProvider.generateToken(new JsonObject().put("sub", "johannes"), new JWTOptions().setIgnoreExpiration(true));

    // Implement the service
    GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        try {
          User identity = JWTAuthInterceptor.USER_CONTEXT_KEY.get();
          should.assertNotNull(identity);
          should.assertEquals("johannes", identity.subject());
          responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
          responseObserver.onCompleted();
        } catch (Exception e) {
          responseObserver.onError(e);
        }
      }
    };

    // Add the JWTAuthInterceptor and start the server
    ServerInterceptor wrappedAuthInterceptor = new JWTAuthInterceptor(authProvider);
    ServerServiceDefinition authedService = ServerInterceptors.intercept(service, wrappedAuthInterceptor);
    GrpcServiceBridge serverStub = GrpcServiceBridge.bridge(authedService);
    serverStub.bind(server);
    startServer(server);
  }

  @Test
  public void testJWTInvalidAuthentication(TestContext should) {
    setupClientServer(should);

    JWTGrpcClient jwtClient = JWTGrpcClient.create(client).withCredentials(new TokenCredentials(INVALID_TOKEN));
    Future<HelloReply> clientReply = sayHello(jwtClient);

    Async test = should.async();
    clientReply
      .onFailure(error -> {
        if (error instanceof GrpcException) {
          GrpcException grpcError = (GrpcException) error;
          should.assertEquals(GrpcStatus.PERMISSION_DENIED, grpcError.status(), "The status code did not match.");
          should.assertNotNull(grpcError.response());
          test.complete();
        } else {
          should.fail(error);
        }
      })
      .onSuccess(reply -> {
        should.fail("The test should fail with an error due to the usage of the invalid token");
      });

  }

  @Test
  public void testJWTValidAuthentication(TestContext should) {
    setupClientServer(should);

    JWTGrpcClient jwtClient = JWTGrpcClient.create(client).withCredentials(new TokenCredentials(validToken));
    Future<HelloReply> clientReply = sayHello(jwtClient);

    Async test = should.async();
    clientReply
      .onFailure(should::fail)
      .onSuccess(reply -> {
        System.out.println("Reply: " + reply.getMessage());
        test.complete();
      });

  }

  private Future<HelloReply> sayHello(JWTGrpcClient jwtClient) {
    return jwtClient
      .request(SocketAddress.inetSocketAddress(8080, "localhost"), GreeterGrpc.getSayHelloMethod())
      .compose(request -> {
        request.end(HelloRequest
          .newBuilder()
          .setName("Johannes")
          .build());

        return request.response()
          .compose(GrpcClientResponse::last);
      });
  }
}
