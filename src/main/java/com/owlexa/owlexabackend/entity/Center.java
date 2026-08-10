package com.owlexa.owlexabackend.entity;

import jakarta.persistence.*;

@Entity
@Table (name = "centers")
public class Center {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String subdomain;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
