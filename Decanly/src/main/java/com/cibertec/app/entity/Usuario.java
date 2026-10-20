package com.cibertec.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "clave_hash", nullable = false, length = 255)
    private String claveHash;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "rol", nullable = false)
    private String rol;
    
    @Column(name = "activo", nullable = false)
    private boolean activo;
    
    public Usuario() {
    }
    public Usuario(
            String nombre,
            String correo,
            String claveHash,
            String rol,
            boolean activo) {

        this.nombre = nombre;
        this.correo = correo;
        this.claveHash = claveHash;
        this.rol = rol;
        this.activo = activo;
    }
    
    public String getNombre() {
        return nombre;
    }
    public String getCorreo() {
        return correo;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public String getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }
}
