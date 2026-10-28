# Preparar y publicar Decanly

La instalación incluida utiliza Java 21 y MySQL 8.4. GitHub Pages no ejecuta Spring Boot.
El Dockerfile compila y prueba el proyecto con Maven 3.9.16 y ejecuta Java con un usuario sin privilegios.

## Si ya tienes una base con productos y fotos

**No reemplaces tu base con `database/001-schema.sql`.** Ese archivo sirve exclusivamente para crear las 11 tablas en una base nueva y vacía. No incluye productos de ejemplo ni usuarios. Las fotos se guardan en `imagen` y sus relaciones en `producto_imagen`.

Antes de migrar, exporta una copia completa desde MySQL Workbench (estructura y datos, incluidas las columnas BLOB) y prueba su restauración en otra base. Primero debemos comparar esa estructura con las entidades del proyecto. No se han modificado bases externas ni se ofrece una migración automática de una base desconocida.

En producción Hibernate usa `validate`: verifica el esquema y no crea, modifica ni borra tablas. Los usuarios y contraseñas ya existentes se conservan.

## Probar desde Windows con una base nueva

1. Instala Docker Desktop con contenedores Linux y descarga esta rama del repositorio.
2. Abre PowerShell en la carpeta `Decanly` (donde está `compose.yaml`).
3. Copia la configuración:

```powershell
Copy-Item .env.example .env
notepad .env
```

Completa `DB_PASSWORD` y `MYSQL_ROOT_PASSWORD` con contraseñas distintas y aleatorias. Para crear el primer administrador configura `BOOTSTRAP_ADMIN=true`, `ADMIN_EMAIL` y `ADMIN_PASSWORD` (16 caracteres como mínimo, hasta 72 bytes). No reutilices la clave de tu correo. Para probar únicamente por HTTP en tu computadora usa `COOKIE_SECURE=false`.

```powershell
docker compose up --build -d
docker compose logs --tail 80 web
```

Abre `http://localhost:8091/cuenta`. El usuario es el correo de `ADMIN_EMAIL` y la clave es `ADMIN_PASSWORD`. No hay una cuenta o contraseña universal en el repositorio. El alta inicial solo funciona si la tabla `usuario` está vacía y guarda un hash BCrypt; nunca sustituye usuarios existentes.

Tras comprobar el acceso, cambia `BOOTSTRAP_ADMIN=false`, borra los valores `ADMIN_EMAIL` y `ADMIN_PASSWORD` de `.env` y ejecuta:

```powershell
docker compose up -d --force-recreate web
```

El administrador permanece en MySQL. Guarda su clave en tu gestor de contraseñas. El usuario de base de datos `decanly` solo recibe SELECT, INSERT, UPDATE y DELETE; la aplicación no usa root. MySQL no publica ningún puerto al exterior. El volumen `db-data` conserva los datos entre reinicios. No uses `docker compose down -v` sobre datos que quieras conservar.

## Publicar con una dirección HTTPS

Hace falta elegir un alojamiento con Docker/Java, almacenamiento persistente para MySQL y copias de seguridad. La cuenta del alojamiento y el dominio o subdominio deben estar a tu nombre. Revisa precio y límites actuales antes de contratar.

- En un servidor con Docker puedes usar `compose.yaml` detrás de un proxy HTTPS. El puerto web se vincula a `127.0.0.1:8091` para que lo publique el proxy; MySQL permanece interno.
- En un alojamiento que despliegue desde GitHub, selecciona esta rama, la carpeta `Decanly` y su Dockerfile; configura `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USER`, `DB_PASSWORD` y `WHATSAPP_NUMBER=51907032488` en los secretos del alojamiento.
- Para una base gestionada externa usa su URL JDBC y TLS con validación de identidad (`sslMode=VERIFY_IDENTITY`) y su certificado CA cuando lo requiera el proveedor. El Compose utiliza `sslMode=REQUIRED` para cifrar la conexión dentro de su red privada con el certificado local de MySQL; ese modo no autentica la identidad del servidor y no sustituye la validación para conexiones externas.
- Activa `COOKIE_SECURE=true` para el acceso público. El perfil de producción usa cookies HttpOnly, SameSite=Lax y sesiones de 30 minutos. Publica exclusivamente a través de un proxy HTTPS que limpie las cabeceras Forwarded/X-Forwarded-*; no expongas directamente el proceso Java a Internet.
- Configura protección contra intentos repetidos de login en el proxy/alojamiento para `/cuenta/login` y `/api/sesion`, y contra envíos automáticos a `/pedido/registrar`. CSRF y los permisos de administración siguen activos.

Para una demostración desde Windows, con Docker Desktop y la aplicación arrancados:

```powershell
winget install --id Cloudflare.cloudflared --exact
# En una nueva ventana de PowerShell:
cloudflared tunnel --url http://localhost:8091
```

Antes de compartir la dirección HTTPS del túnel, activa `COOKIE_SECURE=true` y recrea `web`. Usa una base de demostración. El enlace temporal funciona mientras la computadora, Docker y cloudflared estén encendidos; no es un alojamiento permanente.

## Copias y validación antes de abrir la tienda

Programa exportaciones completas en el alojamiento y verifica una restauración, incluyendo fotos y pedidos. Mantén las copias fuera del repositorio y del mismo disco del servidor. Para una copia local desde Windows utiliza la exportación de MySQL Workbench o `cmd /c` si rediriges un volcado; algunas versiones de PowerShell pueden alterar su codificación.

Comprueba en la URL pública: catálogo e imágenes, selección 3/5/10 ml, carrito, pedido guardado sin tarifa automática de envío, redirección a WhatsApp 51907032488, login/logout, edición de productos y rechazo de accesos sin autorización. Comprueba también las cookies Secure sobre HTTPS. No compartas el administrador con quienes solo van a revisar el catálogo.

La publicación requiere además los productos/fotos reales, la configuración de descuentos, las condiciones de entrega y los textos de privacidad/venta acordes con la operación real. El código y la instalación local no publican por sí solos un sitio ni garantizan la configuración del proveedor.
