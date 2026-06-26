# Sklep / Hurtownia - console trading application (OOP Lab 1)

Console application for trade operations - supplier orders, deliveries (full **and** partial), sales and returns - with JSON persistence and application state **reconstructed from an append-only operation log**. Java 11, no framework. Built for the *Programowanie Obiektowe* (Object-Oriented Programming) course; this Lab 1 is the architectural base from which Lab 2 (helpdesk) later grew.

**Detailed description below in English and Polish. - Szczegółowy opis poniżej po angielsku i polsku.**

---

## English

### What it does
- **Trade operations:** place a supplier order, cancel an order, register a dispatched delivery, confirm a delivery (full or partial arrival), record a sale, record a return.
- **Order lifecycle:** `OPEN → PARTIAL → FULFILLED` (plus `CANCELLED`, only while no delivery exists). Stock changes only on confirmed delivery, sale and return, and never drops below zero.
- **Users and roles:** `ADMIN`, `MANAGER`, `EMPLOYEE` (Manager extends Employee); login, account management, account self-service; the last admin cannot be removed.
- **Persistence:** every operation is stored as a separate JSON file; the product catalog and stock levels are *rebuilt* on startup by replaying the operation history.

### Assignment goals
Demonstrate clean object-oriented design - layering, polymorphism, encapsulation and dependency inversion - on a realistic domain, together with persistence and a role-based access model, implemented as a self-contained console application without external frameworks.

### Tech stack
- **Java 11**
- **org.json 20250517** - operation (de)serialization (`JSONObject`/`JSONArray`); bundled into a fat jar.
- **Java NIO** (`Files`, `Paths`) - UTF-8 JSON file I/O.
- **java.math.BigDecimal** - exact money / VAT arithmetic.
- **javax.crypto / PBKDF2** (`PBKDF2WithHmacSHA256`, 120 000 iterations) - password hashing with a per-user salt (`SecureRandom`) and an environment-supplied pepper.

### Architecture
Layered design, package `local.pk154938.shop`:

```
config        → Main, DataSeeder, TradeBootstrapper, ExitCode
domain.user   → User (+Admin/Manager/Employee), Role, Permission
domain.trade  → ItemOperation, BaseItemOperation, Order/Delivery/Sale/Return,
                OperationLine, Product, OrderStatus, DeliveryStatus
application.*  → auth, repository (ports), service (UserService, TradeService), session
infrastructure.persistence → InMemory*Repository, JsonTradeRepository, OperationJsonCodec
ui.menu       → BaseMenu + menus, ConsoleIo, InputReader, PaginatedSelector, Formatters
util          → SecurityUtils
```

**Patterns & approaches**
- **Ports & adapters / dependency inversion** - services depend on repository interfaces; concrete implementations (in-memory for users/products/stock, JSON for operations) are wired in `Main`.
- **Polymorphic domain** - a common `ItemOperation` interface and `BaseItemOperation` base (immutable lines) with concrete `Order` / `Delivery` / `Sale` / `Return`.
- **Immutability** - `Product` and operation lines are immutable; product identity is its lower-cased name, and its price is a replaceable snapshot (one product = one current price/VAT). The catalog is the single source of price/VAT.
- **Event-sourcing-lite** - only operations are persisted; the catalog and stock are derived state, recomputed chronologically at startup (latest price snapshot wins; confirmed deliveries add, sales/returns subtract).
- **Template method** - `BaseMenu` drives the menu loop; subclasses supply `addOptions()`.

### Key mechanisms
- **Startup reconstruction (`TradeBootstrapper`)** - replays the operation log oldest-first to rebuild the catalog and stock. O(all operations) per start, acceptable at this scale.
- **Business rules (`TradeService`)** - order lifecycle; no over-dispatch; partial-delivery confirmation that rewrites the delivery to what actually arrived (the shortfall stays open against the order); stock-availability checks for sales/returns; returns must reference products from the order.
- **JSON persistence (`JsonTradeRepository`)** - one file per operation under `operations/{orders,deliveries,sales,returns}/`, named `epochMillis_uuid8.json` for chronological ordering; UTF-8; `BigDecimal` stored as strings to round-trip exactly.
- **Security (`SecurityUtils`)** - PBKDF2 + per-user salt + pepper; login recomputes the hash from the entered password and compares it with the stored one.

### Build & run
IntelliJ project (no Maven/Gradle). The `org.json` jar is in `lib/`; the artifact manifest is `META-INF/MANIFEST.MF` (`Main-Class: local.pk154938.shop.config.Main`).

1. Build the fat-jar artifact (IntelliJ: *Build → Build Artifacts*), or compile `src/main/java` against `lib/json-20250517.jar` and package a jar that bundles it.
2. Copy `scripts/start.bat.example` to `scripts/start.bat`, set your own `SHOP_APP_PEPPER` (and optionally `SHOP_DEFAULT_ADMIN_USER` / `SHOP_DEFAULT_ADMIN_PASS`), then run it - or run `java -Dfile.encoding=UTF-8 -jar <artifact>.jar` directly.
3. Operations are written as JSON files in `operations/` next to the jar.

> Users are kept **in memory** (a deliberate simplification) - they are not persisted, so a default admin is seeded on every start from `SHOP_DEFAULT_ADMIN_*`. Always set a real `SHOP_APP_PEPPER`.

### Accepted simplifications
In-memory user repository (no user persistence); no separate product CRUD (products enter via orders); no automated tests. These were within the assignment's accepted scope.

### AI tooling
Developed with the assistance of terminal-based AI tools (**Claude**, **Gemini**) - used for code generation, refactoring, code review, researching patterns and documentation. All design decisions and the final code are the author's.

---

## Polski

### Co robi
- **Operacje handlowe:** złożenie zamówienia u dostawcy, anulowanie zamówienia, rejestracja dostawy nadanej, potwierdzenie dostawy (dotarcie w całości lub częściowe), sprzedaż, zwrot.
- **Cykl życia zamówienia:** `OPEN → PARTIAL → FULFILLED` (oraz `CANCELLED`, tylko dopóki nie ma żadnej dostawy). Stan magazynu zmienia się wyłącznie przy potwierdzonej dostawie, sprzedaży i zwrocie; nigdy nie schodzi poniżej zera.
- **Użytkownicy i role:** `ADMIN`, `MANAGER`, `EMPLOYEE` (menedżer dziedziczy po pracowniku); logowanie, zarządzanie kontami, samoobsługa konta; nie można usunąć ostatniego administratora.
- **Trwałość:** każda operacja jako osobny plik JSON; katalog produktów i stany magazynowe są *odtwarzane* przy starcie z historii operacji.

### Założenia projektu
Pokazanie czystego projektu obiektowego - warstwy, polimorfizm, hermetyzacja i odwrócenie zależności - na realistycznej domenie, wraz z trwałością i modelem dostępu opartym na rolach, jako samodzielna aplikacja konsolowa bez zewnętrznych frameworków.

### Technologie
- **Java 11**
- **org.json 20250517** - (de)serializacja operacji (`JSONObject`/`JSONArray`); wbudowana w fat jar.
- **Java NIO** (`Files`, `Paths`) - odczyt/zapis plików JSON w UTF-8.
- **java.math.BigDecimal** - dokładna arytmetyka cen i VAT.
- **javax.crypto / PBKDF2** (`PBKDF2WithHmacSHA256`, 120 000 iteracji) - hashowanie haseł z solą per użytkownik (`SecureRandom`) i „pieprzem" ze zmiennej środowiskowej.

### Architektura
Architektura warstwowa, pakiet `local.pk154938.shop`:

```
config        → Main, DataSeeder, TradeBootstrapper, ExitCode
domain.user   → User (+Admin/Manager/Employee), Role, Permission
domain.trade  → ItemOperation, BaseItemOperation, Order/Delivery/Sale/Return,
                OperationLine, Product, OrderStatus, DeliveryStatus
application.*  → auth, repository (porty), service (UserService, TradeService), session
infrastructure.persistence → InMemory*Repository, JsonTradeRepository, OperationJsonCodec
ui.menu       → BaseMenu + menusy, ConsoleIo, InputReader, PaginatedSelector, Formatters
util          → SecurityUtils
```

**Wzorce i podejścia**
- **Porty i adaptery / odwrócenie zależności** - serwisy zależą od interfejsów repozytoriów; konkretne implementacje (in-memory dla użytkowników/produktów/stanów, JSON dla operacji) są wstrzykiwane w `Main`.
- **Polimorficzna domena** - wspólny interfejs `ItemOperation` i baza `BaseItemOperation` (niemutowalne pozycje) oraz konkretne `Order` / `Delivery` / `Sale` / `Return`.
- **Niemutowalność** - `Product` i pozycje operacji są niemutowalne; tożsamość produktu to jego nazwa małymi literami, a cena to wymienny snapshot (jeden produkt = jedna aktualna cena/VAT). Katalog jest jedynym źródłem ceny/VAT.
- **Event-sourcing-lite** - trwałe są tylko operacje; katalog i stany to stan pochodny, przeliczany chronologicznie przy starcie (najnowszy snapshot ceny wygrywa; potwierdzone dostawy dodają, sprzedaże/zwroty odejmują).
- **Metoda szablonowa** - `BaseMenu` prowadzi pętlę menu; podklasy dostarczają `addOptions()`.

### Kluczowe mechanizmy
- **Odtwarzanie stanu przy starcie (`TradeBootstrapper`)** - przejście historii operacji od najstarszej, by odbudować katalog i stany. O(wszystkie operacje) na start, akceptowalne przy tej skali.
- **Reguły biznesowe (`TradeService`)** - cykl życia zamówienia; zakaz nadmiernej dostawy; potwierdzenie dostawy niepełnej przepisujące dostawę do tego, co faktycznie dotarło (brakująca część pozostaje otwarta wobec zamówienia); kontrola dostępności przy sprzedaży/zwrocie; zwroty muszą dotyczyć produktów z zamówienia.
- **Trwałość JSON (`JsonTradeRepository`)** - jeden plik na operację w `operations/{orders,deliveries,sales,returns}/`, nazwa `epochMillis_uuid8.json` dla porządku chronologicznego; UTF-8; `BigDecimal` zapisywany jako napis, by odtworzyć dokładną wartość.
- **Bezpieczeństwo (`SecurityUtils`)** - PBKDF2 + sól per użytkownik + pieprz; logowanie liczy hash z podanego hasła i porównuje z zapisanym.

### Uruchomienie
Projekt IntelliJ (bez Mavena/Gradle). Biblioteka `org.json` jest w `lib/`; manifest artefaktu w `META-INF/MANIFEST.MF` (`Main-Class: local.pk154938.shop.config.Main`).

1. Zbuduj artefakt fat jar (IntelliJ: *Build → Build Artifacts*) albo skompiluj `src/main/java` z `lib/json-20250517.jar` i spakuj jar zawierający tę zależność.
2. Skopiuj `scripts/start.bat.example` jako `scripts/start.bat`, ustaw własny `SHOP_APP_PEPPER` (i opcjonalnie `SHOP_DEFAULT_ADMIN_USER` / `SHOP_DEFAULT_ADMIN_PASS`), a następnie go uruchom - albo wywołaj `java -Dfile.encoding=UTF-8 -jar <artefakt>.jar` bezpośrednio.
3. Operacje zapisują się jako pliki JSON w `operations/` obok jara.

> Użytkownicy są przechowywani **w pamięci** (świadome uproszczenie) - nie są trwali, więc przy każdym starcie zakładany jest domyślny admin z `SHOP_DEFAULT_ADMIN_*`. Produkcyjnie zawsze ustaw własny `SHOP_APP_PEPPER`.

### Świadome uproszczenia
Repozytorium użytkowników w pamięci (brak trwałości użytkowników); brak osobnego CRUD produktów (produkty wchodzą przez zamówienia); brak testów automatycznych. Mieściło się to w przyjętym zakresie zadania.

### Narzędzia AI
Projekt powstawał z asystą narzędzi terminalowych AI (**Claude**, **Gemini**) - wykorzystanych do generowania kodu, refaktoryzacji, przeglądu kodu, researchu wzorców i dokumentacji. Wszystkie decyzje projektowe oraz finalny kod są autorstwa twórcy.
