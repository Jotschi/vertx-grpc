package io.vertx.grpc.auth.handler;

import io.vertx.ext.auth.common.AuthenticationHandler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.grpc.auth.handler.impl.GrpcJWTServerAuthHandlerImpl;
import io.vertx.grpc.server.GrpcServerContext;

/**
 * An server side authentication handler that provides JWT authentication support for gRPC.
 */
public interface GrpcJWTServerAuthHandler extends AuthenticationHandler<GrpcServerContext> {

  /**
   * Create a JWT auth handler. When no scopes are explicit declared, the default scopes will be looked up from the route metadata.
   *
   * @param authProvider
   *          the auth provider to use
   * @return the auth handler
   */
  static GrpcJWTServerAuthHandler create(JWTAuth authProvider) {
    return create(authProvider, null);
  }

  /**
   * Create a JWT auth handler for the specified realm.
   *
   * @param authProvider
   *          the auth provider to use
   * @return the auth handler
   */
  static GrpcJWTServerAuthHandler create(JWTAuth authProvider, String realm) {
    return new GrpcJWTServerAuthHandlerImpl(authProvider, realm);
  }
}
