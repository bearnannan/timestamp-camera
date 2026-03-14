# ใช้ Android Studio JDK สำหรับ Gradle 9.1.0

## ขั้นตอน:

### 1. หา JDK path ใน Android Studio
1. เปิด Android Studio
2. ไปที่ **File → Project Structure**
3. คลิก **SDK Location**
4. ดูที่ **JDK Location** จะเห็น path เช่น:
   ```
   C:\Program Files\Android\Android Studio\jbr
   C:\Program Files\Android\Android Studio\jbr-17
   ```

### 2. สร้าง local.properties
สร้างไฟล์ `local.properties` ในโฟลเดอร์โปรเจกต์:
```properties
org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
```

### 3. ลอง build
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

## ประโยชน์:
- ไม่ต้องติดตั้ง JDK ใหม่
- Android Studio มี JDK 17+ มาให้
- ทำงานได้ทันที
