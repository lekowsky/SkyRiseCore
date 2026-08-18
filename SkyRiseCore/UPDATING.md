# Aktualizacja SkyRiseCore — tylko JAR

Przy kolejnej aktualizacji **nie podmieniaj i nie usuwaj** katalogu danych
`plugins/SkyRiseCore/`. Zawiera on konfiguracje modułów, dane Automatów,
ubezpieczenia i zapisany stan włączonych modułów.

## Procedura

1. Zatrzymaj serwer.
2. Usuń stary plik `plugins/SkyRiseCore.jar`.
3. Wgraj nowy `SkyRiseCore.jar` z katalogu `release/` do `plugins/`.
4. Uruchom serwer.

To wszystko. Nie używaj `/reload` ani narzędzi typu PlugMan do podmiany JAR-a
w działającym serwerze.

## Co dzieje się z konfiguracją?

- Przy pierwszym uruchomieniu plugin tworzy domyślne pliki w
  `plugins/SkyRiseCore/<moduł>/config.yml`.
- Późniejsze aktualizacje JAR-a **nie nadpisują** istniejących konfiguracji
  ani danych.
- Jeśli nowa wersja dodaje opcję konfiguracyjną, jej wartość domyślna jest
  dostępna z nowego JAR-a także dla starszej konfiguracji. Własne ustawienia
  administratora mają zawsze pierwszeństwo.
- Stare ścieżki konfiguracji i danych Automatów/Ubezpieczenia są migrowane
  automatycznie przy pierwszym uruchomieniu nowej wersji.

## Plik do wgrania

```text
SkyRiseCore/release/SkyRiseCore.jar
```

Projekt Maven ma stałą nazwę artefaktu, więc po `mvn clean package` wynik to:

```text
target/SkyRiseCore.jar
```
