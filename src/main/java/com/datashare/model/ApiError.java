package com.datashare.model;

import java.util.Objects;
import com.datashare.model.FieldError;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Format commun d’erreur API
 */
@Schema(description = "Format commun d’erreur API")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class ApiError   {
  @JsonProperty("code")
  private Integer code = null;

  @JsonProperty("message")
  private String message = null;

  @JsonProperty("timestamp")
  private OffsetDateTime timestamp = null;

  @JsonProperty("fieldErrors")
  @Valid
  private List<FieldError> fieldErrors = null;
  public ApiError code(Integer code) { 


    this.code = code;
    return this;
  }

  /**
   * Code HTTP de l’erreur
   * @return code
   **/
  
  @Schema(example = "401", required = true, description = "Code HTTP de l’erreur")
  @NotNull 
  public Integer getCode() {  
    return code;
  }
  public void setCode(Integer code){ 


    this.code = code;
  }
  public ApiError message(String message) { 


    this.message = message;
    return this;
  }

  /**
   * Message d’erreur descriptif
   * @return message
   **/
  
  @Schema(example = "Authentification requise", required = true, description = "Message d’erreur descriptif")
  @NotNull 
  public String getMessage() {  
    return message;
  }
  public void setMessage(String message){ 


    this.message = message;
  }
  public ApiError timestamp(OffsetDateTime timestamp) { 


    this.timestamp = timestamp;
    return this;
  }

  /**
   * Date et heure de l’erreur
   * @return timestamp
   **/
  
  @Schema(example = "2024-06-01T16:00Z", required = true, description = "Date et heure de l’erreur")
  @NotNull 
@Valid
  public OffsetDateTime getTimestamp() {  
    return timestamp;
  }
  public void setTimestamp(OffsetDateTime timestamp){ 


    this.timestamp = timestamp;
  }
  public ApiError fieldErrors(List<FieldError> fieldErrors) { 


    this.fieldErrors = fieldErrors;
    return this;
  }

  public ApiError addFieldErrorsItem(FieldError fieldErrorsItem) {
    if (this.fieldErrors == null) {
      this.fieldErrors = new ArrayList<FieldError>();
    }
    this.fieldErrors.add(fieldErrorsItem);
    return this;
  }

  /**
   * Liste des erreurs de champ (optionnel)
   * @return fieldErrors
   **/
  
  @Schema(description = "Liste des erreurs de champ (optionnel)")
   
@Valid
  public List<FieldError> getFieldErrors() {  
    return fieldErrors;
  }
  public void setFieldErrors(List<FieldError> fieldErrors){ 


    this.fieldErrors = fieldErrors;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiError apiError = (ApiError) o;
    return Objects.equals(this.code, apiError.code) &&
        Objects.equals(this.message, apiError.message) &&
        Objects.equals(this.timestamp, apiError.timestamp) &&
        Objects.equals(this.fieldErrors, apiError.fieldErrors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, timestamp, fieldErrors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiError {\n");
    
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    fieldErrors: ").append(toIndentedString(fieldErrors)).append("\n");
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
