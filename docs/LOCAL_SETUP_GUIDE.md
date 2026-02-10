# 🚀 Гайд по локальному развертыванию Legacy WayRecall (Stels)

**Проверено:** февраль 2026, macOS (Apple Silicon M1/M2/M3)  
**Автор:** развернуто через боль, задокументировано для потомков

---

## 📋 Требования

| Компонент | Версия | Примечание |
|---|---|---|
| **Java** | JDK 8 | Обязательно 8, не новее! Рекомендуем [Azul Zulu 8](https://www.azul.com/downloads/?version=java-8-lts&os=macos&architecture=arm-64-bit) для Apple Silicon |
| **Maven** | 3.5.4+ | Есть встроенный `./mvnw` в проекте |
| **Docker** | 20+ | Для MongoDB и PostgreSQL |
| **Порты** | 5193, 27017, 5432 | Должны быть свободны |

---

## 🎯 Быстрый старт (5 шагов)

### Шаг 1: Java 8

```bash
# Проверить есть ли Java 8
/usr/libexec/java_home -V 2>&1 | grep "1.8"

# Если нет — установить Azul Zulu 8:
# https://www.azul.com/downloads/?version=java-8-lts&os=macos&architecture=arm-64-bit

# Переключиться на Java 8 (в каждой новой сессии терминала!)
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH=$JAVA_HOME/bin:$PATH
java -version
# Должно показать: openjdk version "1.8.0_xxx"
```

### Шаг 2: Базы данных (Docker)

```bash
cd <путь-к-проекту>
docker compose up -d
```

Это поднимет:
- **MongoDB 3.4** на порту `27017` (и `27016`)
- **PostgreSQL 9.6 + PostGIS** на порту `5432`

> ⚠️ **Apple Silicon:** Docker покажет warning про `linux/amd64` — это нормально, образ работает через Rosetta эмуляцию.

**Проверка работы баз:**

```bash
# MongoDB
docker exec <имя-контейнера-mongo> mongo --eval "db.version()"
# Ожидание: MongoDB shell version v3.4.24

# PostgreSQL
docker exec -e PGPASSWORD=ttt <имя-контейнера-pg> psql -h localhost -U nickl -d seniel-pg -c "SELECT version();"
# Ожидание: PostgreSQL 9.6.x
```

> 📌 **Credentials PostgreSQL** (захардкожены в docker-compose.yml):
> - Database: `seniel-pg`
> - User: `nickl`
> - Password: `ttt`

Имена контейнеров можно узнать через `docker compose ps`.

### Шаг 3: Переменные окружения

```bash
export WAYRECALL_HOME=$PWD
```

> 📌 `WAYRECALL_HOME` указывает на корень проекта, где лежит папка `conf/`.

### Шаг 4: Сборка

```bash
./mvnw clean install -DskipTests
```

**Ожидаемое время:** 1.5–3 минуты.  
**Результат:** `BUILD SUCCESS`

**Собранные артефакты:**
- `packreceiver/target/packreceiver.jar` (~77 MB) — TCP-сервер GPS
- `monitoring/target/dist/monitoring.jar` (~10 MB) — Web-сервер + REST API

### Шаг 5: Запуск

**Web-сервер (monitoring):**

```bash
java -jar monitoring/target/dist/monitoring.jar 5193
```

Или в фоне:
```bash
nohup java -jar monitoring/target/dist/monitoring.jar 5193 > logs/monitoring-server.log 2>&1 &
```

**TCP-сервер GPS (packreceiver):**

```bash
java -jar packreceiver/target/packreceiver.jar receiverserver
```

---

## 🌐 Доступ к Web-интерфейсу

> ⚠️ **ВАЖНО!** Jetty привязан к виртуальному хосту `127.0.0.2`, а НЕ к `localhost`!

| Интерфейс | URL |
|---|---|
| **Биллинг** | http://127.0.0.2:5193/billing/ |
| **Мониторинг** | http://127.0.0.2:5193/ |

**Первый вход:**
1. Открой http://127.0.0.2:5193/billing/
2. Логин: `12345` / Пароль: `12345` (из `conf/wrcinstances/default/billingAdmins.properties`)
3. Создай пользователя для мониторинга

---

## 📁 Структура проекта

```
wayrecall/
├── conf/                          # Конфигурация
│   ├── global.properties          # Подключения к БД, порты, email
│   └── wrcinstances/default/      # Настройки инстанса (админы, лицензии)
├── core/                          # Общая бизнес-логика (Scala 2.11)
├── monitoring/                    # Web UI + REST API (Jetty 9.2)
│   └── src/main/
│       ├── webapp-monitoring/     # Основной UI (ExtJS + OpenLayers)
│       ├── webapp-billing/        # Биллинг UI (ExtJS)
│       └── resources/             # SQL скрипты, Spring конфиги
├── packreceiver/                  # TCP сервер GPS пакетов (Netty 4.0)
├── modules/                       # Интеграции (ОДСМ, SMS)
├── docker-compose.yml             # MongoDB + PostgreSQL
└── pom.xml                        # Maven multi-module
```

**Технологический стек:**
- Scala 2.11.6 + Java 8
- Spring Framework 4.3.3 + Spring Security 4.1.3
- MongoDB 3.4 (основное хранилище)
- PostgreSQL 9.6 + PostGIS (геозоны)
- Jetty 9.2.29 (web-сервер)
- Netty 4.0.23 (TCP-сервер)
- ExtJS 4.2.1 (UI фреймворк)
- OpenLayers 2.13 (карты)

---

## 🔧 Конфигурация (conf/global.properties)

Основные настройки:

```properties
# MongoDB
global.defaultmongodb.databaseName = Seniel-dev2
# Хост/порт по умолчанию: localhost:27017

# PostgreSQL
global.defaultpg.url=jdbc:postgresql://localhost/seniel-pg
global.defaultpg.user=nickl
global.defaultpg.password=ttt

# Web-сервер
global.port=5193
global.monitoringUrl=http://localhost:5193
```

---

## ⚠️ Известные проблемы и решения

### 1. "ThirdPartyJS" — откуда брать?

ThirdPartyJS — это ExtJS 4.2.1 и OpenLayers, необходимые для веб-интерфейса.

**Старый источник** (`http://maven.uits-labs.ru/...`) — **МЁРТВ**, сервер недоступен.

**Решение:** Файлы ExtJS и OpenLayers уже включены в репозиторий:
- `monitoring/src/main/webapp-monitoring/extjs-4.2.1/` — ExtJS 4.2.1
- `monitoring/src/main/webapp-monitoring/openlayers/OpenLayers.js` — OpenLayers

Если файлов нет (более старая версия), см. раздел ниже.

### 2. Пути к ExtJS в billing — относительные vs абсолютные

В исходниках billing пути к ExtJS были **относительными** (`extjs-4.2.1/...`), а должны быть **абсолютными** (`/extjs-4.2.1/...`). Файлы уже исправлены:
- `webapp-billing/index.html`
- `webapp-billing/login.html`
- `webapp-billing/WRExtUtils/WRGrid.js`
- `webapp-billing/app/view/retranslator/RetranslatorPanel.js`

### 3. PostgreSQL аутентификация через Docker

Команда `psql -U nickl` без пароля может не сработать. Используйте:
```bash
docker exec -e PGPASSWORD=ttt <container> psql -h localhost -U nickl -d seniel-pg
```

### 4. Docker образ PostGIS — платформа

На Apple Silicon `kartoza/postgis:9.6-2.4` работает через Rosetta. Warning можно игнорировать. При серьёзных проблемах добавьте `platform: linux/amd64` в docker-compose.yml.

### 5. Java версия — СТРОГО 8

Проект **не компилируется** на Java 11+. Используйте только Java 8. На macOS удобно переключаться:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
```

### 6. Порт 5193 — виртуальный хост 127.0.0.2

Jetty слушает **127.0.0.2:5193**, а не localhost. Убедись что к 127.0.0.2 можно обратиться (на macOS это работает по умолчанию, loopback принимает весь 127.x.x.x).

---

## 🛑 Остановка

```bash
# Остановить web-сервер
lsof -i :5193 | grep java
kill <PID>

# Остановить Docker контейнеры
docker compose down

# Остановить и удалить данные (полный сброс)
docker compose down -v
```

---

## 📝 Примечания

- **Два процесса** в production: `packreceiver` (GPS) + `monitoring` (Web)
- **MongoDB** — основное хранилище данных (устройства, пользователи, аккаунты)
- **PostgreSQL** — хранение геозон (PostGIS для геометрических запросов)
- **Конфиги** — все в `conf/global.properties`, пароли в открытом виде (legacy)
- Этот проект **переписывается** на Scala 3 + ZIO 2 → см. основной репо `wayrecall-tracker-system`
