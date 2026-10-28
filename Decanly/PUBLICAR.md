# Preparar y publicar Decanly

La instalación incluida utiliza Java 21 y MySQL 8.4. GitHub Pages no ejecuta Spring Boot.
El Dockerfile compila y prueba el proyecto con Maven 3.9.16 y ejecuta Java con un usuario sin privilegios.

## Si ya tienes una base con productos y fotos

**No reemplaces tu base con `database/001-schema.sql`.** Ese archivo sirve exclusivamente para crear las 11 tablas en una base nueva y vacía. No incluye productos de ejemplo ni usuarios. Las fotos se guardan en `imagen` y sus relaciones en `producto_imagen`.

Se comprobó `DecanlysqlNuevo.sql`, del commit `9751147`, en una instancia aislada de MySQL 8.4: contiene 50 productos, 150 presentaciones, 50 imágenes, 21 marcas, 9 promociones y 2 usuarios. El esquema es compatible con la aplicación y no necesita ALTER TABLE. Las imágenes conservan sus hashes SHA-256. Esa copia no contiene pedidos ni fotos adicionales de galería. La importación fuerza UTF-8 (`utf8mb4`) para conservar «Día» y demás acentos; sin ello, el cliente de MySQL del contenedor rechazaba productos por la restricción `chk_producto_ocasion`.

Las cuentas ADMIN y VENDEDOR se conservan con sus hashes BCrypt originales. La aplicación actual permite iniciar sesión únicamente a ADMIN. La cuenta `admin@decanly.local` creada para la base vacía de prueba anterior no pertenece a tu SQL y no se añade al importarlo. Las contraseñas existentes no se pueden recuperar a partir de sus hashes.

El volcado completo se retiró de esta rama y se excluyó de Git y del contexto Docker porque incluye cuentas. La copia original se conserva aparte para la importación. Retirarlo de la versión actual no borra versiones anteriores del historial de GitHub. Antes de publicar, rota las contraseñas de cuentas incluidas en un repositorio accesible a otras personas; una limpieza del historial requiere coordinación aparte.

Si tu base local ya tiene cambios posteriores, exporta nuevamente estructura y datos, incluyendo BLOB, y verifica una restauración antes de migrar. No se han modificado bases externas.

En producción Hibernate usa `validate`: verifica el esquema y no crea, modifica ni borra tablas. Los usuarios y contraseñas ya existentes se conservan.

## Windows: instalar con tu catálogo real

Usa Docker Desktop con contenedores Linux y abre PowerShell en `Decanly`. Este procedimiento inicializa un proyecto nuevo, `decanly-catalogo`, para conservar cualquier instalación anterior.

```powershell
New-Item -ItemType Directory -Force backups
# Ajusta la ruta de origen a donde guardaste tu SQL:
Copy-Item "$env:USERPROFILE\Downloads\DecanlysqlNuevo.sql" .\backups\DecanlysqlNuevo.sql
if (!(Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Configura contraseñas nuevas y distintas para `DB_PASSWORD` y `MYSQL_ROOT_PASSWORD`, y estos valores:

```dotenv
DB_INIT_SQL=./backups/DecanlysqlNuevo.sql
BOOTSTRAP_ADMIN=false
ADMIN_EMAIL=
ADMIN_PASSWORD=
COOKIE_SECURE=false
```

`COOKIE_SECURE=false` sirve solo para probar por HTTP en tu computadora; usa `true` en la dirección pública HTTPS. El SQL debe existir y ser legible por el contenedor. Si falta, Compose falla sin crear un directorio en su lugar. En Linux, guarda el archivo en una carpeta privada y permite que el proceso de MySQL lea el archivo montado.

Si hay otra instalación usando el puerto 8091, detén solo su servicio web (sin eliminar sus volúmenes). Luego:

```powershell
docker compose -p decanly-catalogo up --build -d
docker compose -p decanly-catalogo logs --tail 80 db web
```

Abre `http://localhost:8091` y entra en `/cuenta` con tu ADMIN existente. El SQL se ejecuta **solo con el volumen nuevo y vacío**; cambiar `DB_INIT_SQL` después no reimporta ni actualiza una base ya creada. No uses `down -v` para forzar una importación: eliminaría los datos del proyecto. Si falla la primera importación, revisa los logs antes de continuar.

La comprobación de salud exige una marca de importación terminada y permisos reducidos; una carga parcial no habilita la web. Una instalación creada con el Compose anterior no tiene esa marca. Para actualizar esa instalación, primero respalda y comprueba todas sus tablas, datos y cuentas; después ejecuta `docker compose -p NOMBRE_ANTERIOR exec db bash /docker-entrypoint-initdb.d/20-restrict-user.sh` con los archivos nuevos montados. Ese paso conserva los datos y establece los permisos y la marca, por lo que no debe usarse para dar por completa una importación fallida. La instalación nueva `decanly-catalogo` no necesita este paso.

La contraseña MySQL pertenece al servicio de base de datos y es distinta de la contraseña del administrador de la web. `BOOTSTRAP_ADMIN` permanece desactivado al importar tus usuarios.

## Probar desde Windows con una base nueva

1. Instala Docker Desktop con contenedores Linux y descarga esta rama del repositorio.
2. Abre PowerShell en la carpeta `Decanly` (donde está `compose.yaml`).
3. Copia la configuración:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
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
