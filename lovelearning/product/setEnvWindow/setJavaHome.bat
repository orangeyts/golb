echo ������java��װĿ¼���磺C:\Program Files (x86)\Java\jdk1.6.0_22
:start
set /p JAVA_HOME=
if not exist "%JAVA_HOME%" (echo ������·�������ڣ��� &goto :start)
SETX JAVA_HOME "%JAVA_HOME%"
pause