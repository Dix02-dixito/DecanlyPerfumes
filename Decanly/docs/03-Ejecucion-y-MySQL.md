# Ejecutar y actualizar la base de datos

El mismo proyecto funciona en Spring Tools/Eclipse y VS Code. Utiliza JDK 17, Spring Boot 3.5.10, Maven, WAR y MySQL. Las plantillas están en `src/main/resources/templates`; no uses Live Server, porque necesita Thymeleaf y la sesión de Spring.

## Base nueva o existente

**Nueva:** importa una vez 01-esquema.sql, 02-imagenes.sql y 03-datos.sql. Incluyen 11 tablas, 50 perfumes, 50 imágenes, 150 presentaciones y 9 packs. El stock es cero. No se insertan usuarios con contraseña conocida ni pedidos ficticios. El archivo 02 es grande porque contiene los bytes de las imágenes.

**Existente del proyecto de perfumes:** realiza una copia de seguridad y comprueba:

```sql
SHOW COLUMNS FROM decanly.usuario;
SHOW COLUMNS FROM decanly.usuario LIKE 'rol';
```

Si tiene id/email/nombre/password_hash/activo y falta rol, ejecuta solo 05-integracion-login.sql. Agrega rol VARCHAR(20) DEFAULT 'ADMIN', conservando usuarios e imágenes. No repitas este ALTER si rol ya existe. Una base nueva ya lo incluye en 01, por lo que no necesita 05.

La migración presupone que las cuentas anteriores eran administradores. Si tu tabla también contiene clientes, hay que asignar sus roles por separado. No sirve para convertir a ciegas otro esquema con columnas id_usuario/correo/clave_hash.

`ddl-auto=validate` comprueba el esquema al arrancar; no crea ni modifica tablas. Reimportar el catálogo inicial no es un método para actualizar el panel y puede producir claves duplicadas.

## Tablas

| Tabla | Función |
| --- | --- |
| marca | Catálogo de marcas |
| imagen | Nombre, MIME, bytes LONGBLOB y SHA-256 |
| producto | Datos del perfume, portada, descuento, activo y versión |
| presentacion | Precio y stock por producto y 3/5/10 ml |
| producto_imagen | Fotos extra y orden del carrusel |
| pack | Cabecera, descripción, etiqueta y estado |
| pack_detalle | Composición: producto, tamaño y cantidad |
| configuracion | Descuento de pack |
| usuario | Correo, nombre, hash BCrypt, rol y estado |
| pedido | Estructura para el futuro módulo de pedidos |
| pedido_detalle | Estructura para productos/importes del pedido |

La combinación producto_id/ml es única en presentacion y está referenciada por pack_detalle. Su clave pack_id/producto_id permite una línea por perfume en cada pack. Pedido incluye referencia, estado, clave_solicitud y payload_hash para el trabajo posterior de evitar duplicados y gestionar reservas; esos procesos todavía no están implementados.

## VS Code / PowerShell

Abre la carpeta que contiene pom.xml. Comprueba:

```powershell
java -version
javac -version
.\mvnw.cmd -version
```

Maven debe utilizar JDK 17. Si aparece «No compiler is provided», corrige JAVA_HOME y abre otra terminal.

En esa misma terminal:

```powershell
$env:DB_USER = 'decanly_app'
$claveBD = Read-Host 'Escribe la contraseña de MySQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $claveBD).Password
```

La frase entre comillas de Read-Host es solo el mensaje. Escribe la contraseña cuando aparezca esa pregunta, no como título de Read-Host. Ese usuario debe existir en MySQL y tener permisos sobre decanly; el proyecto no crea usuarios del servidor MySQL.

Si ya tienes una cuenta en la tabla usuario, usa esa cuenta. Si está vacía, configura el primer administrador:

```powershell
$env:ADMIN_EMAIL = 'admin@decanly.pe'
do {
    $claveAdmin = Read-Host 'Escribe una contraseña nueva de al menos 12 caracteres' -AsSecureString
    $env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $claveAdmin).Password
} while (
    $env:ADMIN_PASSWORD.Length -lt 12 -or
    [System.Text.Encoding]::UTF8.GetByteCount($env:ADMIN_PASSWORD) -gt 72
)
.\mvnw.cmd spring-boot:run
```

Abre http://localhost:8091. La terminal debe seguir ocupada con la aplicación. BUILD SUCCESS de Maven por sí solo no significa que el servidor permanezca encendido; si vuelve al prompt, revisa el primer error y Caused by.

La creación inicial solo se ejecuta cuando usuario está vacío. Cambiar ADMIN_PASSWORD después no cambia una contraseña ya guardada.

## Spring Tools/Eclipse

File → Import → Maven → Existing Maven Projects; selecciona Decanly y su pom.xml. Ejecuta Maven → Update Project si hace falta. Selecciona JDK 17.

En Run → Run Configurations → Spring Boot App, selecciona DecanlyApplication. En Environment define DB_USER/DB_PASSWORD y, solo para la primera cuenta, ADMIN_EMAIL/ADMIN_PASSWORD. Ejecuta Run As → Spring Boot App.

Las variables de una terminal de VS Code no pasan automáticamente a una ejecución de Eclipse. Configúralas en su Run Configuration. El puerto es 8091; PORT permite cambiarlo. DB_URL permite cambiar servidor/base/puerto MySQL. COOKIE_SECURE es false para HTTP local y debe ser true cuando el despliegue use HTTPS.

## Comprobar el funcionamiento

En Cuenta inicia sesión. Crea un perfume con sus tres precios/stocks y una foto. Recarga el catálogo, filtra por marca y revisa el modal. Edita stock de 5 ml y agrega una foto. Retira/restaura el producto. Cierra sesión e intenta abrir /producto.

En Workbench comprueba:

```sql
SELECT id,nombre,activo,version FROM producto ORDER BY id DESC;
SELECT producto_id,ml,precio,stock FROM presentacion ORDER BY producto_id,ml;
SELECT id,nombre_archivo,tipo_mime,OCTET_LENGTH(contenido) AS bytes FROM imagen;
SELECT producto_id,imagen_id,orden FROM producto_imagen;
SELECT id,email,rol,activo FROM usuario;
SELECT p.titulo,d.producto_id,d.ml,d.cantidad
FROM pack p JOIN pack_detalle d ON d.pack_id=p.id;
```

Los packs necesitan stock de todos sus componentes en el tamaño seleccionado. Con cero unidades se mostrarán agotados. Si falla la importación de imágenes por max_allowed_packet, ajusta el límite de importación desde una cuenta con permisos y reconecta el cliente. El formulario mantiene su límite de 2 MB por archivo.

## Procedimientos almacenados

04-nombres-procedimientos.sql contiene solo nombres comentados. No tiene cuerpos CREATE PROCEDURE. El proyecto funciona con JPA/Hibernate, sin depender de ellos. Si el profesor los requiere, deben programarlos y conectarlos desde los repositorios con la técnica que les enseñe.

| Grupo | Nombres y reglas |
| --- | --- |
| Usuario | sp_usuario_buscar_por_email, sp_usuario_crear: hashes generados en Java, nunca texto plano |
| Marca | sp_marca_listar, sp_marca_guardar: nombre obligatorio y único |
| Producto | sp_producto_listar, sp_producto_obtener, sp_producto_crear, sp_producto_actualizar, sp_producto_eliminar: validar datos, versión y baja lógica |
| Presentación | sp_presentacion_actualizar: 3/5/10 ml, precio positivo, stock no negativo |
| Imagen/galería | sp_imagen_guardar, sp_imagen_obtener, sp_galeria_reemplazar: bytes/MIME/hash y pertenencia de fotos |
| Pack | sp_pack_listar, sp_pack_crear, sp_pack_actualizar, sp_pack_eliminar: cabecera y detalles en una transacción |
| Configuración | sp_configuracion_obtener, sp_configuracion_actualizar: descuento 0–70 |
| Pedido | sp_pedido_crear, sp_pedido_obtener_por_clave, sp_pedido_listar, sp_pedido_cambiar_estado, sp_pedido_vencer_reservas: importes, duplicados, stock y estados |

## Qué desarrollar después

Primero completa el CRUD de packs: servicio, validación, transacción, rutas protegidas y formularios. Después pedidos: releer precios en Java, recalcular total, controlar stock concurrente, evitar duplicados y guardar referencia/detalle. Más adelante marcas, usuarios y configuración. No confíes en un total calculado solamente por JavaScript.

El enlace de WhatsApp no acredita pagos ni registra pedidos. Esta entrega no incluye una auditoría de seguridad/despliegue ni revisa el cumplimiento legal de los textos del comercio.
