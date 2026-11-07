# LastStandinNote

Videojuego de ritmo inspirado en *Friday Night Funkin’*, con estética pixel art y un enfrentamiento musical entre **Tux** y **Windows**. Sigue las flechas, acierta al ritmo de la canción y mantén las notas sostenidas mientras Kernel reacciona a lo que sucede en el escenario.

El proyecto está en desarrollo y se encuentra en preparación para su primera versión pública.

## Cómo se juega

Las notas descienden hacia los receptores de la parte inferior de la pantalla. Presiona la tecla de su columna cuando la cabeza de la nota coincida con el receptor.

- **Notas normales:** pulsa la tecla al llegar al receptor.
- **Notas sostenidas:** pulsa al inicio y mantén la tecla hasta que termine la cola.
- **Varias columnas:** puedes mantener una nota y tocar otras columnas al mismo tiempo.

Cada acierto recibe una calificación según la distancia entre la pulsación y el momento de la nota. Los fallos afectan a la vida, interrumpen el combo y tienen una respuesta visual y sonora. Las voces acompañan los aciertos y se silencian al fallar; el instrumental mantiene el ritmo de la canción.

Las notas y las voces siguen el tiempo del audio. Cambiar el límite de FPS modifica la frecuencia de dibujo, no la velocidad de la canción.

## Recorrido por los menús

1. Pulsa **Enter** en la pantalla inicial.
2. Elige **Jugar / Play** y selecciona el modo de juego.
3. Selecciona una canción con **arriba y abajo**.
4. Cambia la dificultad con **izquierda y derecha**, desde la misma pantalla de canciones.
5. Elige tu personaje y confirma con **Enter**.
6. Prepárate durante la cuenta atrás y juega la canción.

En el menú principal también encontrarás **Opciones / Options** y **Créditos / Credits**. Pulsar **Escape** en la pantalla inicial abre la confirmación para salir del juego.

## Personajes

| Personaje | Posición en el escenario | Papel |
| --- | --- | --- |
| Windows | Izquierda | Personaje jugable o rival |
| Tux | Derecha | Personaje jugable o rival |
| Kernel | Centro, sobre el monitor | Reacciona a los combos y a su pérdida |

Elegir otro personaje cambia quién controla cada participante, pero Tux conserva el lado derecho y Windows el izquierdo. El monitor central muestra quién lleva la ventaja en la barra de vida; cuando están igualados muestra el enfrentamiento.

## Modos de juego

### Un jugador

Controlas al personaje que elegiste y el bot juega con el otro. Las teclas son siempre **D F J K**, tanto si eliges Tux como Windows.

El bot tiene variaciones de precisión y puede cometer errores. Cuando juegas bien y mantienes un combo, puede aumentar su presión para que la partida siga siendo un reto.

Si tu vida se agota, aparece la animación de derrota y después la pantalla de fin de partida. Puedes reiniciar o volver a elegir canción. Si completas la canción, los resultados muestran únicamente tus estadísticas y tu personaje, sin declarar un ganador o perdedor.

### Dos jugadores en el mismo computador

Los dos participantes comparten el teclado. Las teclas están ligadas al personaje:

| Personaje | Izquierda | Abajo | Arriba | Derecha |
| --- | --- | --- | --- | --- |
| Windows | A | S | D | F |
| Tux | J | K | L | Ñ |

La partida continúa hasta el final de la canción, incluso si un lado de la barra llega a cero. Los resultados comparan a los dos jugadores. La precisión se calcula respecto a las notas de cada lado, para que tener más notas no otorgue por sí solo la victoria. Si la precisión empata, se comparan los puntos por nota.

Para este modo se necesita un teclado o una distribución de teclado que permita utilizar **Ñ**.

## Canciones y dificultades

Las canciones disponibles son **Puzzle**, **Buy something** y **Melancholy**. Cada una tiene tres mapas de notas:

| Dificultad | Experiencia |
| --- | --- |
| USER | Nivel de entrada, con desplazamiento más lento |
| ADMIN | Reto intermedio, con su propio mapa de notas |
| ROOT | Desplazamiento más rápido y efectos que dificultan la lectura |

En **ROOT**, las notas pueden oscilar lateralmente, aparecer con menor opacidad y acompañarse de sacudidas del escenario. Al acercarse al receptor recuperan su posición y visibilidad para permitir leer el momento de pulsación.

La dificultad se cambia directamente con izquierda y derecha en la selección de canciones. El indicador de segmentos muestra el nivel elegido.

## Controles generales

| Acción | Control |
| --- | --- |
| Moverse por las opciones o canciones | Arriba / abajo |
| Cambiar dificultad, personaje o valor de una opción | Izquierda / derecha |
| Confirmar una selección | Enter |
| Volver al menú anterior | Escape |
| Pausar o continuar una canción | P |
| Alternar ventana y pantalla completa | F11 |
| Abrir la confirmación para abandonar una canción en curso | Escape durante el juego |
| Cancelar una confirmación de salida | Escape |

En un jugador, **D**, **F**, **J** y **K** corresponden, respectivamente, a izquierda, abajo, arriba y derecha.

## Calificaciones, precisión y puntuación

La **puntuación** y la **precisión** expresan cosas distintas. La puntuación depende de lo cerca que pulses del momento exacto. La precisión utiliza el peso de la calificación de cada nota:

| Calificación | Diferencia respecto al momento de la nota | Valor para la precisión |
| --- | --- | --- |
| SICK | Hasta 45 ms antes o después | 100 % |
| GOOD | Más de 45 y hasta 90 ms | 100 % |
| BAD | Más de 90 y hasta 135 ms | 50 % |
| SHIT | Más de 135 y hasta 160 ms | 25 % |
| MISS | Nota fallada o sostenida abandonada antes de tiempo | 0 % |

**GOOD conserva la precisión completa**, aunque concede menos puntos que un acierto más cercano al momento exacto. Una nota sostenida se cuenta una sola vez al completarse; soltarla antes de tiempo puede convertirla en fallo.

El combo registra aciertos consecutivos. **BAD** y **MISS** rompen el combo. Los avisos de combo dependen de la cantidad de notas del lado que juegas y se acompañan de una animación y un sonido cuando los consigue una persona.

## Vida y resultados

La barra inferior es compartida: el lado izquierdo pertenece a Windows y el derecho a Tux. Los aciertos y errores desplazan el equilibrio entre ambos personajes. En dos jugadores, la influencia de las notas se ajusta para compensar las diferencias de cantidad entre los dos mapas.

Al terminar una canción, los resultados realizan un conteo rápido y muestran:

- Notas totales y notas acertadas.
- Cantidad de SICK, GOOD, BAD, SHIT y MISS.
- Combo máximo, puntuación y precisión final.
- Opciones para reiniciar o volver a la selección de canciones.

En un jugador se muestran los datos del personaje elegido. En dos jugadores aparecen las dos columnas y el resultado del enfrentamiento.

## Pausa y salida

Con **P** puedes pausar, continuar, reiniciar o volver a la selección de canciones. La música y las notas se detienen juntas. Continuar conserva la posición de la canción y no repite la cuenta atrás.

Si la partida se pausó durante una nota sostenida, el juego puede pedir que mantengas las teclas correspondientes antes de continuar. Cambiar a otra ventana también pausa la partida.

Abandonar una canción en curso o cerrar el juego abre una confirmación para evitar perder el progreso por una pulsación accidental.

## Opciones

| Opción | Qué permite cambiar |
| --- | --- |
| Resolución | 960 × 540, 1280 × 720 o 1920 × 1080 |
| Volumen de música | Música de los menús e instrumental |
| Volumen de voces | Voces de los personajes |
| Volumen de efectos | Sonidos de interfaz, fallos y avisos |
| Modo de ventana | Ventana o pantalla completa |
| Idioma | Español o English |
| Límite de FPS | 30, 60, 120, 244 o un valor personalizado |
| Mostrar FPS | Indicador de fotogramas por segundo |
| Mostrar estadísticas | Puntos, precisión, combo y fallos durante la canción |

La barra de FPS personalizados recorre valores de 0 a 260 y termina en **Sin límite / Unlimited**. Su extremo inferior utiliza un mínimo efectivo de 1 FPS para mantener la pantalla actualizándose.

Los cambios realizados en Opciones quedan pendientes hasta elegir **Confirmar y volver / Confirm and back**. Si intentas salir con cambios pendientes, puedes guardarlos, descartarlos o seguir editando. Lo confirmado se conserva al volver a abrir el juego.

El idioma modifica los menús, avisos y resultados. Los nombres de canciones, personajes, autores y calificaciones conservan su nombre original.

## Organización del proyecto

El juego está desarrollado en Java con Swing y utiliza archivos de recursos para sus imágenes, sonidos y mapas de notas.

| Carpeta | Contenido |
| --- | --- |
| `src/` | Pantallas, reglas del juego, audio y configuración |
| `resources/images/` | Personajes, escenarios, flechas, interfaz y fuente bitmap |
| `resources/audio/` | Música de menús y efectos de sonido |
| `resources/songs/` | Canciones, mapas por dificultad y voces |
| `resources/CREDITS.txt` | Créditos, fuentes y atribuciones de los recursos |

## Equipo y créditos

| Integrante | Aportes |
| --- | --- |
| Jacobo Parada Urriago | Código y música |
| Manuela Gutierrez Diaz | Sprites, letras y código |

Los autores y fuentes de los recursos externos aparecen en los créditos del juego y en [CREDITS.txt](resources/CREDITS.txt). Ese archivo también identifica las atribuciones que todavía necesitan confirmación antes de la publicación.

## Descargas y reportes

Las versiones publicadas estarán disponibles en [Releases de GitHub](https://github.com/MajaProjects/LastStandinNote/releases).

Si encuentras un error, puedes reportarlo en [Issues](https://github.com/MajaProjects/LastStandinNote/issues). Incluye la canción, dificultad, modo, personaje y sistema operativo, junto con los pasos para reproducirlo. Una captura o el mensaje de error ayuda a identificar el problema.

Repositorio principal: [MajaProjects/LastStandinNote](https://github.com/MajaProjects/LastStandinNote).

Espejo del proyecto: [Last Standin Note en GitLab](https://gitlab.com/GLUD/gnubies/proyectos-2023-iii/2026-iii/gnubies-martes-8-10am/last-standin-note).
