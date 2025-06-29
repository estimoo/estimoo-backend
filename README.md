# Estimoo Backend

Estimoo, ekiplerin sprint planlama ve tahmin (planning poker) oturumlarını kolayca yönetebileceği, gerçek zamanlı bir oylama uygulamasıdır.  
Bu repo, Estimoo'nun Spring Boot tabanlı backend servislerini içerir.

---

## Kurulum

### Gereksinimler
- Java 17+
- Maven 3.8+
- Docker (development ve production için)
- PostgreSQL (production için)

### Geliştirme Ortamı Kurulumu

#### 🚀 Hızlı Başlangıç (Önerilen)
```bash
git clone https://github.com/senin-repon/estimoo-backend.git
cd estimoo-backend
./dev.sh
```

Bu komut:
- PostgreSQL'i Docker'da başlatır
- Backend'i development profile ile çalıştırır
- Health check yapar
- Tüm servisleri hazır hale getirir

#### 🔧 Manuel Kurulum
```bash
git clone https://github.com/senin-repon/estimoo-backend.git
cd estimoo-backend

# PostgreSQL ve Backend'i Docker Compose ile başlat
docker-compose up -d

# Veya sadece Maven ile (PostgreSQL ayrı kurulmalı)
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=dev
```

#### 📊 Development Ortamı Bilgileri
- **Backend API:** http://localhost:8080
- **PostgreSQL:** localhost:5432
- **Database:** estimoo_dev
- **Username:** estimoo_user
- **Password:** estimoo_password
- **Profile:** dev

#### 🛠️ Development Komutları
```bash
# Tüm servisleri başlat
./dev.sh

# Logları görüntüle
docker-compose logs -f

# Servisleri durdur
docker-compose down

# Servisleri yeniden başlat
docker-compose restart

# Sadece backend'i yeniden başlat
docker-compose restart estimoo-backend
```

### Prod Ortamı için (Docker/Podman ile)

#### Docker kullanarak:
```bash
# Environment dosyası oluştur
cp env.prod.example .env.prod
# .env.prod dosyasını düzenle (DB_HOST, DB_USERNAME, DB_PASSWORD)

# Deploy et
./deploy.sh
```

#### Manuel Docker deployment:
```bash
mvn clean package -DskipTests
docker build -t estimoo-backend .
docker run -p 8080:8080 estimoo-backend
```

#### Podman kullanarak (Docker alternatifi):
```bash
mvn clean package -DskipTests
podman build -t estimoo-backend .
podman run -p 8080:8080 estimoo-backend
```

#### Sadece JAR dosyasını çalıştırarak:
```bash
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## Veritabanı Yapılandırması

### Development Ortamı
- **PostgreSQL Docker'da çalışır**
- **Otomatik kurulum:** `./dev.sh` komutu ile
- **Veri kalıcılığı:** Docker volume (`postgres_data`)
- **Bağlantı:** `jdbc:postgresql://localhost:5432/estimoo_dev`

### Production Ortamı
- **PostgreSQL normal sunucuda çalışır**
- **Environment variables:** `.env.prod` dosyasında
- **Bağlantı:** `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`

### Environment Variables (Production)
```bash
DB_HOST=your-production-db-host
DB_PORT=5432
DB_NAME=estimoo_prod
DB_USERNAME=estimoo_user
DB_PASSWORD=your-secure-password
```

---

## API Kullanımı

### 1. Oda Oluşturma

- **REST:**  
  `POST /rooms`  
  ```json
  {
    "roomName": "Frontend Sprint"
  }
  ```
  Yanıt:
  ```json
  {
    "roomCode": "ABC123",
    "roomName": "Frontend Sprint",
    "users": {},
    "votesRevealed": false,
    "lastActivity": "2024-06-29T12:34:56"
  }
  ```

- **WebSocket:**  
  Oda oluşturma REST ile yapılır, WebSocket ile sadece katılım ve oy işlemleri yapılır.

---

### 2. Odaya Katılma

- **WebSocket:**  
  `/app/join` endpointine şu mesaj gönderilir:
  ```json
  {
    "roomCode": "ABC123",
    "nickname": "abdullah"
  }
  ```
  Katılım sonrası, tüm katılımcılara `/topic/room/ABC123` üzerinden güncel kullanıcı listesi gönderilir.

---

### 3. Oy Kullanma

- **WebSocket:**  
  `/app/vote` endpointine şu mesaj gönderilir:
  ```json
  {
    "roomCode": "ABC123",
    "vote": "5"
  }
  ```
  Oylar açılana kadar kimse kimsenin oyunu göremez, sadece katılımcı listesi ve kimin oy kullandığı bilgisi gelir.

---

### 4. Oyları Görüntüleme

- **WebSocket:**  
  `/app/reveal` endpointine:
  ```json
  {
    "roomCode": "ABC123"
  }
  ```
  Sonrasında `/topic/room/ABC123` üzerinden tüm kullanıcıların oyları açık şekilde yayınlanır.

---

### 5. Oylamayı Sıfırlama (Yeni Oyun)

- **WebSocket:**  
  `/app/reset` endpointine:
  ```json
  {
    "roomCode": "ABC123"
  }
  ```
  Tüm oylar sıfırlanır, yeni tur başlar.

---

## Frontend Geliştiriciler için Akış

1. **Site açıldığında:**  
   Modal açılır, kullanıcı oda ismini girer ve oda oluşturulur (`POST /rooms`).

2. **Oda oluşturucu:**  
   Odaya otomatik girer, ancak oy kullanabilmek için nickname girmesi gereken bir modal açılır.

3. **Odaya katılan herkes:**  
   Nickname girer, `/app/join` ile odaya katılır.

4. **Oylama:**  
   Katılımcılar oylarını `/app/vote` ile gönderir.  
   Oyunu bitirene kadar kimse kimsenin oyunu göremez.

5. **Oy tekrarına tıklama:**  
   Aynı oya tekrar tıklanırsa oy kaldırılır.  
   Farklı oya tıklanırsa oy güncellenir.

6. **Oy durumu:**  
   Henüz oy vermemişlerin yanında 🧐 emojisi gösterilebilir.  
   Oylama açılana kadar kimse oyları göremez.

7. **Oyları görüntüle:**  
   `/app/reveal` ile tüm oylar açılır.

8. **Ortalama ve istatistik:**  
   Backend sadece oyları döner, ortalama/istatistik frontendde hesaplanır.

9. **Aynı isimli kullanıcılar:**  
   Nickname aynı olsa bile her bağlantı (session) bağımsızdır.

10. **Nickname güvenliği:**  
    Aynı nickname ile farklı kişiler oy kullanabilir, ancak sessionId ile ayrılır.  
    (Daha ileri güvenlik için JWT veya kimlik doğrulama eklenebilir.)

11. **Yeni oyun:**  
    `/app/reset` ile tüm oylar sıfırlanır.

12. **Oda temizliği:**  
    Son oylama veya aktiviteden 10 dakika sonra oda ve kullanıcılar otomatik silinir.

13. **Oda silinmişse:**  
    Tarayıcı yenilendiğinde oda yoksa frontend yeni oda açmaya zorlamalıdır.  
    (Backend 404 döner.)

---

## Kodun Senaryoları Karşılayıp Karşılamadığı

1. **Modal ile oda ismi:**  
   ✔️ REST ile oda ismiyle oluşturulabiliyor.

2. **Oda oluşturucu nickname:**  
   ✔️ Odaya katılımda nickname zorunlu, modal ile alınabilir.

3. **Katılımcıların oylaması:**  
   ✔️ Herkes oy kullanabiliyor.

4. **Oylama bitene kadar oylar gizli:**  
   ✔️ Oylar reveal edilene kadar gizli.

5. **Oy tekrarına tıklama:**  
   ✔️ Aynı oya tekrar tıklanırsa oy kaldırılıyor.

6. **Farklı oya tıklama:**  
   ✔️ Farklı oya tıklanırsa oy güncelleniyor.

7. **Oy vermeyenlerin yanında 🧐:**  
   ✔️ Backend, kimin oy kullandığını döner, frontend emoji ekleyebilir.

8. **Oyları görüntüle butonu:**  
   ✔️ `/app/reveal` ile tüm oylar açılıyor.

9. **Ortalama frontendde:**  
   ✔️ Backend sadece oyları döner, istatistik frontendde.

10. **Aynı isimli farklı kişiler:**  
    ✔️ SessionId ile ayrılıyor, nickname çakışsa bile bağımsız.

11. **Nickname güvenliği:**  
    ✔️ SessionId ile ayrım var, ama nickname çakışması engellenmiyor.  
    (Daha ileri güvenlik için ek geliştirme gerekebilir.)

12. **Start new game:**  
    ✔️ `/app/reset` ile tüm oylar sıfırlanıyor.

13. **10 dakika sonra oda silinmesi:**  
    ✔️ RoomCleanupScheduler ile 10 dakika inaktif odalar siliniyor.

14. **Oda silinmişse frontend yeni oda açmalı:**  
    ✔️ Backend 404 döner, frontend yeni oda açmaya zorlayabilir.

---

## Eksik veya Geliştirilebilecek Noktalar

- **Nickname güvenliği:**  
  Şu an aynı nickname ile farklı kişiler oy kullanabilir.  
  Gerçekten tekil nickname isteniyorsa, backend'de nickname kontrolü eklenmeli.
- **Kimlik doğrulama:**  
  SessionId tabanlı ayrım var, ama daha güvenli bir yapı için JWT veya başka bir auth mekanizması eklenebilir.

---

## Katkı

Pull request ve issue açarak katkıda bulunabilirsiniz. 