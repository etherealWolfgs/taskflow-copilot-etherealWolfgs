package com.taskflow.dto;

/**
 * ProjectProgressResponse — DTO de salida para el reporte de progreso por proyecto.
 * Contiene identificador, nombre, conteos y el porcentaje completado redondeado a un decimal.
 */
public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks,
                                      long doneTasks, double percentDone) {
}
