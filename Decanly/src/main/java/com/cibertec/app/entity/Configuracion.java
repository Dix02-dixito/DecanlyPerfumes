package com.cibertec.app.entity;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion")
public class Configuracion {
    @Id private Integer id;

    @Column(name = "descuento_pack")
    private Integer descuentoPack;
}
