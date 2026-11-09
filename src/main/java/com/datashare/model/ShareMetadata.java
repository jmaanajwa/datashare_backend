package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * ShareMetadata
 */
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class ShareMetadata   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("mimeType")
  private String mimeType = null;

  @JsonProperty("size")
  private Long size = null;

  @JsonProperty("expirationDate")
  private OffsetDateTime expirationDate = null;

  @JsonProperty("passwordRequired")
  private Boolean passwordRequired = null;

  public ShareMetadata name(String name) { 


    this.name = name;
    return this;
  }

  /**
   * Nom du fichier
   * @return name
   **/
  
  @Schema(example = "document.pdf", required = true, description = "Nom du fichier")
  @NotNull 
  public String getName() {  
    return name;
  }
  public void setName(String name){ 


    this.name = name;
  }
  public ShareMetadata mimeType(String mimeType) { 


    this.mimeType = mimeType;
    return this;
  }

  /**
   * Type MIME du fichier
   * @return mimeType
   **/
  
  @Schema(example = "application/pdf", required = true, description = "Type MIME du fichier")
  @NotNull 
  public String getMimeType() {  
    return mimeType;
  }
  public void setMimeType(String mimeType){ 


    this.mimeType = mimeType;
  }
  public ShareMetadata size(Long size) { 


    this.size = size;
    return this;
  }

  /**
   * Taille en octets
   * minimum: 0
   * @return size
   **/
  
  @Schema(example = "123456", required = true, description = "Taille en octets")
  @NotNull 
@Min(0L)  public Long getSize() {  
    return size;
  }
  public void setSize(Long size){ 


    this.size = size;
  }
  public ShareMetadata expirationDate(OffsetDateTime expirationDate) { 


    this.expirationDate = expirationDate;
    return this;
  }

  /**
   * Date d’expiration du lien
   * @return expirationDate
   **/
  
  @Schema(example = "2024-07-01T12:00Z", required = true, description = "Date d’expiration du lien")
  @NotNull 
@Valid
  public OffsetDateTime getExpirationDate() {  
    return expirationDate;
  }
  public void setExpirationDate(OffsetDateTime expirationDate){ 


    this.expirationDate = expirationDate;
  }
  public ShareMetadata passwordRequired(Boolean passwordRequired) { 


    this.passwordRequired = passwordRequired;
    return this;
  }

  /**
   * Indique si un mot de passe est requis pour le téléchargement
   * @return passwordRequired
   **/
  
  @Schema(example = "true", required = true, description = "Indique si un mot de passe est requis pour le téléchargement")
  @NotNull 
  public Boolean isPasswordRequired() {  
    return passwordRequired;
  }
  public void setPasswordRequired(Boolean passwordRequired){ 


    this.passwordRequired = passwordRequired;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShareMetadata shareMetadata = (ShareMetadata) o;
    return Objects.equals(this.name, shareMetadata.name) &&
        Objects.equals(this.mimeType, shareMetadata.mimeType) &&
        Objects.equals(this.size, shareMetadata.size) &&
        Objects.equals(this.expirationDate, shareMetadata.expirationDate) &&
        Objects.equals(this.passwordRequired, shareMetadata.passwordRequired);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, mimeType, size, expirationDate, passwordRequired);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShareMetadata {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    mimeType: ").append(toIndentedString(mimeType)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    expirationDate: ").append(toIndentedString(expirationDate)).append("\n");
    sb.append("    passwordRequired: ").append(toIndentedString(passwordRequired)).append("\n");
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
