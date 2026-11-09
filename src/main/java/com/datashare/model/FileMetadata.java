package com.datashare.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Métadonnées d’un fichier déposé
 */
@Schema(description = "Métadonnées d’un fichier déposé")
@Validated

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")


public class FileMetadata   {
  @JsonProperty("id")
  private UUID id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("size")
  private Long size = null;

  @JsonProperty("uploadDate")
  private OffsetDateTime uploadDate = null;

  @JsonProperty("expirationDate")
  private OffsetDateTime expirationDate = null;

  /**
   * État du lien (active,expired)
   */
  public enum LinkStatusEnum {
    ACTIVE("active"),
    
    EXPIRED("expired");

    private String value;

    LinkStatusEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static LinkStatusEnum fromValue(String text) {
      for (LinkStatusEnum b : LinkStatusEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }
  @JsonProperty("linkStatus")
  private LinkStatusEnum linkStatus = null;

  @JsonProperty("tags")
  @Valid
  private List<String> tags = new ArrayList<String>();
  @JsonProperty("downloadLink")
  private String downloadLink = null;

  public FileMetadata id(UUID id) { 


    this.id = id;
    return this;
  }

  /**
   * Identifiant unique du fichier
   * @return id
   **/
  
  @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true, description = "Identifiant unique du fichier")
  @NotNull 
@Valid
  public UUID getId() {  
    return id;
  }
  public void setId(UUID id){ 


    this.id = id;
  }
  public FileMetadata name(String name) { 


    this.name = name;
    return this;
  }

  /**
   * Nom original du fichier
   * @return name
   **/
  
  @Schema(example = "photo.png", required = true, description = "Nom original du fichier")
  @NotNull 
  public String getName() {  
    return name;
  }
  public void setName(String name){ 


    this.name = name;
  }
  public FileMetadata size(Long size) { 


    this.size = size;
    return this;
  }

  /**
   * Taille en octets
   * minimum: 0
   * @return size
   **/
  
  @Schema(example = "204800", required = true, description = "Taille en octets")
  @NotNull 
@Min(0L)  public Long getSize() {  
    return size;
  }
  public void setSize(Long size){ 


    this.size = size;
  }
  public FileMetadata uploadDate(OffsetDateTime uploadDate) { 


    this.uploadDate = uploadDate;
    return this;
  }

  /**
   * Date d’envoi
   * @return uploadDate
   **/
  
  @Schema(example = "2024-06-01T15:30Z", required = true, description = "Date d’envoi")
  @NotNull 
@Valid
  public OffsetDateTime getUploadDate() {  
    return uploadDate;
  }
  public void setUploadDate(OffsetDateTime uploadDate){ 


    this.uploadDate = uploadDate;
  }
  public FileMetadata expirationDate(OffsetDateTime expirationDate) { 


    this.expirationDate = expirationDate;
    return this;
  }

  /**
   * Date d’expiration du lien
   * @return expirationDate
   **/
  
  @Schema(example = "2024-06-08T15:30Z", required = true, description = "Date d’expiration du lien")
  @NotNull 
@Valid
  public OffsetDateTime getExpirationDate() {  
    return expirationDate;
  }
  public void setExpirationDate(OffsetDateTime expirationDate){ 


    this.expirationDate = expirationDate;
  }
  public FileMetadata linkStatus(LinkStatusEnum linkStatus) { 


    this.linkStatus = linkStatus;
    return this;
  }

  /**
   * État du lien (active,expired)
   * @return linkStatus
   **/
  
  @Schema(example = "active", required = true, description = "État du lien (active,expired)")
  @NotNull 
  public LinkStatusEnum getLinkStatus() {  
    return linkStatus;
  }
  public void setLinkStatus(LinkStatusEnum linkStatus){ 


    this.linkStatus = linkStatus;
  }
  public FileMetadata tags(List<String> tags) { 


    this.tags = tags;
    return this;
  }

  public FileMetadata addTagsItem(String tagsItem) {
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Liste des tags associés
   * @return tags
   **/
  
  @Schema(example = "[\"vacances\",\"famille\"]", required = true, description = "Liste des tags associés")
  @NotNull 
  public List<String> getTags() {  
    return tags;
  }
  public void setTags(List<String> tags){ 


    this.tags = tags;
  }
  public FileMetadata downloadLink(String downloadLink) { 


    this.downloadLink = downloadLink;
    return this;
  }

  /**
   * Lien unique de téléchargement (tokenisé)
   * @return downloadLink
   **/
  
  @Schema(example = "https://example.com/api/shares/abc123token", required = true, description = "Lien unique de téléchargement (tokenisé)")
  @NotNull 
  public String getDownloadLink() {  
    return downloadLink;
  }
  public void setDownloadLink(String downloadLink){ 


    this.downloadLink = downloadLink;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileMetadata fileMetadata = (FileMetadata) o;
    return Objects.equals(this.id, fileMetadata.id) &&
        Objects.equals(this.name, fileMetadata.name) &&
        Objects.equals(this.size, fileMetadata.size) &&
        Objects.equals(this.uploadDate, fileMetadata.uploadDate) &&
        Objects.equals(this.expirationDate, fileMetadata.expirationDate) &&
        Objects.equals(this.linkStatus, fileMetadata.linkStatus) &&
        Objects.equals(this.tags, fileMetadata.tags) &&
        Objects.equals(this.downloadLink, fileMetadata.downloadLink);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, size, uploadDate, expirationDate, linkStatus, tags, downloadLink);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileMetadata {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    uploadDate: ").append(toIndentedString(uploadDate)).append("\n");
    sb.append("    expirationDate: ").append(toIndentedString(expirationDate)).append("\n");
    sb.append("    linkStatus: ").append(toIndentedString(linkStatus)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    downloadLink: ").append(toIndentedString(downloadLink)).append("\n");
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
