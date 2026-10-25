package com.footballai.social.model.entity;

import com.footballai.social.model.enums.EntidadeTipo;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

@Entity
@Table(name = "user_league_officials")
@DiscriminatorValue("OFICIAL_LIGA")
@PrimaryKeyJoinColumn(name = "user_id")
public class LeagueOfficial extends User {
    @Column(name = "entity_name", length = 120) private String entityName;
    @Enumerated(EnumType.STRING) @Column(name = "entidade_tipo", length = 24) private EntidadeTipo entidadeTipo;
    @Column(length = 80) private String jurisdiction;

    @Override public UserRole role() { return UserRole.OFICIAL_LIGA; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public EntidadeTipo getEntidadeTipo() { return entidadeTipo; }
    public void setEntidadeTipo(EntidadeTipo entidadeTipo) { this.entidadeTipo = entidadeTipo; }
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
}
