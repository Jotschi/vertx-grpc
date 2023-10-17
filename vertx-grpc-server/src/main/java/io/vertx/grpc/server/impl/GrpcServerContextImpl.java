package io.vertx.grpc.server.impl;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.common.UserContext;
import io.vertx.grpc.server.GrpcServerContext;
import io.vertx.grpc.server.GrpcServerRequest;

public class GrpcServerContextImpl<Req, Resp> implements GrpcServerContext {

  private GrpcServerRequest<Req, Resp> request;
  private HttpServerResponse response;
  private UserContext userContext;

  public GrpcServerContextImpl(GrpcServerRequest<Req, Resp> request, HttpServerResponse response) {
    this.request = request;
    this.response = response;
    this.userContext = new UserContextImpl();
  }

  @Override
  public HttpServerRequest request() {
    return request.httpRequest();
  }

  @Override
  public HttpServerResponse response() {
    return response;
  }

  @Override
  public UserContext user() {
    return userContext;
  }

}
