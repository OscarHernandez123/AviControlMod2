# Frontend de AviControl Módulo 2

Esta carpeta contiene la aplicación React/Vite del mismo repositorio que el backend Spring Boot.

## Desarrollo local

Desde la raíz del repositorio, inicia PostgreSQL y el backend:

```powershell
docker compose up -d
.\gradlew.bat bootRun
```

En otra terminal, inicia el frontend:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Abre `http://localhost:5173`. Vite envía las solicitudes `/api` al backend en `http://localhost:8080`.

La carpeta `node_modules`, el resultado `dist` y los archivos `.env` no forman parte de Git.
