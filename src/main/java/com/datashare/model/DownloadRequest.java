package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Requête pour téléchargement d’un fichier protégé
 */
@Schema(description = "Requête pour téléchargement d’un fichier protégé")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class DownloadRequest   {
  @JsonProperty("password")
  private String password = null;

  public DownloadRequest password(String password) { 


    this.password = password;
    return this;
  }

  /**
   * Mot de passe du fichier si requis
   * @return password
   **/
  
  @Schema(example = "secret123", description = "Mot de passe du fichier si requis")
   
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
    DownloadRequest downloadRequest = (DownloadRequest) o;
    return Objects.equals(this.password, downloadRequest.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DownloadRequest {\n");
    
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
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
