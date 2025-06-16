# BeeWell: Diabetes Analysis and Prediction Application
## Description
BeeWell is an Android mobile application designed for diabetes analysis and prediction, developed as part of a Final Degree Project (TFG). The application connects to a database hosted on a remote server to store and process patient information.

## Main features
- Patient data analysis
- Diabetes risk prediction using machine learning algorithms
- Intuitive and user-friendly interface
- Data synchronization with remote server
- Secure management of confidential medical information

## System requirements
- Android 6.0 (Marshmallow) or higher
- Internet connection for data synchronization
- 100 MB of free space on the device

## Installation
1. Download the APK file from the releases section of this repository
2. Enable installation of applications from unknown sources on your Android device
3. Install the application by opening the downloaded APK file
4. Follow the on-screen instructions to complete the installation

## Server configuration
1. Make sure you have MySQL installed on your server
2. Run the `database_setup.sql` script to create the database structure
3. Configure access credentials in the `config.properties` file
4. Start the application server that will host the REST API

## Usage
1. Open the BeeWell application on your Android device
2. Register or log in with your user account
3. Follow the on-screen instructions to enter your medical data
4. Use the different analysis and prediction functions available

## Project structure
- `/android`: Contains the Android application source code
- `/server`: Includes server scripts and configurations
- `/docs`: Additional project documentation

## Technologies used
- Android Studio for mobile application development
- Java as the main programming language
- MySQL for the database
- Spring Boot for REST API development on the server

## Contribution
If you wish to contribute to the project, please follow these steps:
1. Fork the repository
2. Create a new branch for your feature: `git checkout -b new-feature`
3. Make your changes and commit: `git commit -am 'Add new feature'`
4. Push the changes to your fork: `git push origin new-feature`
5. Create a Pull Request in this repository

## License
This project is under the MIT license. See the `LICENSE` file for more details.

## Contact
For any questions or suggestions, please contact the main developer:
- Name: [Your name]
- Email: [Your email]
- GitHub: [Your GitHub profile]

## Acknowledgements
We thank [University Name] for the support provided during the development of this Final Degree Project.
