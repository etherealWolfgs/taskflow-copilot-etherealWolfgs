package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectResumenServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService service;

    private final Project proyecto = new Project(1L, "Plataforma", "d", 1L, null);

    @Test
    void resumen_conTareasEnTodosLosEstados_yUnaVencida() throws TaskValidationException {
        Task t1 = new Task(1L, "T01", "d", TaskStatus.TODO, Priority.MED, 1L, null, null);
        Task t2 = new Task(2L, "T02", "d", TaskStatus.IN_PROGRESS, Priority.MED, 1L, 2L, LocalDate.now().plusDays(1));
        Task t3 = new Task(3L, "T03", "d", TaskStatus.IN_PROGRESS, Priority.MED, 1L, 2L, LocalDate.now().minusDays(1)); // vencida
        Task t4 = new Task(4L, "T04", "d", TaskStatus.DONE, Priority.MED, 1L, 2L, LocalDate.now());

        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(t1, t2, t3, t4));

        ProjectSummaryResponse esperado = new ProjectSummaryResponse(1L, "Plataforma", 4,
                Map.of("TODO", 1L, "IN_PROGRESS", 2L, "DONE", 1L), 1L);

        assertEquals(esperado, service.resumen(proyecto));
    }

    @Test
    void resumen_proyectoSinTareas_todoEnCero() {
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        ProjectSummaryResponse esperado = new ProjectSummaryResponse(1L, "Plataforma", 0,
                Map.of("TODO", 0L, "IN_PROGRESS", 0L, "DONE", 0L), 0L);

        assertEquals(esperado, service.resumen(proyecto));
    }
}