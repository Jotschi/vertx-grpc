package examples;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServiceBridge;
import io.vertx.grpc.server.auth.JWTAuthInterceptor;

public class JWTGrpcServerExamples {

  public void setupJWTServer(BindableService service, JWTAuth authProvider, Vertx vertx) {
    GrpcServer server = GrpcServer.server(vertx);
    ServerInterceptor wrappedAuthInterceptor = JWTAuthInterceptor.create(authProvider);
    ServerServiceDefinition authedService = ServerInterceptors.intercept(service, wrappedAuthInterceptor);
    GrpcServiceBridge serverStub = GrpcServiceBridge.bridge(authedService);
    serverStub.bind(server);
  }
  
  public void setupJWTServerShort(BindableService service, JWTAuth authProvider, Vertx vertx) {
    GrpcServer server = GrpcServer.server(vertx);
    GrpcServiceBridge serverStub = JWTAuthInterceptor.create(authProvider, service);
    serverStub.bind(server);
  }

  public void accessUser() {
    GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        try {
          User identity = JWTAuthInterceptor.userFromContext();
          responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName() + " from " + identity.subject()).build());
          responseObserver.onCompleted();
        } catch (Exception e) {
          responseObserver.onError(e);
        }
      }
    };

  }
}
