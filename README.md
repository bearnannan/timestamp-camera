# Timestamp Camera Pro (Replica)

> **📘 คู่มือการใช้งาน (User Manual)**: [คลิกที่นี่เพื่ออ่านคู่มือการใช้งานอย่างละเอียด (Click here for User Manual)](USER_MANUAL.md)

แอปพลิเคชันกล้อง Android ที่มีฟีเจอร์ครบครัน สร้างด้วย **Kotlin** และ **Jetpack Compose**
แอปนี้จำลองการทำงานของแอป Timestamp Camera ยอดนิยม โดยนำเสนอการใส่ลายน้ำขั้นสูง การบันทึกวิดีโอ และการปรับแต่งที่หลากหลาย

## คุณสมบัติ (Features)

### 📸 กล้องและการถ่ายภาพ (Camera & Capture)
- **รองรับภาพนิ่งและวิดีโอ**: สลับโหมดถ่ายภาพและวิดีโอได้อย่างลื่นไหล พร้อม **Video Watermarking** (ฝังลายน้ำลงในวิดีโอโดยตรง)
- **Smart Zoom Pills (ปุ่มซูมอัจฉริยะ)**: ปุ่มลัดซูมที่ปรับตามเลนส์จริงของอุปกรณ์ (เช่น Ultrawide 0.6x) และระยะมาตรฐาน (1x, 2x, 5x) พร้อมแถบเลื่อนละเอียดเพื่อความคล่องตัวสูงสุด
- **Tap to Focus**: แตะที่หน้าจอเพื่อโฟกัสและวัดแสงเฉพาะจุด (Spot Metering) พร้อมวงแหวนแสดงสถานะ
- **ควบคุมแสง (Exposure Control)**: ปรับค่า EV ได้แบบเรียลไทม์ (-2 ถึง +2) พร้อม UI แบบมืออาชีพ
- **Volume Key Shutter (ชัตเตอร์ปุ่มเสียง)**: ใช้ปุ่มเพิ่ม/ลดเสียง เพื่อกดถ่ายภาพหรือเริ่ม/หยุดวิดีโอได้ทันที สะดวกสำหรับการใช้งานมือเดียว
- **โหมดแฟลช**: อัตโนมัติ (Auto), เปิด (On), ปิด (Off), ไฟฉาย (Torch) พร้อมเอฟเฟกต์หน้าจอขาวสำหรับแฟลชกล้องหน้า
- **สลับกล้อง**: รองรับทั้งกล้องหน้าและกล้องหลัง (พร้อมตัวเลือกกลับด้านภาพกล้องหน้า)
- **Camera Extensions (ส่วนขยายกล้อง)**:
    - **Smart Priority**: ระบบอัจฉริยะที่เลือกโหมดที่ดีที่สุดให้โดยอัตโนมัติ (เช่น ใช้ Night Mode เมื่อแสงน้อย)
    - **Manual Selection**: เลือกโหมดเองได้ตามต้องการ (Auto, HDR, Night ฯลฯ)
    - **Standard Mode**: โหมดมาตรฐาน (ปิดส่วนขยายทั้งหมด) เพื่อภาพที่ดูเป็นธรรมชาติที่สุด
- **Professional UI**: อินแทรเฟซแบบมืออาชีพด้วยแถบควบคุมสีดำสนิทบน-ล่าง เพื่อการมองเห็นที่ชัดเจนและจดจ่อกับภาพถ่าย พร้อม **360-Degree UI Rotation** หมุนไอคอนและเมนูตามทิศทางการถือเครื่อง (แนวตั้ง/แนวนอน/กลับหัว) ได้อย่างลื่นไหล
- **แกลเลอรีในตัว**: ดูรูปภาพและวิดีโอที่ถ่ายได้ทันที พร้อมฟังก์ชันลบ

### ☁️ ระบบคลาวด์และสำรองข้อมูล (Cloud & Backup)
- **Google Drive Auto-Upload**: เชื่อมต่อบัญชี Google Drive เพื่ออัปโหลดรูปภาพและวิดีโอโดยอัตโนมัติ
- **Smart Folder Organizer**: สร้างโฟลเดอร์แยกตามวันที่ถ่ายให้อัตโนมัติ เพื่อความเป็นระเบียบ
- **Robust Retry Mechanism**: ระบบสำรองข้อมูลที่เสถียร พร้อมการอัปโหลดซ้ำอัตโนมัติ (Exponential Backoff) เมื่อเครือข่ายขัดข้อง มั่นใจได้ว่ารูปภาพจะไม่สูญหาย

### 🖼️ โหมดแก้ไข (Editor Mode)
- **นำเข้าภาพ (Import Image)**: สามารถนำเข้าภาพถ่ายจากแกลเลอรีภายนอกเพื่อมาใส่ลายน้ำย้อนหลังได้
- **Render ความละเอียดสูง**: รักษาความละเอียดเดิมของภาพที่นำเข้า (ไม่ลดคุณภาพ)
- **รีเซ็ตลายน้ำ**: ลายน้ำในโหมดแก้ไขจะแสดงเวลาปัจจุบันใหม่อัตโนมัติ หรือเลือกใช้เวลาจากภาพเดิม (ตามการตั้งค่า)

### 📍 การซ้อนทับข้อมูลและลายน้ำ (Overlay & Watermarking)
- **Camera Info Overlay (ใหม่)**: แสดงข้อมูลแบบเรียลไทม์บนหน้าจอกล้อง
    - แสดง วัน/เวลา, พิกัด GPS, ที่อยู่แบบละเอียด, ความสูง, และความเร็ว
    - **การแยกที่อยู่อัจฉริยะ**: แยก ถนน, ตำบล, อำเภอ, และจังหวัด โดยอัตโนมัติ
    - **ฟิลด์ข้อมูลมืออาชีพ**: ช่องสำหรับใส่ ชื่อโครงการ (Project Name), ผู้ตรวจสอบ (Inspector Name), และ แท็ก/โน้ต (Tags/Notes)
    - **เปิด/ปิด การแสดงผล**: สามารถเลือกเปิดหรือปิดการแสดง ที่อยู่ และ พิกัด ได้ในตั้งค่า
- **Compass Overlay (เข็มทิศ) (ใหม่)**:
    - **เข็มทิศเรียลไทม์**: ใช้ Sensor Fusion (Accelerometer + Magnetometer) เพื่อความแม่นยำและนุ่มนวล
    - **ตำแหน่งที่ปรับแต่งได้**: เลือกตำแหน่งวางได้ 9 จุด (มุมบนซ้าย, ตรงกลาง, มุมล่างขวา ฯลฯ)
    - **กราฟิกสมจริง**: หน้าปัดและเข็มทิศหมุนตามทิศทางจริง พร้อมตัวเลขบอกองศาที่จัดสมดุล (Perfect Balance) ไม่อัดแน่นจนเกินไป
    - **Sync Text Size**: ขนาดตัวอักษรทิศ (N, E, S, W) ปรับขนาดให้อ่านง่ายและตรงกันทั้งในหน้าจอพรีวิวและลายน้ำที่บันทึก
- **ประทับเวลาแบบไดนามิก**: ปรับรูปแบบวันที่/เวลาได้ พร้อมตัวเลือก **ภาษา (ไทย/อังกฤษ)** และ **ปีศักราช (พ.ศ./ค.ศ.)** ในการตั้งค่า
- **ธีม (Themes)**: แบบ "Modern" (ไล่เฉดสีทอง/กระจก) และ "Minimal" (ลายเส้นเรียบง่าย)
- **ข้อมูลพิกัด**: รองรับรูปแบบ Decimal, DMS, UTM, MGRS, ความสูง, ความเร็ว
- **โลโก้ที่กำหนดเอง**: นำเข้าไฟล์ PNG โปร่งใสเพื่อใช้เป็นโลโก้แบรนด์
- **ธีมแบบไดนามิก (Dark/Light)**: สลับธีมมืด/สว่างได้ทันที พร้อมบันทึกการตั้งค่าไว้

### 🤖 **Image Enhancement ด้วย AI/ML Kit (ใหม่)**
- **Object Detection**: ตรวจจับวัตถุในภาพด้วย AI พร้อมความมั่นใจ (confidence score)
- **Portrait Segmentation**: ตรวจจับคนและสร้าง mask สำหรับ portrait mode (background blur)
- **Auto Enhancement**: ปรับปรุงภาพอัตโนมัติ (contrast, brightness, saturation)
- **Manual Adjustments**: ปรับ brightness (-100 ถึง +100), contrast, saturation แบบ manual
- **Performance Optimization**: Caching และ background processing สำหรับความเร็ว 3-4x
- **Batch Processing**: ประมวลผลหลายภาพพร้อมกันแบบกลุ่ม

### ⚙️ **Advanced Camera Controls (ใหม่)**
- **Manual Mode**: ควบคุมกล้องแบบมืออาชีพด้วย Camera2 API
- **ISO Control**: ปรับค่า ISO (100-3200) แบบ manual สำหรับการถ่ายในแสงน้อย
- **Exposure Time**: ปรับเวลาเปิดชัตเตอร์ (1/1000s ถึง 1/10s) แบบ nanoseconds
- **White Balance**: 5 โหมดสีขาว (AUTO, DAYLIGHT, CLOUDY, FLUORESCENT, INCANDESCENT)
- **Focus Mode**: 4 โหมดโฟกัส (AUTO, MANUAL, MACRO, INFINITY)
- **Hardware Detection**: ตรวจสอบความสามารถของกล้องอัตโนมัติ
- **Graceful Fallback**: ใช้ auto mode ถ้า hardware ไม่รองรับ manual controls

### 🛠 การปรับแต่งและการตั้งค่า (Customization & Settings)
- **ข้อมูลงานและเวิร์กโฟลว์ (Project & Workflow)**:
    - **จัดการข้อมูล**: ชื่อโครงการ, ผู้ตรวจงาน, หมายเหตุ, และ แท็ก
    - **ประวัติการใช้งาน**: จดจำค่าที่เคยกรอกล่าสุดเพื่อให้ทำงานได้เร็วขึ้น
    - **Custom Fields**: เพิ่มฟิลด์ข้อมูลที่กำหนดเองได้ไม่จำกัด
- **การออกแบบลายน้ำ (Watermark Design)**:
    - **เทมเพลต**: ทันสมัย (Modern Pro), มินิมอล (Minimal), ดั้งเดิม (Classic)
    - **โลโก้**: รองรับการนำเข้าโลโก้จากไฟล์ภาพ
    - **ฟอนต์**: เลือกฟอนต์ได้หลากหลาย (Roboto, Oswald, Inter ฯลฯ)
    - **สไตล์ข้อความ**: ปรับสี, ขนาด, ตัวหนา, และเงา
    - **ตำแหน่ง**: เลือกตำแหน่งวางลายน้ำได้ 9 จุด
- **เนื้อหาที่แสดง (Display Content)**:
    - **วัน/เวลา**: ปรับรูปแบบวันที่ และเลือกใช้ พ.ศ. ได้
    - **ที่อยู่และพิกัด**: เลือกแสดง/ซ่อน ที่อยู่, พิกัด GPS (DMS/UTM/MGRS), และแผนที่
    - **ข้อมูลเชิงลึก**: เข็มทิศ, ความสูง, และความเร็ว
- **การตั้งค่ากล้อง (Camera Config)**:
    - **ความละเอียด**: เลือกความละเอียดภาพและสัดส่วน (4:3, 16:9, 1:1)
    - **เครื่องมือช่วยถ่าย**: เส้นตาราง (Grid), ระดับน้ำ (Horizon Level), และตั้งเวลาถ่าย (Timer 3s/10s)
    - **โหมดประหยัดพลังงาน**: Black Screen Mode สำหรับถ่ายวิดีโอนาน (แตะหน้าจอเพื่อปลุก)
    - **ที่เก็บไฟล์**: เลือกบันทึกลงอัลบั้มหรือโฟลเดอร์เฉพาะ
    - **Background Processing**: ระบบประมวลผลพื้นหลัง ทำให้ถ่ายภาพต่อเนื่องได้ทันทีโดยไม่ต้องรอให้การฝังลายน้ำเสร็จสิ้น

## เทคโนโลยีที่ใช้ (Tech Stack)
- **ภาษา**: Kotlin
- **UI Framework**: Jetpack Compose (Material3)
- **กล้อง**: CameraX 1.4.1 (LifecycleCameraController, OverlayEffect)
- **Camera2 API**: Advanced camera controls (ISO, Exposure, White Balance, Focus)
- **AI/ML Kit**: Google ML Kit (Object Detection, Image Segmentation)
- **ระบุตำแหน่ง**: Android LocationManager (GPS) พร้อม Geocoder
- **เซ็นเซอร์**: Android SensorManager (Accelerometer + Magnetometer)
- **การจัดเก็บข้อมูล**: DataStore Preferences
- **การทำงานแบบอะซิงโครนัส**: Coroutines & Flow
- **Cloud Integration**: Google Drive API V3 (สำหรับการสำรองข้อมูล)
- **Performance Optimization**: LRU Cache, Thread Pool, Background Processing
- **Image Processing**: Custom color matrices, portrait segmentation

## การติดตั้งและบิลด์ (Setup & Build)
1. เปิดโปรเจกต์ใน **Android Studio Ladybug** (หรือใหม่กว่า)
2. Sync Gradle dependencies
3. Build และ Run บนอุปกรณ์จริง (ฟีเจอร์กล้องต้องใช้อุปกรณ์ฮาร์ดแวร์)
4. อนุญาตสิทธิ์การเข้าถึง กล้อง, ไมโครโฟน, และ ตำแหน่ง เมื่อถูกถาม
5. **การปรับเทียบเข็มทิศ**: ขยับอุปกรณ์เป็นรูปเลข 8 เพื่อความแม่นยำสูงสุด

## สถาปัตยกรรม (Architecture)
- **MVVM Pattern**:
  - `CameraViewModel`: จัดการสถานะกล้อง, การตั้งค่า, ตรรกะเซ็นเซอร์, และ Business Logic
  - `SettingsRepository`: จัดเก็บการตั้งค่าผู้ใช้ผ่าน DataStore
  - `OverlayUtils`: จัดการการวาดลายน้ำบนรูปภาพ
  - `VideoWatermarkUtils`: จัดการการฝังลายน้ำลงในวิดีโอด้วย CameraX OverlayEffect
  - `WatermarkDrawer`: โลจิกส่วนกลางสำหรับวาดลายน้ำ (ใช้ร่วมกันทั้งภาพนิ่งและวิดีโอ)
  - `CameraInfoOverlay`: Composable สำหรับแสดงข้อมูลบนหน้าจอกล้อง
  - `CompassManager`: รวมตรรกะ Sensor Fusion สำหรับเข็มทิศ
  - `DriveRepository`: จัดการการเชื่อมต่อและอัปโหลดไฟล์ไปยัง Google Drive
  - `OptimizedImageEnhancementManager`: จัดการ AI/ML Kit processing พร้อม performance optimization
  - `Camera2Manager`: จัดการ Camera2 API สำหรับ manual controls
- **UI Components**:
  - `CameraScreen`: หน้าจอหลักของแอป
  - `CameraPreview`: ตัวห่อหุ้ม `PreviewView`
  - `SettingsBottomSheet`: หน้าจอการตั้งค่าที่ครอบคลุม
  - `AdvancedCameraControls`: UI สำหรับ manual camera controls
  - `PerformanceMonitor`: UI สำหรับ monitoring และ optimization
  - `CompassOverlay`: การแสดงผลเข็มทิศด้วย Canvas

## สัญญาอนุญาต (License)
MIT License

## สำหรับนักพัฒนา (For Developers)
โปรเจกต์นี้มีกฎระเบียบและแนวทางการทำงานที่ชัดเจนสำหรับ AI Agent และนักพัฒนา เพื่อรักษาคุณภาพและความสม่ำเสมอของโค้ด

### 📜 กฎระเบียบ (Rules)
ควรอ่านและปฏิบัติตามไฟล์เหล่านี้อย่างเคร่งครัด:
- **[Project Rules](.agent/rules)**: โฟลเดอร์เก็บกฎทั้งหมด
  - [Android Library Standards](.agent/rules/android-library-standards.md): มาตรฐานไลบรารีที่ใช้ (Hilt, Retrofit, Coil ฯลฯ)
  - [Android Style Guide](.agent/rules/android-style-guide.md): แนวทางการเขียนโค้ด (Kotlin, Jetpack Compose, MVVM)
  - [Code Import Safety](.agent/rules/code-import-safety.md): ความปลอดภัยในการ Import และการใช้ AndroidX
  - [Gradle Build Stability](.agent/rules/gradle-build-stability.md): การตั้งค่า Gradle เพื่อความเสถียร

### 🚀 เวิร์กโฟลว์ (Workflows)
ขั้นตอนการทำงานมาตรฐานสำหรับการใช้งานทั่วไป:
- **[Workflows Directory](.agent/workflows)**
  - `/add-api-endpoint`: เพิ่มการเชื่อมต่อ API ใหม่
  - `/fix-build-error`: ช่วยวิเคราะห์และแก้ Error จากการ Build
  - `/generate-android-feature`: สร้างฟีเจอร์ใหม่ครบวงจร (UI + Logic)
  - `/setup-robust-gradle`: ตั้งค่า build.gradle.kts เริ่มต้น

