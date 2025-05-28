package com.viescloud.llc.venzora.model.authentication;

import java.io.Serializable;
import java.util.Set;

import com.viescloud.eco.viesspringutils.interfaces.annotation.Decoding;
import com.viescloud.eco.viesspringutils.interfaces.annotation.Encoding;
import com.viescloud.eco.viesspringutils.model.DecodingType;
import com.viescloud.eco.viesspringutils.model.EncodingType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "VENZORA_USER")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements Serializable {
    
    @Id
    @GeneratedValue
    private Long id;

    @Column(columnDefinition = "TEXT", unique = true)
    private String sub;

    @Column(columnDefinition = "TEXT")
    private String alias;

    @Column(columnDefinition = "TEXT", unique = true)
    private String email;

    @Column(columnDefinition = "TEXT", unique = true)
    private String username;

    @Column(columnDefinition = "TEXT")
    @Encoding(EncodingType.SHA256)
    @Decoding(DecodingType.NULL)
    private String password;

    @ManyToMany(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    private Set<UserGroup> userGroups;
}
