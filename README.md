# Dianjinshou Live Recap

This repository contains the Dianjinshou live recap system.

## Projects

- `dianjinshou-desktop`: Electron + Vue desktop client.
- `dianjinshou-admin`: Vue admin console.
- `dianjinshou-server`: Spring Boot backend service.
- `docs`: Project notes and running guides.

## Local Setup

Install frontend dependencies:

```bash
cd dianjinshou-desktop
npm install

cd ../dianjinshou-admin
npm install
```

Run backend tests or start the backend from `dianjinshou-server` with Maven:

```bash
cd dianjinshou-server
mvn test
```

## Configuration

Runtime secrets are intentionally not committed. Configure database, AI, SMS, storage, and deployment credentials through environment variables such as:

- `SPRING_DATASOURCE_PASSWORD`
- `YUNWU_API_KEY`
- `SMS_DAHAN_ACCOUNT`
- `SMS_DAHAN_PASSWORD`
- `STORAGE_ACCESS_KEY`
- `STORAGE_SECRET_KEY`
- `TENCENT_COS_SECRET_ID`
- `TENCENT_COS_SECRET_KEY`
- `DIANJINSHOU_DEPLOY_HOST`
- `DIANJINSHOU_DEPLOY_PASSWORD`

Local `.env*` files, logs, build output, and packaged desktop artifacts are ignored by Git.
