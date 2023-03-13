package io.vertx.grpc.server.auth;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.grpc.server.GrpcServiceBridge;
import io.vertx.grpc.server.auth.impl.JWTAuthInterceptorImpl;

public interface JWTAuthInterceptor extends ServerInterceptor {

  static ServerInterceptor create(JWTAuth authProvider) {
    return new JWTAuthInterceptorImpl(authProvider);
  }

  static User userFromContext() {
    return JWTAuthInterceptorImpl.userContextKey().get();
  }

  static GrpcServiceBridge create(JWTAuth authProvider, BindableService service) {
    ServerInterceptor authInterceptor = create(authProvider);
    ServerServiceDefinition authedService = ServerInterceptors.intercept(service, authInterceptor);
    return GrpcServiceBridge.bridge(authedService);
  }

}
