package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Réponse d’authentification avec JWT
 */
@Schema(description = "Réponse d’authentification avec JWT")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class AuthResponse   {
  @JsonProperty("token")
  private String token = null;

  public AuthResponse token(String token) { 


    this.token = token;
    return this;
  }

  /**
   * Jeton JWT d’authentification
   * @return token
   **/
  
  @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true, description = "Jeton JWT d’authentification")
  @NotNull 
  public String getToken() {  
    return token;
  }
  public void setToken(String token){ 


    this.token = token;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthResponse authResponse = (AuthResponse) o;
    return Objects.equals(this.token, authResponse.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthResponse {\n");
    
    sb.append("    token: [REDACTED]\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
