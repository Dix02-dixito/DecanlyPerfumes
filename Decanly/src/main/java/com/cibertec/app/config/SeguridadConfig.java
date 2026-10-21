package com.cibertec.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {
	// MAQUETA DE CLASE: pantallas públicas; login y permisos pendientes del
	// integrante 4.
	// Todos los POST de negocio devuelven 501 y no escriben en la base de datos.
	@Bean
	public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).formLogin(f -> f.disable())
				.httpBasic(b -> b.disable()).logout(l -> l.disable());
		// Se conserva CSRF: Thymeleaf agrega el token a los formularios POST.
		return http.build();
	}
}
