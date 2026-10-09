package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rubros", schema = "model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class Rubro extends AuditoriaApp {

    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private Integer codigo;
}