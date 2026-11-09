package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Données pour créer un compte utilisateur
 */
@Schema(description = "Données pour créer un compte utilisateur")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class RegisterRequest   {
  @JsonProperty("email")
  private String email = null;

  @JsonProperty("password")
  private String password = null;

  public RegisterRequest email(String email) { 


    this.email = email;
    return this;
  }

  /**
   * Email unique et valide
   * @return email
   **/
  
  @Schema(example = "user@example.com", required = true, description = "Email unique et valide")
  @NotBlank
  @Email
  @Size(max = 254)
  public String getEmail() {  
    return email;
  }
  public void setEmail(String email){ 


    this.email = email;
  }
  public RegisterRequest password(String password) { 


    this.password = password;
    return this;
  }

  /**
   * Mot de passe (minimum 8 caractères)
   * @return password
   **/
  
  @Schema(example = "StrongPass123", required = true, description = "Mot de passe (minimum 8 caractères)")
  @NotBlank
  @Size(min = 8, max = 72)
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
    RegisterRequest registerRequest = (RegisterRequest) o;
    return Objects.equals(this.email, registerRequest.email) &&
        Objects.equals(this.password, registerRequest.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegisterRequest {\n");
    
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
