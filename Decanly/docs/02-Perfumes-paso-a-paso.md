# Perfumes: guía del backend siguiendo LPII

La estructura conserva entity, repository, service, service.impl, controller y vistas Thymeleaf del proyecto de clase. El objetivo es seguir una operación de principio a fin y explicar por qué cada parte está en su carpeta.

## 1. Qué ocurre al guardar

`templates/producto/formulario.html` envía un POST con campos y archivos. ProductoController recibe los datos. ProductoServiceImpl valida, busca las relaciones y decide qué guardar. Los repositorios hacen las operaciones JPA sobre MySQL. El controlador redirige a `/producto` y el listado vuelve a leer los datos guardados.

La foto que ves antes de guardar es una previsualización del navegador. La foto persistida es el BLOB de MySQL. JavaScript ya no cancela el POST para guardar un producto en memoria.

| Ubicación, dentro de `src/main` | Función |
| --- | --- |
| `java/com/cibertec/app/entity` | Entidades y relaciones de las tablas |
| `java/com/cibertec/app/repository` | Consultas y operaciones de persistencia |
| `java/com/cibertec/app/service` | Contratos de operaciones |
| `java/com/cibertec/app/service/impl` | Reglas, validación y transacciones |
| `java/com/cibertec/app/controller` | Rutas HTTP, formularios y datos de las vistas |
| `resources/templates/producto` | Listado, alta y edición |
| `resources/static/js` | Interacciones visuales y consumo de las APIs de sesión/packs |
| `resources/application.properties` | Conexión, puerto y límites |

## 2. Entidades

```java
@Entity
@Table(name = "producto")
public class Producto implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

`@Entity` registra la clase para JPA; `@Table` indica su tabla. `@Id` marca la clave y `IDENTITY` utiliza AUTO_INCREMENT. Long corresponde a BIGINT. Serializable conserva la convención de clase; no guarda datos por sí mismo.

Lombok `@Getter`, `@Setter`, `@NoArgsConstructor` genera accesores y constructor vacío. `@ManyToOne` relaciona varios perfumes con una marca mediante `marca_id`. Otro `@ManyToOne` relaciona la portada con `imagen_id`.

```java
@OneToMany(mappedBy = "producto", cascade = CascadeType.ALL)
@OrderBy("ml ASC")
private List<Presentacion> presentaciones = new ArrayList<>();
```

Cada perfume tiene tres Presentacion: 3, 5 y 10 ml. `mappedBy` apunta al campo producto del hijo; cascade permite guardar hijos con el padre. El orden ascendente garantiza que el índice 0 del formulario sea 3 ml, el 1 sea 5 y el 2 sea 10.

Presentacion usa BigDecimal para precios y enteros para stock. No hay un stock general que se comparta sin distinguir tamaños. `@Version` en Producto detecta ediciones antiguas; activo permite retirar sin borrar relaciones históricas.

## 3. Repositorios

```java
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @EntityGraph(attributePaths = {"marca", "presentaciones"})
    List<Producto> findAllByOrderByIdDesc();
}
```

Spring implementa esa interfaz y aporta save/findById. El nombre del método expresa la consulta. EntityGraph carga las relaciones necesarias porque `open-in-view=false`: no se deja la conexión disponible para consultas arbitrarias desde la vista.

MarcaRepository ordena marcas por nombre. ImagenRepository almacena fotos. ProductoImagenRepository consulta las referencias y orden de la galería. La consulta parametrizada `contarImagenPublica` verifica si una imagen pertenece a un producto activo antes de entregarla a un visitante.

## 4. Servicio y transacciones

ProductoService declara las operaciones; ProductoServiceImpl las implementa. El controlador conoce el contrato, como en LPII. La lógica no se coloca dentro del HTML ni en el repositorio.

| Método | Operación |
| --- | --- |
| `listarTodosProductos` / `buscarProductoById` | Leer para listado y edición |
| `nuevoProducto` | Preparar marca vacía y tres presentaciones |
| `guardarProducto` | Crear producto, portada, variantes y galería |
| `actualizarProducto` | Editar campos permitidos y conservar IDs |
| `eliminarProductoById` | Retirar mediante activo=false |
| `restaurarProductoById` | Volver a publicar |
| `agotarProductoById` | Stock de todos los tamaños en cero |
| `listarGaleria` | Consultar fotos adicionales en orden |

Fragmento del alta:

```java
@Transactional
public Producto guardarProducto(Producto formulario,
        MultipartFile foto, List<MultipartFile> galeria) {
    validar(formulario);
    Producto nuevo = nuevoProducto();
    copiarCampos(nuevo, formulario);
    nuevo.setImagen(imagenService.guardarImagen(foto));
    Producto guardado = productoRepository.save(nuevo);
    guardarGaleria(guardado, galeria, List.of());
    return guardado;
}
```

Validar comprueba los datos. NuevoProducto prepara la estructura 3/5/10. CopiarCampos solo copia datos editables, no un ID o estado manipulado desde el navegador. GuardarImagen valida y persiste los bytes. Save guarda producto y presentaciones; guardarGaleria relaciona fotos extra.

`@Transactional` significa que estas operaciones se confirman juntas. Si una foto adicional falla, se revierte también la creación del producto y sus otras filas. Evita productos incompletos.

Se validan nombre obligatorio hasta 100 caracteres, marca existente, Día/Noche, descripción hasta 400, descuento 0–70, precio mayor que cero y hasta S/10000 con dos decimales, stock entero 0–9999 y las tres presentaciones completas. Son reglas del servidor; los atributos HTML required/min/max solo ayudan en pantalla.

Editar busca el producto existente, compara versión y modifica sus campos. No elimina y recrea sus presentaciones, por lo que no rompe packs. `OPTIMISTIC_FORCE_INCREMENT` incrementa la versión incluso cuando solo cambió stock o galería. Un formulario antiguo debe reabrirse antes de guardar.

Retirar y agotar son diferentes: retirado deja de publicarse; agotado sigue visible al final del catálogo. Restaurar no inventa stock. La galería permite quitar solo imágenes que pertenecen al perfume y hasta seis fotos adicionales.

## 5. Controlador y formulario

ProductoController es `@Controller`: devuelve nombres de vistas, no JSON. `producto/create` representa `resources/templates/producto/create.html`; `redirect:/producto` pide al navegador abrir el listado después del POST.

| Ruta | Acción |
| --- | --- |
| GET `/producto` | Listar y buscar por `q` |
| GET `/producto/new` | Preparar alta |
| POST `/producto` | Guardar alta |
| GET `/producto/edit/{id}` | Preparar edición |
| POST `/producto/{id}` | Guardar edición |
| POST `/producto/delete/{id}` | Retirar |
| POST `/producto/restore/{id}` | Restaurar |
| POST `/producto/agotado/{id}` | Agotar |
| GET `/imagenes/{id}` | Entregar foto según publicación/permisos |

Se usa POST para cambiar datos; visitar un enlace GET no retira un perfume. Spring Security exige ADMIN en el mantenimiento y CSRF en las escrituras.

`@ModelAttribute("producto")` reúne los campos en Producto. BindingResult, inmediatamente después, contiene errores al convertir valores como precios. MultipartFile representa un archivo subido. Model transporta datos a Thymeleaf. RedirectAttributes lleva el aviso de éxito a la página siguiente.

`@InitBinder` limita las propiedades recibidas: nombre, marca, ocasión, descripción, descuento, versión y precios/stock. Una petición no puede asignar directamente activo, IDs de presentaciones o cualquier imagen de la base.

Ejemplo simplificado del HTML:

```html
<form th:object="${producto}" th:action="@{/producto}"
      method="post" enctype="multipart/form-data">
  <input th:field="*{nombre}" required>
  <input type="file" name="foto" accept="image/png,image/jpeg,image/webp">
  <button type="submit">Guardar producto</button>
</form>
```

El formulario real comparte un fragmento entre create y edit. th:object elige el objeto, th:field enlaza propiedades, th:action decide destino y multipart permite archivos. Thymeleaf incorpora el token CSRF. En errores se conservan los campos; los archivos deben elegirse otra vez por las restricciones del navegador.

**En Eclipse:** abre ProductoController con Ctrl+Shift+T, sigue guardarProducto con F3 y busca la implementación de la interfaz con Ctrl+T. Si Lombok aparece en rojo, revisa su instalación en el IDE y Maven → Update Project; la dependencia ya está en pom.xml.

## 6. Catálogo e imágenes

CatalogoServiceImpl lee los productos activos, variantes y referencias de fotos. DatosTiendaController añade esos datos al Model. El layout los serializa con `th:inline="javascript"` en `window.DECANLY_DATOS`. JavaScript dibuja tarjetas y filtros; packs se consulta además en `/api/packs`. No se necesita una API REST de productos para este enfoque MVC.

Imagen guarda nombre, MIME, contenido LONGBLOB y SHA-256. Producto guarda la portada en imagen_id; producto_imagen relaciona las adicionales. `GET /imagenes/{id}` sirve los bytes de MySQL. Los visitantes solo reciben imágenes vinculadas a productos activos; el administrador puede ver las retiradas para editarlas.

Se admiten PNG/JPG/WebP de 2 MB por archivo y hasta 16 MB por petición. PNG/JPG se reconocen y decodifican, con máximo 8000 píxeles por lado y 16 megapíxeles. WebP se reconoce por firma RIFF/WEBP; una validación por decodificación completa requeriría un lector adicional. Los SVG iniciales se importan desde SQL; el formulario no acepta SVG nuevos.

Quitar una imagen elimina su relación, no necesariamente la fila binaria antigua. Antes de limpiar imágenes sin uso hay que revisar todas sus referencias. La portada actual se conserva si al editar no se envía otra.

## 7. WhatsApp: ubicación exacta

No se utilizó WhatsApp Business Cloud API. Es Click to Chat: un enlace wa.me con el texto codificado que abre WhatsApp para que la persona decida enviarlo.

- `application.properties`: `decanly.seller-phone=51936240578`, contacto del proyecto recibido.
- `DatosTiendaController`: expone el teléfono a la página.
- `static/js/pedido.js`: campos y formato del mensaje.
- `static/js/whatsapp.js`: construye el enlace usando encodeURIComponent.
- `static/js/frontend.js`: responde al formulario y abre el enlace.

El mensaje incluye cliente, WhatsApp, dirección, perfumes/tamaños/cantidades, total y envío. Dice «Por confirmar con el vendedor» como referencia. No hay un pedido registrado en MySQL todavía, ni pago confirmado ni reserva de stock. Cuando implementen pedidos, Java debe releer precios/stock, calcular total y generar una referencia real.

Se conserva el correo decanly@gmail.com. Si cambias el teléfono del carrito, revisa también los enlaces visibles de pie, contacto y políticas.
