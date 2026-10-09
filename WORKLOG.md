# WORKLOG.md (investulator-be)

`investulator-be`'ye özel ayrıntılı durum kaydı. Genel/ürün seviyesi durum için önce [`../WORKLOG.md`](../WORKLOG.md)'yi oku; kural orada (ŞU AN baştan yazılır, GEÇMİŞ'e satır eklenir, asla silinmez).

---

## ŞU AN

**Son güncelleyen:** Codex — **Tarih:** 2026-10-09

### Git kurulumu (2026-10-09)

Git deposu dev branch ile oluşturuldu; origin git@github.com:cemalcanisci/investulator-be.git. Kullanıcı şimdilik public push yapılmasını onayladı; private dönüşümünü daha sonra kendisi yapacak. İlk commit ve push hazırlanıyor. Ortam dosyaları, anahtarlar ve üretilen çıktılar Git dışında. Uygulama kodu değişmedi; backend testleri bu Git kurulumunda yeniden çalıştırılmadı.


Genel inceleme kapsamında mevcut güvenlik yapılandırması, kullanıcı entity/repository'si ve uygulama ayarları okundu. **Backend çalışma kodu değiştirilmedi, backend testleri çalıştırılmadı.** Yalnızca iskelet var; Keycloak, varlık kataloğu, işlem CRUD, kur/fiyat ve portföy hesapları henüz uygulanmadı.

### Sırada

1. Kullanıcıya adım adım açıklayarak bu projeye ait Keycloak container/realm/client kurulumu.
2. Kullanıcı şemasının parola alanı yerine Keycloak 'sub' ile eşlenmesi ve JWT doğrulaması.
3. Varlık kataloğu ve işlem API'leri. Frontend'deki yeni işlem kuralları taşınmalı: tarih/saat/id sırası, işlem öncesi adet ve tüm geçmişte negatif pozisyon kontrolü, nakitsiz bedelsiz/bölünmede kur gerekmemesi, net temettü.
4. Yerel tarih/saatin UTC 'executed_at' karşılığı ve saati bilinmeyen eski kayıtların politikası belirlenmeli. Frontend prototipi saatsiz kayıtları 12:00 kabul ediyor.
5. BigDecimal hassasiyeti, ortalama maliyet, komisyon, temettü, kur snapshot'ı ve işlem türüne göre DTO doğrulaması gerçek backend'de test edilmeli.

Frontend incelemesinin ayrıntıları: '../outputs/review-20261002/INCELEME.md' ve '../investulator-fe/WORKLOG.md'.

---

## GEÇMİŞ

_Yeni satırlar en alta eklenir; var olan satırlar değiştirilmez._

- **2026-09-21 — Claude:** Spring Boot iskeleti kuruldu: `users` entity/tablosu, deny-by-default güvenlik yapılandırması, health/OpenAPI uçları, `CLAUDE.md` yazıldı.

- **2026-10-02 — Codex:** Genel incelemede backend iskeleti okundu; çalışma kodu değiştirilmedi. Frontend düzeltmelerinden backend'e taşınacak işlem sırası, hassasiyet, nakitsiz işlem ve net temettü kuralları devam notlarına eklendi.

- **2026-10-09 — Codex:** dev branch ve GitHub origin kuruldu; ilk aktarım hazırlandı. Kullanıcı public repo kullanımını şimdilik onayladı.
