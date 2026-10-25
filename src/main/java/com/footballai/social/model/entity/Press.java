package com.footballai.social.model.entity;

import com.footballai.social.model.enums.MidiaTipo;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_press")
@DiscriminatorValue("IMPRENSA")
@PrimaryKeyJoinColumn(name = "user_id")
public class Press extends User {
    @Column(length = 120) private String outlet;
    @Enumerated(EnumType.STRING) @Column(name = "midia_tipo", length = 24) private MidiaTipo midiaTipo;
    @Column(name = "press_card_id", length = 40) private String pressCardId;
    @Column(name = "credential_issuer", length = 80) private String credentialIssuer;
    @Column(name = "credential_valid_until") private LocalDate credentialValidUntil;
    @Column(length = 60) private String beat;

    @Override public UserRole role() { return UserRole.IMPRENSA; }
    public String getOutlet() { return outlet; }
    public void setOutlet(String outlet) { this.outlet = outlet; }
    public MidiaTipo getMidiaTipo() { return midiaTipo; }
    public void setMidiaTipo(MidiaTipo midiaTipo) { this.midiaTipo = midiaTipo; }
    public String getPressCardId() { return pressCardId; }
    public void setPressCardId(String pressCardId) { this.pressCardId = pressCardId; }
    public String getCredentialIssuer() { return credentialIssuer; }
    public void setCredentialIssuer(String credentialIssuer) { this.credentialIssuer = credentialIssuer; }
    public LocalDate getCredentialValidUntil() { return credentialValidUntil; }
    public void setCredentialValidUntil(LocalDate credentialValidUntil) { this.credentialValidUntil = credentialValidUntil; }
    public String getBeat() { return beat; }
    public void setBeat(String beat) { this.beat = beat; }
}
