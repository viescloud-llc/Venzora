package com.viescloud.llc.venzora.model.authentication;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserGroup implements Serializable {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(columnDefinition = "TEXT", unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
