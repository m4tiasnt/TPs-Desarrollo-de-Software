package com.facturaarca.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "domicilios") // MODIFICADO: se quitó schema = "model", la tabla pasa al esquema "public" por defecto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class Domicilio extends EntityId {

    private String nombreCalle;
    private String numeroCalle;
}