package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * FieldError
 */
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class FieldError   {
  @JsonProperty("field")
  private String field = null;

  @JsonProperty("message")
  private String message = null;

  public FieldError field(String field) { 


    this.field = field;
    return this;
  }

  /**
   * Get field
   * @return field
   **/
  
  @Schema(example = "password", required = true, description = "")
  @NotNull 
  public String getField() {  
    return field;
  }
  public void setField(String field){ 


    this.field = field;
  }
  public FieldError message(String message) { 


    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   **/
  
  @Schema(example = "Le mot de passe doit contenir au moins 8 caractères", required = true, description = "")
  @NotNull 
  public String getMessage() {  
    return message;
  }
  public void setMessage(String message){ 


    this.message = message;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldError fieldError = (FieldError) o;
    return Objects.equals(this.field, fieldError.field) &&
        Objects.equals(this.message, fieldError.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FieldError {\n");
    
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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
