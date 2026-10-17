# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `038b71a6-71cc-4396-bdb2-ef2a8315dbcd`  
> - **Started:** 17/10/2026, 1:28:14 p.m.  
> - **Duration:** 43s  
> - **Exported:** 17/10/2026, 1:28:58 p.m.  

---

<sub>2s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/progress.md.

---

<sub>7s</sub>

### Copilot

Leeré en paralelo el diff solicitado y la especificación para comparar cambios con el contrato. Esto permite identificar archivos afectados y luego abrir los archivos modificados si hace falta. Ejecutando dos lecturas en paralelo.

---

<sub>7s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\semana6\proyecto-final.diff.**

C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\semana6\proyecto-final.diff

<details>
<summary>249 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ReportController.java b/src/main/java/com/taskflow/controller/ReportController.java
new file mode 100644
index 0000000..7d6b457
--- /dev/null
+++ b/src/main/java/com/taskflow/controller/ReportController.java
@@ -0,0 +1,31 @@
+package com.taskflow.controller;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.service.ProjectService;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.tags.Tag;
+import org.springframework.web.bind.annotation.GetMapping;
+import org.springframework.web.bind.annotation.RestController;
+
+import java.util.List;
+
+/**
+ * ReportController — endpoints de reportes agregados a la API.
+ */
+@RestController
+@Tag(name = "Reports", description = "Reportes agregados: progreso por proyecto y similares.")
+public class ReportController {
+
+    private final ProjectService projectService;
+
+    public ReportController(ProjectService projectService) {
+        this.projectService = projectService;
+    }
+
+    @Operation(summary = "Progreso por proyecto",
+            description = "Devuelve para cada proyecto el total de tareas, cuántas están DONE y el porcentaje completado (un decimal).")
+    @GetMapping("/reports/progress")
+    public List<ProjectProgressResponse> getProgress() {
+        return projectService.progresoPorProyecto();
+    }
+}
diff --git a/src/main/java/com/taskflow/dto/ProjectProgressResponse.java b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
new file mode 100644
index 0000000..57ea540
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
@@ -0,0 +1,9 @@
+package com.taskflow.dto;
+
+/**
+ * ProjectProgressResponse — DTO de salida para el reporte de progreso por proyecto.
+ * Contiene identificador, nombre, conteos y el porcentaje completado redondeado a un decimal.
+ */
+public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks,
+                                      long doneTasks, double percentDone) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index b66705c..60aae78 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -29,4 +29,15 @@ public final class ProjectMapper {
                                                    long overdue) {
         return new ProjectSummaryResponse(projectId, projectName, totalTasks, byStatus, overdue);
     }
+
+    /**
+     * Mapea al DTO de progreso de proyecto usado por GET /reports/progress. El servicio calcula
+     * los conteos y el porcentaje y lo pasa aquí.
+     */
+    public static com.taskflow.dto.ProjectProgressResponse aProgreso(Project proyecto, long totalTasks,
+                                                                     long doneTasks, double percentDone) {
+        return new com.taskflow.dto.ProjectProgressResponse(proyecto.getId(), proyecto.getName(),
+                totalTasks, doneTasks, percentDone);
+    }
 }
+
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index b159eb8..8aa5e2b 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -122,4 +122,29 @@ public class ProjectService {
         long overdue = tareas.stream().filter(Task::estaVencida).count();
         return ProjectMapper.aResumen(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
     }
+
+    /**
+     * Calcula el progreso por proyecto: todos los proyectos, conteos de tareas y porcentaje redondeado
+     * a un decimal. Reglas de negocio: incluye proyectos sin tareas; porcentaje 0.0 cuando totalTasks==0.
+     */
+    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
+        java.util.List<Project> proyectos = new java.util.ArrayList<>(projectRepository.findAll());
+        // Ordenar por id asc (regla 4)
+        proyectos.sort(java.util.Comparator.comparing(Project::getId));
+        java.util.List<com.taskflow.dto.ProjectProgressResponse> salida = new java.util.ArrayList<>();
+        for (Project p : proyectos) {
+            java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
+            long total = tareas.size();
+            long done = tareas.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
+            double percent;
+            if (total == 0) {
+                percent = 0.0;
+            } else {
+                percent = (double) Math.round((done * 1000.0 / total)) / 10.0; // redondeo a 1 decimal
+            }
+            salida.add(ProjectMapper.aProgreso(p, total, done, percent));
+        }
+        return salida;
+    }
 }
+
diff --git a/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
new file mode 100644
index 0000000..4503142
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
@@ -0,0 +1,50 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ReportController;
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.http.MediaType;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.List;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(ReportController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class ProgresoProyectosControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getProgress_retorna200YJsonConCampos() throws Exception {
+        when(projectService.progresoPorProyecto()).thenReturn(List.of(
+                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5L, 1L, 20.0),
+                new ProjectProgressResponse(2L, "App Móvil", 4L, 1L, 25.0)
+        ));
+
+        mockMvc.perform(get("/reports/progress").contentType(MediaType.APPLICATION_JSON))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.length()").value(2))
+                .andExpect(jsonPath("$[0].projectId").value(1))
+                .andExpect(jsonPath("$[0].projectName").value("Plataforma TaskFlow"))
+                .andExpect(jsonPath("$[0].percentDone").value(20.0))
+                .andExpect(jsonPath("$[1].totalTasks").value(4));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
new file mode 100644
index 0000000..5c0766b
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
@@ -0,0 +1,81 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.model.Priority;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import com.taskflow.exception.TaskValidationException;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Optional;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+class ProgresoProyectosServiceTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService projectService;
+
+    @Test
+    void progreso_por_varios_proyectos_redondeo_y_orden() throws Exception {
+        // proyectos devueltos en orden 3,1,2 (para probar el resort en el service)
+        Project p1 = new Project(1L, "P1", "d", 1L, null);
+        Project p2 = new Project(2L, "P2", "d", 1L, null);
+        Project p3 = new Project(3L, "P3", "d", 1L, null);
+
+        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));
+
+        // p1: 5 tareas, 1 DONE -> 20.0
+        List<Task> tareasP1 = List.of(
+                new Task(10L, "Tarea1", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, LocalDate.now()),
+                new Task(11L, "Tarea2", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null),
+                new Task(12L, "Tarea3", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null),
+                new Task(13L, "Tarea4", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null),
+                new Task(14L, "Tarea5", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null)
+        );
+        when(taskRepository.findByProjectId(1L)).thenReturn(tareasP1);
+
+        // p2: 3 tareas, 1 DONE -> 33.3 (redondeo)
+        List<Task> tareasP2 = List.of(
+                new Task(20L, "TareaA", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, null),
+                new Task(21L, "TareaB", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null),
+                new Task(22L, "TareaC", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null)
+        );
+        when(taskRepository.findByProjectId(2L)).thenReturn(tareasP2);
+
+        // p3: sin tareas -> 0.0
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        java.util.List<ProjectProgressResponse> res = projectService.progresoPorProyecto();
+        // debe venir ordenado 1,2,3
+        assertEquals(3, res.size());
+        assertEquals(1L, res.get(0).projectId());
+        assertEquals(20.0, res.get(0).percentDone());
+        assertEquals(2L, res.get(1).projectId());
+        assertEquals(33.3, res.get(1).percentDone());
+        assertEquals(3L, res.get(2).projectId());
+        assertEquals(0.0, res.get(2).percentDone());
+    }
+}
```

</details>


---

<sub>7s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\specs\progress.md.**

C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\specs\progress.md

<details>
<summary>77 lines</summary>

````
# Especificación — `GET /reports/progress`

## Qué

Un reporte con el avance de **cada** proyecto: cuántas tareas tiene, cuántas están terminadas y qué
porcentaje representan. Sirve para una pantalla de «cómo vamos» sin pedir las tareas proyecto por
proyecto.

```
GET /reports/progress
Authorization: ******
```

## Respuesta 200

```json
[
  { "projectId": 1, "projectName": "Plataforma TaskFlow", "totalTasks": 5, "doneTasks": 1, "percentDone": 20.0 },
  { "projectId": 2, "projectName": "App Móvil",           "totalTasks": 4, "doneTasks": 1, "percentDone": 25.0 },
  { "projectId": 3, "projectName": "Migración Legacy",    "totalTasks": 0, "doneTasks": 0, "percentDone": 0.0 }
]
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `doneTasks` | número | cuántas de esas están en `DONE` |
| `percentDone` | número decimal | `doneTasks` entre `totalTasks`, por 100, redondeado a **un decimal** |

## Reglas

1. Aparecen **todos** los proyectos, también los que no tienen tareas.
2. Proyecto sin tareas: `totalTasks` 0, `doneTasks` 0 y `percentDone` **`0.0`**. Nunca `NaN` ni un
   error por dividir entre cero.
3. El porcentaje se calcula en decimal **antes** de dividir (`doneTasks * 100.0 / totalTasks`) y se
   redondea a un decimal: 1 de 3 → `33.3`; 2 de 3 → `66.7`; 1 de 8 → `12.5`. Es la trampa `long/long`
   que ya documenta `ReportService.porcentajeCompletadas()`: con división entera, 1 de 4 da `0`.
4. Orden: por `projectId` ascendente, aunque el repositorio los devuelva en otro orden.
5. Seguridad: cualquier usuario autenticado puede pedirlo (sin token → `401`). No cambies
   `SecurityConfig`.

## Dónde

- DTO de salida nuevo en `com.taskflow.dto`:
  `public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone)`,
  con su Javadoc.
- Mapper: método nuevo `public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone)`
  en `ProjectMapper`.
- Método nuevo en `ProjectService`: `public List<ProjectProgressResponse> progresoPorProyecto()`.
  `ProjectService` ya tiene los dos repositorios que hacen falta (`projectRepository` y
  `taskRepository`): usa `projectRepository.findAll()` y `taskRepository.findByProjectId(Long)`.
  **No agregues métodos a los repositorios.** La cuenta y el redondeo van aquí, no en el controller.
- Controller **nuevo** `ReportController` en `com.taskflow.controller`, con
  `@Tag(name = "Reports", description = …)`, `@GetMapping("/reports/progress")` y `@Operation` en
  español. Recibe `ProjectService` por constructor. Es un controller nuevo, así que ningún slice test
  existente lo conoce y no rompe nada.

## Tests que deben existir al terminar

Los dos en **clases nuevas**:

- **Unit** `src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java`, con `@Mock` de
  `ProjectRepository`, `TaskRepository` y `UserRepository` (los tres del constructor de
  `ProjectService`) e `@InjectMocks ProjectService`. Un caso con tres proyectos que el mock devuelve
  en orden `3, 1, 2`:
  - el 1 con 5 tareas y 1 `DONE` → `20.0`;
  - el 2 con 3 tareas y 1 `DONE` → `33.3` (prueba el redondeo);
  - el 3 sin tareas → `0.0`;
  - y la lista sale en orden `1, 2, 3`.
- **Slice** `src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java`, con
  `@WebMvcTest(ReportController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `ProjectService` y `JwtAuthenticationFilter`. Caso: `200` y cada campo del JSON de los dos
  primeros elementos que devuelve el mock (`$[0].projectId`, `$[0].percentDone`, `$[1].totalTasks`…).

## Resultado esperado con la semilla

Con la app recién arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /reports/progress` | `200` con los tres objetos del ejemplo de arriba, en ese orden |
| `PATCH /tasks/1/status` con `{"status": "DONE"}` y luego `GET /reports/progress` | el proyecto 1 pasa a `doneTasks` 2 y `percentDone` `40.0` (la tarea 1 tiene responsable, así que el PATCH responde 200) |
| `GET /reports/progress` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `ProjectProgressResponse.java` (nuevo), `ProjectMapper.java`,
  `ProjectService.java`, `ReportController.java` (nuevo) y las dos clases de test nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-progress.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>18s</sub>

### Copilot

Abrir archivos relevantes para contexto: Project.java, Task.java, TaskStatus.java, ProjectService.java y ProjectMapper.java actuales. Esto permite verificar constructores, métodos y firmas usados por el diff. Ejecutando varias lecturas en paralelo.

---

<sub>18s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\model\Project.java.**

C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\model\Project.java

<details>
<summary>66 lines</summary>

```
package com.taskflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Project — un proyecto dueño de tareas (entidad JPA).
 *
 * El dueño se guarda como 'Long ownerId', no como un objeto User: forma (id, name, description,
 * ownerId, createdAt). "El repositorio/BD guarda ids": la referencia al dueño, en la BD, es una
 * columna Long.
 *
 * Como Task: @Entity + @Table(name = "projects"), @Id @GeneratedValue(IDENTITY), constructor no-arg
 * protegido para JPA. El id es 'Long' (envoltorio), no 'long' primitivo: un id nuevo es null hasta
 * que la BD lo asigna (sin valores mágicos como 0L para "aún sin id").
 *
 * ownerId NO lleva @ManyToOne (juicio, como assigneeId en Task): no navegamos al User dueño al
 * listar proyectos; con el id basta. Se mapea objeto cuando se NAVEGA, no cuando solo se REFERENCIA.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private Long ownerId;

    private LocalDate createdAt;

    /** Constructor no-arg protegido: EXCLUSIVO de JPA (rehidratación por reflexión). */
    protected Project() {
        // solo para JPA
    }

    public Project(Long id, String name, String description, Long ownerId, LocalDate createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + '\''
                + ", ownerId=" + ownerId
                + ", createdAt=" + createdAt + '}';
    }
}
```

</details>


---

<sub>19s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\model\Task.java.**

C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\model\Task.java

<details>
<summary>225 lines</summary>

```
package com.taskflow.model;

import com.taskflow.exception.TaskValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task — entidad central del dominio TaskFlow (persistida con JPA).
 *
 * Es una clase de dominio normal (8 campos, validaciones, comportamiento): NO hay un "modelo de
 * BD" aparte. Lo que la hace entidad, todo por anotación:
 *   - @Entity + @Table(name = "tasks"): esta clase mapea a la tabla TASKS.
 *   - @Id + @GeneratedValue(IDENTITY): el id lo asigna la BD (columna autoincremental). Nada de
 *     secuencia manual en Java: el id lo pone la BD y nadie más.
 *   - @Enumerated(STRING) en status y priority: se guardan como TEXTO ('TODO'), NUNCA como ORDINAL
 *     (0,1,2): reordenar o insertar un enum corrompería datos históricos. El orden LOW,MED,HIGH
 *     es contrato del SORT (compareTo), no del almacenamiento.
 *   - relación @ManyToOne Task->Project (patrón "asociación de solo lectura"): la COLUMNA la
 *     manda el escalar 'projectId' (por eso Task.crear, TaskService, mappers y DTOs hablan de projectId);
 *     el objeto 'project' solo NAVEGA. Frase ancla: "la columna manda; el objeto navega".
 *
 * Tensión JPA vs validaciones: JPA rehidrata
 * por reflexión con un constructor sin args protegido, SIN pasar por las validaciones. Está bien:
 * Task.crear(...) sigue siendo el ÚNICO camino de CREACIÓN de negocio; JPA solo REHIDRATA datos que
 * ya existían (la distinción crear/rehidratar).
 *
 * OJO: las columnas persistentes NO pueden ser 'final' (JPA las escribe por reflexión tras el no-arg);
 * por eso title/description/projectId/dueDate no son 'final'. La inmutabilidad
 * de negocio la sigue garantizando la ausencia de setters públicos para esos campos.
 */
@Entity
@Table(name = "tasks")
public class Task implements Comparable<Task> {

    // Reglas de longitud del título (regla de negocio).
    private static final int TITULO_MIN = 3;
    private static final int TITULO_MAX = 120;

    // id nace null; lo asigna la BD (IDENTITY). Ver setId (uso interno de JPA/repo).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    private String description;                 // por convención -> columna DESCRIPTION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;                  // texto 'TODO'/'IN_PROGRESS'/'DONE' (nunca 0/1/2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;                  // texto 'LOW'/'MED'/'HIGH'

    // La COLUMNA project_id la posee este escalar (insertable/updatable por default). Sigue siendo
    // el dueño: Task.crear, el mapper y el servicio hablan con projectId, no con el objeto.
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // assigneeId se queda Long a secas (juicio explícito): es nullable, NUNCA navegamos al User
    // completo al leer tareas, y las derived queries operan igual. No toda FK necesita ser objeto:
    // mapea objeto cuando NAVEGAS, deja el id cuando solo REFERENCIAS.
    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDate dueDate;                  // por convención -> columna DUE_DATE (camelCase->snake_case)

    /**
     * Asociación de SOLO LECTURA hacia el proyecto. Comparte la MISMA columna project_id con
     * el escalar de arriba, pero marcada insertable=false/updatable=false: no escribe la columna (el
     * escalar lo hace), solo permite NAVEGAR (task.getProject().getName()). LAZY: no se carga hasta
     * que se toca — y fuera de una sesión abierta, tocarla lanza LazyInitializationException (por eso
     * la API responde con DTOs, nunca con la entidad).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * Constructor NO-ARG protegido: EXCLUSIVO de JPA (rehidratación por reflexión). Nunca es un camino
     * de creación de negocio. Su existencia es obligatoria para cualquier @Entity: sin él, al arrancar
     * Hibernate lanza "No default constructor for entity: com.taskflow.model.Task".
     */
    protected Task() {
        // solo para JPA: rehidratación, nunca creación de negocio
    }

    /**
     * Constructor de REHIDRATACIÓN: reconstruye una tarea que YA existía (semilla del DataSeeder,
     * datos releídos). Valida las INVARIANTES que siempre deben cumplirse (título y proyecto), pero
     * NO la regla temporal de dueDate: una tarea vencida es un dato válido que hay que poder releer.
     */
    public Task(Long id, String title, String description, TaskStatus status,
                Priority priority, Long projectId, Long assigneeId, LocalDate dueDate)
            throws TaskValidationException {
        // Invariante 1: título obligatorio, 3-120 chars.
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("El título es obligatorio (no puede ir vacío).");
        }
        if (title.length() < TITULO_MIN || title.length() > TITULO_MAX) {
            throw new TaskValidationException(
                    "El título debe tener entre " + TITULO_MIN + " y " + TITULO_MAX
                            + " caracteres; recibí " + title.length() + ": \"" + title + "\".");
        }
        // Invariante 2: una tarea no puede existir sin proyecto (regla de negocio).
        if (projectId == null) {
            throw new TaskValidationException(
                    "Una tarea no puede existir sin proyecto (projectId == null).");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.projectId = projectId;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }

    /**
     * Factory de CREACIÓN de negocio: añade la regla temporal (dueDate no en el pasado) y delega en
     * el constructor. id nace null, status nace TODO. La usan el mapper y el servicio tal cual: que
     * Task sea @Entity no cambia esta firma.
     */
    public static Task crear(String title, String description, Priority priority,
                             LocalDate dueDate, Long projectId, Long assigneeId)
            throws TaskValidationException {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new TaskValidationException(
                    "La fecha límite no puede estar en el pasado: " + dueDate + ".");
        }
        return new Task(null, title, description, TaskStatus.TODO, priority, projectId, assigneeId, dueDate);
    }

    // ---- Comportamiento: vive CON los datos ----

    /** true si tiene fecha, ya pasó, y la tarea aún no está terminada. */
    public boolean estaVencida() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && status != TaskStatus.DONE;
    }

    // ---- Getters ----

    public Long getId() {
        return id;
    }

    /** Uso interno (JPA/repo) para el id autoincremental. Nadie más lo llama a mano. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Navega al proyecto asociado (asociación de solo lectura). Fuera de una sesión JPA abierta puede
     * lanzar LazyInitializationException (por eso la API expone DTOs, no la entidad). En el runtime
     * nadie del camino principal lo usa; existe para NAVEGAR cuando haga falta.
     */
    public Project getProject() {
        return project;
    }

    // ---- Setters SOLO donde el dominio muta ----

    /**
     * Cambia el estado aplicando la regla de negocio: no se puede pasar a DONE una tarea sin
     * responsable (assigneeId). La regla vive AQUÍ, no en el servicio ni en el controller.
     */
    public void setStatus(TaskStatus status) throws TaskValidationException {
        if (status == TaskStatus.DONE && assigneeId == null) {
            throw new TaskValidationException(
                    "No se puede marcar como " + TaskStatus.DONE
                            + " una tarea sin responsable: \"" + title + "\".");
        }
        this.status = status;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    // ---- Identidad de ENTIDAD por id (literalmente lo que hace JPA) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task otra = (Task) o;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ---- Orden natural por prioridad (Comparable) ----

    @Override
    public int compareTo(Task otra) {
        return this.priority.compareTo(otra.priority);
    }

    @Override
    public String toString() {
        return "Task{id=" + id
                + ", title='" + title + '\''
                + ", status=" + status
                + ", priority=" + priority
                + ", assigneeId=" + (assigneeId == null ? "sin asignar" : assigneeId)
                + ", dueDate=" + (dueDate == null ? "sin fecha" : dueDate)
                + (estaVencida() ? " *VENCIDA*" : "")
                + '}';
    }
}
```

</details>


---

<sub>23s</sub>

### Copilot

Opening ProjectMapper.java and ProjectService.java in repository to compare with diff changes.

---

<sub>23s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\mapper\ProjectMapper.java.**

C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\mapper\ProjectMapper.java

<details>
<summary>36 lines</summary>

```
package com.taskflow.mapper;

import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Project;

/**
 * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
 *
 * Mapeo trivial: la entidad guarda 'ownerId' directo (no un objeto User), así que aResponse no
 * deriva el id desde un objeto — lee p.getOwnerId() tal cual. Cuando el
 * dominio coincide con el contrato de salida (ProjectResponse con ownerId Long), el mapeo es una línea.
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // no instanciable
    }

    /** Entidad -> DTO de salida. ownerId sale directo del campo (sin puente por objeto). */
    public static ProjectResponse aResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwnerId(), p.getCreatedAt());
    }

    /** Conteo ya calculado por el service -> DTO de salida de GET /projects/{id}/summary. */
    public static ProjectSummaryResponse aResumen(Long projectId, String projectName,
                                                   long totalTasks, java.util.Map<String, Long> byStatus,
                                                   long overdue) {
        return new ProjectSummaryResponse(projectId, projectName, totalTasks, byStatus, overdue);
    }

    /**
     * Mapea al DTO de progreso de proyecto usado por GET /reports/progress. El servicio calcula
     * los conteos y el porcentaje y lo pasa aquí.
     */
    public static com.taskflow.dto.ProjectProgressResponse aProgreso(Project proyecto, long totalTasks,
                                                                     long doneTasks, double percentDone) {
        return new com.taskflow.dto.ProjectProgressResponse(proyecto.getId(), proyecto.getName(),
                totalTasks, doneTasks, percentDone);
    }
}
```

</details>


---

<sub>24s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\service\ProjectService.java.**

C:\Users\Lenovo\taskflow-copilot-etherealWolfgs\src\main\java\com\taskflow\service\ProjectService.java

<details>
<summary>135 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.mapper.ProjectMapper;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
 * orElseThrow -> ProjectNotFoundException; el advice le pone el 404).
 *
 * Dos decisiones que se ven aquí:
 *   - Project guarda un ownerId (Long), no un objeto User, así que 'crear' y 'reemplazar' manejan
 *     ese Long. 'crear' construye con id null y la BD asigna el id (IDENTITY).
 *   - 'tareasDe' usa la derived query taskRepository.findByProjectId(id) en vez de filtrar findAll()
 *     con un stream: el filtro lo hace la BD.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;   // para resolver el owner desde el username del JWT

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Todos los proyectos. */
    public List<Project> listar() {
        return projectRepository.findAll();
    }

    /** Un proyecto por id (el Optional sube tal cual: el controller decide 404 vs 200). */
    public Optional<Project> buscarPorId(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * Las tareas de un proyecto, con la derived query taskRepository.findByProjectId(projectId):
     * el WHERE project_id = ? lo hace
     * la BD (viajan solo las filas del proyecto). "no existe el proyecto" (404) lo decide el
     * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
     * [] (-> 200 con []). No son lo mismo.
     */
    public List<Task> tareasDe(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Crea un proyecto (POST): el request trae name y description; el ownerId NO es una constante
     * — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
     * Authentication). El dueño no lo decide el cliente ni una semilla: es QUIEN llama. createdAt = hoy;
     * id == null -> la BD lo asigna (IDENTITY).
     */
    public Project crear(ProjectRequest request, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new UsernameNotFoundException(ownerUsername));
        Project nuevo = new Project(null, request.name(), request.description(),
                owner.getId(), LocalDate.now());
        return projectRepository.save(nuevo);
    }

    /**
     * Reemplazo COMPLETO (PUT): existe -> reconstruye con el MISMO id, conservando ownerId y createdAt
     * (el cliente no los toca); name y description vienen del request. save con id presente = update.
     * No existe -> 404.
     */
    public Project reemplazar(Long id, ProjectRequest request) {
        Project actual = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        Project reemplazo = new Project(actual.getId(), request.name(), request.description(),
                actual.getOwnerId(), actual.getCreatedAt());
        return projectRepository.save(reemplazo);
    }

    /**
     * Elimina un proyecto (DELETE) con CASCADA MANUAL: borra primero sus tareas (regla "no Task sin
     * Project" — y además la FK task.project_id lo EXIGE: borrar el proyecto con tareas colgando daría
     * una violación de integridad) y luego el proyecto. No existe -> 404. La regla owner/ADMIN no
     * vive aquí: la aplica el @PreAuthorize del controller antes de llegar a este método.
     */
    public void eliminar(Long id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
        projectRepository.deleteById(id);
    }

    /**
     * Resumen de un proyecto: conteo por estado y vencidas.
     * Recibe el proyecto ya encontrado (el controller decide 404).
     */
    public ProjectSummaryResponse resumen(Project proyecto) {
        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
        long total = tareas.size();
        Map<String, Long> byStatus = tareas.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));
        // Asegurar las tres claves TODO, IN_PROGRESS, DONE
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.putIfAbsent(s.name(), 0L);
        }
        long overdue = tareas.stream().filter(Task::estaVencida).count();
        return ProjectMapper.aResumen(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
    }

    /**
     * Calcula el progreso por proyecto: todos los proyectos, conteos de tareas y porcentaje redondeado
     * a un decimal. Reglas de negocio: incluye proyectos sin tareas; porcentaje 0.0 cuando totalTasks==0.
     */
    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
        java.util.List<Project> proyectos = new java.util.ArrayList<>(projectRepository.findAll());
        // Ordenar por id asc (regla 4)
        proyectos.sort(java.util.Comparator.comparing(Project::getId));
        java.util.List<com.taskflow.dto.ProjectProgressResponse> salida = new java.util.ArrayList<>();
        for (Project p : proyectos) {
            java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
            long total = tareas.size();
            long done = tareas.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            double percent;
            if (total == 0) {
                percent = 0.0;
            } else {
                percent = (double) Math.round((done * 1000.0 / total)) / 10.0; // redondeo a 1 decimal
            }
            salida.add(ProjectMapper.aProgreso(p, total, done, percent));
        }
        return salida;
    }
}
```

</details>


---

<sub>43s</sub>

### Copilot

Resumen breve: la diff implementa exactamente lo solicitado (DTO record, mapper, service, controller y dos tests). No hay fallos que bloqueen merge; solo dos observaciones menores.

| # | Gravedad | Archivo:línea | Punto de la checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/mapper/ProjectMapper.java:+ | Convenciones (imports/mapeo) | El método aProgreso usa el tipo totalmente cualificado (com.taskflow.dto.ProjectProgressResponse) en vez de un import. | Importar ProjectProgressResponse para mejorar legibilidad. |
| 2 | Sugerencia | src/test/java/.../ProgresoProyectosControllerTest.java (nuevo) | Tests que prueban algo / Seguridad | No hay test que valide el comportamiento ante petición sin token (401) ni un test de integración que verifique los valores contra la semilla H2. | Añadir un slice/integration test que verifique 401 sin token y un IT/smoke que arranque la app con h2 y compruebe los valores seed (según "Resultado esperado con la semilla"). |

Casos sin test:
- GET /reports/progress sin token → 401 (no cubierto).
- Escenario end-to-end con la semilla H2 (valores y orden esperados) → no cubierto.
- Efecto de un PATCH que marque una tarea DONE y cambio en percentDone (mencionado en la spec) → no cubierto.

Veredicto: APROBADO

Si se desea, puedo crear los tests faltantes (401 y el IT con perfil h2) y proponer el pequeño import en ProjectMapper; indícalo y los añado.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
