# Arquitectura de TaskFlow

Este documento explica la estructura para un desarrollador nuevo: capas y paquetes, el recorrido de `POST /projects/{projectId}/tasks`, dónde viven las reglas de negocio, cómo funciona la seguridad JWT y cómo están organizados los tests.

## Capas y paquetes

- Capa controller (puerta HTTP): contiene los controladores REST que traducen HTTP ↔ DTOs.
  - Ejemplo: `src/main/java/com/taskflow/controller/TaskController.java` (`TaskController`).
  - Otros: `ProjectController`.

- Capa service (casos de uso / orquestación): implementa la lógica de aplicación, orquesta repositorios y mappers.
  - Ejemplo: `src/main/java/com/taskflow/service/TaskService.java` (`TaskService`).
  - `ProjectService` valida existencia de proyecto cuando hace falta.

- Capa repository (persistencia): interfaces Spring Data JPA que exponen `save`, `findById`, `deleteById`, etc.
  - Rutas: `src/main/java/com/taskflow/repository/*`.

- Capa model (dominio/entidades): entidades JPA con comportamiento y reglas de dominio.
  - Ejemplo: `src/main/java/com/taskflow/model/Task.java` (`Task`).
  - `TaskStatus`, `Priority`, `Project` también viven aquí.

- Mapper / dto: mappers manuales que convierten entre DTOs y entidades.
  - Ejemplo: `src/main/java/com/taskflow/mapper/TaskMapper.java` (`TaskMapper`).

- Advice / excepciones: manejo centralizado de errores.
  - Ejemplo: `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` (`GlobalExceptionHandler`).

- Config: configuración de seguridad, datos de ejemplo.
  - Ejemplo: `src/main/java/com/taskflow/config/SecurityConfig.java` (`SecurityConfig`), `src/main/java/com/taskflow/config/DataSeeder.java` (`DataSeeder`).

## Recorrido completo de POST /projects/{projectId}/tasks

1. El cliente hace `POST /projects/{projectId}/tasks` con un `TaskRequest` JSON.
   - Controlador: `src/main/java/com/taskflow/controller/TaskController.java` (`TaskController`) expone `@PostMapping("/projects/{projectId}/tasks")`.
2. Validación automática de Bean Validation: `@Valid` en el `@RequestBody` → 400 si falla.
3. El `TaskController` primero verifica que el proyecto existe usando `ProjectService.buscarPorId(projectId)`.
   - Si no existe, lanza `ProjectNotFoundException` → `GlobalExceptionHandler` mapea a 404.
4. `TaskController` llama a `TaskService.crear(request, projectId)`.
   - Archivo: `src/main/java/com/taskflow/service/TaskService.java` (`TaskService`).
5. `TaskService` usa `TaskMapper` para convertir el `TaskRequest` en entidad.
   - `TaskMapper.aEntidadNueva(request, projectId)` delega a la factory de dominio.
6. La fábrica/dominio: `Task.crear(...)` en `src/main/java/com/taskflow/model/Task.java` valida reglas (por ejemplo: `dueDate` no puede estar en el pasado) y crea la entidad con `status = TODO`.
   - Si la validación falla lanza `TaskValidationException` (checked) → sube y es mapeada por `GlobalExceptionHandler` a 400.
7. `TaskService` guarda la entidad con `TaskRepository.save(nueva)` (Spring Data JPA).
   - Repositorio asigna `id` y la entidad persiste.
8. `TaskController` crea `Location` apuntando a `/tasks/{id}` y responde `201 Created` con `TaskResponse` (usando `TaskMapper.aResponse`).

Puntos clave: el controller orquesta validaciones de existencia y HTTP, el service orquesta la creación, pero las reglas de validación de negocio concretas (por ejemplo, no crear con fecha pasada, no poner DONE sin assignee) residen en la entidad `Task`.

## Dónde viven las reglas de negocio

- Reglas invariantes y de estado pertenecen al dominio (entidad): `src/main/java/com/taskflow/model/Task.java`.
  - Ejemplos: `Task.crear(...)` (no dueDate en pasado), `Task.setStatus(...)` (no pasar a `DONE` sin `assigneeId`), `Task.estaVencida()`.
- `TaskService` orquesta llamadas y traduce excepciones de dominio a excepciones de aplicación (`TaskStateException`) para mapear a códigos HTTP adecuados (422 para reglas de estado). No dupliques reglas en el servicio.
- Validaciones de contrato API (DTO) usan Bean Validation en los `record` DTOs; esas producen 400 y se manejan por el advice global.

## Seguridad (JWT)

- Configuración en `src/main/java/com/taskflow/config/SecurityConfig.java` (`SecurityConfig`).
- Esquema general:
  - Endpoints públicos: `/auth/**`, `/info`, Swagger UI, consola H2 y recursos estáticos (`src/main/resources/static`).
  - Resto requiere token JWT en la cabecera `Authorization: Bearer <token>`.
  - Filtros de seguridad validan el JWT, extraen `username` y `roles` y construyen `Authentication` en el `SecurityContext`.
  - Autorización basada en roles y `@PreAuthorize` donde se necesita (por ejemplo, borrar proyecto solo si es dueño o `ADMIN`).
- Token lifecycle: el servidor emite JWT en `/auth/login`; cada petición posterior debe incluirlo. `SecurityConfig` define los endpoints expuestos y el filtro que valida firmas y expiración.

## Organización de tests

- Tests por tipo y carpeta bajo `src/test/java/com/taskflow/`:
  - `unit`: pruebas unitarias sin Spring (JUnit5 + Mockito).
    - Ejemplo: `src/test/java/com/taskflow/unit/TaskServiceTest.java` (`TaskServiceTest`).
  - `slice`: pruebas de capa (por ejemplo `@WebMvcTest` para controladores, `@DataJpaTest` para repositorios) en `slice/`.
    - Ejemplo: `src/test/java/com/taskflow/slice/TaskControllerTest.java` (`TaskControllerTest`).
  - `integration` (si existen): `@SpringBootTest` con el perfil `test` y, cuando corresponde, Testcontainers para pruebas que requieren DB real. Nota: las `*IT.java` usan Testcontainers y no corren por defecto en `mvn test` (se activan con `-Ddocker.tests=true`).

- Convenciones:
  - Nombres: sufijos `Test` (unit/slice) y `IT` para integración con contenedores.
  - No modificar tests existentes; arreglar el código si fallan.
  - Comandos útiles (entorno Windows/PowerShell):
    - `mvn -q test` — ejecutar la suite normal.
    - `mvn -q test "-Dtest=TaskServiceTest"` — ejecutar una clase específica.
    - `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"` — arrancar la app con H2 y datos de `DataSeeder`.

## Resumen rápido de responsabilidades

- `TaskController`: interfaz HTTP, validaciones de contrato y existencias básicas.
- `TaskService`: orquesta casos de uso, usa `TaskMapper` y `TaskRepository`.
- `Task` (modelo): implementa reglas de dominio y comportamiento (aquí vive la lógica que garantiza invariantes).
- `TaskMapper`: convierte DTOs ↔ entidades (la API nunca expone entidades directamente).
- `GlobalExceptionHandler`: mapea excepciones a respuestas HTTP uniformes.
- `SecurityConfig`: define protección JWT, endpoints públicos y reglas de acceso.

Si quieres, genero un diagrama simple o un fragmento con el flujo de llamadas (controller → service → mapper → dominio → repository) para añadir al mismo archivo.
Las fechas límite se validan en la fábrica/dominio `Task.crear(...)` en `src/main/java/com/taskflow/model/Task.java`.
