1. docker compose up --build
2. curl http://localhost:8000/health (status ok)
3. connect the phone to the computer with a USB cable and enable USB debugging
4. Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" (True)
5. & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
6. & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
7. & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --list
8. test from phone browser http://localhost:8000/health (status ok)
9. run the app