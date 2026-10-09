# CLAUDE.md

Bu dosya, `investulator-be` üzerinde çalışırken Claude Code'a yol gösterir. Ürün ve domain kararları için önce kökteki [`../CLAUDE.md`](../CLAUDE.md)'yi oku (işlem tipleri, kur snapshot'ı, ortalama maliyet, çok kullanıcılılık orada; oradaki [`../AGENTS.md`](../AGENTS.md), Codex için). "Şu an nerede kalındığı" için önce kök [`../WORKLOG.md`](../WORKLOG.md) (genel durum), sonra bu klasördeki [`WORKLOG.md`](WORKLOG.md) (backend'e özel ayrıntı) — oturum biterken ikisi de güncellenir.

## Proje durumu

Spring Boot iskeleti hazır. Şu an yalnızca `users` tablosu/entity'si, deny-by-default güvenlik yapılandırması ve health/OpenAPI uçları var. Auth akışı (kayıt/giriş/token), varlık kataloğu, işlemler, kur ve fiyat modülleri henüz yazılmadı — yol haritası kökteki `CLAUDE.md`'de. Oluşmamış bir konvansiyonu varsayma; aşağıdakiler dışında idiomatic Spring Boot kullan.

## Komutlar

```bash
./mvnw spring-boot:run          # uygulamayı çalıştır (yerel MySQL gerekir, aşağıya bak)
./mvnw spring-boot:test-run     # Testcontainers'ın MySQL container'ı ile çalıştır (yerel DB gerekmez)
./mvnw test                     # testleri çalıştır
./mvnw verify                   # test + Spotless kontrolü + JaCoCo %80 eşiği (CI'da bunu kullan)
./mvnw spotless:apply           # Google Java Style ile biçimlendir
```

Tek test sınıfı: `./mvnw test -Dtest=UserRepositoryTest`. JaCoCo raporu: `target/site/jacoco/index.html`.

Yerelde `mvn` de çalışır (Maven 3.9 kurulu).

## Yığın

- **Java 21** (LTS). Makinede JDK 24 kurulu; `java.version=21` ile `--release 21` derlenir. JDK 25'e geçmek için önce JDK 25 kurulmalı.
- **Spring Boot 4.1** (Spring Framework 7, Jackson 3, Hibernate 7). Boot 3 alışkanlıklarına güvenme: starter adları (`spring-boot-starter-webmvc`, `-flyway`, `-data-jpa-test` …) ve test paketleri değişti, örneğin:
  - `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
  - `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
  - `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
- **MySQL 9.4** + **Flyway** (şema yalnızca migration ile değişir) + **Spring Data JPA/Hibernate** (`ddl-auto: validate`).
- **Spring Security** (stateless), **springdoc-openapi 3.x** (`/swagger-ui.html`, `/v3/api-docs`), **Actuator** (yalnızca `health` açık).
- Test: **JUnit 5, AssertJ, Spring test, Testcontainers** (MySQL 9.4 imajı; `TestcontainersConfiguration` tek yerde tanımlı).
- Lombok **kullanılmıyor** (kasıtlı): DTO'lar `record`, entity'ler açık getter'lı.

## Yerel çalıştırma

`application.yml` ortam değişkenleri okur (varsayılanlar parantez içinde):

| Değişken | Varsayılan |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/investulator?createDatabaseIfNotExist=true&connectionTimeZone=UTC` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | _(boş)_ |
| `SERVER_PORT` | `8081` |

Port **8081**: geliştirme makinesinde 8080'i bir Keycloak container'ı tutuyor. Frontend'in Vite proxy'si (`investulator-fe/vite.config.ts`) bu porta bağlı; portu değiştirirsen ikisini birlikte güncelle.

Şifre ve gizli anahtarlar **asla** repoya yazılmaz; ortam değişkeni ya da (gitignore'lu) yerel bir profil dosyasıyla verilir. Docker Desktop kapalıysa `mvn test` Testcontainers yüzünden hata verir — bu kasıtlı, testler sessizce atlanmıyor.

## Mimari ve paketleme

`com.investulator` altında **feature bazlı** paketler (modüler monolit). Şu an `config` ve `user` var; planlananlar: `auth`, `asset`, `platform`, `transaction`, `portfolio`, `fx`, `pricing`. Bir feature paketi kendi controller / service / repository / entity / DTO'sunu içerir. Paketler arası bağımlılık servis arayüzleri üzerinden olur, başka feature'ın repository'sine doğrudan erişilmez.

- API öneki: `/api/v1/...`. Frontend her zaman aynı origin'de `/api` çağırır (dev'de Vite proxy, prod'da reverse proxy), bu yüzden CORS'a bağımlı olma.
- Controller ince kalır: doğrulama + servis çağrısı + DTO dönüşü. İş kuralı servis katmanındadır.
- Entity'ler API'den **asla** doğrudan dönmez; `record` DTO kullan.
- **Çok kullanıcılılık:** kullanıcıya ait her tablo `user_id` içerir ve **her sorgu** oturumdaki kullanıcıya göre filtrelenir. Başka kullanıcının kaydına erişim 404 döner (varlığını sızdırma).
- Constructor injection (alanlara `@Autowired` yazma, tek constructor'da anotasyon gerekmez).
- Hata yanıtları `ProblemDetail` (RFC 9457) ile, tek bir `@RestControllerAdvice` üzerinden. Henüz yazılmadı; ilk endpoint ile birlikte eklenecek.
- Dış servis çağrıları (`PriceProvider`, kur sağlayıcıları) arayüz arkasında; testlerde gerçek ağ çağrısı yapılmaz.

## Veri ve para kuralları (zorunlu)

- Para, adet, fiyat, kur: **`BigDecimal`** ve `DECIMAL` sütunlar; `double`/`float` yasak. Adet ve birim fiyat `DECIMAL(38,18)`, para tutarları için ölçek en az 8. Ara hesaplarda yuvarlama yapma, yalnızca saklarken/gösterirken yuvarla.
- API'de bu alanlar JSON **string** olarak taşınır (bkz. kök `CLAUDE.md`). Jackson 3 yapılandırması ilk kullanıldığı yerde eklenecek.
- Zaman: veritabanında `DATETIME(6)` **UTC**, Java'da `Instant` (tarih-only alanlarda `LocalDate`). Hibernate `jdbc.time_zone: UTC` ayarlı.
- E-posta küçük harfe normalize edilerek saklanır (`User` constructor'ı yapar).
- **Flyway:** uygulanmış bir migration'ı **asla düzenleme**; yeni `V<n>__aciklama.sql` ekle. Dosya adı İngilizce snake_case. Sütun/tablo adları snake_case.
- `ddl-auto: validate` — şemayı Hibernate üretmez; entity ile migration uyuşmazsa uygulama açılmaz (bu istenen davranış).

## Güvenlik

`SecurityConfig`: stateless, CSRF kapalı (token tabanlı API), `/actuator/health/**` ve OpenAPI uçları açık, geri kalan her şey `authenticated`; kimliksiz istek 401 döner. Token doğrulaması henüz bağlı değil, dolayısıyla korumalı uçların hepsi şimdilik 401.

**Karar: kimlik doğrulama Keycloak ile** (bkz. kök `CLAUDE.md`). Backend token üretmez ve parola tutmaz; `spring-boot-starter-oauth2-resource-server` ile Keycloak'ın JWT'sini doğrular (issuer/JWKS). Oturumdaki kullanıcı JWT'nin `sub` claim'inden gelir ve `users` tablosundaki kayda eşlenir. Faz 1'de `users` şeması `password_hash` yerine Keycloak `sub` (`keycloak_id`) tutacak şekilde yeniden yazılacak; henüz hiçbir gerçek veritabanına uygulanmadığı için V1 düzenlenebilir. Keycloak kurulumu ve realm/client ayarları kullanıcıyla birlikte, adım adım yapılır (kullanıcı Keycloak'ı bu yönetim düzeyinde ilk kez işletiyor).

Loglara parola, token ya da e-posta yazılmaz.

## Kod stili

- **Google Java Style**, Spotless (google-java-format) ile otomatik: yazdıktan sonra `./mvnw spotless:apply`. `verify` biçimlendirilmemiş kodu reddeder.
- Yorum: yalnızca **neden**'i açıkla (backend/spec kısıtı, workaround, incelikli bir değişmez). Kodu tekrar anlatan yorum yazma. Public API için Javadoc, gerisi tek satır `//`.
- Log: SLF4J (`private static final Logger log`). `System.out` ve `printStackTrace` yasak. Hassas veri loglama.
- Örnek/test verisinde gerçek kişi adı kullanma; `"User"`, `user@example.com` gibi genel yer tutucular.
- Null: `Optional` yalnızca dönüş tipinde; parametre/alan olarak kullanma. Koleksiyon dönüşlerinde `null` yerine boş koleksiyon.

## Test kuralları

- Yazılan her kod için test yaz. Eşik **%80 satır ve dal** (JaCoCo, `coverage.minimum` özelliği); `./mvnw verify` altında eşik altına düşerse build kırılır. `InvestulatorBeApplication` kapsam dışı.
- Birim testler (servis/domain) Mockito ile, Spring context'i olmadan. Repository ve entegrasyon testleri gerçek MySQL ile (Testcontainers), H2 **kullanılmaz** — MySQL'e özgü davranış (unique kısıt, tip eşlemesi) test edilmeli.
- Repository testi kalıbı: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(TestcontainersConfiguration.class)` (bkz. `UserRepositoryTest`). Web/güvenlik: `@SpringBootTest` + `@AutoConfigureMockMvc` (bkz. `SecurityConfigTest`). Aynı yapılandırmayı paylaşan testler context'i (ve container'ı) yeniden kullanır.
- Test adları davranışı anlatır (`rejectsDuplicateEmail`), `test1` gibi değil.
- Para/K-Z hesaplarında (ortalama maliyet, bedelsiz, satış) sınır durumları test et: sıfır adet, tam satış, eldekinden fazla satış, geriye dönük işlem, iptal edilmiş işlem (hesaplara katılmamalı; iptali pozisyonu negatife düşürüyorsa reddedilmeli).

## Git

Repo: git@github.com:cemalcanisci/investulator-be.git; çalışma branch: dev. Kullanıcının 2026-10-09 kararıyla şimdilik public; daha sonra kullanıcı private yapacak. target/ Git dışında tutulur. Commit mesajı standardı henüz seçilmedi.

## İleride (yol haritasında, şimdi yapma)

Dockerfile ve docker-compose, Jenkins pipeline'ı, SonarQube (kendi kuracağız; `sonar-maven-plugin` + JaCoCo XML raporu ile beslenecek), Jackson 3 ile BigDecimal→string serileştirme, `ProblemDetail` advice'ı.
