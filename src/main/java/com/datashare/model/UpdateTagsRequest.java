package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Requête pour remplacer les tags d’un fichier
 */
@Schema(description = "Requête pour remplacer les tags d’un fichier")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class UpdateTagsRequest   {
  @JsonProperty("tags")
  @Valid
  private List<String> tags = new ArrayList<String>();
  public UpdateTagsRequest tags(List<String> tags) { 


    this.tags = tags;
    return this;
  }

  public UpdateTagsRequest addTagsItem(String tagsItem) {
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Nouvelle liste de tags
   * @return tags
   **/
  
  @Schema(example = "[\"projet\",\"confidentiel\"]", required = true, description = "Nouvelle liste de tags")
  @NotNull 
  public List<String> getTags() {  
    return tags;
  }
  public void setTags(List<String> tags){ 


    this.tags = tags;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateTagsRequest updateTagsRequest = (UpdateTagsRequest) o;
    return Objects.equals(this.tags, updateTagsRequest.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateTagsRequest {\n");
    
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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
