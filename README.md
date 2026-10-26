# แอปเชื่อมต่อ TrueMoney เข้าสู่ Money Manager (กระปุกหมูสีแดง) อัตโนมัติ

โปรเจกต์ Android (Kotlin) สำหรับดักจับการแจ้งเตือนภาษาไทยของแอป **TrueMoney** (`th.co.truemoney.wallet`) แล้วนำยอดเงินและชื่อร้านค้าส่งเข้าแอป **Money Manager Expense & Budget (Realbyte)**

## ฟีเจอร์หลักในแอป
1. **ดักจับการแจ้งเตือนภาษาไทยอัตโนมัติ (`TrueMoneyNotificationService`)**
   - สกัดยอดเงิน (เช่น `65.00 บาท`) และชื่อร้านค้า (เช่น `7-Eleven`) จากแจ้งเตือนการชำระเงิน/โอนเงิน
   - กรองข้อความโฆษณาหรือโปรโมชันออกอัตโนมัติ
2. **รองรับการนำเข้า Money Manager 2 รูปแบบพร้อมกัน**
   - **แบบ Real-time:** คัดลอกยอดเงินลง Clipboard + สั่งเปิดแอป Money Manager ทันที และใช้ Accessibility Service ช่วยกดปุ่ม `+` พร้อมกรอกชื่อร้านค้า
   - **แบบ Export ไฟล์ `.xls` มาตรฐาน Money Manager (`TransactionStore`)**: บันทึกทุกรายการสะสมไว้ในแอป แล้วกดปุ่ม **"ส่งออกไฟล์ .xls"** เพื่อนำไปกด Import ในเมนู `More > Backup > Import Excel File` ของแอป Money Manager ได้ทีเดียว 100%

---

## วิธีสร้างไฟล์ `.apk` โดยไม่ต้องลงโปรแกรมในคอมพิวเตอร์ (ผ่าน GitHub Actions ฟรี)
1. สมัครและเข้าสู่ระบบ [GitHub.com](https://github.com)
2. กดปุ่ม **New repository** ตั้งชื่อตามต้องการ แล้วกด **Create repository**
3. แตกไฟล์ `.zip` นี้ แล้วอัปโหลดไฟล์ทั้งหมด (รวมถึงโฟลเดอร์ `.github`) ขึ้นไปบน Repository
4. ไปที่แท็บ **Actions** ด้านบนของหน้าเว็บ GitHub
5. รอประมาณ 2-3 นาทีให้รายการ **Build Android APK** ขึ้นเครื่องหมายถูกสีเขียว ✅
6. คลิกเข้าไปในรายการนั้น เลื่อนลงมาที่หัวข้อ **Artifacts** แล้วกดดาวน์โหลด **`TrueMoney-To-MoneyManager-APK`** จะได้ไฟล์ `app-debug.apk` นำไปติดตั้งในมือถือได้เลย!
