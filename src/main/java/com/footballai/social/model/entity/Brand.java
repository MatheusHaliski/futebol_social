package com.footballai.social.model.entity;

import com.footballai.social.model.enums.MarcaCategoria;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

@Entity
@Table(name = "user_brands")
@DiscriminatorValue("MARCA")
@PrimaryKeyJoinColumn(name = "user_id")
public class Brand extends User {
    @Column(name = "trade_name", length = 120) private String tradeName;
    @Enumerated(EnumType.STRING) @Column(length = 16) private MarcaCategoria categoria;
    @Column(length = 60) private String segment;
    @Column(length = 255) private String website;
    @Column(length = 18) private String cnpj;
    @Column(name = "fazenda_authorization_id", length = 60) private String fazendaAuthorizationId;

    @Override public UserRole role() { return UserRole.MARCA; }
    /** APOSTAS: 18+, advertência obrigatória e exclusão de atletas/staff/árbitros/liga. */
    public boolean isBettingBrand() { return categoria == MarcaCategoria.APOSTAS; }
    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }
    public MarcaCategoria getCategoria() { return categoria; }
    public void setCategoria(MarcaCategoria categoria) { this.categoria = categoria; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getFazendaAuthorizationId() { return fazendaAuthorizationId; }
    public void setFazendaAuthorizationId(String fazendaAuthorizationId) { this.fazendaAuthorizationId = fazendaAuthorizationId; }
}
