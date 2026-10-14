# КР №2 — прогресс

Работа ведётся на `main`. Сдача КР №1 зафиксирована тегом `kr1-final`.

Этап 0: разведка без кода. Пакет `carmarket`, схема 3НФ. БД КР №1 — локальный PostgreSQL `localhost:5432/auto_sale`, не Docker.

Этап 1: Java 21, JavaFX 21.0.8, эталонный pom преподавателя (groupId/artifactId/mainClass свои, добавлен POI). Консольный `ui/` перенесён в `legacy-console/` (`git mv`), Maven его не компилирует. Запуск: `carmarket.fx.CarMarketApplication` + `fxml/main-view.fxml` (заголовок и пустой BorderPane).
