@echo off
echo สร้าง local.properties สำหรับ JDK 17...
echo.
echo กรุณาแก้ไข path ของ JDK ให้ถูกต้องก่อนรัน!
echo.

REM สร้างไฟล์ local.properties ด้วย Android Studio JDK path
echo org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr > local.properties

echo ไฟล์ local.properties ถูกสร้างแล้ว!
echo Path: org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
echo.
echo ถ้า path ไม่ถูกต้อง กรุณาแก้ไขไฟล์ local.properties ด้วยตนเอง
echo หรือรันสคริปต์นี้ใหม่หลังจากติดตั้ง JDK 17
pause
