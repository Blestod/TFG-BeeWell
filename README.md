=======
# BeeWell – Diabetes Management & Health Assistant

## Overview

**BeeWell** is an Android application designed to assist diabetic patients by gathering real-time data from glucose monitoring devices, electronic watches, and manual user input. The application provides predictive analytics about blood sugar levels and personalized health recommendations, enhancing patient autonomy and improving diabetes management.

## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Screenshots](#screenshots)
* [Technology Stack](#technology-stack)
* [Getting Started](#getting-started)
* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Usage](#usage)
* [Architecture](#architecture)
* [Contributing](#contributing)
* [License](#license)
* [Acknowledgments](#acknowledgments)

## Features

* **Analysis & Prediction:** Real-time prediction of blood sugar levels based on personalized data (sex, age, diabetic type, heart rate, blood pressure, temperature, etc.).
* **Chat Assistant:** AI-powered assistant offering insights, nutritional advice, and exercise recommendations.
* **Day Schedule & Advice:** Daily personalized health plan for diet and exercise routines.
* **Achievements & Progress:** User progress tracking and achievement recognition to encourage healthy lifestyle habits.

<h2>Screenshots</h2>

<p float="left">
  <img src="https://github.com/user-attachments/assets/bf56761c-e4fb-46d4-b3cf-f7a750eccf29" width="220"/>
  <img src="https://github.com/user-attachments/assets/5adc20c1-3a07-4794-96b5-53252b19d628" width="220"/>
  <img src="https://github.com/user-attachments/assets/9c520ded-8b64-43be-b4f1-d3fe0ad2b7aa" width="220"/>
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/48f66e67-6c56-4d92-8cc5-c54b22d02200" width="220"/>
  <img src="https://github.com/user-attachments/assets/a6b0b46e-5ce9-4924-9866-b576733183f8" width="220"/>
  <img src="https://github.com/user-attachments/assets/bc3a2d32-8f4b-4195-b95c-a5c856d00373" width="220"/>
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/9337a730-de3b-4fde-a3b3-fb1d75688054" width="220"/>
  <img src="https://github.com/user-attachments/assets/b6ed9fd3-6298-4068-b15f-c71e60c6fc2f" width="220"/>
  <img src="https://github.com/user-attachments/assets/67833f71-4166-4849-8b80-0df987d256f1" width="220"/>
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/cfe7fc34-2347-4ab2-9e8b-fb625533b8f3" width="220"/>
</p>


## Technology Stack

### Frontend
- **Kotlin** and **Java** (Android SDK)
- **Jetpack Compose** and **Material Components**
- **MVVM Architecture** with ViewModel, LiveData
- **Room Database** and SharedPreferences for local persistence
- **Health Connect API** (heart rate, calories, other vitals)
- **GlucoDataHandler** (Bluetooth CGM ingestion for FreeStyle Libre 3)
- **WorkManager**, **Coroutines**, **Retrofit**, **OkHttp**, **MPAndroidChart**, etc.

### Backend
- **Python 3.11**
- **Flask** web framework
- **MySQL** relational database (via MySQL Workbench, hosted on PythonAnywhere)
- **SQLAlchemy** ORM
- RESTful API with endpoints for vitals, meals, insulin, activity, predictions, and user management
- **bcrypt** for secure password hashing

### Data & Machine Learning
- **Apache Commons Math3**

### AI & Assistant
- **OpenAI GPT-3.5 API**: Monthly summaries of glucose trends
- **Sciling Core (TINA)**: Real-time advice using Retrieval-Augmented Generation (RAG) (⚠️ Not available)

### Additional Tools
- **PySpark** + **pandas**: preprocessing and ingestion of >1.5M OpenFoodFacts entries
- **Android Studio**
- **PythonAnywhere** (⚠️ Not available)


## Getting Started

### Prerequisites

* Android Studio
* Python 3

### Installation

1. **Clone the Repository**

   ```bash
   git clone https://github.com/Blestod/TFG-BeeWell.git
   ```

2. **Android Setup**

   * Open the project in Android Studio
   * Sync Gradle and install dependencies

3. **Backend Setup**

> ⚠️ The backend for this project is already set up and running using **MySQL Workbench (with Forward Engineering)** and deployed on **PythonAnywhere**.  
> The files `main.py` and `tables.py` define the API logic and database structure.  
> If you're building your own version, you’ll need to host and configure your own backend environment.

**To set up your own backend:**

1. Review how `main.py` and `tables.py` are structured.
2. Create your own **MySQL** database (e.g., using MySQL Workbench).
3. Use **Forward Engineering** to generate the schema from the model.
4. Deploy your backend (e.g., on PythonAnywhere, Railway, or your own VPS).
   
![image](https://github.com/user-attachments/assets/b480c2a6-fab1-459a-8e8d-4428aad63f16)

### Nutrition Database Processing

The app uses a filtered OpenFoodFacts database with over 1.5 million food items to estimate carbohydrate, fat, protein, and glycemic index values.

These scripts perform:
- Filtering and cleaning of raw nutritional data
- Deduplication of food entries
- Nutrient completeness checks
- Bulk upload into the MySQL database

6. **Run the Android App**
   * Connect your Android device or emulator
   * Build and run the app

## Usage

* **Login/Sign Up:** Create an account to personalize the experience.
* **Device Pairing:** Easily connect glucose monitors and wearables.
* **Manual Data Entry:** Log meals or physical activities manually (e.g., "I ate a chocolate cake slice"). The app calculates nutritional values and predicts their impact based on regional nutritional data.
* **AI Assistant:** Ask questions about health, nutrition, exercise, or diabetes management.

## Architecture

```
Android Application ↔ REST API Server ↔ Database ↔ Predictive ML Model & AI Assistant
```
![image](https://github.com/user-attachments/assets/0c91a10d-9adc-41ff-8cb7-4915052676c9)

* **Frontend:** User-friendly interface for seamless interaction and data visualization.
* **Backend:** Manages user authentication, data storage, and real-time data synchronization.
* **ML & AI:** Provides personalized predictions and actionable health insights.

## Contributing

1. Fork and clone the repository.
2. Create a new feature branch.
3. Submit pull requests to the `develop` branch.

## License

MIT License. See `LICENSE` file for more details.

## Limitations & Future Work

- The glucose prediction model is a proof-of-concept and not a certified medical tool.
- Future versions may support emergency alerts, long-term forecasting, and more vitals like SpO2 or sleep duration.
- The assistant is text-only for now; voice and multilingual support are planned.


## Acknowledgments

I would like to thank Emilio Granell Romero, academic supervisor of this
project, for his key role in the development of the glucose forecasting logic and for his
overall guidance throughout the work. The AI-based recommendation system TINA,
used within this project, was developed independently by him through his professional
collaboration with Sciling S.L., and its integration significantly enhanced the project’s
capabilities.
Gratitude is also extended to Nikolai Markozov, who contributed the BeeWell logo
and the illustrated tutorial assets, significantly enhancing the clarity and visual identity
of the application.
This project draws its core motivation from a close family member whose experience living with type 1 diabetes provided real-world context
and urgency to the system’s design goals.
Finally, special thanks are extended to the broader open-source and developer commu-
nities for the availability of software tools, frameworks, and documentation that supported
this project. In particular, this work reuses parts of the xDrip+ codebase, specifically
for the integration with GlucoDataHandler and the initial estimation of one-hour glucose
forecasts.
>>>>>>> 2f13c690c824524b370f0d485bbe410768d59b77
