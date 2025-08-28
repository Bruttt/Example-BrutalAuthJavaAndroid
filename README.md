# 🔐 BrutalAuth Android Sample

This is a minimal Android sample project showing how to implement a **Login/Register flow** using a custom `BrutalAuth` client.  

The app demonstrates:
- ✅ Login and Registration with BrutalAuth API  
- ✅ Storing username & license locally with `SharedPreferences`  
- ✅ Navigating to an **Auth page** after successful login/registration  
- ✅ Continuing to a final **Home page** (success screen)  
- 🎨 Clean, modern, **programmatic UI** (no XML layouts required)  

---

## 📸 Screenshots (Example)

> 

- **Login / Register**
- **Auth Page**
- **Home Page**

---

## 📂 Project Structure

```
app/
 ├─ src/main/java/com/Bruttt/brutalauth/
 │   ├─ MainActivity.java      # Entry point
 │   ├─ Login.java             # Login/Register logic + UI
 │   ├─ AuthActivity.java      # Authentication success screen
 │   ├─ HomeActivity.java      # Final success page
 │   ├─ BrutalAuth.java        # API client (provided separately)
 │   └─ Utils.java             # dp → px helper
 │
 ├─ src/main/AndroidManifest.xml
 ├─ build.gradle
 └─ ...
```

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (latest recommended)
- Java 8+ (bundled with Android toolchain)
- Git (if cloning from GitHub)

---

### Clone Repository

```bash
git clone https://github.com/<YOUR_USERNAME>/BrutalAuthSample](https://github.com/Bruttt/Example-BrutalAuthJavaAndroid.git
cd BrutalAuthSample
```

Open the project in **Android Studio**, wait for Gradle sync, then run on device/emulator.

---

## ⚙️ Configuration

Edit these constants in `Login.java`:

```java
private static final String APPLICATION_ID = "you_BrutalAuth_appid";
```

### BrutalAuth Client (required)

You must provide an implementation of `BrutalAuth.java` with methods:

```java
public class BrutalAuth {
    public static class AuthResult {
        public boolean success;
        public String message;
    }

    public BrutalAuth(Context ctx, String applicationId, String baseUrl) { /* ... */ }

    public AuthResult loginUser(String username, String password) { /* ... */ }

    public AuthResult registerUser(String license, String username, String password) { /* ... */ }
}
```

---

## 🛡️ Manifest & Permissions

`AndroidManifest.xml` includes essential permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

All three activities are declared:

```xml
<application ...>

    <activity android:name=".HomeActivity" />
    <activity android:name=".AuthActivity" />

    <activity android:name=".MainActivity">
        <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent-filter>
    </activity>

</application>
```

⚠️ If your API uses **HTTP** instead of HTTPS, you’ll need to allow cleartext traffic:

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

---

## 🧭 App Flow

1. **MainActivity** → Builds Login/Register screen dynamically.  
2. **Login/Register** → Calls BrutalAuth API.  
3. **On success** → Navigates to **AuthActivity**.  
4. **AuthActivity** → Shows “authenticated” message, user taps continue.  
5. **HomeActivity** → Final success page. 🎉  

---

## 📝 .gitignore (Recommended)

Place this in your project root:

```
# Gradle
.gradle/
build/

# Android Studio
.idea/
*.iml

# Local configs
local.properties

# Logs
*.log

# NDK / CMake
.cxx/
```

---

## 📦 Build & Run (CLI)

You can also build/run from terminal:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤝 Contributing

Contributions are welcome!  
- Fork the repo  
- Create a feature branch (`git checkout -b feature/my-feature`)  
- Commit your changes (`git commit -m "Added my feature"`)  
- Push (`git push origin feature/my-feature`)  
- Open a Pull Request 🎉  

---

## 📄 License

This project is released under the **MIT License**.  
See [LICENSE](LICENSE) file for details.
