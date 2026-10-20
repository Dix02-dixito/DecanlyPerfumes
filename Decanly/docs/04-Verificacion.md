# Verificación de esta entrega

Comprobaciones realizadas el 17 de octubre de 2026 sobre el código integrado.

| Comprobación | Resultado | Alcance |
| --- | --- | --- |
| Maven / Java 17 | Compilación y WAR correctos | Proyecto Spring Boot 3.5.10 |
| JUnit + MockMvc + H2 | 7 pruebas, 0 fallos, 0 errores | Login/sesión/CSRF, permisos, formulario MVC, CRUD de perfumes, fotos, transacciones y JSON de packs |
| JavaScript + JSDOM | 80 comprobaciones aprobadas | Menú/búsqueda, filtros, modal/tamaños, carrito/WhatsApp, login, logout, POST nativo, rutas con contexto y errores de API |
| MySQL 8.4.7 real | 18 comprobaciones aprobadas | Importación del catálogo, migración del usuario, autenticación HTTP, creación/edición de perfumes con fotos, retiro/restauración, packs y persistencia tras reiniciar |

La prueba MySQL usó una base aislada de prueba: 50 perfumes, 50 imágenes, 150 presentaciones y 9 packs. Se partió del esquema anterior sin rol, se creó una cuenta de prueba con hash BCrypt y se aplicó la migración 05. Se comprobó que conservase esa cuenta y su hash. No se conectó ni modificó tu instalación de MySQL.

Los 7 tests Java están en `src/test/java/com/cibertec/app/IntegracionTests.java`. El perfil test usa H2 en memoria y no tu base MySQL. Para ejecutarlos en Windows:

```powershell
.\mvnw.cmd test
```

Generan HTML renderizado por Spring en `target/frontend-qa`. Para las pruebas de interfaz, con Node.js instalado:

```powershell
cd src\test\frontend
npm ci
npm test
```

Estas pruebas DOM usan respuestas preparadas para las APIs; no representan una prueba visual de píxeles en un navegador real. Las peticiones contra el servidor y MySQL se comprobaron por separado. Los resultados resumidos están en `docs/verificacion`.

No se probaron como terminados los módulos que todavía faltan: CRUD de packs, registro/pago de pedidos y demás mantenimientos. El enlace WhatsApp prepara una consulta; no confirma una compra ni modifica inventario.
