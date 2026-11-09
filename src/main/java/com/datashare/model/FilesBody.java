package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * FilesBody
 */
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class FilesBody   {
  @JsonProperty("file")
  private Resource file = null;

  @JsonProperty("password")
  private String password = null;

  @JsonProperty("expirationDays")
  private Integer expirationDays = 7;

  @JsonProperty("tags")
  @Valid
  private List<String> tags = null;
  public FilesBody file(Resource file) { 


    this.file = file;
    return this;
  }

  /**
   * Fichier binaire à déposer
   * @return file
   **/
  
  @Schema(required = true, description = "Fichier binaire à déposer")
  @NotNull 
@Valid
  public Resource getFile() {  
    return file;
  }
  public void setFile(Resource file){ 


    this.file = file;
  }
  public FilesBody password(String password) { 


    this.password = password;
    return this;
  }

  /**
   * Mot de passe facultatif pour protéger le fichier
   * @return password
   **/
  
  @Schema(description = "Mot de passe facultatif pour protéger le fichier")
   
@Size(min=6)   public String getPassword() {  
    return password;
  }
  public void setPassword(String password){ 


    this.password = password;
  }
  public FilesBody expirationDays(Integer expirationDays) { 


    this.expirationDays = expirationDays;
    return this;
  }

  /**
   * Durée de validité du lien en jours
   * minimum: 1
   * maximum: 7
   * @return expirationDays
   **/
  
  @Schema(description = "Durée de validité du lien en jours")
   
@Min(1) @Max(7)   public Integer getExpirationDays() {  
    return expirationDays;
  }
  public void setExpirationDays(Integer expirationDays){ 


    this.expirationDays = expirationDays;
  }
  public FilesBody tags(List<String> tags) { 


    this.tags = tags;
    return this;
  }

  public FilesBody addTagsItem(String tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<String>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Liste facultative de tags (uniquement pour utilisateurs authentifiés)
   * @return tags
   **/
  
  @Schema(description = "Liste facultative de tags (uniquement pour utilisateurs authentifiés)")
   
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
    FilesBody filesBody = (FilesBody) o;
    return Objects.equals(this.file, filesBody.file) &&
        Objects.equals(this.password, filesBody.password) &&
        Objects.equals(this.expirationDays, filesBody.expirationDays) &&
        Objects.equals(this.tags, filesBody.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(file, password, expirationDays, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FilesBody {\n");
    
    sb.append("    file: ").append(toIndentedString(file)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    expirationDays: ").append(toIndentedString(expirationDays)).append("\n");
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
