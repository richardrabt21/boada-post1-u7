# Post-contenido — Unidad 7: Patrones Arquitectónicos I

## Descripción
Repositorio del post-contenido de la Unidad 7 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`multas-biblioteca-api`) para la gestión de multas de biblioteca, con dos partes: una API REST en capas (Model, Repository, Service, Controller) sobre H2, y el pago en línea de multas con dos pasarelas intercambiables (Parte 2, en desarrollo).

## Parte 1 — Arquitectura en Capas
`MultaRepository` extiende `JpaRepository` y agrega una consulta agregada (`countByEstudianteIdAndEstado`). `MultaService` concentra las reglas de negocio que necesitan datos (límite de multas pendientes); el cálculo del monto vive en la propia entidad (`Multa.calcularMonto`). `MultaController` expone `/api/multas` y `GlobalExceptionHandler` traduce las excepciones de negocio a códigos HTTP.

### Estructura de paquetes
```
boada-post1-u7/
├── README.md
├── docs/capturas/                 (evidencia de los endpoints)
└── multas-biblioteca-api/
    ├── pom.xml
    └── src/main/java/com/example/multas/
        ├── controller/   Capa de Presentación: MultaController, GenerarMultaRequest, GlobalExceptionHandler
        ├── service/      Capa de Aplicación: MultaService
        ├── model/        Capa de Dominio: Multa, EstadoMulta y excepciones de negocio
        ├── repository/   Capa de Infraestructura: MultaRepository
        └── MultasBibliotecaApiApplication.java
```
El flujo de dependencias es siempre hacia abajo: `controller` → `service` → `repository` → `model`. El controlador nunca accede al repositorio directamente.

### Endpoints
| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/multas` | Lista todas las multas | 200 |
| GET | `/api/multas/{id}` | Busca una multa | 200, 404 |
| GET | `/api/multas/estudiante/{estudianteId}` | Multas de un estudiante | 200 |
| POST | `/api/multas` | Genera una multa | 201, 400, 409 |
| PATCH | `/api/multas/{id}/pagar` | Pago en ventanilla | 200, 404, 409 |

### Reglas de negocio
- Monto = días de atraso × 500, con un tope máximo de 15000.
- Un estudiante no puede tener más de 3 multas pendientes.
- Una multa ya pagada no puede pagarse de nuevo.

## Cómo ejecutar
```
cd multas-biblioteca-api
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```
La API queda en http://localhost:8080 y la consola H2 en http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:multas_biblioteca_db`, usuario `sa`, sin contraseña).

## Herramientas utilizadas
- Java 17, Spring Boot 4.0.8, Spring Data JPA, H2, Bean Validation
- Apache Maven, Postman, Git, GitHub

## Pruebas de los endpoints (Parte 1)
| # | Prueba | Resultado | Captura |
|---|---|---|---|
| 1 | Listar multas al inicio | 200, lista vacía | ![](docs/capturas/01-listar-vacio.png) |
| 2 | Crear multa válida | 201, monto 2500 | ![](docs/capturas/02-crear-201.png) |
| 3 | Crear multa sin `estudianteId` | 400, mensaje de validación | ![](docs/capturas/03-validacion-400.png) |
| 4 | Multa de 40 días (tope) | 200, monto 15000 | ![](docs/capturas/04-tope-monto.png) |
| 5 | Cuarta multa pendiente | 409, límite de 3 | ![](docs/capturas/05-limite-409.png) |
| 6 | Multa inexistente | 404 | ![](docs/capturas/06-no-encontrada-404.png) |
| 7 | Pago en ventanilla | 200, estado PAGADA | ![](docs/capturas/07-pagar-200.png) |
| 8 | Pagar una multa ya pagada | 409 | ![](docs/capturas/08-ya-pagada-409.png) |
| 9 | Multas por estudiante | 200, 3 multas | ![](docs/capturas/09-por-estudiante.png) |

## Decisiones de diseño

### Punto de decisión 1 — Cálculo del monto: ¿entidad o Service?
**Decisión:** el cálculo del monto vive en la entidad. `Multa.calcularMonto(int diasAtraso)` es un método estático que aplica 500 por día con un tope de 15000.

**Por qué:** la regla solo depende del dato que recibe (los días de atraso). No necesita el repositorio ni ningún otro bean, así que su lugar natural es el objeto de dominio. Si una regla no necesita colaboradores externos, se queda en la entidad, que así tiene comportamiento y no es un simple contenedor de datos. Por la misma razón, `marcarComoPagada` también vive en `Multa`: protege su propio estado, y la entidad no expone setters para `estado`, `fechaPago` ni `metodoPago`.

**Alternativa descartada:** calcular el monto en `MultaService`. Funcionaría, pero la entidad quedaría anémica (solo getters y setters) y la regla quedaría atada a Spring: para probarla habría que construir el Service con sus dependencias.

### Punto de decisión 2 — Conteo de multas pendientes: ¿consulta o filtrado en memoria?
**Decisión:** el conteo de multas pendientes se resuelve con una consulta derivada del repositorio, `countByEstudianteIdAndEstado`, y el Service solo compara el resultado con el límite de 3.

**Por qué:** la decisión de negocio (¿se permite otra multa?) sigue en el Service, pero el dato que la respalda se obtiene donde es más barato: el motor de base de datos cuenta directamente (`SELECT COUNT(*)`) sin transferir las filas a la aplicación.

**Alternativa descartada:** usar `findByEstudianteId` y filtrar con un stream en el Service. Eso carga en memoria todas las multas del estudiante, incluidas las ya pagadas, solo para contarlas, y el costo crece con el historial de cada estudiante.

## Parte 2 — Pago en Línea con Dos Pasarelas
[Pendiente: se documentará al implementarla.]