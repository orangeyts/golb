echo ������ɱ���Ķ˿�:
set /p portNumber=
echo "�˿ں� %portNumber%"
netstat -ano | find "%portNumber%"
echo ��������̺�:
set /p processId=
TASKKILL /PID %processId% /F
pause