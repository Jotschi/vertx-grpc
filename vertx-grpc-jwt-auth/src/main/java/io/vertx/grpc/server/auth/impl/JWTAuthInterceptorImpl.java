package io.vertx.grpc.server.auth.impl;

import java.util.concurrent.atomic.AtomicReference;

import com.google.common.net.HttpHeaders;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.grpc.common.GrpcException;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.auth.JWTAuthInterceptor;

public class JWTAuthInterceptorImpl implements JWTAuthInterceptor {

  static final GrpcException UNAUTHENTICATED = new GrpcException(GrpcStatus.UNAUTHENTICATED);
  static final GrpcException UNKNOWN = new GrpcException(GrpcStatus.UNKNOWN);
  static final GrpcException UNIMPLEMENTED = new GrpcException(GrpcStatus.UNIMPLEMENTED);

  public enum Type {
    BEARER("Bearer"),
    // these have no known implementation
    BASIC("Basic"),

    DIGEST("Digest"),

    HOBA("HOBA"),

    MUTUAL("Mutual"),

    NEGOTIATE("Negotiate"),

    OAUTH("OAuth"),

    SCRAM_SHA_1("SCRAM-SHA-1"),

    SCRAM_SHA_256("SCRAM-SHA-256");

    private final String label;

    Type(String label) {
      this.label = label;
    }

    public boolean is(String other) {
      return label.equalsIgnoreCase(other);
    }

    @Override
    public String toString() {
      return label;
    }
  }

  public static final Key<String> AUTH_HEADER_KEY = Key.of(HttpHeaders.AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER);

  private static final Context.Key<User> USER_CONTEXT_KEY = Context.key("identity");

  private final JWTAuth provider;

  private final Type type;

  public JWTAuthInterceptorImpl(JWTAuth provider) {
    this.type = Type.BEARER;
    this.provider = provider;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    AtomicReference<Context> current = new AtomicReference<>();
    parseAuthorization(metadata, false, authorizationHandler -> {
      if (authorizationHandler.failed()) {
        call.close(Status.PERMISSION_DENIED.withDescription("Auth Token Parser failure").withCause(authorizationHandler.cause()), metadata);
      } else {
        provider.authenticate(new TokenCredentials().setToken(authorizationHandler.result()), res -> {
          if (res.succeeded()) {
            User user = res.result();
            current.set(Context.current().withValue(USER_CONTEXT_KEY, user));
          } else {
            call.close(Status.PERMISSION_DENIED.withDescription("Auth Token Invalid").withCause(res.cause()), metadata);
          }
        });
      }
    });
    if (current.get() != null) {
      return Contexts.interceptCall(current.get(), call, metadata, next);
    } else {
      return next.startCall(call, metadata);
    }
  }

  protected final void parseAuthorization(Metadata metadata, boolean optional, Handler<AsyncResult<String>> handler) {

    final String authorization = metadata.get(AUTH_HEADER_KEY);
    if (authorization == null) {
      if (optional) {
        // this is allowed
        handler.handle(Future.succeededFuture());
      } else {
        handler.handle(Future.failedFuture(UNAUTHENTICATED));
      }
      return;
    }

    try {
      int idx = authorization.indexOf(' ');

      if (idx <= 0) {
        handler.handle(Future.failedFuture(UNKNOWN));
        return;
      }

      if (!type.is(authorization.substring(0, idx))) {
        handler.handle(Future.failedFuture(UNAUTHENTICATED));
        return;
      }

      handler.handle(Future.succeededFuture(authorization.substring(idx + 1)));
    } catch (Throwable e) {
      handler.handle(Future.failedFuture(e));
    }
  }

  public static Context.Key<User> userContextKey() {
    return USER_CONTEXT_KEY;
  }

}
