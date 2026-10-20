# Qué hicieron tus compañeros y qué se integró

Se revisaron `Decanly-Spring-Login.rar` y `LPII_SPRING_DECANLY-promociones backend.zip`. Se aplicaron sobre el proyecto de perfumes conservando su estructura por capas. El frontend mantiene su diseño, fuentes, carruseles y selección de productos; ahora obtiene los datos reales de Spring y MySQL.

## 1. Aporte de login

Tu compañero implementó autenticación real con Spring Security, BCrypt y una sesión del servidor. No es simplemente comparar dos textos en JavaScript.

| Archivo dentro de `src/main/java/com/cibertec/app` | Responsabilidad |
| --- | --- |
| `entity/Usuario.java` | Representa la cuenta de MySQL |
| `repository/UsuarioRepository.java` | Busca usuarios por correo |
| `service/UsuarioService.java` | Declara las operaciones de usuario |
| `service/impl/UsuarioServiceImpl.java` | Conecta los usuarios con Spring Security |
| `config/SeguridadConfig.java` | Configura autenticación, contraseña, sesión y permisos |
| `controller/SesionController.java` | Expone consultar sesión, iniciar sesión y cerrar sesión |

### Usuario y repositorio

La entidad contiene nombre, correo, hash de contraseña, rol y estado. JPA relaciona cada propiedad Java con una columna. Se conservaron los nombres Java del compañero y se adaptaron al esquema de perfumes:

```java
@Column(name = "email", nullable = false, unique = true, length = 120)
private String correo;

@Column(name = "password_hash", nullable = false, length = 100)
private String claveHash;
```

El código usa `correo`, pero MySQL almacena la columna `email`. `@Column` evita tener que renombrar la tabla o perder datos. El ID ahora es Long porque el esquema utiliza BIGINT.

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
}
```

`JpaRepository` aporta operaciones comunes. Spring Data interpreta `findByCorreoIgnoreCase` y crea la consulta usando la propiedad `correo`, sin distinguir mayúsculas. `Optional` representa que puede no encontrar una cuenta. No hace falta escribir una implementación del repositorio a mano.

### El servicio y UserDetailsService

`UsuarioServiceImpl` implementa tanto `UsuarioService`, que pertenece al proyecto, como `UserDetailsService`, que utiliza Spring Security. El método `loadUserByUsername` busca la cuenta y construye:

```java
return User.withUsername(usuario.getCorreo())
        .password(usuario.getClaveHash())
        .roles(usuario.getRol())
        .disabled(!usuario.isActivo())
        .build();
```

`withUsername` usa el correo como identificador. `password` entrega el hash almacenado al proveedor de autenticación; no guarda una contraseña nueva. `roles("ADMIN")` genera la autoridad `ROLE_ADMIN`, por lo que en MySQL se escribe ADMIN sin el prefijo. `disabled(true)` impide entrar a una cuenta inactiva.

El compañero inyectó el repositorio por constructor. Spring le entrega el objeto cuando crea el servicio. En perfumes se conserva `@Autowired` en campos, como en clase; ambos estilos pueden convivir. `@Transactional(readOnly = true)` delimita una consulta de datos.

### Configuración de seguridad

| Componente | Qué hace |
| --- | --- |
| `BCryptPasswordEncoder(12)` | Genera y verifica hashes; 12 es el costo de cálculo |
| `DaoAuthenticationProvider` | Busca el usuario y comprueba la contraseña con el encoder |
| `AuthenticationManager` | Recibe la solicitud de autenticación y la delega al proveedor |
| `HttpSessionSecurityContextRepository` | Conserva la autenticación en la sesión HTTP |
| `SecurityFilterChain` | Aplica las reglas antes de ejecutar los controladores |

`@Bean` registra esos objetos para que Spring los comparta. El hash BCrypt no se descifra para recuperar la contraseña: se verifica la contraseña ingresada contra ese hash. No se devuelve el hash al navegador.

La configuración original protegía escrituras API, pero dejaba otras rutas públicas. Perfumes utiliza formularios MVC, por lo que agregué protección ADMIN a `/producto/**`, `/promocion/**`, `/pedidos/**` y `/vistas`. Es una comprobación del servidor, aunque alguien escriba la URL directamente.

### SesionController: tres operaciones

| Ruta | Qué recibe | Qué devuelve |
| --- | --- | --- |
| GET `/api/sesion` | Cookie de sesión, si existe | Usuario público o `user: null` |
| POST `/api/sesion` | JSON con email/password y token CSRF | Sesión iniciada o error de credenciales |
| DELETE `/api/sesion` | Sesión y token CSRF | 204 y sesión invalidada |

Es `@RestController`: responde JSON. Los `record` de petición/respuesta agrupan datos y evitan exponer la entidad Usuario completa. `UsuarioPublico` contiene nombre, correo y rol, nunca contraseña ni hash.

El POST realiza estos pasos:

1. Comprueba que lleguen los campos y sus longitudes.
2. `authenticationManager.authenticate(...)` valida las credenciales contra MySQL.
3. Revisa que el usuario esté activo.
4. Crea la sesión, cambia su identificador y renueva CSRF.
5. Crea un SecurityContext y coloca la autenticación.
6. `contextRepository.saveContext(...)` guarda ese contexto para futuras peticiones.
7. Devuelve el usuario público; JavaScript navega al mantenimiento.

Guardar el contexto ya era parte del aporte de tu compañero. Se añadió la estrategia para renovar la sesión y CSRF, y se limpia también el contexto persistido cuando una nueva autenticación falla. Así no queda autenticada silenciosamente una cuenta de un intento anterior.

CSRF es un token asociado a la sesión para operaciones que cambian datos. El layout lo coloca en etiquetas `meta`; `api.js` lo envía como cabecera. Thymeleaf agrega `_csrf` a los formularios POST. No se desactivó CSRF para hacer funcionar los botones.

Hay dos formas de salir: DELETE `/api/sesion` desde Cuenta y POST `/logout` desde el panel. Ambas invalidan la sesión. El frontend navega después de entrar/salir para recibir los datos y el token actuales.

`AdministradorInicial`, agregado a la integración, crea la primera cuenta con variables de entorno solo cuando `usuario` está vacío. Si ya hay cuentas no las cambia ni sustituye sus contraseñas.

## 2. Aporte de promociones

El proyecto recibido implementa **consulta de packs**, no su mantenimiento completo.

| Archivo | Qué hace |
| --- | --- |
| `entity/Pack.java` | Cabecera del pack y lista de detalles |
| `entity/PackDetalle.java` | Perfume, tamaño y cantidad de una línea |
| `repository/PackRepository.java` | Accede a los packs de MySQL mediante JPA |
| `service/PackService.java` | Declara `listarTodos()` |
| `service/impl/PackServiceImpl.java` | Implementa la consulta |
| `controller/PackController.java` | Devuelve la lista con GET `/api/packs` |

El controlador conserva el flujo del compañero:

```java
@GetMapping
public ResponseEntity<List<Pack>> listarPacks() {
    List<Pack> lista = packService.listarTodos();
    return ResponseEntity.ok(lista);
}
```

`@GetMapping` atiende una consulta. `ResponseEntity.ok` devuelve HTTP 200 y el cuerpo JSON. El servicio consulta al repositorio; el frontend lo recibe mediante `fetch` y dibuja las tarjetas.

### Relaciones y JSON

`@OneToMany` expresa que un pack tiene varios detalles. `@ManyToOne` expresa que cada detalle pertenece a un pack. `cascade = ALL` permite propagar operaciones de persistencia entre las entidades, pero **no crea endpoints ni botones de CRUD**.

El compañero usó `@JsonProperty` para conservar el contrato del frontend:

```java
@JsonProperty("title")
private String nombre;

@JsonProperty("items")
private List<PackDetalle> detalles;
```

En Java se llaman nombre/detalles; en JSON se llaman title/items. `@JsonIgnore` evita serializar el enlace de vuelta detalle → pack, que produciría un ciclo pack → detalles → pack.

Su detalle enviaba `idPresentacion` como `id`. El carrito espera el ID del perfume, no el de su presentación. Se corrigió para devolver, por ejemplo:

```json
{
  "id": 1,
  "title": "Pack de ejemplo",
  "text": "Descripción",
  "badge": "Favorito de muchos",
  "active": true,
  "items": [{"id": 7, "size": "10", "qty": 1}]
}
```

Es un ejemplo del contrato: perfume 7, tamaño 10 ml, cantidad 1. Si la presentación tuviera ID 42, no se debe enviar 42 como perfume.

La tabla existente `pack_detalle` tiene clave compuesta `(pack_id, producto_id)`. Se representa con `PackDetalleId` y `@IdClass`. No permite dos filas del mismo perfume en un pack, aunque sí cantidad mayor que uno. Cambiar esa regla requeriría modificar la clave y la interfaz.

En la integración, `@EntityGraph` carga detalles y productos necesarios para la respuesta. El servicio filtra packs vacíos o con productos retirados. Stock cero es distinto de retirado: el pack con componentes agotados sigue apareciendo como agotado. Se retiró `@CrossOrigin("*")` porque frontend y API se sirven desde la misma aplicación.

## 3. Qué se adaptó

| Diferencia | Ajuste |
| --- | --- |
| Login esperaba `id_usuario`, `correo`, `clave_hash` | Mapeo a `id`, `email`, `password_hash` |
| Esquema de perfumes no tenía rol | Migración 05 agrega `usuario.rol` sin borrar cuentas |
| IDs Integer frente a BIGINT | Long en entidades y repositorios |
| Packs esperaba `id_pack` y `nombre` | Mapeo a `id` y `titulo` |
| Detalle confundía ID de presentación y perfume | JSON separa `id` de perfume y `size` |
| Implementación de packs estaba en `service` | Se ubica en `service.impl`, como en clase |
| Login solo protegía otras rutas API | Se protegen también los formularios MVC de perfumes |
| Existían datos/CRUD/login simulados en JavaScript | Se retiraron y se conectaron los módulos implementados a Spring |
| Propiedades de referencias incluían credenciales locales | Variables de entorno; no se copiaron contraseñas |

No se borraron ni recrearon las tablas para hacer coincidir los proyectos. El archivo 05 está destinado únicamente al esquema anterior de Decanly, cuando todavía no tiene rol.

## 4. MVC y REST dentro del mismo proyecto

Perfumes usa `@Controller`, Model, formularios y Thymeleaf, siguiendo LPII. Login y packs conservan `@RestController` con respuestas JSON, siguiendo a tus compañeros. Son dos maneras de presentar la respuesta; comparten servicios, repositorios y MySQL.

`api.js` es un cliente HTTP, no un backend. `sessionStorage` guarda solo la selección del carrito. No decide si el administrador está autenticado ni guarda productos. Los precios y stock provienen de MySQL, pero el total del carrito sigue siendo visual hasta implementar pedidos.

## 5. Lo que todavía falta

El compañero de packs no implementó POST/PUT/DELETE. Por ello faltan crear, editar y retirar/restaurar packs desde el panel. El listado sí funciona. Para completarlo agreguen métodos a PackService, transacciones y validaciones en PackServiceImpl, controladores protegidos y formularios. Validar productos activos, tamaños, cantidades, duplicados y versión antes de guardar cabecera/detalles juntos.

Faltan registro de pedidos, referencia real, recálculo de importes en Java, control concurrente/reserva de stock, cambios de estado e idempotencia para evitar duplicados. Las tablas existen, pero el carrito por WhatsApp todavía no las utiliza. También siguen pendientes el CRUD de marcas, usuarios y configuración; autenticarse no equivale a tener un CRUD de usuarios.

No se implementaron cuerpos de procedimientos almacenados: se mantienen sus nombres para que el grupo los desarrolle si el profesor los solicita.

## 6. Para explicarlo en clase

Puedes decir, con tus palabras:

> Integramos el mantenimiento MVC de perfumes con dos aportes REST. El login usa Spring Security, obtiene el usuario con JPA y verifica su hash BCrypt. La autenticación queda en la sesión del servidor y los permisos protegen el mantenimiento. Promociones consulta packs con sus detalles y devuelve JSON. Adaptamos las columnas y los IDs al esquema existente y retiramos la simulación del frontend. El siguiente trabajo es completar el CRUD de packs y el registro de pedidos.

Demuestra un caso: entra, cambia stock de 5 ml, recarga el catálogo y comprueba la fila en Workbench. Después abre `/api/packs` para mostrar los detalles. Cierra sesión y prueba entrar a `/producto`: debe pedir acceso.

**Protip de Eclipse:** `Ctrl+Shift+T` abre una clase por nombre. `F3` sobre una llamada abre su declaración; `Ctrl+T` ayuda a pasar de la interfaz a su implementación. Sigue primero una operación completa, en lugar de memorizar todas las clases a la vez.
