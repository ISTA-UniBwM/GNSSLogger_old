<div align="left"><font color="#0000FF"><h2>User Manual Running Source Code</h2></font></div>

This package have been developed completely in java on Android Studio IDE. This section is your step by step guide running the source code on the Android Studio.
 <li>Download and Install the Android Studio </li> 
 <li>download the source code from the repository window </li>
 <li>exract the downloaded repository i.e by default GNSSLogger-Master </li>
 <li>Open Android Studio Application </li>
 <li>go to file -> Open -> location of your repository -> click on GNSSLogger-Master Android icon </li>
 <li>wait for the Gradle Sync to finish </li> 
 <li>go to Build tab on the top panel of Android studio and click Rebuild Project </li>
 <li>Connect the desired Smartphone where you want to install the Android Application </li>
 <li>Make sure your Smartphone is displayed in the Available device list in the Android Studio window </li>
 <li>Once the 'successful build message is displayed', Click on Run tab -> Run App </li>


<div align="left"><font color="#0000FF"><h2>User Manual Android Application Version v2.1.0.0</h2></font></div>
This section is your step by step guide for logging different data types available.

<h3>About the App </h3>
<ul>
    <li>Supports GNSS data from GPS/GALILEO/ BEIDOU for L1/L5/E1B/E1C/E5A (as supported by the device) </li>
    <li>Log GNSS measurements in the RINEX 3.03 format (Pseudorange, Carrier-Phase, Doppler and Noise level) </li>
    <li>Log RAW GNSS Measurements with Navigation Message and/or NMEA Message </li>
    <li>Logs Accelerometer (m/s2), Gyroscope (rad/s) and Magnetometer (μT) data </li>
    <li>Real-Time Visualization of Code Minus Carrier (CMC) plot </li>

</ul>
</a>
<br>

<h3>Logging the Sensor Data</h3>
<ul>
  <li>Go to Settings Tab </li>
  <li>Turn on the "Screen Always ON" Switch </li>
  <li>Go to Sensor Tab </li>
  <li>Turn ON the "Log Sensor Data" Switch </li>
  <li>Accelerometer, Gyroospcope and Magnetometer measurements will be logged in ASCII format</li>
  <li>Turn OFF the "Log Sensor Data" Switch to stop logging</li>
  <li>Go to your device storage root directory </li>
  <li>Open "GNSS-IMU Logger" folder</li>
  <li>Open "IMU_Log" folder inside "GNSS-IMU Logger" folder </li>
  <li>Your log file will be located inside this folder in the format devicename_IMU_YearMonthDayHourMinuteSecond </li>
  <li>Convert the Sensor out file (ASCII) to .mif format or .imr format to process with MuSNAT or Inertial Explorer respectively using MATLAB Tool </li>
</ul>
</a>
<br>

<h3>Logging the RINEX Data</h3>
<ul>
  <li>Go to Settings Tab </li>
  <li>Turn on the "Location" and "Measurement" Switch </li>
  <li>Wait for the Display Message "GNSS Measurement are Available" (displayed when more than 4 satellites are available for logging) </li>
  <li>Go to GNSS Tab and Press "RNX LOG" button to start the logging</li>
  <li>Stop & Send button: Stops logging and shows dialog to send the file via email or other options</li>
  <li>Go to your device storage root directory </li>
  <li>Open "GNSS-IMU Logger" folder</li>
  <li>Open "Rinex_Log" folder inside "GNSS-IMU Logger" folder</li>
</ul>
</a>
<br>

<h3>Logging the RAW GNSS Measurements</h3>
<ul>
  <li>Go to Settings Tab </li>
  <li>Turn on the "Location" and "Measurement" Switch </li>
  <li>Wait for the Display Message "GNSS Measurement are Available" (displayed when more than 4 satellites are available for logging) </li>
  <li>Go to GNSS Tab and press "RAW LOG" button for logging RAW GNSS Measurements</li>
  <li>Turn ON "Navigation Message" and or "NMEA" Switch to include Navigation message or NMEA message respectively inside the RAW Measurements</li>
  <li>Stop & Send button: Stops logging and shows dialog to send the file via email or other options</li>
  <li>Go to your device storage root directory </li>
  <li>Open "GNSS-IMU Logger" folder</li>
  <li>Open "Raw_Log" folder or "Rinex_Log" folder inside "GNSS-IMU Logger" folder</li>
  <li>Your log file will be located inside the "Raw_Log" folder in the format devicename_Raw_YearMonthDayHourMinuteSecond</li>
</ul>
</a>
<br>

<h3>Logging the RINEX + Sensor Data</h3>
<ul>
  <li>Go to Settings Tab </li>
  <li>Turn on the Location and Measurement Switch </li>
  <li>Turn on the "Screen Always ON" Switch </li>
  <li>Wait for the Display Message "GNSS Measurement are Available" (displayed when more than 4 satellites are available for logging) </li>
  <li>Go to Sensor Tab </li>
  <li>Turn ON the "Log RNX + Sensor Data" Switch </li>
  <li>Turn OFF the "Log RNX + Sensor Data" Switch to stop logging</li>
  <li>Go to your device storage root directory </li>
  <li>Open "GNSS-IMU Logger" folder</li>
  <li>Open "IMU_Log" folder inside "GNSS-IMU Logger" folder and your IMU log file is located there </li>
  <li>Open "Rinex_Log" folder inside "GNSS-IMU Logger" folder and your Rinex log file is located there </li>
  <li>Convert the Sensor output file (ASCII) to .mif format or .imr format using MATLAB Tool to process with MuSNAT or Inertial Explorer respectively</li>
</ul>
</a>
<br>
<h3>Code Minus Carrier Plot</h3>
<ul>
  <li>Go to Settings Tab </li>
  <li>Turn on the Location and Measurement Switch </li>
  <li>Turn on the "Screen Always ON" Switch </li>
  <li>Wait for the Display Message "GNSS Measurement are Available" (displayed when more than 4 satellites are available for logging) </li>
  <li>Go to GNSS Tab and Press "RNX LOG" button to start the logging</li>
  <li>Go to the CMC Plot tab for visualization </li>
  <li>"Code Minus Carrier (m)" plot shows a "line" for each satellite without any cycle slip on the current epoch. If a cycle slip occur, it will be indicated by a "sphere" and will reset. </li>
  <li>"Code Minus Carrier Normalized (m)" plot shows a "line" for each satellite without any cycle slip on the current epoch. the cycle slip is indicated by a a gap in the line. All the CMC values are normalized with first CMC values (no-cycle slip) for each individual satellite.</li>
</ul>
</a>
<br>
<div align="left"><font color="#F73030">Note : For continuous logging of GNSS data with the Smartphone, we encourage user to have "Force full GNSS measurements" ON from developer options during logging.</font></div>

<h3>Clock Error Plot </h3>
<ul>
  <li>Go to Settings Tab </li>
  <li>Turn on the Location and Measurement Switch </li>
  <li>Turn on the "Screen Always ON" Switch </li>
  <li>Wait for the Display Message "GNSS Measurement are Available" (displayed when more than 4 satellites are available for logging) </li>
  <li>Turn ON the "Log RNX + Sensor Data" Switch </li>
  <li>Go to the CLOCKOFFSET Plot tab for visualization </li>
  <li>"Clock offset" plot shows a an offset between between the GPSTime (from GnssClock Class) used for logging RINEX data and ElapsedRealTime (from SystemClock) used for logging Sensor data. </li>
  <li> The Moving Average offset indicate the normalized value (with first epoch) using dedicated averaging window.</li>
</ul>
</a>
<br>


