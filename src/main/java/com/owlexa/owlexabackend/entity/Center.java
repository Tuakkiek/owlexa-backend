package com.owlexa.owlexabackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table (name = "centers")
@Data
public class Center {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String subdomain;
}
