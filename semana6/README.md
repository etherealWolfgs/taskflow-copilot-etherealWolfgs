# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** Genaro Salvador Morales Paoli · **Usuario de GitHub:** etherealWolfgs

## 1. Qué construí

| | Feature | Especificación |
|---|---|---|
| | `GET /reports/progress` | [`specs/progress.md`](../specs/progress.md) |

## 2. El pull request

- **URL del PR (mergeado):** https://github.com/etherealWolfgs/taskflow-copilot-etherealWolfgs/pull/5
- **Commit del merge en `main`:** `b93d59b (HEAD -> main, origin/main, origin/HEAD) Merge pull request #5 from etherealWolfgs/feature/progress`
- **Comentarios de Copilot code review:** 2

## 3. Cómo lo hice

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/progress` y copié la spec a `specs/` | `git log --oneline main..feature/progress` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` |
| Tests | `mvn test` en verde | 80 |
| Comprobación REST | `verificar.ps1` con `casos-progress.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | Faltaba un tiempo límite (`-TimeoutSec`) en la llamada PATCH — `casos-progress.ps1` | Copilot review | El agente agregó `-TimeoutSec 10` tras leer `code-review.md` (commit "fix: comentarios de Copilot code review") |
| 2 | Faltaban aserciones para verificar `doneTasks` en el segundo proyecto — `ProgresoProyectosControllerTest.java` | Copilot review | El agente agregó las aserciones faltantes en el test (commit "fix: comentarios de Copilot code review") |

**Lo que el agente hizo bien a la primera:** La implementación del controlador `ReportController` y la lógica del servicio pasaron las pruebas unitarias y de integración sin necesidad de cambios manuales (solo requirió ajustes menores de imports en sus propios tests).

## 5. Comprobaciones REST

```text
Repositorio: C:\Users\Lenovo\taskflow-copilot-etherealWolfgs
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 14052). Esperando a que /info responda...
App lista en 8 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    GET /reports/progress devuelve los 3 proyectos con la semilla
[OK]    El proyecto 1 se llama «Plataforma TaskFlow»
[OK]    Tras pasar la tarea 1 a DONE, el proyecto 1 queda en 2/5 = 40
[OK]    GET /reports/progress sin token responde 401
App detenida (PID 14052).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 12/12 OK