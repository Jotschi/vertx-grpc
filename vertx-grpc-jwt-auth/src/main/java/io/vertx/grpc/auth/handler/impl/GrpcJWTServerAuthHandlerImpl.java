package io.vertx.grpc.auth.handler.impl;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.common.UserContextInternal;
import io.vertx.ext.auth.common.handler.impl.HTTPAuthorizationHandler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.common.HttpException;
import io.vertx.grpc.auth.handler.GrpcJWTServerAuthHandler;
import io.vertx.grpc.common.GrpcException;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServerContext;

public class GrpcJWTServerAuthHandlerImpl extends HTTPAuthorizationHandler<GrpcServerContext, JWTAuth> implements GrpcJWTServerAuthHandler {

  static final GrpcException UNAUTHENTICATED = new GrpcException(GrpcStatus.UNAUTHENTICATED);
  static final GrpcException UNKNOWN = new GrpcException(GrpcStatus.UNKNOWN);
  static final GrpcException UNIMPLEMENTED = new GrpcException(GrpcStatus.UNIMPLEMENTED);

  public GrpcJWTServerAuthHandlerImpl(JWTAuth authProvider, String realm) {
    super(authProvider, Type.BEARER, realm);
  }

  @Override
  public void postAuthentication(GrpcServerContext ctx, User authenticated) {
    System.out.println("POST auth");
    System.out.println(authenticated.subject());
    ((UserContextInternal) ctx.user())
      .setUser(authenticated);
  }

  @Override
  public Future<User> authenticate(GrpcServerContext context) {
    return parseAuthorization(context)
      .compose(token -> {
        int segments = 0;
        for (int i = 0; i < token.length(); i++) {
          char c = token.charAt(i);
          if (c == '.') {
            if (++segments == 3) {
              return Future.failedFuture(new HttpException(400, "Too many segments in token"));
            }
            continue;
          }
          if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
            continue;
          }
          // invalid character
          return Future.failedFuture(new GrpcException(GrpcStatus.UNKNOWN, "Invalid character in token: " + (int) c));
        }

        // TODO Add gRPC security audit feature?
        // final TokenCredentials credentials = new TokenCredentials(token);
        // final SecurityAudit audit = ((RoutingContextInternal) context).securityAudit();
        // audit.credentials(credentials);

        return authProvider
          .authenticate(new TokenCredentials(token))
          // .andThen(op -> audit.audit(Marker.AUTHENTICATION, op.succeeded()))
          .recover(err -> Future.failedFuture(new GrpcException(GrpcStatus.UNAUTHENTICATED, err)));
      });
  }

  @Override
  protected void processException(GrpcServerContext ctx, Throwable cause) {
    System.out.println("Exception");
    if (cause != null) {
      cause.printStackTrace();
    }

  }

}
