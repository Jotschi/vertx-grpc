package io.vertx.grpc.server.auth;

import io.grpc.ServerInterceptor;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.grpc.server.auth.impl.JWTAuthInterceptorImpl;

public interface JWTAuthInterceptor extends ServerInterceptor {

  static ServerInterceptor create(JWTAuth authProvider) {
    return new JWTAuthInterceptorImpl(authProvider);
  }

  static User userFromContext() {
    return JWTAuthInterceptorImpl.userContextKey().get();
  }

}
