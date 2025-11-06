# Agendados

Proyecto base de Android en Kotlin para la aplicación **Agendados**. Incluye una única pantalla llamada "Ficha" que muestra un número de teléfono fijo y un botón "Llamar" que solicita el permiso correspondiente e inicia la llamada nativa al número cuando el usuario lo permite.

## Requisitos

- Android Studio Hedgehog o superior.
- Android SDK 34 instalado.
- JDK 17 (Android Studio ya incluye uno compatible).

## Ejecución

1. Abrir el directorio del proyecto en Android Studio.
2. Sincronizar el proyecto cuando se solicite.
3. Conectar un dispositivo físico con Android 8.0 (API 26) o superior y habilitar la depuración USB.
4. Ejecutar la app desde Android Studio seleccionando el dispositivo conectado.

Al tocar el botón **Llamar**, la app pedirá el permiso `CALL_PHONE` si aún no se ha concedido y, una vez concedido, lanzará la llamada manteniendo visible la interfaz de la aplicación.
