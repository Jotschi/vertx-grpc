package io.vertx.grpc.server.auth;

import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.auth.impl.JWTGrpcClientImpl;

public interface JWTGrpcClient extends GrpcClient {

  JWTGrpcClient withCredentials(io.vertx.ext.auth.authentication.TokenCredentials credentials);

  static JWTGrpcClient create(GrpcClient client) {
    return new JWTGrpcClientImpl(client);
  }

}
