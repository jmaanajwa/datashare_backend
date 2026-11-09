package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Données pour authentification utilisateur
 */
@Schema(description = "Données pour authentification utilisateur")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class LoginRequest   {
  @JsonProperty("email")
  private String email = null;

  @JsonProperty("password")
  private String password = null;

  public LoginRequest email(String email) { 


    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   **/
  
  @Schema(example = "user@example.com", required = true, description = "")
  @NotBlank
  @Email
  @Size(max = 254)
  public String getEmail() {  
    return email;
  }
  public void setEmail(String email){ 


    this.email = email;
  }
  public LoginRequest password(String password) { 


    this.password = password;
    return this;
  }

  /**
   * Get password
   * @return password
   **/
  
  @Schema(example = "StrongPass123", required = true, description = "")
  @NotBlank
  @Size(max = 72)
  public String getPassword() {  
    return password;
  }
  public void setPassword(String password){ 


    this.password = password;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoginRequest loginRequest = (LoginRequest) o;
    return Objects.equals(this.email, loginRequest.email) &&
        Objects.equals(this.password, loginRequest.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LoginRequest {\n");
    
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    password: [REDACTED]\n");
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
