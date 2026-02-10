# WayRecall (Legacy Stels)

**GPS мониторинг транспорта** — legacy-версия на Scala 2.11 + Java 8 + Spring Framework.

> ⚠️ Этот проект **переписывается** на Scala 3 + ZIO 2 → [wayrecall-tracker-system](https://github.com/revarewerd/wayrecall-tracker-system)

## Быстрый старт

```bash
# 1. Java 8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)

# 2. Базы данных
docker compose up -d

# 3. Переменные
export WAYRECALL_HOME=$PWD

# 4. Сборка
./mvnw clean install -DskipTests

# 5. Запуск web-сервера
java -jar monitoring/target/dist/monitoring.jar 5193
# Открыть: http://127.0.0.2:5193/billing/
# Логин: 12345 / Пароль: 12345
```

**Подробная инструкция:** [docs/LOCAL_SETUP_GUIDE.md](docs/LOCAL_SETUP_GUIDE.md)  
**Старая документация:** [docs/start.md](docs/start.md)