package com.owlexa.owlexabackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "classes")
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String level;
    private Integer maxStudents;
    private Double feePerMonth;

    @ManyToOne
    @JoinColumn(name = "center_id")
    private Center center;
}