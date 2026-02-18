📱 Overview

This project is a native Android application developed in Kotlin that captures and analyzes palm and finger biometrics using a custom CameraX implementation integrated with MediaPipe Hand Landmarker.

The application:

Requests runtime permissions for Camera and Storage access

Detects left and right palm

Extracts palm landmark-based features (simulated minutiae)

Validates each finger against the stored palm template

Detects dorsal (back) side of palm and finger

Performs blur detection before saving images

Performs luminosity (lighting) analysis

Stores images in a structured folder format

Displays a final biometric scan report

🏗 Architecture

The project follows MVVM (Model-View-ViewModel) architecture.

Layers:

UI Layer

Built using Jetpack Compose

Handles screen navigation and user interaction

Camera Layer

CameraX Preview

ImageCapture

ImageAnalysis

Custom Luminosity Analyzer

Detection Layer

MediaPipe HandLandmarker

Landmark extraction (21 key points)

Validation Layer

Landmark-based feature embedding

Cosine similarity matching

Hand-side verification

Utility Layer

Blur detection (variance method)

Storage utilities

Palm data memory storage

🧠 AI / ML Used

MediaPipe Hand Landmarker (Google ML Task API)

Landmark-based feature extraction

Vector normalization

Cosine similarity for biometric validation

Blur detection using variance technique

Lighting classification using luminosity analyzer

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

Minimum SDK: 24

Compile SDK: 34

Kotlin 1.9+

Physical device with camera (recommended)

Steps

Clone the repository:

https://github.com/Vidhwanhd/Palm-Finger-Biometric-Detection.git


Open the project in Android Studio.

Sync Gradle.

Add the MediaPipe model file:

Place the following file in:

app/src/main/assets/


File name:

hand_landmarker.task


Connect a real Android device.

Run the application.

📸 Image Storage Format

Images are stored in:

Pictures/Finger Data

Palm Format
Left_Hand_yyyyMMdd_HHmmss.jpg
Right_Hand_yyyyMMdd_HHmmss.jpg

Finger Format
Left_Hand_Thumb_Finger_timestamp.jpg
Left_Hand_Index_Finger_timestamp.jpg
Left_Hand_Middle_Finger_timestamp.jpg
Left_Hand_Ring_Finger_timestamp.jpg
Left_Hand_Little_Finger_timestamp.jpg
Right_Hand_Thumb_Finger_timestamp.jpg
Right_Hand_Index_Finger_timestamp.jpg
Right_Hand_Middle_Finger_timestamp.jpg
Right_Hand_Ring_Finger_timestamp.jpg
Right_Hand_Little_Finger_timestamp.jpg

⚠ Challenges Faced & Solutions
1️⃣ Dorsal Side Detection for Right Hand

Issue:
Right hand was misclassified due to coordinate orientation.

Solution:
Adjusted cross-product orientation logic based on detected hand side.

2️⃣ Extra (Sixth) Finger Capture Crash

Issue:
Async capture caused index overflow leading to app crash.

Solution:
Implemented strict boundary checks and safe index handling.

3️⃣ Embedding Instability

Issue:
Raw landmark distances fluctuated across frames.

Solution:
Applied normalization before cosine similarity matching.

4️⃣ Blur & Lighting Variations

Issue:
Unstable lighting and motion caused poor quality images.

Solution:
Added blur threshold validation and luminosity-based feedback.

📊 Final Output

The Result Screen displays:

Device ID

Brightness score

Light type (Low / Normal / Bright)

Blur score

Detected hand side

Finger count

Scan success status