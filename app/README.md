# Aplikacja do śledzenia Kursów Walut

Aplikacja na system Android służąca do śledzenia kursów walut w czasie rzeczywistym oraz analizy trendów historycznych. Projekt został stworzony przy użyciu **Jetpack Compose** oraz najnowszych standardów programowania w języku Kotlin.

## Główne Funkcje

- **Ekran Główny**: Przegląd obserwowanych walut z dynamicznymi wskaźnikami trendów (wzrosty/spadki), informacją o dacie oraz czasem ostatniej synchronizacji.
- **Wyszukiwarka i Ulubione**: Wyszukiwanie walut i możliwość zarządzania listą obserwowanych walut.
- **Wykresy**: Wykresy liniowe pozwalające na analizę zmian kursu w przedziałach: dzień, tydzień oraz miesiąc.
- **Automatyzacja w tle**: **WorkManager** dbający o regularne pobieranie danych historycznych raz na dobę.
- **Pełna Konfiguracja**: Możliwość zmiany waluty bazowej, formatu wyświetlania kursu i częstotliwości odświeżania.
- **Bezpieczeństwo**: Przechowywanie klucza API w zaszyfrowanej pamięci urządzenia (**EncryptedSharedPreferences**).

## Wykorzystane Technologie

- **Język**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Sieć**: Retrofit 2 + GSON
- **Wykresy**: Vico Charts
- **Praca w tle**: WorkManager
- **Pamięć**: SharedPreferences, EncryptedSharedPreferences & Internal File Storage
- **Bezpieczeństwo**: AndroidX Security Crypto

## Konfiguracja

1. Pobierz darmowy klucz API ze strony [exchangerate-api.com](https://www.exchangerate-api.com/).
2. Uruchom aplikację i przejdź do sekcji **Ustawienia**.
3. Wklej swój klucz w polu **Klucz API**.
4. Odśwież dane przyciskiem.

## Struktura Danych

Aplikacja wykorzystuje lokalne pliki tekstowe do przechowywania informacji, co zapewnia działanie w trybie offline:
- `latest_rates.txt`: Najświeższe dane z ostatniego pobrania.
- `historical_rates.txt`: Historia kursów do generowania wykresów.
- `followed.txt`: Lista obserwowanych walut przez użytkownika.
