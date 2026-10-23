# App17_Proximidad

Aplicación Android en Java para experimentar con el sensor de proximidad.

## Funciones
- Muestra el estado lógico **1 (cerca)** o **0 (lejos)**.
- Anuncia el estado en español cada dos segundos mientras la pantalla está abierta.
- Vibra al detectar cercanía; la intensidad se ajusta según la distancia reportada por el sensor.
- Muestra el valor bruto del sensor, fabricante y rango máximo.

## Compilar APK
En GitHub, abre **Actions** y revisa el flujo **Compilar App17 Proximidad**. Cuando termine correctamente, descarga el artefacto `App17_Proximidad-debug`.

**Nota:** algunos sensores solo reportan dos estados (cerca/lejos); en esos teléfonos la intensidad no podrá variar gradualmente por distancia.
