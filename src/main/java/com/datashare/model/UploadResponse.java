package com.datashare.model;

import java.util.Objects;
import com.datashare.model.FileMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Réponse après dépôt de fichier
 */
@Schema(description = "Réponse après dépôt de fichier")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class UploadResponse   {
  @JsonProperty("metadata")
  private FileMetadata metadata = null;

  public UploadResponse metadata(FileMetadata metadata) { 


    this.metadata = metadata;
    return this;
  }

  /**
   * Get metadata
   * @return metadata
   **/
  
  @Schema(required = true, description = "")
  @NotNull 
@Valid
  public FileMetadata getMetadata() {  
    return metadata;
  }
  public void setMetadata(FileMetadata metadata){ 


    this.metadata = metadata;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadResponse uploadResponse = (UploadResponse) o;
    return Objects.equals(this.metadata, uploadResponse.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UploadResponse {\n");
    
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
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
