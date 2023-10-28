package io.vertx.grpc.server.impl;

import io.vertx.ext.auth.common.AbstractUserContext;
import io.vertx.ext.auth.common.AuthenticationContext;

public class UserContextImpl extends AbstractUserContext {

  public UserContextImpl(AuthenticationContext ctx) {
    super(ctx);
  }
}
