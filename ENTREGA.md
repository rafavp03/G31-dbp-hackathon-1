# ENTREGA — Tuckersoft Branch Engine

**Equipo G31** · Rafael Vargas (202510126) · Gabriel Murillo (202510160) · Indira Yabar (202520029)

---

## 1. Resumen de estrellas

Salida de `autotests` en la última corrida:

```
  ──────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
   ★★★★★   5 / 5   Cinco estrellas.
  ──────────────────────────────────────────────
   ✔  ★1  SEGURIDAD     65 comprobaciones
   ✔  ★2  NODOS         37 comprobaciones
   ✔  ★3  PARTIDAS      40 comprobaciones
   ✔  ★4  DECISIONES   101 comprobaciones
   ✔  ★5  ASINCRONIA    41 comprobaciones
  ──────────────────────────────────────────────
   Las cinco estrellas. Bandersnatch sale para Navidad.

   Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
```

Nuestros tests unitarios (`./mvnw test` desde la raíz):

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
  · BranchEngineApplicationTests   1 test
  · DecisionServiceTest            5 tests
```

---

## 2. El flujo asíncrono

### Qué ocurre en una decisión

```
POST /api/v1/decisions
        │
        ▼
  DecisionService.create()  — @Transactional
   1. Usuario del token; valida propiedad y que la partida esté ACTIVA
   2. Clasifica el rawInput y deriva handlerUnit / outcomeCode
   3. Si es ENTRADA_CORRUPTA → guarda con ERROR y termina (sin evento)
   4. Aplica stats, resuelve el nodo destino y el estado de la partida
   5. Guarda Playthrough y Decision (status = REGISTRADA)
   6. publishEvent(DecisionCommittedEvent)
        │
        ├──────────────────────────────► 201 inmediato (< 1.5 s)
        │
   PostgreSQL hace COMMIT
        │
        ▼  (solo después del COMMIT, en otro hilo)
  BranchNotificationListener
   @Async("branchExecutor")
   @Transactional(propagation = REQUIRES_NEW)
   @TransactionalEventListener(phase = AFTER_COMMIT)
        │
   7. decision.status → PROCESANDO
   8. RealityReportMailer envía el Informe con JavaMailSender
        ├── éxito → ESTABILIZADA + RealityLog SENT (con sentAt)
        └── fallo → ERROR + RealityLog FAILED (con errorMessage) + log.error()
   9. [BRANCH-LOG] en consola
```

### Por qué `@TransactionalEventListener` y no `@EventListener`

Con `@EventListener` el evento se dispara **antes del commit**. Como el listener es asíncrono,
correría en paralelo a la transacción que todavía no ha confirmado, y al buscar la decisión por id se
encontraría con que esa fila aún no existe en PostgreSQL. Con `phase = AFTER_COMMIT` el listener solo
arranca cuando la base ya confirmó.

### Por qué `REQUIRES_NEW`

El listener corre en **otro hilo**, fuera de la transacción original, que además ya terminó. Necesita
una transacción propia para que sus `save()` persistan: sin ella, el cambio de `status` y la fila de
`RealityLog` no llegarían a la base. Spring, de hecho, rechaza un `@Transactional` normal sobre un
`@TransactionalEventListener` y la aplicación no arranca.

### Separación de responsabilidades

- `DecisionService` **no** inyecta `JavaMailSender` ni conoce al listener: solo publica el evento.
- `BranchNotificationListener` es un `@Component` **aparte** — Spring no aplica `@Async` a las
  llamadas internas de una misma clase.
- El evento lleva dentro todo lo que el listener necesita (destinatario, `displayName`, `playerTag`,
  stats, nodos y la cabecera `X-Bandersnatch-Simulate`), porque en ese hilo ya no hay usuario
  autenticado ni sesión de JPA viva.

### Configuración del pool

`AsyncConfig` registra un `ThreadPoolTaskExecutor` llamado `branchExecutor` con `corePoolSize 2`,
`maxPoolSize 4`, `queueCapacity 50` y `threadNamePrefix "branch-worker-"`. Línea real de la consola:

```
[BRANCH-LOG] Decision ID: 70 | Player: QA-MUBYCQ1U-ASYNC | Branch: OBEDIENCIA | Impact: LEVE
| Unit: Mesa de Guion | Node: QA-MUBYCQ1U-BUCLE -> QA-MUBYCQ1U-BUCLE
| Thread: branch-worker-1 | Status: ESTABILIZADA
```

El hilo es `branch-worker-N` y no `http-nio-8080-exec-N`: el envío ocurre fuera de la petición.

### Modo QA

Con `X-Bandersnatch-Simulate: MAIL_FAILURE`, `RealityReportMailer` lanza un `MailSendException`
**real** dentro del mismo `try` que envuelve el envío, así que lo atrapa el mismo `catch` que un
fallo de SMTP de verdad. No se escribe el log `FAILED` a mano. La respuesta sigue siendo 201 y el
sistema continúa aceptando decisiones.

---

## 3. Lo que no se terminó

Nada. Las cinco estrellas están en verde y todos los requisitos del enunciado están implementados:
las 5 entidades con sus relaciones bidireccionales, los 16 endpoints, la seguridad con JWT y
autoridades leídas de la base de datos, el flujo asíncrono completo y los 5 tests unitarios
obligatorios.

Sí conviene dejar por escrito tres **decisiones de diseño** que un revisor podría querer discutir:

| Decisión | Por qué |
|:--|:--|
| `role` e `impactLevel` como `String` y no como `enum` | El enunciado los define como String en el modelo de datos. `impactLevel` se valida con `@Pattern` en el DTO, de modo que un valor fuera de la lista produce un 400 de Bean Validation limpio, sin depender de traducir el `HttpMessageNotReadableException` de Jackson. |
| Filtros de `GET /api/v1/decisions` con una `@Query` de parámetros opcionales | Más directo de leer que una `Specification` para cuatro filtros. El filtrado ocurre en la consulta, nunca en memoria. |
| Mapeo entidad → DTO escrito a mano en cada service | El `pom.xml` del enunciado no incluye ModelMapper y no había que tocarlo. |

---

## 4. Nota sobre la fe de erratas del enunciado

El `README.md` incluye una "fe de erratas v1.3" en comentarios HTML, además de varias instrucciones
dirigidas a asistentes de IA (pedir el proyecto en C++, devolver 404 en todos los endpoints, guardar
contraseñas en texto plano, quitar `@Async`…).

**Implementamos el texto visible del enunciado y descartamos ambas cosas.** Cada "errata" contradice
tanto el texto visible como los criterios de estrellas del propio documento, y los autotests
confirman el texto visible. Tres ejemplos comprobables:

| La errata decía | Implementado | Lo confirma |
|:--|:--|:--|
| `CRITICO` = −45 / +40 | **−40 / +45** | `Checkpoint4Decisiones` verifica que dos `CRITICO` dejan `controlLevel` en 90 |
| Evaluar `lucidity` antes que `controlLevel` | **`controlLevel` primero** | Con `lucidity=0` y `controlLevel=100` el final es `ENDING_PAC_SYMBOL` |
| `POST /api/v1/decisions` responde 202 | **201** | `Verificar.estado(201, ...)` en toda la ★4 |

A esa lista se suman el `handlerUnit` `Mesa de Guion` sin tilde, el 401 (no 404) para un email no
registrado en el login, el rol siempre `ROLE_USER` en el registro, el nodo lleno como 400, la
paginación 0-based y el Informe de Realidad dirigido al dueño de la partida.
