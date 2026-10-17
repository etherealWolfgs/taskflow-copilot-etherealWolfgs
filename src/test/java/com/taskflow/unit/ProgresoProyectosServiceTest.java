package com.taskflow.unit;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.Priority;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import com.taskflow.exception.TaskValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgresoProyectosServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void progreso_por_varios_proyectos_redondeo_y_orden() throws Exception {
        // proyectos devueltos en orden 3,1,2 (para probar el resort en el service)
        Project p1 = new Project(1L, "P1", "d", 1L, null);
        Project p2 = new Project(2L, "P2", "d", 1L, null);
        Project p3 = new Project(3L, "P3", "d", 1L, null);

        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));

        // p1: 5 tareas, 1 DONE -> 20.0
        List<Task> tareasP1 = List.of(
                new Task(10L, "Tarea1", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, LocalDate.now()),
                new Task(11L, "Tarea2", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null),
                new Task(12L, "Tarea3", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null),
                new Task(13L, "Tarea4", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null),
                new Task(14L, "Tarea5", "d", TaskStatus.TODO, Priority.MED, 1L, 1L, null)
        );
        when(taskRepository.findByProjectId(1L)).thenReturn(tareasP1);

        // p2: 3 tareas, 1 DONE -> 33.3 (redondeo)
        List<Task> tareasP2 = List.of(
                new Task(20L, "TareaA", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, null),
                new Task(21L, "TareaB", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null),
                new Task(22L, "TareaC", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null)
        );
        when(taskRepository.findByProjectId(2L)).thenReturn(tareasP2);

        // p3: sin tareas -> 0.0
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        java.util.List<ProjectProgressResponse> res = projectService.progresoPorProyecto();
        // debe venir ordenado 1,2,3
        assertEquals(3, res.size());
        assertEquals(1L, res.get(0).projectId());
        assertEquals(20.0, res.get(0).percentDone());
        assertEquals(2L, res.get(1).projectId());
        assertEquals(33.3, res.get(1).percentDone());
        assertEquals(3L, res.get(2).projectId());
        assertEquals(0.0, res.get(2).percentDone());
    }
}
