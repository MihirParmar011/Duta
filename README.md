# 📱 Duta Chat Application

**Duta Chat Application** is a real-time mobile messaging app built for Android devices, providing secure and instant communication between users.  
It offers features like user authentication, friend management, real-time messaging, media sharing, and modern UI experiences — all powered by Google Firebase backend services.

---

## 📜 Table of Contents

- [About the Project](#-about-the-project)
- [Key Features](#-key-features)
- [Tech Stack and Tools](#-tech-stack-and-tools)
- [Programming Languages](#-programming-languages)
- [Platform Used](#-platform-used)
- [App Screenshots](#-app-screenshots)
- [Installation Guide](#-installation-guide)
- [Project Structure and Working](#-project-structure-and-working)
- [Future Scope](#-future-scope)
- [Contact Information](#-contact-information)
- [License](#-license)

---

## 📖 About the Project

**Duta Chat Application** is designed to provide a simple, fast, and secure chatting experience for users.  
The goal is to build a lightweight chat platform where users can connect through unique IDs, send and accept friend requests, and exchange messages and media securely.

This project follows modern mobile development practices and uses a cloud-powered backend to ensure scalability, security, and performance.

---

## 🚀 Key Features

- 🔐 **Secure User Authentication** — Users can register, log in, and reset passwords using Firebase Authentication.
- 🢑 **Friend System** — Search users by unique ID and manage friend requests (send, receive, accept).
- 💬 **Real-Time Chatting** — Text-based chatting in real-time using Firebase Realtime Database.
- 🖼️ **Media Sharing** — Share images during chats; view full-screen images with download capability.
- 🛡️ **End-to-End Encryption (Planned)** — User data and messages are kept private and secure.
- 📜 **Organized Contact List** — Chat list and friend list maintained for easy conversation management.
- 📱 **Modern UI and UX** — Smooth user experience using ConstraintLayout and Material Design concepts.
- 🔔 **Notifications (Optional Future Upgrade)** — Push notifications for new messages.

---

## 🛠️ Tech Stack and Tools

| Category | Technologies and Tools |
|:--------:|:-----------------------|
| **Mobile App Development** | Android Studio |
| **Frontend Technologies** | Java (Programming), XML (UI Layouts) |
| **Backend Services** | Google Firebase (Authentication, Realtime Database, Firestore, Storage) |
| **Libraries Used** | Glide (Image Loading), RecyclerView (Dynamic Lists), CardView (UI Components) |
| **Testing and Debugging** | Android Emulator, Physical Device Testing |
| **Version Control** | Git, GitHub |
| **Cloud Hosting** | Firebase Console |

---

## 🧑‍💻 Programming Languages

- **Java** — Used to develop the core Android application functionalities.
- **XML** — Used for designing user interfaces and layouts.
- **Firebase Security Rules** — Used to control and secure data access for Firebase services.

---

## 📱 Platform Used

| Category | Details |
|:--------:|:--------|
| **Mobile Platform** | Android OS (Minimum SDK Version: 21 - Android 5.0 Lollipop) |
| **Cloud Backend Platform** | Google Firebase |
| **Development Environment** | Android Studio IDE |

---

## 📸 App Screenshots

| Splash Screen | Login Screen | Chat Screen |
|:-------------:|:------------:|:-----------:|
| ![Splash](images/splash.png) | ![Login](images/login.png) | ![Chat](images/chat.png) |

| Profile Page | Friend Request Screen | Full-Screen Image View |
|:------------:|:----------------------:|:----------------------:|
| ![Profile](images/profile.png) | ![Friend Request](images/friend_request.png) | ![Full Image](images/full_image.png) |

---

## ⚙️ Installation Guide

Follow these steps to set up the project locally on your machine:

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/yourusername/duta-chat-application.git
   ```

2. **Open Project in Android Studio:**
   - Launch **Android Studio**.
   - Click on **Open an Existing Project** and select the cloned folder.

3. **Firebase Configuration:**
   - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   - Enable the following services:
     - Authentication (Email/Password)
     - Realtime Database
     - Firestore Database
     - Storage
   - Download the `google-services.json` file and place it in the `app/` directory.

4. **Build the Project:**
   - Click **Sync Project with Gradle Files**.
   - Resolve any missing dependencies if prompted.

5. **Run the App:**
   - Connect a physical Android device or start an emulator.
   - Click **Run** to build and launch the app.

---

## 🛠️ Project Structure and Working

### Authentication:
- Firebase Authentication handles user registration, login, and password reset securely.

### Friend Management:
- Users can search for others using their unique user ID and send friend requests.
- Requests are stored in the Firestore database.
- Accepted friends appear in the user's Friend List.

### Messaging:
- Chats between users are synced in real-time using Firebase Realtime Database.
- Messages can contain text and images.

### Media Sharing:
- Uploaded images are stored in Firebase Storage.
- Shared images are displayed using Glide and can be opened in full-screen mode with download support.

---

## 🌟 Future Scope

- 📹 **Audio and Video Calling Integration** (using WebRTC or Firebase)
- 🌙 **Dark Mode Support** for the entire application
- 📝 **Typing Indicators** to show when someone is typing
- 💬 **Message Reactions and Emojis**
- 📂 **File Sharing Support** (PDFs, Documents)
- 🔔 **Push Notifications** for real-time updates (using Firebase Cloud Messaging)
- 🛡️ **Full End-to-End Encryption** for chats (planned improvement)

---

## 📬 Contact Information

**Developer:** Mihir Parmar  
- 📧 Email: [mihir.parmar11.01@gmail.com](mailto:mihir.parmar11.01@gmail.com)  
- 🔗 LinkedIn: [Mihir Parmar](https://www.linkedin.com/in/mihirparmar1101)  
- 💻 GitHub: [mihirparmar011](https://github.com/mihirparmar011)

Feel free to reach out for suggestions, contributions, or collaborations!

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

# 📢 Important Note

To use the full functionality of the app:
- Proper Firebase setup is necessary (Authentication, Firestore, Storage rules).
- You must configure permissions in the app (Camera, Storage, Internet).

---

# ✅ Final Summary:

| Info | Details |
|:----:|:-------:|
| 🛠️ Project Type | Android Chat Application |
| 🧑 Developer | Mihir Parmar |
| 🛡️ Security | Firebase Authentication + Secure Access Rules |
| ☁️ Backend | Firebase (Realtime Database, Firestore, Storage) |
| 💻 IDE | Android Studio |
| 📝 Languages Used | Java, XML, Firebase Rules |

---
