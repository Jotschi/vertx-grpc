package examples;

import io.vertx.core.Vertx;
import io.vertx.docgen.Source;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.auth.JWTGrpcClient;

@Source
public class JWTGrpcClientExamples {

  private String TOKEN;

  public void createClient(Vertx vertx) {
    JWTGrpcClient jwtClient = JWTGrpcClient.create(GrpcClient.client(vertx))
      .withCredentials(new TokenCredentials(TOKEN));
  }

}
