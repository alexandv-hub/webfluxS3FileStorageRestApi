# Описание задачи

Необходимо реализовать REST API, которое взаимодействует с файловым хранилищем AWS S3 и предоставляет возможность получать доступ к файлам и истории загрузок. Логика безопасности должна быть реализована средствами JWT токена. Приложение должно быть докеризировано и готово к развертыванию в виде Docker контейнера.

### Сущности

- **User** (List<Event> events, Status status, ...)
- **Event** (User user, File file, Status status)
- **File** (id, location, Status status, ...)
- **User** ->  ... List<Events> events ...

Взаимодействие с S3 должно быть реализовано с помощью AWS SDK.

### Уровни доступа

- **ADMIN** - полный доступ к приложению
- **MODERATOR** - права USER + чтение всех User + чтение/изменение/удаление всех Events + чтение/изменение/удаление всех Files
- **USER** - только чтение всех своих данных + загрузка файлов для себя

### Технологии

Java, MySQL, Spring (Boot, Reactive Data, WebFlux, Security), AWS SDK, Docker, JUnit, Mockito, Gradle.

# Инструкции по запуску

Для запуска приложения следуйте этим шагам:

1. Клонируйте репозиторий на ваш локальный компьютер.
2. Установите и настройте MySQL Server.
3. Установите корректные значения в `application.yaml` для подключения к базе данных и AWS S3:

```yaml
spring:
  r2dbc:
    username: <your-database-username>
    password: <your-database-password>

app:
  s3:
    bucket-name: <your-s3-bucket-name>
    key-prefix: <your-s3-key-prefix>
    aws-access-key-id: <your-s3-aws-access-key-id>
    aws-secret-access-key: <your-s3-aws-secret-access-key>
```
4. Откройте терминал и перейдите в директорию проекта.
Выполните эту команду для сборки проекта.
```bash
./gradlew clean build 
```

### Для создания и запуска docker контейнера:
```bash
docker build -t <your-image-name> .
```
```bash
docker run -p 8080:8080 <your-image-name>
```

### Для запуска на локальном docker:
Запустите приложение с помощью Docker Compose:
```bash
docker-compose up --build
```
   Приложение теперь должно быть доступно по адресу http://localhost:8080.

5. Swagger документация к REST API доступна по адресу http://localhost:8080/webjars/swagger-ui/index.html.


## Технические требования

Java версии 17 или выше.

Наличие JRE (Java Runtime Environment) на компьютере.

Разработчик
(VA)
