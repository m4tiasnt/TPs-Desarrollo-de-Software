package com.facturaarca.entities;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class EntityId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;


    public EntityId() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}