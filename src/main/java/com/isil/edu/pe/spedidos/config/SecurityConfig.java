package com.isil.edu.pe.spedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security.
 *
 * <p>[PA3] Matriz de accesos vigente:</p>
 * <ul>
 *   <li>GET /reportes/** y GET de los endpoints de reportes: OPERADOR y CONSULTA.</li>
 *   <li>Otros métodos sobre /reportes/**: denegados (módulo de solo lectura).</li>
 *   <li>POST /pedidos: exclusivo de OPERADOR.</li>
 *   <li>Usuario anónimo: redirigido al formulario de login ({@code formLogin}).</li>
 * </ul>
 *
 * <p>La protección CSRF se mantiene <strong>habilitada</strong> (comportamiento por
 * defecto; no se invoca {@code csrf().disable()}): todo POST con sesión debe
 * enviar un token válido, incluidos el registro de pedidos y el logout.</p>
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
    return httpSecurity.authorizeHttpRequests(auth -> auth
            .requestMatchers("/assets/**").permitAll()
            .requestMatchers("/h2-console/**").denyAll()
            .requestMatchers(HttpMethod.POST, "/pedidos")
              .hasAnyRole("OPERADOR")
            // [PA3] Vista MVC de reportes: lectura para OPERADOR y CONSULTA.
            .requestMatchers(HttpMethod.GET, "/reportes/**")
              .hasAnyRole("OPERADOR", "CONSULTA")
            // [PA3] Los reportes son de solo lectura: cualquier otro método se rechaza.
            .requestMatchers("/reportes/**")
              .denyAll()
            // [PA3] Endpoints JSON del módulo de reportes, declarados explícitamente.
            .requestMatchers(HttpMethod.GET,
                "/api/v1/pedidos/busqueda-avanzada",
                "/api/v1/pedidos/clientes-frecuentes")
              .hasAnyRole("OPERADOR", "CONSULTA")
            .requestMatchers(HttpMethod.GET, "/api/v1/pedidos", "/api/v1/pedidos/**")
              .hasAnyRole("OPERADOR", "CONSULTA")
            .requestMatchers(HttpMethod.GET, "/pedidos")
              .hasAnyRole("OPERADOR", "CONSULTA")
            .anyRequest().authenticated()
        )
        .formLogin(Customizer.withDefaults())
        .logout(Customizer.withDefaults())
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(
      PasswordEncoder encoder,
      @Value("${demo.security.operador-password}")
      String operadorPassword,
      @Value("${demo.security.consulta-password}")
      String consultaPassword) {

    UserDetails operador = User.withUsername("operador")
        .password(encoder.encode(operadorPassword))
        .roles("OPERADOR")
        .build();
    UserDetails consulta = User.withUsername("consulta")
        .password(encoder.encode(consultaPassword))
        .roles("CONSULTA")
        .build();

    return new InMemoryUserDetailsManager(operador, consulta);
  }

}
