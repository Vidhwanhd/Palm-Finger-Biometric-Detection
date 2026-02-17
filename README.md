📱 Overview

This project is a native Android application developed in Kotlin that captures and analyzes palm and finger biometrics using a custom CameraX implementation and MediaPipe Hand Landmarker.

The application:

Detects left/right palm

Extracts palm landmark-based features (simulated minutiae)

Validates each finger against stored palm template

Detects dorsal side

Performs blur detection

Performs luminosity analysis

Stores images in structured format

Displays final biometric report

🏗 Architecture

The project follows MVVM architecture.

Layers:

UI Layer (Jetpack Compose)

Camera Layer (CameraX + Analyzer)

Detection Layer (MediaPipe HandLandmarker)

Validation Layer (Cosine Similarity Matching)

Utility Layer (Storage + Blur Detection)

🧠 AI / ML Used

MediaPipe HandLandmarker (Google ML Task API)

Landmark-based feature extraction

Cosine similarity for template validation

Blur detection via variance method

Lighting detection via luminosity analyzer

📂 Folder Structure
base/
camera/
detection/
ui/
utils/
viewmodel/

🚀 How to Build & Run
Requirements

Android Studio Hedgehog or later

Minimum SDK 24

Compile SDK 34

Kotlin 1.9+

Device with camera

Steps

Clone repository:

git clone https://github.com/YOUR_USERNAME/Palm-Finger-Biometric-Detection.git


Open project in Android Studio.

Sync Gradle.

Add the MediaPipe model file:

Place the file below in:

app/src/main/assets/


File name:

hand_landmarker.task


Run on real device (recommended).

📸 Image Storage Format

Images are stored in:

Pictures/Finger Data

Palm Format:
Left_Hand_yyyyMMdd_HHmmss.jpg
Right_Hand_yyyyMMdd_HHmmss.jpg

Finger Format:
Left_Hand_Thumb_Finger_timestamp.jpg
Left_Hand_Index_Finger_timestamp.jpg
...
Right_Hand_Little_Finger_timestamp.jpg
