package com.footballai.social.model.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.time.Instant;
@MappedSuperclass
public abstract class ClubResource {
    @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(columnDefinition="BINARY(16)")
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) public UUID id;
    @Column(name="club_id", columnDefinition="BINARY(16)", nullable=false)
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) public UUID clubId;
    @NotBlank @Size(max=240) @Column(length=240) public String source;
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) public Instant createdAt;
    @JsonProperty(access=JsonProperty.Access.READ_ONLY) public Instant updatedAt;
    @Version @JsonProperty(access=JsonProperty.Access.READ_ONLY) public long version;
}
